package com.example.epe3_moviles.location

import kotlin.math.*

/**
 * Utilidad matemática para el cálculo preciso de distancias geográficas mediante la fórmula de Haversine.
 * Permite ejecutar pruebas unitarias en la JVM sin dependencias de hardware ni mocks de Android Location.
 */
object DistanceCalculator {

    private const val RADIO_TIERRA_KM = 6371.0

    /**
     * Calcula la distancia geodésica en kilómetros entre dos coordenadas (latitud/longitud en grados decimales).
     */
    fun calcularDistanciaKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return RADIO_TIERRA_KM * c
    }

    /**
     * Retorna una representación textual legible de la distancia calculada (ej: "850 m" o "3.4 km").
     */
    fun formatearDistancia(distanciaKm: Double): String {
        return if (distanciaKm < 1.0) {
            val metros = (distanciaKm * 1000).roundToInt()
            "$metros m"
        } else {
            String.format(java.util.Locale.US, "%.1f km", distanciaKm)
        }
    }
}
