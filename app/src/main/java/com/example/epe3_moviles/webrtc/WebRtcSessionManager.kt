package com.example.epe3_moviles.webrtc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Administrador de sesión WebRTC local (Loopback) para la pantalla de Videoconsulta.
 *
 * Implementa dos instancias locales de [PeerConnection] interconectadas para realizar
 * una videoconsulta de prueba real (captura, codificación, negociación SDP, candidatos ICE,
 * y decodificación) sin requerir un servidor de señalización externo.
 *
 * Cumplimiento de requerimientos EPE3:
 * - PeerConnectionFactory real con códecs de hardware/software mediante EglBase.
 * - Capturador de cámara real (con fallback seguro en emulador si no hay hardware).
 * - Pistas locales de audio y video.
 * - Negociación SDP real (Offer/Answer) y pasaje de ICE candidates entre pares.
 * - Registro estructurado en Logcat con tag "EPE3_WebRTC" y timestamp HH:mm:ss.
 * - Liberación explícita de todos los recursos nativos en [release].
 */
class WebRtcSessionManager(private val context: Context) {

    companion object {
        const val TAG = "EPE3_WebRTC"
    }

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private fun logTimestamp(msg: String) {
        val hora = timeFormat.format(Date())
        Log.d(TAG, "[$hora] $msg")
    }

    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var localPeer: PeerConnection? = null
    private var remotePeer: PeerConnection? = null

    private var isCallActive = false
    private var isReleased = false

    // Colas de ICE candidates pendientes si la descripción remota aún no se ha establecido
    private val pendingLocalIceCandidates = CopyOnWriteArrayList<IceCandidate>()
    private val pendingRemoteIceCandidates = CopyOnWriteArrayList<IceCandidate>()

