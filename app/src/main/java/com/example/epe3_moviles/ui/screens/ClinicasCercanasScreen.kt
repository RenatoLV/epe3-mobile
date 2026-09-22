package com.example.epe3_moviles.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.epe3_moviles.location.ClinicaModel

/**
 * Pantalla Clínicas Cercanas adaptada para medición de energía y geolocalización (Paso 5).
 *
 * Diseño clínico modernizado:
 * - Tarjetas con badges destacados de distancia (fórmula Haversine).
 * - Indicadores claros de estado abierto/cerrado.
 * - Banner con información de frecuencia GPS de alto contraste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicasCercanasScreen(
    onBack: () -> Unit,
    viewModel: ClinicasViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isBaseline = viewModel.isBaseline

    // Launcher de permisos de ubicación en runtime
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.onPermisosConcedidos()
        } else {
            viewModel.onPermisosDenegados()
        }
    }

    // Verificar permisos al iniciar el Composable
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.onPermisosConcedidos()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    // Gestión estricta de ciclo de vida: se detienen las actualizaciones al salir
    DisposableEffect(Unit) {
        onDispose {
            viewModel.detenerSeguimiento(
                origenCierre = if (isBaseline) "Salida de Pantalla Baseline" else "DisposableEffect onDispose Optimized",
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Clínicas Cercanas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBaseline) "GPS cada 2s (High Accuracy)" else "GPS cada 30s (Balanced Power)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Volver a Mi Consulta" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Banner de la variante indicando la frecuencia y prioridad del GPS
            GpsFlavorBanner(isBaseline = isBaseline, state = state)

            // Contenido dinámico según el estado accesible
            when (val s = state) {
                is ClinicasLocationState.PermisoRequerido -> {
                    EstadoPermisoRequerido(
                        onSolicitar = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        },
                    )
                }
                is ClinicasLocationState.PermisoDenegado -> {
                    EstadoPermisoDenegado(
                        onReintentar = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )
                }
                is ClinicasLocationState.BuscandoUbicacion -> {
                    EstadoBuscandoUbicacion()
                }
                is ClinicasLocationState.UbicacionDisponible -> {
                    ListaClinicasConDistancia(
                        clinicasConDistancia = s.clinicasConDistancia
                    )
                }
                is ClinicasLocationState.UbicacionDetenida -> {
                    EstadoUbicacionDetenida(ultimaHora = s.ultimaHora, total = s.totalActualizaciones)
                }
                is ClinicasLocationState.Error -> {
                    EstadoError(mensaje = s.mensaje)
                }
            }
        }
    }
}

/**
 * Banner superior que identifica la variante, la frecuencia de sondeo y el estado del sensor.
 */
@Composable
private fun GpsFlavorBanner(isBaseline: Boolean, state: ClinicasLocationState) {
    val borderColor = if (isBaseline) Color(0xFFF59E0B) else Color(0xFF10B981)
    val bgColor = if (isBaseline) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFF064E3B).copy(alpha = 0.25f)
    val textColor = if (isBaseline) Color(0xFFFDE68A) else Color(0xFFA7F3D0)

    val titulo = if (isBaseline)
        "BASELINE: GPS cada 2s · HIGH_ACCURACY"
    else
        "OPTIMIZADO: GPS cada 30s · BALANCED_POWER"

    val detalle = when (state) {
        is ClinicasLocationState.UbicacionDisponible ->
            "Muestra #${state.conteoActualizaciones} a las ${state.horaTexto} (±${state.precisionMetros.toInt()}m). Se detiene al salir."
        is ClinicasLocationState.BuscandoUbicacion ->
            "Obteniendo primera coordenada satelital/red..."
        is ClinicasLocationState.UbicacionDetenida ->
            "Ubicación detenida. Sensor liberado al cerrar la pantalla."
        else ->
            if (isBaseline) "Actualización cada 2s para provocar consumo medible en Energy Profiler."
            else "Actualización cada 30s para optimizar la batería."
    }

    Surface(
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, borderColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .semantics { contentDescription = "$titulo. $detalle" },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ListaClinicasConDistancia(
    clinicasConDistancia: List<Pair<ClinicaModel, String>>
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(clinicasConDistancia, key = { it.first.id }) { (clinica, distanciaTexto) ->
            ClinicaCardConDistancia(clinica = clinica, distanciaTexto = distanciaTexto)
        }
    }
}

@Composable
private fun ClinicaCardConDistancia(
    clinica: ClinicaModel,
    distanciaTexto: String
) {
    val accesible = "${clinica.nombre}, a $distanciaTexto de distancia. " +
            "Dirección: ${clinica.direccion}. Teléfono: ${clinica.telefono}. " +
            if (clinica.abierta) "Abierta ahora." else "Cerrada ahora."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                ),
                RoundedCornerShape(16.dp)
            )
            .semantics { contentDescription = accesible },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clinica.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Badge con distancia calculada en tiempo real
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = distanciaTexto,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = clinica.especialidades,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = clinica.direccion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = clinica.telefono,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = if (clinica.abierta) "Atención 24 Horas" else "Cerrada",
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (clinica.abierta) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (clinica.abierta)
                        Color(0xFF064E3B).copy(alpha = 0.25f)
                    else
                        Color(0xFF7F1D1D).copy(alpha = 0.25f),
                    labelColor = if (clinica.abierta)
                        Color(0xFFA7F3D0)
                    else
                        Color(0xFFFCA5A5)
                ),
                border = BorderStroke(
                    1.dp,
                    if (clinica.abierta) Color(0xFF10B981).copy(alpha = 0.3f)
                    else Color(0xFFEF4444).copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// ── Estados Accesibles ────────────────────────────────────────────────────────

@Composable
private fun EstadoPermisoRequerido(onSolicitar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "Permiso de ubicación necesario",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Para calcular la distancia real a las clínicas más cercanas, la aplicación necesita acceso a la ubicación de este dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onSolicitar,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .semantics { contentDescription = "Conceder permisos de ubicación" }
            ) {
                Text("Conceder permisos", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EstadoPermisoDenegado(onReintentar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = "Permiso de ubicación denegado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "No es posible calcular la distancia a las clínicas sin el permiso de ubicación. Puedes concederlo para continuar la prueba.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onReintentar,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .semantics { contentDescription = "Reintentar solicitud de permiso de ubicación" }
            ) {
                Text("Reintentar permiso", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EstadoBuscandoUbicacion() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Buscando señal de ubicación satelital y de red..."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Buscando ubicación satelital...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Calculando coordenadas GPS en tiempo real",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EstadoUbicacionDetenida(ultimaHora: String, total: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Ubicación Detenida",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sensor GPS liberado exitosamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (ultimaHora.isNotEmpty()) {
                    Text(
                        text = "Última muestra a las $ultimaHora ($total actualizaciones)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EstadoError(mensaje: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Error al obtener ubicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
