package com.example.epe3_moviles.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.example.epe3_moviles.BuildConfig
import com.example.epe3_moviles.config.NetworkConfig
import com.example.epe3_moviles.ui.theme.EPE3_MovilesTheme

/**
 * Modelo representativo de una consulta médica ficticia para la prueba de red.
 */
data class ConsultaFicticia(
    val id: Int,
    val medico: String,
    val especialidad: String,
    val fecha: String,
    val diagnostico: String
) {
    /**
     * URL de la imagen del médico obtenida desde el archivo central de configuración [NetworkConfig].
     */
    val fotoUrl: String
        get() = NetworkConfig.getFotoMedicoUrl(id)
}

/**
 * Lista de 8 consultas médicas y 8 médicos ficticios clearly identificados.
 * Coincide exactamente con el escenario de prueba del informe (8 fotos de perfil médico).
 */
val consultasFicticiasPaso3 = listOf(
    ConsultaFicticia(1, "Dr. Andrés Morales", "Cardiología", "12 ago 2026", "Control post-cirugía: evolución favorable"),
    ConsultaFicticia(2, "Dra. Isabel Fuentes", "Medicina General", "28 jul 2026", "Gripe estacional, prescripción de paracetamol"),
    ConsultaFicticia(3, "Dr. Carlos Leiva", "Traumatología", "15 jun 2026", "Seguimiento fractura de muñeca derecha"),
    ConsultaFicticia(4, "Dra. Valentina Ríos", "Dermatología", "02 jun 2026", "Dermatitis atópica, crema con corticoides"),
    ConsultaFicticia(5, "Dr. Sebastián Torres", "Gastroenterología", "18 may 2026", "Gastritis leve, indicaciones alimentarias"),
    ConsultaFicticia(6, "Dra. Camila Espinoza", "Oftalmología", "30 abr 2026", "Revisión anual, sin novedades"),
    ConsultaFicticia(7, "Dr. Felipe Navarro", "Neurología", "10 abr 2026", "Cefalea tensional recurrente"),
    ConsultaFicticia(8, "Dra. Patricia Vega", "Endocrinología", "22 mar 2026", "Control tiroideo, TSH dentro del rango")
)

/**
 * Pantalla Historial Clínico adaptada para medición real con Network Inspector en Android Studio.
 *
 * BASELINE:
 * - Descarga 8 imágenes originales de alta resolución (~2.4 MB c/u).
 * - Caché de disco y memoria DESACTIVADAS para permitir repeticiones idénticas en la medición.
 * - Comportamiento identificado explícitamente como didáctico en el banner superior.
 *
 * OPTIMIZED:
 * - Descarga 8 imágenes WebP de 480 px (~100-180 KB c/u).
 * - Carga diferida (lazy loading) al aparecer en pantalla con LazyColumn.
 * - Caché de disco y memoria ACTIVADAS para no re-descargar datos ya consultados.
 *
 * Accesibilidad (WCAG):
 * - LiveRegion y contentDescription en estados de carga ("Cargando fotografía...").
 * - Estado de error visual y anunciado por TalkBack si el servidor no responde.
 * - Objetivos táctiles mínimos de 48 dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialClinicoScreen(onBack: () -> Unit) {
    val isBaseline = BuildConfig.FLAVOR == "baseline"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial Clínico") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Volver a Mi Consulta"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Banner técnico de la variante para la prueba de red
            NetworkFlavorBanner(isBaseline = isBaseline)

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(consultasFicticiasPaso3) { index, consulta ->
                    ConsultaCard(
                        consulta = consulta,
                        numero = index + 1,
                        isBaseline = isBaseline
                    )
                }
            }
        }
    }
}

/**
 * Banner que explica el modo de red activo para facilitar la inspección.
 */
@Composable
private fun NetworkFlavorBanner(isBaseline: Boolean) {
    val backgroundColor = if (isBaseline) Color(0xFFFFF3CD) else Color(0xFFD4EDDA)
    val contentColor = if (isBaseline) Color(0xFF664D00) else Color(0xFF155724)
    val titulo = if (isBaseline) "⚗️ BASELINE: Red sin optimizar (Didáctico)" else "✅ OPTIMIZED: Red optimizada (WebP + Caché)"
    val detalle = if (isBaseline)
        "Descargando 8 fotos originales (~2.4 MB c/u). Caché desactivada para permitir repeticiones en Network Inspector."
    else
        "Descargando 8 fotos WebP 480px (~150 KB c/u). Carga diferida y caché de disco/memoria activadas."

    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$titulo. $detalle" }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}

/**
 * Tarjeta individual de consulta médica con descarga real por HTTP mediante Coil.
 */
@Composable
private fun ConsultaCard(
    consulta: ConsultaFicticia,
    numero: Int,
    isBaseline: Boolean
) {
    val context = LocalContext.current
    val descripcionAccesible =
        "Consulta $numero: ${consulta.medico}, especialidad ${consulta.especialidad}, " +
        "fecha ${consulta.fecha}. Diagnóstico: ${consulta.diagnostico}"

    // Configuración del ImageRequest según la variante
    val imageRequest = if (isBaseline) {
        // BASELINE: fuerza petición de red real deshabilitando caché de disco y memoria
        ImageRequest.Builder(context)
            .data(consulta.fotoUrl)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    } else {
        // OPTIMIZED: aprovecha caché de disco/memoria, crossfade suave y redimensionamiento
        ImageRequest.Builder(context)
            .data(consulta.fotoUrl)
            .crossfade(true)
            .size(Size(480, 480))
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = descripcionAccesible },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenedor de la fotografía con estados de carga y error accesibles
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "Fotografía de ${consulta.medico}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        // Estado de carga accesible anunciado por TalkBack
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics {
                                    liveRegion = LiveRegionMode.Polite
                                    contentDescription = "Cargando fotografía de ${consulta.medico}"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        // Estado de error si el servidor HTTP local no está iniciado
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .semantics {
                                    contentDescription = "Error al conectar con servidor local de imágenes para ${consulta.medico}"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BrokenImage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = consulta.medico,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = consulta.especialidad,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = consulta.fecha,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = consulta.diagnostico,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Historial Clínico — Preview")
@Composable
fun HistorialClinicoScreenPreview() {
    EPE3_MovilesTheme {
        HistorialClinicoScreen(onBack = {})
    }
}
