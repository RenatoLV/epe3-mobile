package com.example.epe3_moviles.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para [DistanceCalculator].
 * Valida la exactitud matemática de la fórmula de Haversine y el formateo accesible de distancias.
 */
class DistanceCalculatorTest {

    @Test
    fun distanciaMismoPunto_esCero() {
        val dist = DistanceCalculator.calcularDistanciaKm(-33.4569, -70.6483, -33.4569, -70.6483)
        assertEquals(0.0, dist, 0.0001)
    }

    @Test
    fun distanciaSimetrica_esIgualEnAmbosSentidos() {
        val latA = -33.4372
        val lonA = -70.6506 // Plaza de Armas, Santiago
        val latB = -33.4262
        val lonB = -70.6124 // Providencia, Santiago

        val distAB = DistanceCalculator.calcularDistanciaKm(latA, lonA, latB, lonB)
        val distBA = DistanceCalculator.calcularDistanciaKm(latB, lonB, latA, lonA)

        assertEquals("La distancia debe ser simétrica", distAB, distBA, 0.0001)
        assertTrue("La distancia real debe estar entre 3.5 y 4.0 km", distAB in 3.5..4.0)
    }

    @Test
    fun formatearDistancia_muestraMetrosCuandoEsMenorAUnKilometro() {
        val formato = DistanceCalculator.formatearDistancia(0.450)
        assertEquals("450 m", formato)
    }

    @Test
    fun formatearDistancia_muestraKilometrosConUnDecimalCuandoEsMayorAUnKilometro() {
        val formato = DistanceCalculator.formatearDistancia(3.742)
        assertEquals("3.7 km", formato)
    }
}
