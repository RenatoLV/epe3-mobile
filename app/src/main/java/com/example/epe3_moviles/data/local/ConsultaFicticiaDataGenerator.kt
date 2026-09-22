package com.example.epe3_moviles.data.local

/**
 * Generador de datos reproducibles para pruebas de rendimiento en Room y Paging 3.
 *
 * Cumple con el requisito:
 * "Más de 200 consultas ficticias reproducibles.
 * La misma información funcional debe estar disponible en ambas variantes."
 *
 * Genera exactamente 220 registros clínicos ficticios estructurados con 8 médicos,
 * diagnósticos realistas, fechas cronológicas y tratamientos.
 */
object ConsultaFicticiaDataGenerator {

    data class MedicoInfo(
        val id: Int,
        val nombre: String,
        val especialidad: String
    )

    private val medicos = listOf(
        MedicoInfo(1, "Dr. Andrés Morales", "Cardiología"),
        MedicoInfo(2, "Dra. Isabel Fuentes", "Medicina General"),
        MedicoInfo(3, "Dr. Carlos Leiva", "Traumatología"),
        MedicoInfo(4, "Dra. Valentina Ríos", "Dermatología"),
        MedicoInfo(5, "Dr. Sebastián Torres", "Gastroenterología"),
        MedicoInfo(6, "Dra. Camila Espinoza", "Oftalmología"),
        MedicoInfo(7, "Dr. Felipe Navarro", "Neurología"),
        MedicoInfo(8, "Dra. Patricia Vega", "Endocrinología")
    )

