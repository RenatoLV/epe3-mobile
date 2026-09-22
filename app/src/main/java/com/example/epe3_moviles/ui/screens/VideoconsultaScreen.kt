package com.example.epe3_moviles.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
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
 * Cumplimiento de requerimientos EPE3 Paso 6:
 * - Permisos CAMERA y RECORD_AUDIO solicitados en runtime.
 * - Estados de UI completos y accesibles:
 *   Idle, SolicitandoPermisos, Inicializando, CreandoOferta,
 *   IntercambiandoIce, Conectada, Finalizada y Error.
 * - Botones accesibles con etiquetas exactas:
 *   "Iniciar videoconsulta" y "Finalizar videoconsulta".
 * - Registro estructurado en Logcat con tag "EPE3_WebRTC".
 * - Liberación estricta de todos los recursos en [DisposableEffect].
 * - Diferenciación entre Baseline (Hilo principal didáctico) y Optimized (Dispatchers.Default).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoconsultaScreen(
    onBack: () -> Unit,
    viewModel: VideoconsultaViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var camaraMuted by remember { mutableStateOf(false) }
    var microfonoMuted by remember { mutableStateOf(false) }

    // Launcher para permisos CAMERA y RECORD_AUDIO en tiempo de ejecución
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val camaraOk = permissions[Manifest.permission.CAMERA] == true
        val audioOk = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (camaraOk && audioOk) {
            viewModel.onPermisosConcedidos()
        } else {
            viewModel.onPermisosDenegados()
        }
    }

    // Efecto de ciclo de vida: Al salir de la pantalla se liberan todos los recursos nativos
    DisposableEffect(Unit) {
        onDispose {
            viewModel.release()
        }
    }

    // Manejador del disparo de permisos cuando el estado pasa a SolicitandoPermisos
    LaunchedEffect(state) {
        if (state is WebRtcState.SolicitandoPermisos) {
            val camaraConcedida = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            val audioConcedido = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (camaraConcedida && audioConcedido) {
                viewModel.onPermisosConcedidos()
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Videoconsulta WebRTC") },
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner de la variante activa (Baseline vs Optimized)
            VarianteBanner(isBaseline = viewModel.isBaseline)

            // Contenedor principal de video / estado loopback
            VideoDisplayCard(
                state = state,
                camaraMuted = camaraMuted
            )

            // Tarjeta de información del médico y la cita programada
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Consulta programada  ·  [DATOS FICTICIOS]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dra. Isabel Fuentes — Medicina General",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Sesión WebRTC Loopback en tiempo real",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                onFinalizarLlamada = { viewModel.finalizarVideoconsulta() },
                onReiniciar = { viewModel.reiniciarEstado() }
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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
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

            // Nota académica y de diagnóstico
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Diagnóstico WebRTC (Tag Logcat: EPE3_WebRTC)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "La llamada se ejecuta en un loopback local auténtico con códecs y pistas de medios. " +
                                if (viewModel.isBaseline) {
                                    "Baseline inicializa en el Hilo Principal para evidenciar el costo en CPU y congelamiento de UI."
                                } else {
                                    "Optimized traslada la inicialización a Dispatchers.Default manteniendo la interfaz fluida."
                                },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Banner informativo que destaca la estrategia de concurrencia según la variante.
 */
@Composable
private fun VarianteBanner(isBaseline: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isBaseline) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isBaseline) "Variante: BASELINE (Didáctica)"
                       else "Variante: OPTIMIZED",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isBaseline) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = if (isBaseline) "Inicialización y negociación SDP en Hilo Principal (Main Thread)."
                       else "Inicialización y negociación SDP asíncrona en Dispatchers.Default.",
                style = MaterialTheme.typography.bodySmall,
                color = if (isBaseline) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Visor de estado y contenedor visual de la videoconsulta loopback.
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
            .semantics {
                contentDescription = "Área de video de videoconsulta: ${state.displayLabel}"
            },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large
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
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Videoconsulta en Reposo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Presiona \"Iniciar videoconsulta\" para establecer la conexión WebRTC local.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.displayLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Negociando códecs de audio y video...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is WebRtcState.Conectada -> {
                    // Contenedor de videollamada activa
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E293B))
                    ) {
                        // Indicador de conexión activa
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (camaraMuted) {
                                Icon(
                                    imageVector = Icons.Filled.VideocamOff,
                                    contentDescription = "Cámara pausada",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cámara pausada por el usuario",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = "Video activo",
                                    modifier = Modifier.size(56.dp),
                                    tint = Color(0xFF4ADE80)
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
                                    color = Color.LightGray
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
                                color = Color(0xCC0F172A),
                                shape = MaterialTheme.shapes.small
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
                                color = Color(0xCC0F172A),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Loopback 640x480",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF93C5FD),
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Videoconsulta finalizada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Los recursos nativos y de hardware fueron liberados exitosamente.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se pudo conectar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.mensaje,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
            is WebRtcState.Conectada -> MaterialTheme.colorScheme.primaryContainer
            is WebRtcState.Error -> MaterialTheme.colorScheme.errorContainer
            is WebRtcState.Finalizada -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.semantics {
            contentDescription = "Estado actual de la videoconsulta: ${state.displayLabel}"
        }
    ) {
        Text(
            text = state.displayLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = when (state) {
                is WebRtcState.Conectada -> MaterialTheme.colorScheme.onPrimaryContainer
                is WebRtcState.Error -> MaterialTheme.colorScheme.onErrorContainer
                is WebRtcState.Finalizada -> MaterialTheme.colorScheme.onSecondaryContainer
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
    onFinalizarLlamada: () -> Unit,
    onReiniciar: () -> Unit
) {
    val enLlamada = state is WebRtcState.Conectada
    val negociando = state is WebRtcState.Inicializando ||
                     state is WebRtcState.CreandoOferta ||
                     state is WebRtcState.IntercambiandoIce ||
                     state is WebRtcState.SolicitandoPermisos

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
                .size(56.dp)
                .semantics {
                    contentDescription = if (camaraMuted) "Activar cámara" else "Desactivar cámara"
                }
        ) {
            Icon(
                imageVector = if (!camaraMuted) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
        }

        // Control de Micrófono
        FilledIconToggleButton(
            checked = !microfonoMuted,
            onCheckedChange = { onToggleMicrofono() },
            enabled = enLlamada,
            modifier = Modifier
                .size(56.dp)
                .semantics {
                    contentDescription = if (microfonoMuted) "Activar micrófono" else "Silenciar micrófono"
                }
        ) {
            Icon(
                imageVector = if (!microfonoMuted) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
        }

        // Botón principal accesible (Iniciar o Finalizar videoconsulta)
        if (enLlamada || negociando) {
            Button(
                onClick = onFinalizarLlamada,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
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
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
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
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
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
