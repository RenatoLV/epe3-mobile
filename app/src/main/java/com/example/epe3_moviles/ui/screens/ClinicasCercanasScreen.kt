package com.example.epe3_moviles.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.epe3_moviles.location.ClinicaModel
import com.example.epe3_moviles.ui.components.ClinicasMapComponent

/**
 * Pantalla Clínicas Cercanas optimizada para alta resiliencia y experiencia de usuario moderna (Paso 5).
 *
 * Mejoras UI/UX y Conexión:
 * - Mapa interactivo vectorial integrado: visualiza al usuario y a los centros de salud en tiempo real.
 * - Modo resiliente: ante mala conexión o satélites demorados, carga inmediatamente la información
 *   referencial de Santiago sin bloquear al usuario en "Buscando ubicación".
 * - Botones directos para navegación GPS en Google Maps/Waze y llamadas telefónicas.
 * - Paleta clínica refinada con tonos Slate, Emerald y Sky inspirados en Tailwind.
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
                actions = {
                    IconButton(
                        onClick = { viewModel.reintentarGps() },
                        modifier = Modifier.semantics { contentDescription = "Reintentar o refrescar GPS" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
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
                    EstadoBuscandoUbicacion(
                        onForzarReferencial = { viewModel.usarUbicacionReferencialManual() }
                    )
                }
                is ClinicasLocationState.UbicacionDisponible -> {
                    ContenidoMapaYLista(
                        estado = s,
                        onReintentarGps = { viewModel.reintentarGps() }
                    )
                }
                is ClinicasLocationState.UbicacionDetenida -> {
                    EstadoUbicacionDetenida(ultimaHora = s.ultimaHora, total = s.totalActualizaciones)
                }
                is ClinicasLocationState.Error -> {
                    EstadoError(
                        mensaje = s.mensaje,
                        onUsarReferencial = { viewModel.usarUbicacionReferencialManual() }
                    )
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
            if (state.esReferencial) "Modo referencial Santiago. Sincronizando satélites en segundo plano."
            else "Muestra #${state.conteoActualizaciones} a las ${state.horaTexto} (±${state.precisionMetros.toInt()}m). Se detiene al salir."
        is ClinicasLocationState.BuscandoUbicacion ->
            "Sincronizando coordenadas satelitales y de red..."
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, borderColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .semantics { contentDescription = "$titulo. $detalle" },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(18.dp)
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

/**
 * Contenedor visual que combina el mapa interactivo y la lista de clínicas.
 */
@Composable
private fun ContenidoMapaYLista(
    estado: ClinicasLocationState.UbicacionDisponible,
    onReintentarGps: () -> Unit
) {
    val context = LocalContext.current
    var verSoloLista by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Aviso si se está en modo referencial por mala señal satelital
        if (estado.esReferencial) {
            item {
                Surface(
                    color = Color(0xFF78350F).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo Referencial Activo",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE68A)
                            )
                            Text(
                                text = "Mostrando distancias estimadas respecto a Santiago Centro mientras se fijan los satélites GPS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFDE68A).copy(alpha = 0.9f)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = onReintentarGps,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Reintentar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }
                }
            }
        }

        // Selector de vista: Mapa interactivo o Lista (Pill Segmented Control moderno sin salto de línea)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mapa de Red Asistencial",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!verSoloLista) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { verSoloLista = false }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (!verSoloLista) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mapa",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!verSoloLista) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (verSoloLista) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { verSoloLista = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (verSoloLista) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lista",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (verSoloLista) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Mapa Interactivo Real de Red Asistencial (OpenStreetMap / Satélite)
        if (!verSoloLista) {
            item {
                ClinicasMapComponent(
                    userLat = estado.latitud,
                    userLon = estado.longitud,
                    clinicasConDistancia = estado.clinicasConDistancia,
                    esReferencial = estado.esReferencial,
                    precisionMetros = estado.precisionMetros,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                )
            }
        }

        // Encabezado de la lista
        item {
            Text(
                text = "Centros de Atención Disponibles (${estado.clinicasConDistancia.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Lista de tarjetas de clínicas
        items(estado.clinicasConDistancia, key = { it.first.id }) { (clinica, distanciaTexto) ->
            ClinicaCardConDistancia(
                clinica = clinica,
                distanciaTexto = distanciaTexto,
                onNavegar = { openExternalNavigation(context, clinica) },
                onLlamar = { callClinic(context, clinica.telefono) }
            )
        }
    }
}

@Composable
private fun ClinicaCardConDistancia(
    clinica: ClinicaModel,
    distanciaTexto: String,
    onNavegar: () -> Unit,
    onLlamar: () -> Unit
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
            // Nombre y badge de distancia
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

                // Badge de distancia
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

            Spacer(modifier = Modifier.height(8.dp))

            // Dirección
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = clinica.direccion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Teléfono
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = clinica.telefono,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de acciones y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Estado 24h
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = if (clinica.abierta) "24 Horas" else "Cerrada",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
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

                // Botones Navegar y Llamar
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onLlamar,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Llamar",
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Button(
                        onClick = onNavegar,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ruta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
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
private fun EstadoBuscandoUbicacion(onForzarReferencial: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Buscando señal de ubicación satelital y de red..."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            Text(
                text = "Cargando red asistencial...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Conectando con centros de atención médica",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botón de acción rápida para no quedar bloqueado ante mal internet
            OutlinedButton(
                onClick = onForzarReferencial,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cargar mapa de inmediato", fontSize = 13.sp)
            }
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
private fun EstadoError(mensaje: String, onUsarReferencial: () -> Unit) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                Button(
                    onClick = onUsarReferencial,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cargar mapa con ubicación referencial", color = Color.White)
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
    } catch (e: Exception) {
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
