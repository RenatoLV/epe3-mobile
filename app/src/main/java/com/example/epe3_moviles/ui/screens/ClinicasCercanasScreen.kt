package com.example.epe3_moviles.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
 * BASELINE:
 * - Frecuencia alta: Cada 2 segundos con [Priority.PRIORITY_HIGH_ACCURACY].
 * - Muestra estado "Medición activa".
 * - Se detiene al salir de la pantalla con log de confirmación.
 *
 * OPTIMIZED:
 * - Frecuencia moderada: Cada 30 segundos con [Priority.PRIORITY_BALANCED_POWER_ACCURACY].
 * - Detención mediante [DisposableEffect] con [onDispose].
 * - Muestra estado "Ubicación detenida" al finalizar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicasCercanasScreen(
    onBack: () -> Unit,
    viewModel: ClinicasViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isBaseline = viewModel.isBaseline

    // Launcher de permisos de ubicación en runtime
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
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
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.onPermisosConcedidos()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Gestión estricta de ciclo de vida: se detienen las actualizaciones al salir del Composable
    DisposableEffect(Unit) {
        onDispose {
            viewModel.detenerSeguimiento(
                origenCierre = if (isBaseline) "Salida de Pantalla Baseline" else "DisposableEffect onDispose Optimized"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clínicas cercanas") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Volver a Mi Consulta" }
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
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
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
    val backgroundColor = if (isBaseline) Color(0xFFFFF3CD) else Color(0xFFD4EDDA)
    val contentColor = if (isBaseline) Color(0xFF664D00) else Color(0xFF155724)

    val titulo = if (isBaseline)
        "⚗️ BASELINE: GPS cada 2s · HIGH_ACCURACY (Medición activa)"
    else
        "✅ OPTIMIZED: GPS cada 30s · BALANCED_POWER"

    val detalle = when (state) {
        is ClinicasLocationState.UbicacionDisponible ->
            "Muestra #${state.conteoActualizaciones} a las ${state.horaTexto} (precisión ±${state.precisionMetros.toInt()}m). Se detiene al salir."
        is ClinicasLocationState.BuscandoUbicacion ->
            "Obteniendo primera coordenada satelital/red..."
        is ClinicasLocationState.UbicacionDetenida ->
            "Ubicación detenida. Sensor liberado al cerrar la pantalla."
        else ->
            if (isBaseline) "Actualización cada 2s para provocar consumo medible en Energy Profiler."
            else "Actualización cada 30s para optimizar la batería."
    }

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

@Composable
private fun ListaClinicasConDistancia(
    clinicasConDistancia: List<Pair<ClinicaModel, String>>
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            .semantics { contentDescription = accesible },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    modifier = Modifier.weight(1f)
                )
                // Badge con distancia calculada en tiempo real
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
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
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = clinica.direccion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = clinica.telefono,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AssistChip(
                onClick = {},
                label = { Text(if (clinica.abierta) "Abierta 24h" else "Cerrada") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (clinica.abierta)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
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
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Permiso de ubicación necesario",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Para calcular la distancia real a las clínicas más cercanas, la aplicación necesita acceso a la ubicación de este dispositivo.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onSolicitar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Conceder permisos de ubicación" }
            ) {
                Text("Conceder permisos")
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
            Icon(
                imageVector = Icons.Filled.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Permiso de ubicación denegado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "No es posible calcular la distancia a las clínicas sin el permiso de ubicación. Puedes concederlo para continuar la prueba.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onReintentar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Reintentar solicitud de permiso de ubicación" }
            ) {
                Text("Reintentar permiso")
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Buscando señal de ubicación...",
                style = MaterialTheme.typography.bodyMedium
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Ubicación detenida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "El sensor GPS ha sido liberado correctamente. Total de lecturas: $total (última: $ultimaHora).",
                style = MaterialTheme.typography.bodyMedium
            )
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
        Text(
            text = "Error: $mensaje",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
