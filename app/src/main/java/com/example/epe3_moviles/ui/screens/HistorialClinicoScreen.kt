package com.example.epe3_moviles.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.example.epe3_moviles.config.NetworkConfig
import com.example.epe3_moviles.data.local.ConsultaEntity

/**
 * Pantalla Historial Clínico conectada a Room y Paging 3 (Paso 4).
 *
 * BASELINE (Didáctico):
 * - Consulta Room monolítica (`getAllConsultas`) que trae los 220 registros completos a memoria.
 * - Muestra el tiempo de consulta y el conteo total.
 * - Carga de imágenes HTTP originales sin caché.
 *
 * OPTIMIZED:
 * - Consulta Room con Paging 3 (`getPagingConsultas`) en páginas de 20 registros bajo demanda.
 * - Carga incremental al desplazarse, optimizada por índice en columna `fecha`.
 * - Carga de imágenes HTTP WebP con caché en disco y memoria.
 *
 * Accesibilidad (WCAG):
 * - Anuncio accesible de estado de carga mediante [LiveRegionMode.Polite].
 * - contentDescription completo en cada elemento.
 * - Objetivos táctiles mínimos de 48 dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialClinicoScreen(
    onBack: () -> Unit,
    viewModel: HistorialViewModel = viewModel(),
) {
    val isBaseline = viewModel.isBaseline
    val isLoading by viewModel.isLoading.collectAsState()
    val totalRegistros by viewModel.totalRegistros.collectAsState()
    val tiempoBaselineMs by viewModel.tiempoConsultaBaselineMs.collectAsState()

    // Para BASELINE: lista completa
    val baselineItems by viewModel.baselineConsultas.collectAsState()

    // Para OPTIMIZED: flujo paginado de Paging 3 (20 items/página)
    val pagingItems = viewModel.pagingConsultasFlow.collectAsLazyPagingItems()

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
            // Banner de la variante indicando la estrategia de Room
            RoomFlavorBanner(
                isBaseline = isBaseline,
                totalRegistros = totalRegistros,
                tiempoConsultaMs = tiempoBaselineMs
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Cargando historial clínico desde base de datos Room"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isBaseline) {
                        // RENDERIZADO BASELINE: 220 registros cargados monolíticamente
                        itemsIndexed(
                            items = baselineItems,
                            key = { _, item -> item.id }
                        ) { index, consulta ->
                            ConsultaEntityCard(
                                consulta = consulta,
                                numero = index + 1,
                                isBaseline = true
                            )
                        }
                    } else {
                        // RENDERIZADO OPTIMIZED: Paging 3 (páginas de 20 elementos)
                        items(
                            count = pagingItems.itemCount,
                            key = pagingItems.itemKey { it.id },
                        ) { index ->
                            pagingItems[index]?.let { item ->
                                ConsultaEntityCard(
                                    consulta = item,
                                    numero = index + 1,
                                    isBaseline = false,
                                )
                            }
                        }

                        // Indicador de carga al solicitar la siguiente página de 20 elementos
                        when (pagingItems.loadState.append) {
                            is LoadState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .semantics {
                                                liveRegion = LiveRegionMode.Polite
                                                contentDescription = "Cargando página siguiente de 20 consultas..."
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                            is LoadState.Error -> {
                                item {
                                    Text(
                                        text = "Error al cargar más consultas",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

/**
 * Banner informativo que detalla la estrategia de Room y Paging activa.
 */
@Composable
private fun RoomFlavorBanner(
    isBaseline: Boolean,
    totalRegistros: Int,
    tiempoConsultaMs: Long?
) {
    val backgroundColor = if (isBaseline) Color(0xFFFFF3CD) else Color(0xFFD4EDDA)
    val contentColor = if (isBaseline) Color(0xFF664D00) else Color(0xFF155724)
    val titulo = if (isBaseline)
        "⚗️ BASELINE: Room sin paginación (Didáctico)"
    else
        "✅ OPTIMIZED: Room + Paging 3 (Páginas de 20)"

    val detalle = if (isBaseline) {
        val extra = if (tiempoConsultaMs != null) " Tiempo de consulta: ${tiempoConsultaMs}ms." else ""
        "Carga monolítica de $totalRegistros registros en memoria de una sola vez.$extra Caché de imágenes desactivada."
    } else {
        "Paginación reactiva de $totalRegistros registros indexados por fecha. Caché de imágenes y WebP activos."
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

/**
 * Tarjeta para representar una [ConsultaEntity] de Room con su imagen HTTP respectiva.
 */
@Composable
private fun ConsultaEntityCard(
    consulta: ConsultaEntity,
    numero: Int,
    isBaseline: Boolean
) {
    val context = LocalContext.current
    val fotoUrl = NetworkConfig.getFotoMedicoUrl(consulta.medicoId)
    val descripcionAccesible =
        "Consulta $numero: ${consulta.medicoNombre}, especialidad ${consulta.especialidad}, " +
        "fecha ${consulta.fechaTexto}. Diagnóstico: ${consulta.diagnostico}. Tratamiento: ${consulta.tratamiento}"

    // Configuración de Coil según la variante
    val imageRequest = if (isBaseline) {
        ImageRequest.Builder(context)
            .data(fotoUrl)
            .crossfade(enable = false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    } else {
        ImageRequest.Builder(context)
            .data(fotoUrl)
            .crossfade(enable = true)
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
            // Avatar del médico con descarga HTTP real
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "Fotografía de ${consulta.medicoNombre}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics {
                                    liveRegion = LiveRegionMode.Polite
                                    contentDescription = "Cargando foto de ${consulta.medicoNombre}"
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .semantics {
                                    contentDescription = "Foto no disponible para ${consulta.medicoNombre}"
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = consulta.medicoNombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "#$numero",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = consulta.especialidad,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "📅 ${consulta.fechaTexto}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = consulta.diagnostico,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Rx: ${consulta.tratamiento}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
