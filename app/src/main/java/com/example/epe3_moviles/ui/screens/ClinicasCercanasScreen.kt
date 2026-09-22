package com.example.epe3_moviles.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.epe3_moviles.ui.theme.EPE3_MovilesTheme

/**
 * Modelo de clínica cercana ficticia.
 * En Fase 5 se calculará la distancia real desde la ubicación del usuario.
 */
data class ClinicaFicticia(
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val distanciaKm: String,
    val abierta: Boolean,
    val especialidades: String
)

/** Clínicas ficticias de Santiago de Chile — solo para estructura de la UI. */
private val clinicasFicticias = listOf(
    ClinicaFicticia(
        "CESFAM Pedro Aguirre Cerda",
        "Av. Lo Encalada 1408, Ñuñoa",
        "+56 2 2524 0000",
        "1.2 km",
        true,
        "Medicina General · Pediatría"
    ),
    ClinicaFicticia(
        "Clínica Dávila",
        "Recoleta 464, Recoleta",
        "+56 2 2730 8000",
        "3.5 km",
        true,
        "Urgencias · Cardiología · Traumatología"
    ),
    ClinicaFicticia(
        "Clínica Santa María",
        "Av. Santa María 0410, Providencia",
        "+56 2 2913 0000",
        "4.8 km",
        true,
        "Oncología · Ginecología · Neurología"
    ),
    ClinicaFicticia(
        "Hospital El Carmen",
        "Camino Rinconada 1201, Maipú",
        "+56 2 2573 0001",
        "7.1 km",
        false,
        "Cirugía · Maternidad"
    ),
    ClinicaFicticia(
        "CESFAM Lo Barnechea",
        "Camino El Alba 11357, Lo Barnechea",
        "+56 2 2219 0700",
        "9.3 km",
        true,
        "Medicina General · Salud Mental"
    )
)

/**
 * Pantalla Clínicas Cercanas.
 *
 * FASE 1: Muestra una lista estática de clínicas ficticias con su
 * información básica. Las distancias son textos fijos y no provienen
 * de una posición GPS real.
 *
 * FASE 5: Se solicitará permiso de ubicación en tiempo de ejecución,
 * se calculará la distancia real con FusedLocationProviderClient y se
 * registrará el intervalo de actualización en Logcat. La variante
 * baseline usará alta precisión cada 2 s; la optimized, precisión
 * balanceada cada 30 s con cancelación al salir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicasCercanasScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clínicas cercanas") },
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
            // Aviso de datos ficticios y GPS
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Datos ficticios · Distancias estimadas · " +
                           "GPS real se implementa en Fase 5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(clinicasFicticias) { clinica ->
                    ClinicaCard(clinica = clinica)
                }
            }
        }
    }
}

/**
 * Tarjeta de clínica con nombre, dirección, teléfono, distancia
 * y estado de atención (abierta / cerrada).
 * El chip de estado usa colores semánticos accesibles.
 */
@Composable
private fun ClinicaCard(clinica: ClinicaFicticia) {
    val estadoTexto = if (clinica.abierta) "Abierta" else "Cerrada"
    val descripcionAccesible =
        "${clinica.nombre}, ${estadoTexto}, a ${clinica.distanciaKm}. " +
        "Dirección: ${clinica.direccion}. Teléfono: ${clinica.telefono}. " +
        "Especialidades: ${clinica.especialidades}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = descripcionAccesible },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = clinica.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                // Chip de estado accesible
                Surface(
                    color = if (clinica.abierta)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = estadoTexto,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (clinica.abierta)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dirección
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = clinica.direccion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Teléfono
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = clinica.telefono,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Distancia y especialidades
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📍 ${clinica.distanciaKm}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = clinica.especialidades,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Clínicas Cercanas")
@Composable
fun ClinicasCercanasScreenPreview() {
    EPE3_MovilesTheme {
        ClinicasCercanasScreen(onBack = {})
    }
}
