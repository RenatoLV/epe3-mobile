package com.example.epe3_moviles.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.epe3_moviles.BuildConfig
import com.example.epe3_moviles.webrtc.WebRtcSessionManager
import com.example.epe3_moviles.webrtc.WebRtcState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

data class Profesional(
    val id: Int,
    val nombre: String,
    val especialidad: String
)

val profesionalesDisponibles = listOf(
    Profesional(1, "Dr. Andrés Morales", "Cardiología"),
    Profesional(2, "Dra. Isabel Fuentes", "Medicina General"),
    Profesional(3, "Dr. Carlos Leiva", "Traumatología"),
    Profesional(4, "Dra. Valentina Ríos", "Dermatología"),
    Profesional(5, "Dr. Sebastián Torres", "Gastroenterología"),
    Profesional(6, "Dra. Camila Espinoza", "Oftalmología"),
    Profesional(7, "Dr. Felipe Navarro", "Neurología"),
    Profesional(8, "Dra. Patricia Vega", "Endocrinología")
)

/**
 * ViewModel que orquesta la sesión de Videoconsulta WebRTC en loopback.
 *
 * Implementa la separación arquitectónica entre variantes requerida por EPE3:
 *
 * BASELINE (Didáctico para evidencia de consumo de CPU e impacto en UI):
 * - Ejecuta o inicia la inicialización del pipeline WebRTC (EglBase, PeerConnectionFactory,
 *   capturador de video de cámara y pistas de audio) y la negociación SDP (createOffer,
 *   setLocalDescription, createAnswer) directamente en el HILO PRINCIPAL (Main Thread).
 * - Provoca bloqueos transitorios en el hilo de UI medibles en System Trace / CPU Profiler.
 *
 * OPTIMIZED:
 * - Traslada la inicialización pesada de códecs nativos y la negociación SDP a [Dispatchers.Default]
 *   mediante Coroutines de Kotlin.
 * - Mantiene el hilo principal completamente liberado y fluido, reportando transiciones de estado
 *   a la interfaz sin congelar fotogramas (0 jank frames inducidos por negociación).
 */
class VideoconsultaViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = WebRtcSessionManager.TAG

    @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
    val isBaseline: Boolean = BuildConfig.FLAVOR == "baseline"

    private val sessionManager = WebRtcSessionManager(application)

    private val _state = MutableStateFlow<WebRtcState>(WebRtcState.Idle)
    val state: StateFlow<WebRtcState> = _state.asStateFlow()

    private val _profesionales = MutableStateFlow(profesionalesDisponibles)
    val profesionales: StateFlow<List<Profesional>> = _profesionales.asStateFlow()

    private val _profesionalSeleccionado = MutableStateFlow(profesionalesDisponibles.first())
    val profesionalSeleccionado: StateFlow<Profesional> = _profesionalSeleccionado.asStateFlow()

    val eglContext get() = sessionManager.eglContext
    val videoTrack get() = sessionManager.videoTrack

    private var durationJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun seleccionarProfesional(profesional: Profesional) {
        _profesionalSeleccionado.value = profesional
    }

    private fun logTimestamp(msg: String) {
        val hora = timeFormat.format(Date())
        Log.d(tag, "[$hora] $msg")
    }

    /**
     * Inicia la solicitud de permisos o el arranque de la llamada si ya están concedidos.
     */
    fun solicitarInicioLlamada() {
        if ((_state.value is WebRtcState.Conectada) || (_state.value is WebRtcState.Inicializando)) {
            return
        }
        _state.value = WebRtcState.SolicitandoPermisos
        logTimestamp("Estado cambiado a: SolicitandoPermisos (CAMERA y RECORD_AUDIO)")
    }

    fun onPermisosDenegados() {
        logTimestamp("Permisos de cámara o micrófono denegados por el usuario.")
        _state.value = WebRtcState.Error(
            mensaje = "Se requieren permisos de cámara y micrófono para la videoconsulta.",
            esRecuperable = true,
        )
    }

    /**
     * Inicia la configuración de WebRTC y la llamada loopback según la variante activa.
     */
    fun onPermisosConcedidos() {
        if (isBaseline) {
            iniciarLlamadaBaseline()
        } else {
            iniciarLlamadaOptimized()
        }
    }

    /**
     * Variante BASELINE:
     * Ejecuta la inicialización de hardware y la negociación SDP en el hilo principal
     * para evidenciar el bloqueo de UI y el costo de CPU en System Trace.
     */
    private fun iniciarLlamadaBaseline() {
        logTimestamp("[BASELINE] Iniciando pipeline WebRTC en HILO PRINCIPAL (${Thread.currentThread().name}). Escenario didáctico.")
        _state.value = WebRtcState.Inicializando

        try {
            // Inicialización de PeerConnectionFactory y Capturador de Cámara en Main Thread
            val (videoActivo, infoCamara) = sessionManager.initializePipeline()
            logTimestamp("[BASELINE] Pipeline inicializado en Main Thread. Video activo: $videoActivo ($infoCamara)")

            // Negociación SDP e intercambio ICE loopback
            sessionManager.startLoopbackCall { nuevoEstado ->
                handleStateChange(nuevoEstado, videoActivo, infoCamara)
            }
        } catch (e: Exception) {
            logTimestamp("[BASELINE] Error al inicializar WebRTC en Main Thread: ${e.message}")
            _state.value = WebRtcState.Error("Error en baseline: ${e.message}")
        }
    }

    /**
     * Variante OPTIMIZED:
     * Traslada la inicialización y negociación costosa a un hilo de fondo (Dispatchers.Default),
     * notificando cambios de estado a la UI sin degradar la tasa de fotogramas.
     */
    private fun iniciarLlamadaOptimized() {
        logTimestamp("[OPTIMIZED] Despachando inicialización WebRTC a Dispatchers.Default...")
        _state.value = WebRtcState.Inicializando

        viewModelScope.launch(Dispatchers.Default) {
            val backgroundThreadName = Thread.currentThread().name
            logTimestamp("[OPTIMIZED] Ejecutando en hilo de fondo: $backgroundThreadName")

            try {
                val (videoActivo, infoCamara) = sessionManager.initializePipeline()
                logTimestamp("[OPTIMIZED] Pipeline inicializado exitosamente en $backgroundThreadName. Video activo: $videoActivo ($infoCamara)")

                sessionManager.startLoopbackCall { nuevoEstado ->
                    viewModelScope.launch(Dispatchers.Main) {
                        handleStateChange(nuevoEstado, videoActivo, infoCamara)
                    }
                }
            } catch (e: Exception) {
                logTimestamp("[OPTIMIZED] Error en hilo de fondo: ${e.message}")
                withContext(Dispatchers.Main) {
                    _state.value = WebRtcState.Error("Error en optimized: ${e.message}")
                }
            }
        }
    }

    private fun handleStateChange(
        nuevoEstado: WebRtcState,
        videoActivo: Boolean,
        infoCamara: String,
    ) {
        logTimestamp("Transición de estado WebRTC: ${nuevoEstado::class.simpleName} -> ${nuevoEstado.displayLabel}")

        if (nuevoEstado is WebRtcState.Conectada) {
            val estadoConectado = WebRtcState.Conectada(
                tiempoConectadaSegundos = 0L,
                videoActivo = videoActivo,
                audioActivo = true,
                infoCamara = infoCamara,
            )
            _state.value = estadoConectado
            iniciarTemporizadorLlamada(videoActivo, infoCamara)
        } else {
            if ((nuevoEstado is WebRtcState.Finalizada) || (nuevoEstado is WebRtcState.Error)) {
                detenerTemporizadorLlamada()
            }
            _state.value = nuevoEstado
        }
    }

    private fun iniciarTemporizadorLlamada(videoActivo: Boolean, infoCamara: String) {
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            var segundos = 0L
            while (isActive) {
                delay(1.seconds)
                segundos++
                (_state.value as? WebRtcState.Conectada)?.let { actual ->
                    _state.value = actual.copy(
                        tiempoConectadaSegundos = segundos,
                        videoActivo = videoActivo,
                        infoCamara = infoCamara,
                    )
                }
            }
        }
    }

    private fun detenerTemporizadorLlamada() {
        durationJob?.cancel()
        durationJob = null
    }

    /**
     * Finaliza la videoconsulta activa y cierra los canales loopback.
     */
    fun finalizarVideoconsulta() {
        logTimestamp("Usuario solicitó finalizar videoconsulta.")
        detenerTemporizadorLlamada()
        sessionManager.endCall("Finalizada por el usuario")
        _state.value = WebRtcState.Finalizada("Videoconsulta finalizada por el usuario")
    }

    /**
     * Reinicia al estado Idle después de una llamada finalizada o error.
     */
    fun reiniciarEstado() {
        detenerTemporizadorLlamada()
        _state.value = WebRtcState.Idle
    }

    /**
     * Libera todos los recursos de hardware y WebRTC.
     * Se invoca cuando el usuario sale de la pantalla o la actividad se destruye.
     */
    fun release() {
        detenerTemporizadorLlamada()
        sessionManager.release()
    }

    override fun onCleared() {
        super.onCleared()
        logTimestamp("VideoconsultaViewModel onCleared() -> liberando recursos...")
        release()
    }
}
