# Guía de Medición de CPU y System Trace: Videoconsulta WebRTC

Esta guía detalla el procedimiento experimental para medir el impacto de la inicialización, codificación y negociación WebRTC sobre el consumo de CPU y la fluidez del hilo principal en las variantes **baseline** y **optimized** de **Mi Consulta**.

---

## 1. Contexto y Justificación del Experimento

El protocolo WebRTC demanda recursos significativos del dispositivo móvil:
1. **Inicialización de códecs nativos:** Creación del contexto gráfico (`EglBase`), enumeración de códecs de hardware (H.264/VP8/VP9) y configuración del capturador de cámara.
2. **Negociación SDP:** Generación de la oferta (`createOffer`), parsing sintáctico SDP y serialización de descripciones de sesión local y remota (`setLocalDescription`, `setRemoteDescription`).
3. **Recolección e intercambio de ICE:** Detección de puertos locales e intercambio de candidatos de transporte.
4. **Pipeline continuo de medios:** Captura de fotogramas, escalado, codificación y reproducción de audio/video.

### Comparación entre Variantes

| Aspecto | Variante Baseline | Variante Optimized |
| :--- | :--- | :--- |
| **Despacho de Tareas** | Hilo Principal (`Main Thread`) | Hilo de Trabajo (`Dispatchers.Default`) |
| **Inicialización de Códecs** | Bloquea el hilo de renderizado Compose | Asíncrono en grupo de subprocesos |
| **Negociación SDP** | Síncrona/Directa en UI Thread | Coroutines en segundo plano |
| **Impacto en UI** | Congelamiento observable (Jank / Dropped Frames) | Interfaz fluida a 60 fps con indicadores de progreso |
| **Liberación de Recursos** | Limpieza total en salida | Limpieza total en salida |

> [!NOTE]
> La videoconsulta implementa un **Loopback Local** auténtico: dos instancias de `PeerConnection` en el mismo dispositivo se conectan entre sí mediante un intercambio de SDP y candidatos ICE reales, sin necesidad de un servidor de señalización externo.

---

## 2. Requisitos y Preparación

1. **Dispositivo o Emulador:**
   - Emulador Android (ej. Pixel 6 con API 34) o dispositivo físico Android 8.0+.
   - Si se usa emulador sin webcam en el equipo anfitrión, la aplicación cuenta con un **mecanismo de fallback seguro** que genera una pista de video dummy sin capturador físico para garantizar que la negociación SDP y el pipeline WebRTC se completen con éxito sin crashear.
2. **Permisos en Runtime:**
   - La aplicación solicitará automáticamente permisos de `android.permission.CAMERA` y `android.permission.RECORD_AUDIO`. Conceder ambos permisos al iniciar.
3. **Android Studio Profiler:**
   - Asegurarse de tener abierta la pestaña **Profiler** (`View > Tool Windows > Profiler`).

---

## 3. Procedimiento Experimental de Medición (3 Repeticiones)

Para cada variante (`baseline` y `optimized`), ejecutar el siguiente protocolo **tres veces** (Repetición 1, 2 y 3) partiendo de la pantalla inicial:

### Paso 1: Iniciar la sesión de profiling
1. En Android Studio, desplegar la variante correspondiente (`baselineDebug` u `optimizedDebug`).
2. Abrir la pestaña **Profiler** y seleccionar el proceso `com.example.epe3_moviles.baseline` (o `.optimized`).
3. Hacer clic en la fila de **CPU**.
4. Seleccionar el modo de captura:
   - **System Trace** (Recomendado para observar jank frames, interacción del hilo principal y bloqueo de Choreographer).
   - O bien **Java/Kotlin Method Trace** (para observar la duración exacta de métodos WebRTC).

### Paso 2: Ejecutar la videoconsulta
1. Iniciar la grabación en el Profiler (**Record**).
2. En la aplicación, navegar a **Videoconsulta**.
3. Presionar el botón accesible **"Iniciar videoconsulta"**.
4. Observar las transiciones de estado:
   - `Solicitando permisos`
   - `Inicializando WebRTC y códecs`
   - `Creando oferta SDP`
   - `Intercambiando candidatos ICE`
   - `Conectada (Loopback activo)`
5. Mantener la llamada activa durante **15 a 30 segundos** en estado `Conectada` para registrar el consumo sostenido de CPU del pipeline de medios.
6. Presionar el botón accesible **"Finalizar videoconsulta"**.
7. Salir de la pantalla con el botón "Volver".

### Paso 3: Detener la captura y analizar métricas
1. Detener la grabación en el Profiler (**Stop Recording**).
2. Extraer los siguientes valores:
   - **CPU Peak (%):** Pico máximo de CPU alcanzado durante la fase de inicialización y negociación.
   - **Main Thread Block Time (ms):** Tiempo acumulado de bloqueo del hilo principal durante la inicialización de códecs y SDP.
   - **Jank Frames:** Fotogramas de UI perdidos o retrasados durante el arranque de la llamada.
