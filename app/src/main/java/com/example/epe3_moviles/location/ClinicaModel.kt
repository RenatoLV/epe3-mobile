package com.example.epe3_moviles.location

/**
 * Modelo representativo de una clínica o centro de salud con coordenadas geográficas fijas.
 */
data class ClinicaModel(
    val id: Int,
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val especialidades: String,
    val abierta: Boolean,
    val latitud: Double,
    val longitud: Double,
)

/**
 * Red de clínicas y centros de salud ficticios ubicados en la Región Metropolitana de Santiago de Chile.
 * Permite calcular distancias geodésicas reales según el punto GPS obtenido en el emulador o dispositivo físico.
 */
val clinicasSantiagoFicticias = listOf(
    ClinicaModel(
        id = 1,
        nombre = "CESFAM Pedro Aguirre Cerda",
        direccion = "Av. Lo Encalada 1408, Ñuñoa",
        telefono = "+56 2 2524 0000",
        especialidades = "Medicina General · Pediatría",
        abierta = true,
        latitud = -33.4560,
        longitud = -70.6280,
    ),
    ClinicaModel(
        id = 2,
        nombre = "Clínica Dávila Recoleta",
        direccion = "Av. Recoleta 464, Recoleta",
        telefono = "+56 2 2730 8000",
        especialidades = "Urgencias · Cardiología · Traumatología",
        abierta = true,
        latitud = -33.4285,
        longitud = -70.6480,
    ),
    ClinicaModel(
        id = 3,
        nombre = "Clínica Santa María",
        direccion = "Av. Santa María 0410, Providencia",
        telefono = "+56 2 2913 0000",
        especialidades = "Oncología · Ginecología · Neurología",
        abierta = true,
        latitud = -33.4330,
        longitud = -70.6300,
    ),
    ClinicaModel(
        id = 4,
        nombre = "Hospital San Borja Arriarán",
        direccion = "Santa Rosa 1234, Santiago Centro",
        telefono = "+56 2 2574 9000",
        especialidades = "Medicina Interna · Cirugía Adultos",
        abierta = false,
        latitud = -33.4600,
        longitud = -70.6450,
    ),
    ClinicaModel(
        id = 5,
        nombre = "Hospital El Carmen",
        direccion = "Camino Rinconada 1201, Maipú",
        telefono = "+56 2 2573 0001",
        especialidades = "Urgencias · Maternidad",
        abierta = true,
        latitud = -33.5150,
        longitud = -70.7720,
    ),
    ClinicaModel(
        id = 6,
        nombre = "CESFAM Lo Barnechea",
        direccion = "Camino El Alba 11357, Lo Barnechea",
        telefono = "+56 2 2219 0700",
        especialidades = "Medicina General · Salud Mental",
        abierta = true,
        latitud = -33.3600,
        longitud = -70.5100,
    ),
)
