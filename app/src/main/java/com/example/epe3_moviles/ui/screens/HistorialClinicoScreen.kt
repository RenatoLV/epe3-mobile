package com.example.epe3_moviles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
 * Diseño clínico modernizado:
 * - Banner informativo estilizado.
 * - Tarjetas de consulta con jerarquía visual médica clara (Médico, Especialidad, Fecha, Diagnóstico y Receta Rx).
 * - Avatar con fallback elegante a iniciales del médico si la imagen HTTP no está disponible.
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
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Historial Clínico",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBaseline) "Carga completa (Room monolítico)" else "Carga paginada (Room + Paging 3)",
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
                .padding(innerPadding)
        ) {
            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por doctor, diagnóstico o receta...") },
                leadingIcon = {
                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Search, contentDescription = "Buscar")
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Consultando base de datos local...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            key = pagingItems.itemKey { it.id }
                        ) { index ->
                            val consulta = pagingItems[index]
                            if (consulta != null) {
                                ConsultaEntityCard(
                                    consulta = consulta,
                                    numero = index + 1,
                                    isBaseline = false
                                )
                            }
                        }

                        when (val refreshState = pagingItems.loadState.refresh) {
                            is LoadState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                            is LoadState.Error -> {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Text(
                                            text = "Error al inicializar Paging: ${refreshState.error.message}",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }

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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Cargando 20 registros más...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
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
 * Banner informativo estilizado que detalla la estrategia de Room y Paging activa.
 */
@Composable
private fun RoomFlavorBanner(
    isBaseline: Boolean,
    totalRegistros: Int,
    tiempoConsultaMs: Long?
) {
    val borderColor = if (isBaseline) Color(0xFFF59E0B) else Color(0xFF10B981)
    val bgColor = if (isBaseline) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFF064E3B).copy(alpha = 0.25f)
    val textColor = if (isBaseline) Color(0xFFFDE68A) else Color(0xFFA7F3D0)

    val titulo = if (isBaseline)
        "BASELINE: Room Monolítico ($totalRegistros registros)"
    else
        "OPTIMIZADO: Room + Paging 3 (Páginas de 20)"

    val detalle = if (isBaseline) {
        val extra = if (tiempoConsultaMs != null) " Tiempo: ${tiempoConsultaMs}ms." else ""
        "Carga los $totalRegistros registros completos en memoria.$extra Sin paginación ni caché."
    } else {
        "Paginación bajo demanda ($totalRegistros registros indexados por fecha). Caché WebP activa."
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

/**
 * Tarjeta médica para representar una [ConsultaEntity] de Room.
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

    // Iniciales para fallback del avatar (ej. "Dr. Andrés Morales" -> "AM")
    val iniciales = consulta.medicoNombre
        .replace("Dr. ", "")
        .replace("Dra. ", "")
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")

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
            .semantics { contentDescription = descripcionAccesible },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila superior: Avatar, Nombre del médico, especialidad y número de consulta
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar del médico con descarga HTTP y fallback elegante
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = "Fotografía de ${consulta.medicoNombre}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = iniciales,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = iniciales,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = consulta.medicoNombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = consulta.especialidad,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = consulta.fechaTexto,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "#$numero",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sección: Diagnóstico
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalInformation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Diagnóstico Clínico",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = consulta.diagnostico,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sección: Tratamiento Rx en cajita destacada
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rx: ${consulta.tratamiento}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
