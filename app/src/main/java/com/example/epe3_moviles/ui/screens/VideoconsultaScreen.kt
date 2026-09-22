package com.example.epe3_moviles.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.epe3_moviles.webrtc.WebRtcState

/**
 * Pantalla de Videoconsulta con conexión WebRTC real en Loopback.
 *
 * Diseño clínico modernizado:
 * - Visor de llamada con estética contemporánea (modo oscuro nocturno en área de video, indicadores de calidad).
 * - Barra de controles de telemedicina con acceso rápido a silencio, cámara y colgar.
 * - Tarjetas informativas con alto contraste y legibilidad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoconsultaScreen(
    onBack: () -> Unit,
    viewModel: VideoconsultaViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var camaraMuted by remember { mutableStateOf(false) }
    var microfonoMuted by remember { mutableStateOf(false) }

    // Launcher para permisos CAMERA y RECORD_AUDIO en runtime
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val camaraOk = permissions[Manifest.permission.CAMERA] == true
        val audioOk = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (camaraOk && audioOk) {
            viewModel.onPermisosConcedidos()
        } else {
            viewModel.onPermisosDenegados()
        }
    }

    // Efecto de ciclo de vida: Liberación estricta de hardware
    DisposableEffect(Unit) {
        onDispose {
            viewModel.release()
        }
    }

    // Manejador del disparo de permisos
    LaunchedEffect(state) {
        if (state is WebRtcState.SolicitandoPermisos) {
            val camaraConcedida = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
            val audioConcedido = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED

            if (camaraConcedida && audioConcedido) {
                viewModel.onPermisosConcedidos()
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Videoconsulta WebRTC",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Telemedicina en tiempo real",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner de la variante activa
            VarianteBanner(isBaseline = viewModel.isBaseline)

            // Contenedor principal de video / streaming loopback
            VideoDisplayCard(
                state = state,
                camaraMuted = camaraMuted
            )

            // Tarjeta de información del médico y la cita programada
            DoctorCallInfoCard()

            // Chip accesible de estado
            StatusChip(state = state)

            // Botones de acción principales
            ActionControlsRow(
                state = state,
                camaraMuted = camaraMuted,
                microfonoMuted = microfonoMuted,
                onToggleCamara = { camaraMuted = !camaraMuted },
                onToggleMicrofono = { microfonoMuted = !microfonoMuted },
                onIniciarLlamada = { viewModel.solicitarInicioLlamada() },
                onFinalizarLlamada = { viewModel.finalizarVideoconsulta() }
            )

            // Mensaje de error si ocurre alguno
            AnimatedVisibility(
                visible = state is WebRtcState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val errorState = state as? WebRtcState.Error
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aviso de Videoconsulta",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = errorState?.mensaje ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Tarjeta de diagnóstico y referencia técnica
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Diagnóstico WebRTC (Filtro Logcat: EPE3_WebRTC)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "La llamada se ejecuta en un loopback local auténtico con códecs y pistas de medios. " +
                                if (viewModel.isBaseline) {
                                    "Baseline inicializa en el Hilo Principal para evidenciar el costo en CPU y congelamiento de UI."
                                } else {
                                    "Optimized traslada la inicialización a Dispatchers.Default manteniendo la interfaz fluida."
                                },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Banner informativo que destaca la estrategia de concurrencia según la variante.
 */