    /**
     * Inicializa el pipeline de WebRTC (PeerConnectionFactory, Audio, Video).
     *
     * @return Par con (videoActivo: Boolean, infoCamara: String)
     */
    fun initializePipeline(): Pair<Boolean, String> {
        if (peerConnectionFactory != null) {
            logTimestamp("Pipeline ya inicializado previamente.")
            return Pair(localVideoTrack != null, "Reutilizando inicialización")
        }

        logTimestamp("Inicializando PeerConnectionFactory y entorno EglBase...")
        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        val egl = EglBase.create()
        eglBase = egl

        val encoderFactory = DefaultVideoEncoderFactory(egl.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(egl.eglBaseContext)

        val factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
        peerConnectionFactory = factory

        // 1. Audio Track
        val audioConstraints = MediaConstraints()
        val aSource = factory.createAudioSource(audioConstraints)
        audioSource = aSource
        val aTrack = factory.createAudioTrack("ARDAMSa0", aSource)
        aTrack.setEnabled(true)
        localAudioTrack = aTrack
        logTimestamp("Pista de audio local creada con éxito.")

        // 2. Video Track con fallback para emuladores
        var videoActivo = false
        var infoCamara = "Sin cámara disponible"

        val capturer = createCameraCapturer()
        if (capturer != null) {
            videoCapturer = capturer
            try {
                val surfaceHelper = SurfaceTextureHelper.create("EPE3_CaptureThread", egl.eglBaseContext)
                surfaceTextureHelper = surfaceHelper
                val vSource = factory.createVideoSource(capturer.isScreencast)
                videoSource = vSource
                capturer.initialize(surfaceHelper, context, vSource.capturerObserver)
                capturer.startCapture(640, 480, 30)

                val vTrack = factory.createVideoTrack("ARDAMSv0", vSource)
                vTrack.setEnabled(true)
                localVideoTrack = vTrack
                videoActivo = true
                infoCamara = "Cámara activa (640x480 @ 30fps)"
                logTimestamp("Pista de video local iniciada exitosamente con capturador de cámara.")
            } catch (e: Exception) {
                logTimestamp("Aviso: Falló la inicialización de captura de video (${e.message}). Activando fallback de video dummy.")
                val dummySource = factory.createVideoSource(false)
                videoSource = dummySource
                val vTrack = factory.createVideoTrack("ARDAMSv0", dummySource)
                vTrack.setEnabled(true)
                localVideoTrack = vTrack
                videoActivo = true
                infoCamara = "Fallback video dummy (Emulador sin sensor físico)"
            }
        } else {
            logTimestamp("Aviso: No se detectó cámara física ni virtual. Creando fuente dummy para permitir negociación SDP.")
            val dummySource = factory.createVideoSource(false)
            videoSource = dummySource
            val vTrack = factory.createVideoTrack("ARDAMSv0", dummySource)
            vTrack.setEnabled(true)
            localVideoTrack = vTrack
            videoActivo = true
            infoCamara = "Fallback video dummy (Emulador)"
        }

        return Pair(videoActivo, infoCamara)
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }

        val deviceNames = enumerator.deviceNames
        // Priorizar cámara frontal
        for (name in deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                val capturer = enumerator.createCapturer(name, null)
                if (capturer != null) {
                    logTimestamp("Capturador seleccionado: Front-facing ($name)")
                    return capturer
                }
            }
        }
        // Fallback a cualquier cámara
        for (name in deviceNames) {
            val capturer = enumerator.createCapturer(name, null)
            if (capturer != null) {
                logTimestamp("Capturador seleccionado: Back/Other ($name)")
                return capturer
            }
        }
        return null
    }

    /**
     * Inicia la llamada en loopback negociando SDP y candidatos ICE entre dos PeerConnections locales.
     */
    fun startLoopbackCall(
        onStateChange: (WebRtcState) -> Unit
    ) {
        if (isCallActive) {
            logTimestamp("Llamada loopback ya activa.")
            return
        }
        isCallActive = true

        val factory = peerConnectionFactory
        if (factory == null) {
            logTimestamp("Error: PeerConnectionFactory no inicializado.")
            onStateChange(WebRtcState.Error("WebRTC no inicializado"))
            return
        }

        logTimestamp("Configurando PeerConnections para loopback local...")
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        // Listener para el peer local
        val localObserver = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                logTimestamp("ICE Candidate descubierto en Peer Local: ${candidate.sdpMid}")
                onStateChange(WebRtcState.IntercambiandoIce)
                remotePeer?.let { remote ->
                    if (remote.remoteDescription != null) {
                        remote.addIceCandidate(candidate)
                    } else {
                        pendingRemoteIceCandidates.add(candidate)
                    }
                }
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                logTimestamp("Peer Local ICE State cambio a: $newState")
                if (newState == PeerConnection.IceConnectionState.CONNECTED) {
                    onStateChange(WebRtcState.Conectada())
                } else if (newState == PeerConnection.IceConnectionState.FAILED) {
                    onStateChange(WebRtcState.Error("Falla de conexión ICE local"))
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {
                logTimestamp("Peer Local Signaling State: $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                logTimestamp("Peer Local ICE Gathering: $state")
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(dc: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {}
        }

        // Listener para el peer remoto
        val remoteObserver = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                logTimestamp("ICE Candidate descubierto en Peer Remoto: ${candidate.sdpMid}")
                localPeer?.let { local ->
                    if (local.remoteDescription != null) {
                        local.addIceCandidate(candidate)
                    } else {
                        pendingLocalIceCandidates.add(candidate)
                    }
                }
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                logTimestamp("Peer Remoto ICE State cambio a: $newState")
                if (newState == PeerConnection.IceConnectionState.CONNECTED) {
                    onStateChange(WebRtcState.Conectada())
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {
                logTimestamp("Peer Remoto Signaling State: $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                logTimestamp("Peer Remoto ICE Gathering: $state")
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(dc: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {}
        }

        val lp = factory.createPeerConnection(rtcConfig, localObserver)
        val rp = factory.createPeerConnection(rtcConfig, remoteObserver)

        if (lp == null || rp == null) {
            logTimestamp("Error creando instancias de PeerConnection.")
            onStateChange(WebRtcState.Error("No se pudo instanciar PeerConnection"))
            return
        }

        localPeer = lp
        remotePeer = rp

        // Agregar pistas de audio y video al peer local
        val streamIds = listOf("EPE3_Stream")
        localAudioTrack?.let { lp.addTrack(it, streamIds) }
        localVideoTrack?.let { lp.addTrack(it, streamIds) }

        // Agregar transceivers en peer remoto para recibir audio y video
        rp.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY))
        rp.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY))

        // Paso 1: Crear Oferta SDP
        logTimestamp("Iniciando creación de oferta SDP (createOffer)...")
        onStateChange(WebRtcState.CreandoOferta)

        val sdpConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        lp.createOffer(object : SimpleSdpObserver("localPeer.createOffer") {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) {
                    onStateChange(WebRtcState.Error("Oferta SDP nula"))
                    return
                }
                val offerSdp = desc
                logTimestamp("Oferta SDP creada exitosamente (${offerSdp.type}). Estableciendo localDescription en localPeer...")
                lp.setLocalDescription(object : SimpleSdpObserver("localPeer.setLocalDesc") {
                    override fun onSetSuccess() {
                        logTimestamp("LocalDescription establecida con éxito en localPeer. Estableciendo remoteDescription en remotePeer...")
                        rp.setRemoteDescription(object : SimpleSdpObserver("remotePeer.setRemoteDesc") {
                            override fun onSetSuccess() {
                                logTimestamp("RemoteDescription establecida con éxito en remotePeer. Drenando ICE pendientes...")
                                drainRemoteCandidates()

                                // Paso 2: Crear Respuesta SDP (createAnswer)
                                logTimestamp("Creando respuesta SDP (createAnswer) en remotePeer...")
                                rp.createAnswer(object : SimpleSdpObserver("remotePeer.createAnswer") {
                                    override fun onCreateSuccess(desc: SessionDescription?) {
                                        if (desc == null) {
                                            onStateChange(WebRtcState.Error("Respuesta SDP nula"))
                                            return
                                        }
                                        val answerSdp = desc
                                        logTimestamp("Respuesta SDP creada exitosamente (${answerSdp.type}).")
                                        rp.setLocalDescription(object : SimpleSdpObserver("remotePeer.setLocalDesc") {
                                            override fun onSetSuccess() {
                                                logTimestamp("LocalDescription establecida en remotePeer. Estableciendo remoteDescription en localPeer...")
                                                lp.setRemoteDescription(object : SimpleSdpObserver("localPeer.setRemoteDesc") {
                                                    override fun onSetSuccess() {
                                                        logTimestamp("RemoteDescription establecida en localPeer. Drenando ICE locales pendientes...")
                                                        drainLocalCandidates()
                                                        logTimestamp("Negociación SDP completada. Esperando estabilización ICE loopback...")
                                                    }

                                                    override fun onSetFailure(err: String?) {
                                                        logTimestamp("Falla al setear remoteDescription en localPeer: $err")
                                                        onStateChange(WebRtcState.Error("Falla SDP en localPeer: $err"))
                                                    }
                                                }, answerSdp)
                                            }

                                            override fun onSetFailure(err: String?) {
                                                logTimestamp("Error al setear localDescription en remotePeer: $err")
                                                onStateChange(WebRtcState.Error("Falla SDP en remotePeer: $err"))
                                            }
                                        }, answerSdp)
                                    }

                                    override fun onCreateFailure(err: String?) {
                                        logTimestamp("Error creando respuesta SDP: $err")
                                        onStateChange(WebRtcState.Error("Falla createAnswer: $err"))
                                    }
                                }, sdpConstraints)
                            }

                            override fun onSetFailure(err: String?) {
                                logTimestamp("Error al setear remoteDescription en remotePeer: $err")
                                onStateChange(WebRtcState.Error("Falla remoteDescription en remotePeer: $err"))
                            }
                        }, offerSdp)
                    }

                    override fun onSetFailure(err: String?) {
                        logTimestamp("Error al setear localDescription en localPeer: $err")
                        onStateChange(WebRtcState.Error("Falla localDescription en localPeer: $err"))
                    }
                }, offerSdp)
            }

            override fun onCreateFailure(err: String?) {
                logTimestamp("Error creando oferta SDP: $err")
                onStateChange(WebRtcState.Error("Error creando oferta SDP: $err"))
            }
        }, sdpConstraints)
    }

    private fun drainRemoteCandidates() {
        for (candidate in pendingRemoteIceCandidates) {
            remotePeer?.addIceCandidate(candidate)
        }
        pendingRemoteIceCandidates.clear()
    }

    private fun drainLocalCandidates() {
        for (candidate in pendingLocalIceCandidates) {
            localPeer?.addIceCandidate(candidate)
        }
        pendingLocalIceCandidates.clear()
    }

    /**
     * Finaliza la llamada y detiene el streaming loopback.
     */
    fun endCall(motivo: String = "Llamada finalizada por el usuario") {
        if (!isCallActive) return
        logTimestamp("Finalizando videoconsulta loopback: $motivo")
        isCallActive = false

        try {
            localPeer?.close()
            localPeer?.dispose()
            localPeer = null

            remotePeer?.close()
            remotePeer?.dispose()
            remotePeer = null

            pendingLocalIceCandidates.clear()
            pendingRemoteIceCandidates.clear()
            logTimestamp("PeerConnections cerrados y liberados exitosamente.")
        } catch (e: Exception) {
            logTimestamp("Excepción cerrando PeerConnections: ${e.message}")
        }
    }

    /**
     * Libera completamente todos los recursos de hardware y nativos de WebRTC.
     * Debe llamarse indefectiblemente al destruir el ViewModel o salir de la pantalla.
     */
    fun release() {
        if (isReleased) return
        isReleased = true
        logTimestamp("Liberando completamente todos los recursos WebRTC (cámara, audio, tracks, factory, eglBase)...")

        endCall("Liberación total de recursos")

        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null
            logTimestamp("VideoCapturer detenido y destruido.")
        } catch (e: Exception) {
            logTimestamp("Excepción liberando VideoCapturer: ${e.message}")
        }

        try {
            localVideoTrack?.dispose()
            localVideoTrack = null
            videoSource?.dispose()
            videoSource = null
            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null
            logTimestamp("Pista de video y recursos gráficos liberados.")
        } catch (e: Exception) {
            logTimestamp("Excepción liberando video track: ${e.message}")
        }

        try {
            localAudioTrack?.dispose()
            localAudioTrack = null
            audioSource?.dispose()
            audioSource = null
            logTimestamp("Pista de audio y AudioSource liberados.")
        } catch (e: Exception) {
            logTimestamp("Excepción liberando audio track: ${e.message}")
        }

        try {
            peerConnectionFactory?.dispose()
            peerConnectionFactory = null
            logTimestamp("PeerConnectionFactory destruido.")
        } catch (e: Exception) {
            logTimestamp("Excepción liberando PeerConnectionFactory: ${e.message}")
        }

        try {
            eglBase?.release()
            eglBase = null
            logTimestamp("EglBase liberado completamente.")
        } catch (e: Exception) {
            logTimestamp("Excepción liberando EglBase: ${e.message}")
        }

        logTimestamp("RECURSOS WEBRTC LIBERADOS AL 100%. Memoria nativa limpia.")
    }
}

/**
 * Clase base auxiliar para observar eventos SDP de WebRTC de forma concisa.
 */
private open class SimpleSdpObserver(
    private val observerName: String = "WebRtcSDP"
) : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(err: String?) {
        Log.e(WebRtcSessionManager.TAG, "[$observerName] onCreateFailure: $err")
    }
    override fun onSetFailure(err: String?) {
        Log.e(WebRtcSessionManager.TAG, "[$observerName] onSetFailure: $err")
    }
}
