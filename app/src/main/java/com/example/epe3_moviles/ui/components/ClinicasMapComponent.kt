package com.example.epe3_moviles.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.epe3_moviles.location.ClinicaModel

/**
 * Mapa interactivo real de la Red Asistencial de Salud impulsado por OpenStreetMap y Leaflet.
 *
 * Características Técnicas:
 * 1. Cartografía real con calles, avenidas, hospitales y relieve geográfico auténtico de Coquimbo,
 *    La Serena, Santiago o cualquier comuna de Chile.
 * 2. Soporte para capa de calles y capa Satelital en alta resolución (Esri World Imagery).
 * 3. Marcador del usuario con pulso GPS animado y radio de precisión métrica.
 * 4. Pines médicos de centros asistenciales interactivos con cruz de salud, tooltip y tarjeta de acción.
 * 5. Cero dependencias externas frágiles: motor Leaflet empaquetado en assets locales (0ms de arranque
 *    y funcionalidad offline).
 * 6. Sin elementos invasivos ni insignias ficticias que obstruyan la visualización.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ClinicasMapComponent(
    userLat: Double,
    userLon: Double,
    clinicasConDistancia: List<Pair<ClinicaModel, String>>,
    esReferencial: Boolean,
    precisionMetros: Float = 15f,
    modifier: Modifier = Modifier,
    onClinicSelected: ((ClinicaModel) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedClinica by remember { mutableStateOf(clinicasConDistancia.firstOrNull()?.first) }

    // Mantener sincronizada la clínica seleccionada si cambia la lista
    LaunchedEffect(clinicasConDistancia) {
        if (selectedClinica == null || clinicasConDistancia.none { it.first.id == selectedClinica?.id }) {
            selectedClinica = clinicasConDistancia.firstOrNull()?.first
        }
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    // Generar JSON seguro de las clínicas para Leaflet
    val clinicsJson = remember(clinicasConDistancia) {
        clinicasConDistancia.joinToString(prefix = "[", postfix = "]") { (c, dist) ->
            val safeName = c.nombre.replace("\"", "\\\"")
            val safeDir = c.direccion.replace("\"", "\\\"")
            val safeEsp = c.especialidades.replace("\"", "\\\"")
            """{"id":${c.id},"nombre":"$safeName","direccion":"$safeDir","especialidades":"$safeEsp","abierta":${c.abierta},"latitud":${c.latitud},"longitud":${c.longitud},"distancia":"$dist"}"""
        }
    }

    // Actualizar ubicación en tiempo real en el mapa sin recargar la página (60fps glide)
    LaunchedEffect(userLat, userLon, precisionMetros, isMapLoaded) {
        if (isMapLoaded) {
            val script = "if (typeof updateUser === 'function') { updateUser($userLat, $userLon, $precisionMetros); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // Actualizar clínicas si cambian dinámicamente
    LaunchedEffect(clinicsJson, isMapLoaded) {
        if (isMapLoaded) {
            val script = "if (typeof setClinics === 'function') { setClinics('$clinicsJson', false); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
    ) {
        // WebView interactivo con Leaflet
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    // Puente Javascript -> Android
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onClinicSelected(clinicId: Int) {
                            post {
                                val found = clinicasConDistancia.find { it.first.id == clinicId }?.first
                                if (found != null) {
                                    selectedClinica = found
                                    onClinicSelected?.invoke(found)
                                }
                            }
                        }
                    }, "AndroidBridge")

                    // Evitar que el LazyColumn padre intercepte gestos táctiles de paneo/zoom
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                            val initScript = """
                                initMap($userLat, $userLon);
                                setClinics('$clinicsJson', true);
                            """.trimIndent()
                            view?.evaluateJavascript(initScript, null)
                        }
                    }

                    loadUrl("file:///android_asset/leaflet/map.html")
                    webViewRef = this
                }
            },
            update = {
                // El estado se sincroniza vía LaunchedEffects
            }
        )

        // Contador discreto de centros disponibles en la esquina superior izquierda
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            color = Color(0xDD0F172A),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Text(
                text = "${clinicasConDistancia.size} centros asistenciales",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Tarjeta flotante interactiva con la clínica seleccionada (Navegar / Llamar)
        selectedClinica?.let { clinica ->
            val dist = clinicasConDistancia.find { it.first.id == clinica.id }?.second ?: "En rango"

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF21E293B), // Tailwind Slate 800
                border = BorderStroke(1.dp, Color(0xFF475569).copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = clinica.nombre,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = clinica.direccion,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Badge de distancia
                        Surface(
                            color = Color(0xFF0284C7).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = dist,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botón Navegar en Google Maps / Waze
                        Button(
                            onClick = { openExternalNavigation(context, clinica) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Directions,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navegar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Botón Llamar a Clínica
                        OutlinedButton(
                            onClick = { callClinic(context, clinica.telefono) },
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF64748B)),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Llamar a ${clinica.nombre}",
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun openExternalNavigation(context: Context, clinica: ClinicaModel) {
    try {
        val gmmIntentUri = Uri.parse("geo:${clinica.latitud},${clinica.longitud}?q=${Uri.encode(clinica.nombre)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${clinica.latitud},${clinica.longitud}")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        context.startActivity(webIntent)
    }
}

private fun callClinic(context: Context, phone: String) {
    try {
        val cleanPhone = phone.replace(" ", "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
        context.startActivity(intent)
    } catch (_: Exception) {}
}
