package com.example.epe3_moviles.config

import com.example.epe3_moviles.BuildConfig

/**
 * Configuración centralizada de red y URLs de imágenes para pruebas de rendimiento.
 *
 * Cumple con el requisito técnico:
 * "Las URLs de imagen deben configurarse en un único archivo."
 *
 * Configuración de hosts para pruebas:
 * - Emulador Android (AVD): usa "http://10.0.2.2:8080" (enruta automáticamente al localhost de la máquina host).
 * - Teléfono físico conectado a la misma red Wi-Fi: cambiar BASE_URL por la IP local de tu PC,
 *   por ejemplo: "http://192.168.1.150:8080".
 */
object NetworkConfig {
    /**
     * URL base del servidor HTTP local de pruebas.
     * Por defecto usa puerto 8085 (o 8080 según el puerto donde inicies scripts/image_server.js).
     * En teléfono físico, reemplazar por tu IP LAN (ej. "http://192.168.96.156:8085").
     */
    var BASE_URL: String = "http://10.0.2.2:8085"

    /**
     * Retorna la URL remota para la fotografía de perfil del médico según la variante:
     * - BASELINE (didáctico): URL de imagen original sin comprimir (~2.4 MB c/u en /images/original/).
     * - OPTIMIZED: URL de imagen WebP de 480 px (~100-180 KB c/u en /images/optimized/).
     *
     * @param medicoId Identificador numérico del médico (1 a 8).
     */
    fun getFotoMedicoUrl(medicoId: Int): String {
        val isBaseline = BuildConfig.FLAVOR == "baseline"
        return if (isBaseline) {
            "$BASE_URL/images/original/medico_$medicoId.jpg"
        } else {
            "$BASE_URL/images/optimized/medico_$medicoId.webp"
        }
    }
}
