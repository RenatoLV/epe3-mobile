package com.example.epe3_moviles.config

import android.os.Build
import com.example.epe3_moviles.BuildConfig

/**
 * Configuración centralizada de red y URLs de imágenes para pruebas de rendimiento.
 *
 * Cumple con el requisito técnico:
 * "Las URLs de imagen deben configurarse en un único archivo."
 *
 * Detección automática de entorno:
 * - Emulador Android (AVD): Enruta automáticamente a "http://10.0.2.2:8085".
 * - Teléfono físico conectado por USB (ADB reverse): Enruta a "http://127.0.0.1:8085".
 *   (Garantiza que el Network Inspector capture las 8 solicitudes HTTP sin depender de Wi-Fi ni bloqueos de firewall).
 * - Teléfono en red Wi-Fi: Permite cambiar a "http://192.168.96.156:8085".
 */
object NetworkConfig {

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    private val defaultUrl: String
        get() = if (isEmulator()) {
            "http://10.0.2.2:8085"
        } else {
            // Teléfono físico conectado por USB: 127.0.0.1 enruta a través de adb reverse tcp:8085 tcp:8085
            "http://127.0.0.1:8085"
        }

    private var _customBaseUrl: String? = null

    /**
     * URL base del servidor HTTP local de pruebas.
     * Por defecto usa 10.0.2.2 en emulador y 127.0.0.1 en teléfono físico (con adb reverse).
     */
    var BASE_URL: String
        get() = _customBaseUrl ?: defaultUrl
        set(value) {
            _customBaseUrl = value
        }

    /**
     * Retorna la URL remota para la fotografía de perfil del médico según la variante:
     * - BASELINE (didáctico): URL de imagen original sin comprimir (~2.4 MB c/u en /images/original/).
     * - OPTIMIZED: URL de imagen WebP de 480 px (~100-180 KB c/u en /images/optimized/).
     *
     * @param medicoId Identificador numérico del médico (1 a 8).
     */
    fun getFotoMedicoUrl(medicoId: Int): String {
        val safeId = if (medicoId in 1..8) medicoId else ((Math.abs(medicoId) % 8) + 1)
        val isBaseline = BuildConfig.FLAVOR == "baseline"
        return if (isBaseline) {
            "$BASE_URL/images/original/medico_$safeId.jpg"
        } else {
            "$BASE_URL/images/optimized/medico_$safeId.webp"
        }
    }
}