@Composable
private fun VarianteBanner(isBaseline: Boolean) {
    val borderColor = if (isBaseline) Color(0xFFF59E0B) else Color(0xFF10B981)
    val bgColor = if (isBaseline) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFF064E3B).copy(alpha = 0.25f)
    val textColor = if (isBaseline) Color(0xFFFDE68A) else Color(0xFFA7F3D0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        color = bgColor,
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
                    text = if (isBaseline) "BASELINE (Didáctica)" else "OPTIMIZADA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = if (isBaseline) "Inicialización y negociación SDP en Hilo Principal (Main Thread)."
                           else "Inicialización asíncrona en Dispatchers.Default (UI fluida).",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Contenedor visual moderno de la videoconsulta loopback.
 */
@Composable
private fun VideoDisplayCard(
    state: WebRtcState,
    camaraMuted: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(20.dp))
            .semantics {
                contentDescription = "Área de video de videoconsulta: ${state.displayLabel}"
            },
        color = Color(0xFF0B132B),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is WebRtcState.Idle -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Videoconsulta en Reposo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Presiona \"Iniciar videoconsulta\" para conectar el loopback WebRTC.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                is WebRtcState.SolicitandoPermisos,
                is WebRtcState.Inicializando,
                is WebRtcState.CreandoOferta,
                is WebRtcState.IntercambiandoIce -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(44.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = state.displayLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Negociando códecs de audio y video...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                is WebRtcState.Conectada -> {
                    // Contenedor de videollamada activa
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A))
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (camaraMuted) {
                                Icon(
                                    imageVector = Icons.Filled.VideocamOff,
                                    contentDescription = "Cámara pausada",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cámara pausada por el usuario",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF94A3B8)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = "Video activo",
                                    modifier = Modifier.size(54.dp),
                                    tint = Color(0xFF34D399)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.infoCamara,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Streaming WebRTC Loopback bidireccional",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Badge superior con duración de la llamada
                        val minutos = state.tiempoConectadaSegundos / 60
                        val segundos = state.tiempoConectadaSegundos % 60
                        val tiempoTexto = String.format("%02d:%02d", minutos, segundos)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .align(Alignment.TopStart),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xDD0B132B),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "EN VIVO · $tiempoTexto",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xDD0B132B),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Loopback 640x480",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                is WebRtcState.Finalizada -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF34D399)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Videoconsulta finalizada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Los recursos nativos y de hardware fueron liberados exitosamente.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                is WebRtcState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = Color(0xFFF87171)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se pudo conectar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.mensaje,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de información del médico asignado.
 */
@Composable
private fun DoctorCallInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dra. Isabel Fuentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Medicina General · Teleconsulta WebRTC",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Chip que comunica de forma textual y accesible el estado de la conexión WebRTC.
 */
@Composable
private fun StatusChip(state: WebRtcState) {
    Surface(
        color = when (state) {
            is WebRtcState.Conectada -> Color(0xFF064E3B)
            is WebRtcState.Error -> Color(0xFF7F1D1D)
            is WebRtcState.Finalizada -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.semantics {
            contentDescription = "Estado actual de la videoconsulta: ${state.displayLabel}"
        }
    ) {
        Text(
            text = state.displayLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = when (state) {
                is WebRtcState.Conectada -> Color(0xFFA7F3D0)
                is WebRtcState.Error -> Color(0xFFFCA5A5)
                is WebRtcState.Finalizada -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

/**
 * Fila de controles accesibles para gestionar la videoconsulta.
 */
@Composable
private fun ActionControlsRow(
    state: WebRtcState,
    camaraMuted: Boolean,
    microfonoMuted: Boolean,
    onToggleCamara: () -> Unit,
    onToggleMicrofono: () -> Unit,
    onIniciarLlamada: () -> Unit,
    onFinalizarLlamada: () -> Unit
) {
    val enLlamada = state is WebRtcState.Conectada
    val negociando = (state is WebRtcState.Inicializando) ||
                     (state is WebRtcState.CreandoOferta) ||
                     (state is WebRtcState.IntercambiandoIce) ||
                     (state is WebRtcState.SolicitandoPermisos)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Control de Cámara
        FilledIconToggleButton(
            checked = !camaraMuted,
            onCheckedChange = { onToggleCamara() },
            enabled = enLlamada,
            modifier = Modifier
                .size(54.dp)
                .semantics {
                    contentDescription = if (camaraMuted) "Activar cámara" else "Desactivar cámara"
                },
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = if (!camaraMuted) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        // Control de Micrófono
        FilledIconToggleButton(
            checked = !microfonoMuted,
            onCheckedChange = { onToggleMicrofono() },
            enabled = enLlamada,
            modifier = Modifier
                .size(54.dp)
                .semantics {
                    contentDescription = if (microfonoMuted) "Activar micrófono" else "Silenciar micrófono"
                },
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                imageVector = if (!microfonoMuted) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        // Botón principal accesible (Iniciar o Finalizar videoconsulta)
        if (enLlamada || negociando) {
            Button(
                onClick = onFinalizarLlamada,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp)
                    .semantics {
                        contentDescription = "Finalizar videoconsulta"
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Finalizar videoconsulta", fontWeight = FontWeight.Bold)
            }
        } else if (state is WebRtcState.Finalizada || state is WebRtcState.Error) {
            Button(
                onClick = onIniciarLlamada,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp)
                    .semantics {
                        contentDescription = "Iniciar videoconsulta"
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar videoconsulta", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onIniciarLlamada,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp)
                    .semantics {
                        contentDescription = "Iniciar videoconsulta"
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar videoconsulta", fontWeight = FontWeight.Bold)
            }
        }
    }
}
