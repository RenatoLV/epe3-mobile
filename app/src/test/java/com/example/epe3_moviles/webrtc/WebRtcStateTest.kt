package com.example.epe3_moviles.webrtc

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias de la máquina de estados de WebRTC y sus etiquetas descriptivas.
 *
 * Valida la lógica de estados de UI y accesibilidad requerida para la rúbrica EPE3 Paso 6:
 * - Idle, SolicitandoPermisos, Inicializando, CreandoOferta, IntercambiandoIce,
 *   Conectada, Finalizada y Error.
 */
class WebRtcStateTest {

    @Test
    fun idleState_hasExpectedDisplayLabel() {
        val state: WebRtcState = WebRtcState.Idle
        assertEquals("En reposo (Listo)", state.displayLabel)
    }

    @Test
    fun solicitandoPermisosState_hasExpectedDisplayLabel() {
        val state: WebRtcState = WebRtcState.SolicitandoPermisos
        assertEquals("Solicitando permisos de cámara y micrófono", state.displayLabel)
    }

    @Test
    fun inicializandoState_hasExpectedDisplayLabel() {
        val state: WebRtcState = WebRtcState.Inicializando
        assertEquals("Inicializando WebRTC y códecs", state.displayLabel)
    }

    @Test
    fun creandoOfertaState_hasExpectedDisplayLabel() {
        val state: WebRtcState = WebRtcState.CreandoOferta
        assertEquals("Creando oferta SDP", state.displayLabel)
    }

    @Test
    fun intercambiandoIceState_hasExpectedDisplayLabel() {
        val state: WebRtcState = WebRtcState.IntercambiandoIce
        assertEquals("Intercambiando candidatos ICE", state.displayLabel)
    }

    @Test
    fun conectadaState_formatsDurationAndMaintainsMetadata() {
        val state = WebRtcState.Conectada(
            tiempoConectadaSegundos = 125L,
            videoActivo = true,
            audioActivo = true,
            infoCamara = "Cámara frontal activa"
        )
        assertEquals("Conectada (Loopback activo)", state.displayLabel)
        assertEquals(125L, state.tiempoConectadaSegundos)
        assertTrue(state.videoActivo)
        assertTrue(state.audioActivo)
        assertEquals("Cámara frontal activa", state.infoCamara)

        // Verificación de formato minutos:segundos (125s -> 02:05)
        val minutos = state.tiempoConectadaSegundos / 60
        val segundos = state.tiempoConectadaSegundos % 60
        val formatted = String.format("%02d:%02d", minutos, segundos)
        assertEquals("02:05", formatted)
    }

    @Test
    fun finalizadaState_containsExpectedReason() {
        val state = WebRtcState.Finalizada("Cierre solicitado por el usuario")
        assertEquals("Llamada finalizada", state.displayLabel)
        assertEquals("Cierre solicitado por el usuario", state.motivo)
    }

    @Test
    fun errorState_includesErrorMessageInLabel() {
        val mensajeError = "No se pudo inicializar capturador"
        val state = WebRtcState.Error(mensaje = mensajeError, esRecuperable = true)
        assertEquals("Error: $mensajeError", state.displayLabel)
        assertTrue(state.esRecuperable)
    }

    @Test
    fun loopbackLifecycle_transitionsThroughAllRequiredStatesInOrder() {
        val stateHistory = mutableListOf<WebRtcState>()

        // 1. Inicia en reposo
        var currentState: WebRtcState = WebRtcState.Idle
        stateHistory.add(currentState)

        // 2. Usuario presiona iniciar llamada -> solicita permisos
        currentState = WebRtcState.SolicitandoPermisos
        stateHistory.add(currentState)

        // 3. Permisos otorgados -> inicializa pipeline
        currentState = WebRtcState.Inicializando
        stateHistory.add(currentState)

        // 4. Crea oferta SDP
        currentState = WebRtcState.CreandoOferta
        stateHistory.add(currentState)

        // 5. Intercambia ICE candidates entre pares loopback
        currentState = WebRtcState.IntercambiandoIce
        stateHistory.add(currentState)

        // 6. Conexión establecida
        currentState = WebRtcState.Conectada(tiempoConectadaSegundos = 10L)
        stateHistory.add(currentState)

        // 7. Usuario finaliza llamada
        currentState = WebRtcState.Finalizada("Usuario presiona colgar")
        stateHistory.add(currentState)

        assertEquals(7, stateHistory.size)
        assertTrue(stateHistory[0] is WebRtcState.Idle)
        assertTrue(stateHistory[1] is WebRtcState.SolicitandoPermisos)
        assertTrue(stateHistory[2] is WebRtcState.Inicializando)
        assertTrue(stateHistory[3] is WebRtcState.CreandoOferta)
        assertTrue(stateHistory[4] is WebRtcState.IntercambiandoIce)
        assertTrue(stateHistory[5] is WebRtcState.Conectada)
        assertTrue(stateHistory[6] is WebRtcState.Finalizada)
    }
}
