package com.example.epe3_moviles.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.epe3_moviles.ui.theme.EPE3_MovilesTheme

/**
 * Pantalla Videoconsulta.
 *
 * FASE 1: Presenta la estructura de la pantalla con controles de cámara
 * y micrófono y el estado "Sin conexión activa". En esta fase no hay
 * integración de WebRTC.
 *
 * WebRTC (inicialización de codecs y negociación SDP) se implementará en
 * la Fase 6, separada en las variantes baseline (hilo principal) y
 * optimized (Dispatchers.Default con Coroutines).
 *
 * NOTA: Esta pantalla NO simula una llamada real. No presenta un video
 * local, un temporizador ni una carga de CPU artificial como videollamada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoconsultaScreen(onBack: () -> Unit) {
    var camaraActiva by remember { mutableStateOf(false) }
    var microfonoActivo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Videoconsulta") },
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
            // Área de video (placeholder — WebRTC se agrega en Fase 6)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vista de cámara\n(disponible en Fase 6 — WebRTC)",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Información de la consulta programada (datos ficticios)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Consulta programada  ·  [DATOS FICTICIOS]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dra. Isabel Fuentes — Medicina General",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Lunes 29 sep 2026 · 10:30",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Estado de la sesión
            StatusChip(texto = "Sin conexión activa")

            // Controles de cámara y micrófono
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Botón cámara
                FilledIconToggleButton(
                    checked = camaraActiva,
                    onCheckedChange = { camaraActiva = it },
                    modifier = Modifier
                        .size(64.dp)
                        .semantics {
                            contentDescription =
                                if (camaraActiva) "Desactivar cámara" else "Activar cámara"
                        }
                ) {
                    Icon(
                        imageVector = if (camaraActiva) Icons.Filled.Videocam
                                      else Icons.Filled.VideocamOff,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Botón micrófono
                FilledIconToggleButton(
                    checked = microfonoActivo,
                    onCheckedChange = { microfonoActivo = it },
                    modifier = Modifier
                        .size(64.dp)
                        .semantics {
                            contentDescription =
                                if (microfonoActivo) "Silenciar micrófono" else "Activar micrófono"
                        }
                ) {
                    Icon(
                        imageVector = if (microfonoActivo) Icons.Filled.Mic
                                      else Icons.Filled.MicOff,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botón "Iniciar llamada" (deshabilitado hasta Fase 6)
                Button(
                    onClick = { /* WebRTC se implementa en Fase 6 */ },
                    enabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .semantics {
                            contentDescription =
                                "Iniciar llamada — disponible en Fase 6 cuando WebRTC esté configurado"
                        }
                ) {
                    Text("Iniciar llamada")
                }
            }

            // Aviso informativo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "⚠ La integración de video real (WebRTC) se implementa " +
                           "en la Fase 6. Esta pantalla muestra la estructura de la interfaz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.semantics { contentDescription = "Estado de la llamada: $texto" }
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Preview(showBackground = true, name = "Videoconsulta")
@Composable
fun VideoconsultaScreenPreview() {
    EPE3_MovilesTheme {
        VideoconsultaScreen(onBack = {})
    }
}
