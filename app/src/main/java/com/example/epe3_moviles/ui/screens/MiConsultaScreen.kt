package com.example.epe3_moviles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.epe3_moviles.BuildConfig
import com.example.epe3_moviles.ui.theme.*
import com.example.epe3_moviles.util.DoctorImageHelper

/**
 * Pantalla principal "Mi Consulta".
 *
 * Diseño clínico modernizado inspirado en Tailwind UI y Material 3:
 * - Fotografías de doctores reales con carga HTTP y respaldo offline garantizado.
 * - Paleta de colores profesionales de alta fidelidad médica (Slate, Emerald, Sky, Indigo).
 * - Carrusel dinámico de especialistas disponibles con indicadores de estado y horarios.
 * - Tarjetas interactivas de servicios rápidos con micro-interacciones fluidas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiConsultaScreen(
    onNavigateToHistorial: () -> Unit,
    onNavigateToVideoconsulta: () -> Unit,
    onNavigateToClinicas: () -> Unit,
) {
    @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
    val isBaseline = BuildConfig.FLAVOR == "baseline"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.MedicalServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mi Consulta",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Portal Clínico Integrado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner de variante académica
            FlavorBanner(isBaseline = isBaseline)

            // Encabezado de bienvenida con avatar del paciente
            UserHeaderCard()

            // Lista de doctores seleccionables con fotos reales
            DoctoresSelectionCard(
                isBaseline = isBaseline,
                onNavigateToVideoconsulta = onNavigateToVideoconsulta
            )

            // Sección de Accesos Rápidos
            Text(
                text = "Servicios y Módulos de Atención",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )

            AccesoRapidoCard(
                titulo = "Historial Clínico",
                subtitulo = "220 consultas, diagnósticos y recetas médicas",
                badgeTexto = if (isBaseline) "Monolítico" else "Paging 3",
                icon = Icons.Filled.CalendarMonth,
                iconBackgroundColor = Color(0xFF0284C7).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF38BDF8),
                onClick = onNavigateToHistorial,
                contentDescription = "Ir a Historial Clínico"
            )

            AccesoRapidoCard(
                titulo = "Videoconsulta WebRTC",
                subtitulo = "Telemedicina en tiempo real (cámara, audio y loopback)",
                badgeTexto = if (isBaseline) "Main Thread" else "Asíncrono",
                icon = Icons.Filled.Videocam,
                iconBackgroundColor = Color(0xFF10B981).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF34D399),
                onClick = onNavigateToVideoconsulta,
                contentDescription = "Ir a Videoconsulta WebRTC"
            )

            AccesoRapidoCard(
                titulo = "Clínicas Cercanas y Mapa",
                subtitulo = "Mapa interactivo, geolocalización GPS y cálculo de distancias",
                badgeTexto = if (isBaseline) "2s High Accuracy" else "30s Balanced",
                icon = Icons.Filled.LocationOn,
                iconBackgroundColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF818CF8),
                onClick = onNavigateToClinicas,
                contentDescription = "Ir a Clínicas Cercanas"
            )

            // Banner informativo complementario
            EmergencyInfoCard()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Banner superior que identifica con estilo profesional la variante activa.
 */
@Composable
private fun FlavorBanner(isBaseline: Boolean) {
    val borderColor = if (isBaseline) Color(0xFFF59E0B) else Color(0xFF10B981)
    val bgColor = if (isBaseline) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFF064E3B).copy(alpha = 0.25f)
    val textColor = if (isBaseline) Color(0xFFFDE68A) else Color(0xFFA7F3D0)
    val tagText = if (isBaseline) "BASELINE DIDÁCTICO" else "OPTIMIZADO"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .semantics {
                contentDescription = "Variante activa: $tagText. " +
                        if (isBaseline) "Versión didáctica con problemas intencionales para medición de rendimiento."
                        else "Versión optimizada con mejoras de rendimiento aplicadas."
            },
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                    text = tagText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = if (isBaseline)
                        "Modo didáctico para evaluar impacto de recursos"
                    else
                        "Modo de alto rendimiento con optimizaciones activas",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Encabezado con saludo y avatar del usuario.
 */
