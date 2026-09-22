package com.example.epe3_moviles.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para el generador reproducible de consultas médicas.
 * Verifica la integridad del conjunto de datos de más de 200 registros.
 */
class ConsultaDataGeneratorTest {

    @Test
    fun generador_creaMasDe200Consultas() {
        val consultas = ConsultaFicticiaDataGenerator.generateConsultas(220)
        assertTrue("Debe generar más de 200 registros", consultas.size > 200)
        assertEquals(220, consultas.size)
    }

    @Test
    fun generador_asignaIdsUnicos() {
        val consultas = ConsultaFicticiaDataGenerator.generateConsultas(220)
        val idsUnicos = consultas.map { it.id }.toSet()
        assertEquals("Todos los IDs deben ser únicos", consultas.size, idsUnicos.size)
    }

    @Test
    fun generador_mantieneMedicosEnRangoValido() {
        val consultas = ConsultaFicticiaDataGenerator.generateConsultas(220)
        consultas.forEach { consulta ->
            assertTrue(
                "El medicoId debe estar entre 1 y 8 para coincidir con las imágenes",
                consulta.medicoId in 1..8
            )
            assertTrue("El nombre del médico no debe estar vacío", consulta.medicoNombre.isNotBlank())
            assertTrue("La especialidad no debe estar vacía", consulta.especialidad.isNotBlank())
            assertTrue("El diagnóstico no debe estar vacío", consulta.diagnostico.isNotBlank())
            assertTrue("El tratamiento no debe estar vacío", consulta.tratamiento.isNotBlank())
        }
    }

    @Test
    fun generador_ordenaCronologicamenteDescendente() {
        val consultas = ConsultaFicticiaDataGenerator.generateConsultas(220)
        for (i in 0 until consultas.size - 1) {
            assertTrue(
                "La consulta en posición $i debe tener fecha mayor o igual a la posición ${i + 1}",
                consultas[i].fecha >= consultas[i + 1].fecha
            )
        }
    }
}
