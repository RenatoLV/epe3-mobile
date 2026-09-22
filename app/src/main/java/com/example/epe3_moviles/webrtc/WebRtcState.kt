package com.example.epe3_moviles.webrtc

/**
 * Estados accesibles del flujo de videoconsulta WebRTC en loopback.
 *
 * Estados requeridos por la rúbrica EPE3:
 * - Idle: En reposo, listo para iniciar.
 * - SolicitandoPermisos: Solicitando permisos CAMERA y RECORD_AUDIO en runtime.
 * - Inicializando: Configurando PeerConnectionFactory, codecs y pistas multimedia locales.
 * - CreandoOferta: Generando SDP Offer local y configurando descripciones.
 * - IntercambiandoIce: Negociando candidatos ICE entre peer emisor y peer receptor loopback.
 * - Conectada: Ambos pares en estado conectado con streaming activo de audio/video.
 * - Finalizada: Sesión cerrada y recursos de hardware liberados.
 * - Error: Falla en permisos, inicialización, negociación o captura.
 */
sealed interface WebRtcState {
    data object Idle : WebRtcState
    data object SolicitandoPermisos : WebRtcState
    data object Inicializando : WebRtcState
    data object CreandoOferta : WebRtcState
    data object IntercambiandoIce : WebRtcState
    data class Conectada(
        val tiempoConectadaSegundos: Long = 0L,
        val videoActivo: Boolean = true,
        val audioActivo: Boolean = true,
        val infoCamara: String = "Cámara activa"
    ) : WebRtcState
    data class Finalizada(val motivo: String = "Videoconsulta finalizada por el usuario") : WebRtcState
    data class Error(val mensaje: String, val esRecuperable: Boolean = false) : WebRtcState

    /**
     * Etiqueta amigable y accesible para lectores de pantalla y chips de estado.
     */
    val displayLabel: String
        get() = when (this) {
            is Idle -> "En reposo (Listo)"
            is SolicitandoPermisos -> "Solicitando permisos de cámara y micrófono"
            is Inicializando -> "Inicializando WebRTC y códecs"
            is CreandoOferta -> "Creando oferta SDP"
            is IntercambiandoIce -> "Intercambiando candidatos ICE"
            is Conectada -> "Conectada (Loopback activo)"
            is Finalizada -> "Llamada finalizada"
            is Error -> "Error: $mensaje"
        }
}
