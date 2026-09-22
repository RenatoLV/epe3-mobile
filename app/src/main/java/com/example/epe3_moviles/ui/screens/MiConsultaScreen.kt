package com.example.epe3_moviles.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.epe3_moviles.BuildConfig
import com.example.epe3_moviles.ui.theme.EPE3_MovilesTheme

/**
 * Pantalla principal "Mi Consulta".
 *
 * Muestra la próxima cita médica y tres accesos rápidos a las otras secciones.
 * En la parte superior incluye un banner que identifica claramente la variante
 * de compilación activa:
 *  - BASELINE DIDÁCTICO: versión destinada a medir problemas de rendimiento.
 *  - OPTIMIZED: versión con las optimizaciones implementadas.
 *
 * Esta distinción es para uso académico y de comparación; ninguna variante
 * representa una aplicación de salud en producción.
 *
 * Accesibilidad:
 * - Objetivos táctiles ≥ 56 dp.
 * - contentDescription en cada botón para TalkBack.
 * - El banner de variante tiene su propio contentDescription completo.
 * - Contraste ≥ 4.5:1 mediante tokens de Material 3.
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
                    Text(
                        text = "Mi Consulta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Banner de variante — visible y accesible, identifica el propósito
            FlavorBanner(isBaseline = isBaseline)

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bienvenida
                Text(
                    text = "Hola, Renato",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Tarjeta de próxima cita
                ProximaCitaCard()

                // Sección de accesos rápidos
                Text(
                    text = "Accesos rápidos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                AccesoRapidoButton(
                    label = "Historial Clínico",
                    description = "Ir a Historial Clínico",
                    icon = Icons.Filled.DateRange,
                    onClick = onNavigateToHistorial
                )

                AccesoRapidoButton(
                    label = "Videoconsulta",
                    description = "Ir a Videoconsulta",
                    icon = Icons.Filled.VideoCall,
                    onClick = onNavigateToVideoconsulta
                )

                AccesoRapidoButton(
                    label = "Clínicas cercanas",
                    description = "Ir a Clínicas cercanas",
                    icon = Icons.Filled.LocationOn,
                    onClick = onNavigateToClinicas
                )
            }
        }
    }
}

/**
 * Banner visible en la pantalla principal que identifica la variante activa.
 *
 * BASELINE DIDÁCTICO: variante diseñada para exhibir problemas de rendimiento
 * medibles (mayor consumo de CPU, memoria, red o batería). No representa un
 * error real descubierto en producción; es un escenario controlado de
 * comparación académica.
 *
 * OPTIMIZED: variante con las optimizaciones implementadas. Muestra el mismo
 * trabajo funcional con mejor eficiencia de recursos.
 *
 * El banner tiene contentDescription completo para que TalkBack lo anuncie
 * al navegar a esta pantalla.
 */
@Composable
private fun FlavorBanner(
    isBaseline: Boolean,
) {
    val label: String
    val description: String
    val backgroundColor: Color
    val contentColor: Color
    val emoji: String

    if (isBaseline) {
        label = "BASELINE DIDÁCTICO"
        description = "Variante BASELINE DIDÁCTICO activa. " +
            "Esta versión está diseñada para medir cuellos de botella de " +
            "rendimiento en un ejercicio académico de comparación. " +
            "No representa una aplicación en producción."
        backgroundColor = Color(0xFFFFF3CD)   // amarillo suave — contraste OK sobre texto oscuro
        contentColor = Color(0xFF664D00)
        emoji = "⚗️"
    } else {
        label = "OPTIMIZED"
        description = "Variante OPTIMIZED activa. " +
            "Esta versión aplica las técnicas de optimización del ejercicio académico " +
            "para comparar su rendimiento frente a la variante baseline. " +
            "No representa una aplicación en producción."
        backgroundColor = Color(0xFFD4EDDA)   // verde suave — contraste OK sobre texto oscuro
        contentColor = Color(0xFF155724)
        emoji = "✅"
    }

    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyLarge
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = if (isBaseline)
                        "Versión con problemas intencionales para medición académica"
                    else
                        "Versión con optimizaciones aplicadas para comparación académica",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
        }
    }
}

/**
 * Tarjeta que muestra la próxima cita médica ficticia.
 */
@Composable
private fun ProximaCitaCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Próxima cita: Dra. Isabel Fuentes, Medicina General, " +
                    "lunes 29 de septiembre de 2026 a las 10:30. Datos ficticios de prueba."
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Próxima cita  ·  [DATOS FICTICIOS]",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Dra. Isabel Fuentes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Medicina General",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "📅  Lunes 29 sep 2026  ·  10:30",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Botón de acceso rápido con ícono, etiqueta y descripción para TalkBack.
 * Alto mínimo de 56 dp (requisito WCAG 2.5.5).
 */
@Composable
private fun AccesoRapidoButton(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Mi Consulta — preview (baseline)")
@Composable
private fun MiConsultaBaselinePreview() {
    EPE3_MovilesTheme {
        // La preview no puede leer BuildConfig.FLAVOR en tiempo de diseño,
        // así que llamamos directamente al composable con parámetros vacíos.
        // El banner usa el valor real de BuildConfig en el dispositivo.
        MiConsultaScreen(
            onNavigateToHistorial = {},
            onNavigateToVideoconsulta = {},
            onNavigateToClinicas = {},
        )
    }
}