3. Repetir el proceso dos veces más para registrar `Repeticion_1`, `Repeticion_2` y `Repeticion_3`.
4. Calcular el promedio y volcarlo en `RESULTADOS_METRICAS.csv`.

---

## 4. Inspección de Tiempos y Eventos en Logcat

Para validar el flujo real y la duración de cada fase sin depender exclusivamente del Profiler, filtrar en Logcat por la etiqueta estructurada:

```text
tag:EPE3_WebRTC
```

### Ejemplo de traza en variante BASELINE:
```text
D/EPE3_WebRTC: [10:15:00.120] Estado cambiado a: SolicitandoPermisos (CAMERA y RECORD_AUDIO)
D/EPE3_WebRTC: [10:15:01.050] [BASELINE] Iniciando pipeline WebRTC en HILO PRINCIPAL (main). Escenario didáctico.
D/EPE3_WebRTC: [10:15:01.052] Inicializando PeerConnectionFactory y entorno EglBase...
D/EPE3_WebRTC: [10:15:01.320] Pista de audio local creada con éxito.
D/EPE3_WebRTC: [10:15:01.410] Pista de video local iniciada exitosamente con capturador de cámara.
D/EPE3_WebRTC: [10:15:01.415] Configurando PeerConnections para loopback local...
D/EPE3_WebRTC: [10:15:01.480] Iniciando creación de oferta SDP (createOffer)...
D/EPE3_WebRTC: [10:15:01.520] Oferta SDP creada exitosamente (offer). Estableciendo localDescription en localPeer...
D/EPE3_WebRTC: [10:15:01.590] RemoteDescription establecida con éxito en remotePeer. Drenando ICE pendientes...
D/EPE3_WebRTC: [10:15:01.630] Creando respuesta SDP (createAnswer) en remotePeer...
D/EPE3_WebRTC: [10:15:01.680] Respuesta SDP creada exitosamente (answer).
D/EPE3_WebRTC: [10:15:01.750] Negociación SDP completada. Esperando estabilización ICE loopback...
D/EPE3_WebRTC: [10:15:01.780] ICE Candidate descubierto en Peer Local: audio
D/EPE3_WebRTC: [10:15:01.790] ICE Candidate descubierto en Peer Remoto: video
D/EPE3_WebRTC: [10:15:01.810] Peer Local ICE State cambio a: CONNECTED
D/EPE3_WebRTC: [10:15:01.812] Transición de estado WebRTC: Conectada -> Conectada (Loopback activo)
```

### Ejemplo de traza en variante OPTIMIZED:
```text
D/EPE3_WebRTC: [10:16:00.100] [OPTIMIZED] Despachando inicialización WebRTC a Dispatchers.Default...
D/EPE3_WebRTC: [10:16:00.105] [OPTIMIZED] Ejecutando en hilo de fondo: DefaultDispatcher-worker-1
D/EPE3_WebRTC: [10:16:00.350] [OPTIMIZED] Pipeline inicializado exitosamente en DefaultDispatcher-worker-1.
...
```

### Verificación de Liberación Total de Recursos
Al presionar "Finalizar videoconsulta" o retroceder de la pantalla, Logcat debe registrar:
```text
D/EPE3_WebRTC: [10:16:30.500] Liberando completamente todos los recursos WebRTC (cámara, audio, tracks, factory, eglBase)...
D/EPE3_WebRTC: [10:16:30.520] VideoCapturer detenido y destruido.
D/EPE3_WebRTC: [10:16:30.535] Pista de video y recursos gráficos liberados.
D/EPE3_WebRTC: [10:16:30.540] Pista de audio y AudioSource liberados.
D/EPE3_WebRTC: [10:16:30.550] PeerConnectionFactory destruido.
D/EPE3_WebRTC: [10:16:30.560] EglBase liberado completamente.
D/EPE3_WebRTC: [10:16:30.562] RECURSOS WEBRTC LIBERADOS AL 100%. Memoria nativa limpia.
```

---

## 5. Limitaciones del Emulador y Consideraciones de Hardware

1. **Cámara Virtual vs Cámara Física:**
   - En emuladores de Android Studio, la cámara puede proyectar un patrón de ajedrez móvil (`VirtualScene`) o usar la webcam del equipo.
   - Si el emulador no dispone de backend de video emulado, la app conmuta automáticamente a la fuente dummy de video sin detener la negociación SDP.
2. **Aceleración de Códecs:**
   - En emuladores x86_64, la codificación suele ejecutarse mediante códecs de software (VP8/VP9 CPU software codecs), lo que genera un uso de CPU proporcionalmente más alto que en dispositivos físicos con códecs dedicados por hardware (H.264 MediaCodec).
3. **Sin carga artificial:**
   - La aplicación **no** utiliza `Thread.sleep()` ni bucles sintéticos de cálculo hash. Toda la carga de CPU y la latencia observadas provienen exclusivamente de la pila WebRTC nativa (`libjingle_peerconnection_so.so`).