    private val diagnosticosPorEspecialidad = mapOf(
        "Cardiología" to listOf(
            "Hipertensión arterial estadio 1: control periódico" to "Enalapril 10mg cada 12 hrs, reducción de sodio",
            "Evaluación de soplo funcional asintomático" to "Ecocardiograma Doppler de control en 6 meses",
            "Arritmia extrasistólica supraventricular aislada" to "Monitoreo Holter 24h, evitar cafeína",
            "Control post-revascularización miocárdica favorable" to "Aspirina 100mg/día, atorvastatina 40mg",
            "Dislipidemia mixta con riesgo cardiovascular moderado" to "Dieta mediterránea, rosuvastatina 10mg"
        ),
        "Medicina General" to listOf(
            "Síndrome gripal estacional afebril" to "Paracetamol 500mg c/8h SOS, hidratación abundante",
            "Chequeo preventivo anual: parámetros dentro de límites" to "Mantener actividad física 150 min/semana",
            "Rinosinusitis aguda bacteriana leve" to "Amoxicilina 875mg c/12h por 7 días",
            "Lumbago mecánico no irradiado" to "Ketorolaco 10mg c/8h por 3 días, calor local",
            "Infección urinaria baja no complicada" to "Nitrofurantoína 100mg c/8h por 5 días"
        ),
        "Traumatología" to listOf(
            "Tendinitis del manguito rotador derecho" to "Kinesioterapia 10 sesiones, reposo articular",
            "Esguince de tobillo grado 1 por inversión" to "Inmovilización elástica, hielo local 15 min",
            "Gonalgia mecánica bilateral asociada a sobrepeso" to "Ejercicios de bajo impacto, paracetamol SOS",
            "Fascitis plantar matutina bilateral" to "Elongación matutina, taloneras de silicona",
            "Control post fractura de radio distal: consolidada" to "Alta de inmovilización, fortalecimiento progresivo"
        ),
        "Dermatología" to listOf(
            "Dermatitis atópica en pliegues flexurales" to "Crema humectante con ceramidas, hidrocortisona 1%",
            "Acné vulgar grado 2 inflamatorio" to "Gel de peróxido de benzoilo 2.5%, protector solar",
            "Psoriasis en placas en codos y rodillas" to "Corticoides tópicos de alta potencia, calcipotriol",
            "Revisión de nevus melanocíticos: benignos" to "Seguimiento fotográfico anual, fotoprotección FPS 50+",
            "Pitiriasis versicolor en tronco superior" to "Ketoconazol shampoo 2% tópico por 14 días"
        ),
        "Gastroenterología" to listOf(
            "Enfermedad por reflujo gastroesofágico (ERGE) erosiva" to "Esomeprazol 40mg en ayunas, elevar cabecera",
            "Síndrome de intestino irritable con constipación" to "Aporte de fibra soluble, trimebutina 200mg SOS",
            "Gastritis crónica antral superficial" to "Famotidina 20mg nocturna, dieta libre de irritantes",
            "Intolerancia secundaria a la lactosa" to "Enzima lactasa preingesta lácteos, leche deslactosada",
            "Esteatosis hepática metabólica no alcohólica leve" to "Plan nutricional hipocalórico, ejercicio aeróbico"
        ),
        "Oftalmología" to listOf(
            "Vicio de refracción: presbicia incipiente" to "Receta de lentes de lectura +1.25 dioptrías",
            "Síndrome de ojo seco evaporativo leve" to "Lágrimas artificiales sin preservantes 4 v/día",
            "Astigmatismo miópico estable bilateral" to "Renovación de cristales ópticos antirreflejo",
            "Sospecha de glaucoma: PIO 16 mmHg normal" to "Curva de tensión ambulatoria, campo visual 24-2",
            "Conjuntivitis alérgica bilateral estacional" to "Olopatadina colirio 0.1% 1 gota c/12h"
        ),
        "Neurología" to listOf(
            "Cefalea tensional episódica recurrente" to "Ibuprofeno 400mg SOS, pausas activas laborales",
            "Migraña episódica sin aura controlada" to "Naratriptán 2.5mg en fase inicial, diario de cefaleas",
            "Insomnio de conciliación transitorio" to "Higiene del sueño, melatonina 3mg 30 min antes",
            "Parestesias distales en extremidades superiores" to "Electromiografía solicitada, complejo B",
            "Crisis vertiginosa postural benigna resuelta" to "Maniobra de Epley realizada, ejercicios en casa"
        ),
        "Endocrinología" to listOf(
            "Hipotiroidismo primario en rango eutiroideo" to "Levotiroxina 75 mcg ayuno estricto, TSH en 3 meses",
            "Diabetes mellitus tipo 2 bien compensada" to "Metformina 850mg c/12h con comidas, HbA1c 6.4%",
            "Resistencia a la insulina con acantosis nigricans" to "Dieta de bajo índice glicémico, metformina 500mg",
            "Déficit moderado de vitamina D" to "Colecalciferol 50.000 UI mensual por 3 meses",
            "Nódulo tiroideo solitario benigno TIRADS 2" to "Ecografía tiroidea de seguimiento en 12 meses"
        )
    )

    private val meses = listOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic"
    )

    /**
     * Genera exactamente 220 consultas médicas ficticias reproducibles.
     * Los timestamps decrecen desde el 25 de septiembre de 2026 hacia atrás (~4 días por consulta).
     */
    fun generateConsultas(count: Int = 220): List<ConsultaEntity> {
        val baseTimestamp = 1790342400000L // ~25 de septiembre de 2026
        val cuatroDiasMillis = 4L * 24 * 60 * 60 * 1000L

        return (0 until count).map { index ->
            val timestamp = baseTimestamp - (index * cuatroDiasMillis)
            val medico = medicos[index % medicos.size]
            val opcionesDiagnostico = diagnosticosPorEspecialidad[medico.especialidad] ?: listOf(
                "Consulta de control general" to "Continuar tratamiento habitual"
            )
            val (diag, trat) = opcionesDiagnostico[(index / medicos.size) % opcionesDiagnostico.size]

            // Calcular fecha legible
            val dia = 1 + ((index * 3) % 28)
            val mes = meses[(index + 8) % 12]
            val anio = 2026 - (index / 90)
            val fechaTexto = "$dia $mes $anio"

            ConsultaEntity(
                id = index + 1,
                fecha = timestamp,
                fechaTexto = fechaTexto,
                medicoId = medico.id,
                medicoNombre = medico.nombre,
                especialidad = medico.especialidad,
                diagnostico = diag,
                tratamiento = trat,
                pacienteNombre = "Renato Alvarez"
            )
        }
    }
}