@Composable
private fun UserHeaderCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "¡Hola, Renato!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "Cobertura Médica Activa · Fonasa / Isapre",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Avatar con iniciales
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "RN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

data class DoctorFicticio(
    val id: Int,
    val nombre: String,
    val especialidad: String,
    val disponible: Boolean,
    val horario: String?,
    val rating: String,
    val totalConsultas: Int
)

val listaDoctores = listOf(
    DoctorFicticio(1, "Dra. Isabel Fuentes", "Medicina General", true, "10:30 hrs", "4.9", 142),
    DoctorFicticio(2, "Dr. Carlos Medina", "Pediatría", true, "11:15 hrs", "4.8", 98),
    DoctorFicticio(3, "Dra. Laura Soto", "Dermatología", false, null, "5.0", 215),
    DoctorFicticio(4, "Dr. Andrés Morales", "Cardiología", true, "14:00 hrs", "4.9", 187),
    DoctorFicticio(5, "Dra. Camila Rojas", "Traumatología", true, "15:30 hrs", "4.8", 124),
    DoctorFicticio(6, "Dr. Felipe Silva", "Neurología", true, "17:00 hrs", "4.9", 156),
)

/**
 * Sección de selección de doctores con tarjetas dinámicas y fotografías reales.
 */
@Composable
private fun DoctoresSelectionCard(
    isBaseline: Boolean,
    onNavigateToVideoconsulta: () -> Unit
) {
    val context = LocalContext.current

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Especialistas Médicos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "${listaDoctores.count { it.disponible }} disponibles",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF10B981),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(listaDoctores) { doctor ->
                val imageRequest = DoctorImageHelper.buildDoctorImageRequest(
                    context = context,
                    medicoId = doctor.id,
                    isBaseline = isBaseline,
                    targetSizePx = 360
                )

                Card(
                    modifier = Modifier
                        .width(230.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable(enabled = doctor.disponible) {
                            if (doctor.disponible) onNavigateToVideoconsulta()
                        }
                        .semantics {
                            contentDescription = "Doctor ${doctor.nombre}, especialidad ${doctor.especialidad}, " +
                                    if (doctor.disponible) "Disponible" else "En consulta"
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Badge de disponibilidad superior
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (doctor.disponible) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFF78350F).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (doctor.disponible) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFF59E0B).copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = if (doctor.disponible) "● EN LÍNEA" else "● EN CONSULTA",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (doctor.disponible) Color(0xFF34D399) else Color(0xFFFDE68A),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            // Rating de estrellas
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = doctor.rating,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Avatar con fotografía real y respaldo offline
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = imageRequest,
                                    contentDescription = "Fotografía de ${doctor.nombre}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Image(
                                            painter = painterResource(id = DoctorImageHelper.getDoctorDrawableRes(doctor.id)),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    },
                                    error = {
                                        Image(
                                            painter = painterResource(id = DoctorImageHelper.getDoctorDrawableRes(doctor.id)),
                                            contentDescription = "Foto de ${doctor.nombre}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = doctor.nombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (doctor.disponible) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                    maxLines = 1
                                )
                                Text(
                                    text = doctor.especialidad,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Horario / Próximo cupo
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (doctor.disponible) "Hoy · ${doctor.horario}" else "Próximo cupo mañana",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (doctor.disponible) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onNavigateToVideoconsulta,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Atención Online", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva para cada acceso rápido con iconos temáticos y badges.
 */
@Composable
private fun AccesoRapidoCard(
    titulo: String,
    subtitulo: String,
    badgeTexto: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTintColor: Color,
    onClick: () -> Unit,
    contentDescription: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono en contenedor circular suave
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Título y subtítulo
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeTexto,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Flecha indicadora
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EmergencyInfoCard() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Servicio de Salud Conectado",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tus registros médicos, videoconsultas y mapa asistencial están sincronizados de forma segura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Mi Consulta — preview")
@Composable
private fun MiConsultaPreview() {
    EPE3_MovilesTheme {
        MiConsultaScreen(
            onNavigateToHistorial = {},
            onNavigateToVideoconsulta = {},
            onNavigateToClinicas = {},
        )
    }
}
