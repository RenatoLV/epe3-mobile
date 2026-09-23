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
    ClinicaModel(
        id = 7,
        nombre = "Hospital San Pablo",
        direccion = "Av. Videla 499, Coquimbo",
        telefono = "+56 51 233 6000",
        especialidades = "Urgencias · Cirugía · Maternidad",
        abierta = true,
        latitud = -29.9533,
        longitud = -71.3395,
    ),
    ClinicaModel(
        id = 8,
        nombre = "Hospital San Juan de Dios",
        direccion = "Av. Balmaceda 916, La Serena",
        telefono = "+56 51 233 3000",
        especialidades = "Oncología · Traumatología",
        abierta = true,
        latitud = -29.9045,
        longitud = -71.2489,
    ),
    ClinicaModel(
        id = 9,
        nombre = "CESFAM San Juan",
        direccion = "San Juan s/n, Coquimbo",
        telefono = "+56 51 231 0000",
        especialidades = "Medicina General · Pediatría",
        abierta = true,
        latitud = -29.9711,
        longitud = -71.3283,
    ),
    ClinicaModel(
        id = 10,
        nombre = "Sede Médica Random Coquimbo",
        direccion = "Aldunate 123, Coquimbo",
        telefono = "+56 51 299 8877",
        especialidades = "Consulta General · Exámenes",
        abierta = true,
        latitud = -29.9575,
        longitud = -71.3361,
    ),
    ClinicaModel(
        id = 11,
        nombre = "Sede Médica Random La Serena",
        direccion = "Av. Francisco de Aguirre 400, La Serena",
        telefono = "+56 51 288 7766",
        especialidades = "Kinesiología · Traumatología",
        abierta = true,
        latitud = -29.9027,
        longitud = -71.2519,
    )
)
