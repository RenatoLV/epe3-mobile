package com.example.epe3_moviles.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.epe3_moviles.BuildConfig
import com.example.epe3_moviles.ui.theme.*

/**
 * Pantalla principal "Mi Consulta".
 *
 * Presenta un diseño clínico moderno inspirado en aplicaciones de salud líderes:
 * - Encabezado personalizado con avatar del paciente.
 * - Banner de variante académica con estética limpia y legible.
 * - Tarjeta destacada de próxima cita con indicadores de estado.
 * - Accesos rápidos en formato de tarjetas interactivas con iconografía distintiva.
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
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.MedicalServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Mi Consulta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
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

            // Lista de doctores seleccionables
            DoctoresSelectionCard(onNavigateToVideoconsulta = onNavigateToVideoconsulta)

            // Sección de Accesos Rápidos
            Text(
                text = "Servicios y Accesos Rápidos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )

            AccesoRapidoCard(
                titulo = "Historial Clínico",
                subtitulo = "220 consultas, diagnósticos y recetas",
                badgeTexto = if (isBaseline) "Monolítico" else "Paging 3",
                icon = Icons.Filled.CalendarMonth,
                iconBackgroundColor = Color(0xFF0284C7).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF38BDF8),
                onClick = onNavigateToHistorial,
                contentDescription = "Ir a Historial Clínico"
            )

            AccesoRapidoCard(
                titulo = "Videoconsulta WebRTC",
                subtitulo = "Conexión local en loopback (cámara y audio)",
                badgeTexto = if (isBaseline) "Main Thread" else "Asíncrono",
                icon = Icons.Filled.Videocam,
                iconBackgroundColor = Color(0xFF10B981).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF34D399),
                onClick = onNavigateToVideoconsulta,
                contentDescription = "Ir a Videoconsulta WebRTC"
            )

            AccesoRapidoCard(
                titulo = "Clínicas Cercanas",
                subtitulo = "Ubicación en tiempo real y distancias GPS",
                badgeTexto = if (isBaseline) "2s High Accuracy" else "30s Balanced",
                icon = Icons.Filled.LocationOn,
                iconBackgroundColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF818CF8),
                onClick = onNavigateToClinicas,
                contentDescription = "Ir a Clínicas Cercanas"
            )

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
            Text(
                text = "Portal de atención médica ambulatoria",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Avatar con iniciales
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
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
    val horario: String?
)

val listaDoctores = listOf(
    DoctorFicticio(1, "Dra. Isabel Fuentes", "Medicina General", true, "10:30 hrs"),
    DoctorFicticio(2, "Dr. Carlos Medina", "Pediatría", false, null),
    DoctorFicticio(3, "Dra. Laura Soto", "Dermatología", true, "14:15 hrs")
)

/**
 * Sección de selección de doctores con tarjetas dinámicas.
 */
@Composable
private fun DoctoresSelectionCard(onNavigateToVideoconsulta: () -> Unit) {
    Column {
        Text(
            text = "Especialistas Disponibles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(listaDoctores) { doctor ->
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable(enabled = doctor.disponible) {
                            if (doctor.disponible) onNavigateToVideoconsulta()
                        }
                        .semantics {
                            contentDescription = "Doctor ${doctor.nombre}, ${doctor.especialidad}, ${if(doctor.disponible) "Disponible" else "No disponible"}"
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (doctor.disponible) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (doctor.disponible) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (doctor.disponible) "● DISPONIBLE" else "● OCUPADO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (doctor.disponible) Color(0xFF34D399) else Color(0xFFF87171),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = if (doctor.disponible) 0.15f else 0.05f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (doctor.disponible) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = doctor.nombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (doctor.disponible) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                                Text(
                                    text = doctor.especialidad,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (doctor.disponible) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }

                        if (doctor.horario != null && doctor.disponible) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Hoy · ${doctor.horario}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
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
            .heightIn(min = 72.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(14.dp),
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
                    .size(46.dp)
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
                        shape = RoundedCornerShape(4.dp)
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
