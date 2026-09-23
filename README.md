# EPE3 Móviles — Plataforma de Consultas Médicas Remotas

Aplicación Android desarrollada con **Kotlin y Jetpack Compose** para el curso
Programación de Aplicaciones Móviles II (IPCHILE). Demuestra análisis y
optimización de rendimiento con evidencias reales obtenidas desde Android Studio.

---

## Estado actual

| Fase | Descripción | Estado |
| :--- | :--- | :--- |
| **1** | Interfaz base con navegación entre 4 pantallas | ✅ Completada |
| **2** | Variantes `baseline` y `optimized` (productFlavors) | ✅ Completada |
| **3** | Historial Clínico con imágenes HTTP reales + servidor Node | ✅ Completada |
| **4** | Room + Paging 3 con 200+ registros e índices | ✅ Completada |
| **6** | Videoconsulta con WebRTC real (negociación SDP loopback) | ✅ Completada |

---

## Requisitos del sistema

| Herramienta | Versión verificada |
| :--- | :--- |
| Android Studio | 2025.3.4 (AI-253.32098.37.2534.15232325) |
| JDK (JBR integrado) | 21.0.10 JetBrains |
| Kotlin | 2.2.10 |
| Gradle | 9.4.1 |
| Android Gradle Plugin | 9.2.1 |
| Compose BOM | **2025.12.01** (el BOM 2026.x requiere compileSdk 37, no instalado) |
| Navigation Compose | **2.8.9** (2.10.x requiere compileSdk 37) |
| lifecycle-runtime-ktx | **2.9.1** |
| activity-compose | **1.10.0** |
| material-icons-extended | gestionado por BOM 2025.12.01 |
| compileSdk | 36.1 (plataforma instalada) |
| targetSdk | 36 |
| minSdk | 24 (Android 7.0) |
| Node.js (Fases 3+) | v24.x |

> **Nota de compatibilidad**: El BOM `2026.02.01` original del proyecto jala
> `lifecycle 2.11.0` y `navigation-compose 2.10.1`, ambos con requerimiento
> de `compileSdk 37`. Como solo está instalada la plataforma `android-36.1`,
> se usó el BOM `2025.12.01` que es totalmente funcional con SDK 36.
> Para subir al BOM 2026.x, instala `android-37` desde Android Studio →
> SDK Manager → Platforms.

---

## Fase 1: Compilar y ejecutar

### Opción A — Android Studio (recomendado)

1. Abre Android Studio `2025.3.4`.
2. Selecciona **File → Open** y elige la carpeta `EPE3_Moviles`.
3. Espera a que Gradle sincronice las dependencias (primera vez: ~2 min).
4. Abre **Device Manager** (`View → Tool Windows → Device Manager`) y
   verifica que tengas un AVD o conecta un dispositivo físico.
5. Haz clic en el botón **▶ Run 'app'** (Shift+F10).
6. La pantalla inicial **"Mi Consulta"** aparecerá con los tres botones de acceso.

### Opción B — Línea de comandos

```powershell
# Desde la raíz del proyecto
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

El APK queda en:
```
app\build\outputs\apk\debug\app-debug.apk
```

Para instalarlo en un dispositivo/emulador conectado por ADB:
```powershell
$adb = 'C:\Users\U-1133\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install app\build\outputs\apk\debug\app-debug.apk
```

---

## Navegación de la aplicación (Fase 1)

```
Mi Consulta (inicio)
   ├── Historial Clínico   → lista de 8 consultas ficticias + placeholder de foto
   ├── Videoconsulta       → estructura de la pantalla (WebRTC en Fase 6)
   └── Clínicas cercanas   → lista de 5 clínicas ficticias (GPS en Fase 5)
```

Cada pantalla secundaria tiene un botón ← que regresa a **Mi Consulta**.

---

## Nota sobre datos ficticios

Todos los datos mostrados (nombre del paciente, médicos, fechas, diagnósticos,
clínicas y distancias) son **ficticios** y se identifican explícitamente con
la etiqueta `[DATOS FICTICIOS]` en la interfaz. No provienen de ningún sistema
de salud real.

---

## Accesibilidad (Fase 1)

- Objetivos táctiles ≥ 56 dp en botones principales.
- `contentDescription` en todos los elementos interactivos para TalkBack.
- Colores con contraste ≥ 4.5:1 mediante tokens de Material 3.
- Datos de carga y estado anunciados con chip de estado.

Para verificar con TalkBack:
1. En el emulador: **Settings → Accessibility → TalkBack → On**.
2. Navega con gestos de deslizamiento; el lector anunciará cada elemento.

---

## Archivos modificados en Fase 1

| Archivo | Cambio |
| :--- | :--- |
| `gradle/libs.versions.toml` | Versiones compatibles con SDK 36.1 |
| `app/build.gradle.kts` | `navigation-compose` + `material-icons-extended` |
| `app/src/main/java/.../MainActivity.kt` | Reemplaza "Hello Android" por `AppNavHost` |
| `app/src/main/java/.../ui/navigation/Screen.kt` | [NUEVO] Rutas de navegación |
| `app/src/main/java/.../ui/navigation/AppNavHost.kt` | [NUEVO] Grafo NavHost |
| `app/src/main/java/.../ui/screens/MiConsultaScreen.kt` | [NUEVO] Pantalla principal |
| `app/src/main/java/.../ui/screens/HistorialClinicoScreen.kt` | [NUEVO] Historial |
| `app/src/main/java/.../ui/screens/VideoconsultaScreen.kt` | [NUEVO] Videoconsulta |
| `app/src/main/java/.../ui/screens/ClinicasCercanasScreen.kt` | [NUEVO] Clínicas |

---

## Fase 2: Variantes baseline y optimized

### Identificadores de cada variante

| Variante | applicationId | APK |
| :--- | :--- | :--- |
| **baseline** | `com.example.epe3_moviles.baseline` | `app-baseline-debug.apk` |
| **optimized** | `com.example.epe3_moviles.optimized` | `app-optimized-debug.apk` |

Ambas variantes son **instalables simultáneamente** en el mismo dispositivo porque tienen `applicationId` distinto.

### Compilar ambas variantes

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'

# Compilar solo baseline
.\gradlew.bat assembleBaselineDebug

# Compilar solo optimized
.\gradlew.bat assembleOptimizedDebug

# Compilar ambas de una vez
.\gradlew.bat assembleBaselineDebug assembleOptimizedDebug
```

### Rutas de los APK generados

```
app\build\outputs\apk\baseline\debug\app-baseline-debug.apk
app\build\outputs\apk\optimized\debug\app-optimized-debug.apk
```

### Instalar en dispositivo/emulador conectado por ADB

```powershell
$adb = 'C:\Users\U-1133\AppData\Local\Android\Sdk\platform-tools\adb.exe'

# Instalar baseline (se muestra como "Mi Consulta Baseline")
& $adb install app\build\outputs\apk\baseline\debug\app-baseline-debug.apk

# Instalar optimized (se muestra como "Mi Consulta Optimized")
& $adb install app\build\outputs\apk\optimized\debug\app-optimized-debug.apk

# En el emulador ambas apps aparecen con íconos separados en el launcher.
```

### Seleccionar variante desde Android Studio

1. En Android Studio: **Build → Select Build Variant** (o panel lateral **Build Variants**).
2. Elige `baselineDebug` o `optimizedDebug` en la columna **Active Build Variant**.
3. Presiona ▶ **Run** — se instala esa variante en el dispositivo seleccionado.

### Qué muestra cada variante

Ambas variantes tienen la **misma navegación e interfaz** en este paso.
La diferencia visible es el **banner en la pantalla "Mi Consulta"**:

| Variante | Banner | Color |
| :--- | :--- | :--- |
| `baseline` | `⚗️ BASELINE DIDÁCTICO` + descripción del propósito | Amarillo |
| `optimized` | `✅ OPTIMIZED` + descripción del propósito | Verde |

El banner tiene `contentDescription` completo para TalkBack y declara
explícitamente que es una variante académica de comparación, no una app
en producción.

### Archivos modificados en Fase 2

| Archivo | Cambio |
| :--- | :--- |
| `app/build.gradle.kts` | `flavorDimensions`, `productFlavors`, `buildConfig = true` |
| `ui/screens/MiConsultaScreen.kt` | `FlavorBanner` con `BuildConfig.FLAVOR` |

---

## Fase 3 y 4: Persistencia con Room y Paging 3

### Arquitectura de Datos

- **Entidad:** `ConsultaEntity` con índice explícito en la columna `fecha` (`@Index(value = ["fecha"])`) para optimizar ordenamientos cronológicos inversos.
- **DAO:** `ConsultaDao` con:
  * `insertAll(consultas: List<ConsultaEntity>)` para inserción masiva reproducible.
  * `getAllConsultas(): List<ConsultaEntity>` (Baseline didáctico: carga los 220 registros completos en memoria).
  * `getPagingConsultas(): PagingSource<Int, ConsultaEntity>` (Optimized: Paging 3 en páginas de 20 elementos bajo demanda).
- **Conjunto de datos reproducible:** `ConsultaFicticiaDataGenerator` genera exactamente **220 consultas clínicas ficticias** con diagnósticos y tratamientos realistas distribuidos entre 8 médicos.
- **Base de datos:** `AppDatabase` (SQLite/Room) con pre-poblado automático de las 220 consultas.
- **Imágenes HTTP:** Cada consulta enlaza con `NetworkConfig.getFotoMedicoUrl(medicoId)` servida por `scripts/image_server.js` (fotos originales en baseline, WebP en optimized).
  Para ejecutar el servidor:
  ```powershell
  node scripts/image_server.js
  node scripts/image_server.js 8091
  node scripts/image_server.js --port 8091
  ```

### Comparación Medible entre Variantes

| Característica | Baseline (Didáctico) | Optimized |
| :--- | :--- | :--- |
| **Estrategia de carga BD** | Monolítica (`getAllConsultas`) | Paginada con Paging 3 (`getPagingConsultas`) |
| **Registros en memoria** | 220 entidades instanciadas a la vez | 20 entidades por página activa |
| **Índices en BD** | Presente en tabla | Presente y aprovechado por el cursor de Paging |
| **Imágenes HTTP** | Originales (~2.4 MB) sin caché | WebP 480px (~150 KB) con caché |
| **Cálculo de tiempo** | Mide y muestra tiempo de consulta completo en UI | Carga instantánea de página visible |

---

## Fase 5: Clínicas Cercanas y Gestión de Energía GPS

### Características Técnicas

- **Biblioteca:** Google Play Services Location (`play-services-location:21.3.0`).
- **Permisos:** `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` solicitados en tiempo de ejecución en Compose con `rememberLauncherForActivityResult`.
- **Cálculo Geodésico:** Fórmula matemática de Haversine (`DistanceCalculator`) para calcular la distancia real a 6 clínicas de Santiago de Chile sin depender de servicios de terceros.
- **Ciclo de vida estricto:** `DisposableEffect` con `onDispose` para garantizar que el sensor GPS se apague al salir de la pantalla y nunca consuma batería en segundo plano.

### Comparativa de Comportamiento

| Parámetro | Baseline (Didáctico) | Optimized |
| :--- | :--- | :--- |
| **Prioridad GPS** | `PRIORITY_HIGH_ACCURACY` (hardware continuo) | `PRIORITY_BALANCED_POWER_ACCURACY` (bajo impacto) |
| **Intervalo de sondeo** | **2 segundos** (2.000 ms) | **30 segundos** (30.000 ms) |
| **Impacto en Energy Profiler** | Nivel constante "Medium/High" por sensor GPS activo | Nivel "Light" con pulsos aislados cada 30s |
| **Filtro Logcat** | `tag:EPE3_Location` muestra logs cada 2s | `tag:EPE3_Location` muestra logs cada 30s |
| **Liberación de sensor** | Log al presionar botón atrás | Log en `onDispose` del Composable |

---

## Fase 6: Videoconsulta con WebRTC Real en Loopback y Evaluación de CPU

### Características Técnicas

- **Biblioteca:** Stream WebRTC Android (`io.getstream:stream-webrtc-android:1.3.10`), compilada y verificada contra Kotlin 2.2.10, AGP 9.2.1 y compileSdk 36.1.
- **Permisos:** `CAMERA` y `RECORD_AUDIO` solicitados en runtime antes de iniciar la llamada.
- **Loopback Local:** Dos instancias de `PeerConnection` (`localPeer` y `remotePeer`) interconectadas localmente sin necesidad de servidor de señalización externo.
- **Pipeline Multimedia:** `PeerConnectionFactory`, `EglBase`, `DefaultVideoEncoderFactory`, `DefaultVideoDecoderFactory`, capturador de cámara real (con fallback automático a video dummy en emuladores sin hardware de cámara) y pista de audio local.
- **Negociación SDP Real:** `createOffer`, `setLocalDescription`, `setRemoteDescription`, `createAnswer`, intercambio bidireccional de `IceCandidate`.
- **Máquina de Estados Accesible:** `Idle`, `SolicitandoPermisos`, `Inicializando`, `CreandoOferta`, `IntercambiandoIce`, `Conectada`, `Finalizada`, `Error`.
- **Botones Accesibles:** "Iniciar videoconsulta" y "Finalizar videoconsulta".
- **Liberación Estricta:** `DisposableEffect` y `onCleared()` liberan al 100% capturadores, pistas de video/audio, PeerConnections, fábrica y contexto OpenGL.

### Comparativa de Concurrencia entre Variantes

| Aspecto | Baseline (Didáctico) | Optimized |
| :--- | :--- | :--- |
| **Hilo de inicialización y negociación** | **Hilo Principal (Main Thread)** | **Dispatchers.Default** (Pool de subprocesos) |
| **Impacto en UI (Jank / Congelamiento)** | Bloqueo observable de fotogramas (Jank) medible en System Trace | 0 fotogramas perdidos por negociación; interfaz totalmente fluida |
| **Carga de CPU** | Pico concentrado en el hilo de renderizado | Carga distribuida eficientemente en hilos de trabajo |
| **Filtro Logcat** | `tag:EPE3_WebRTC` | `tag:EPE3_WebRTC` |
| **Guía de medición** | Ver [GUIA_CPU_WEBRTC.md](GUIA_CPU_WEBRTC.md) | Ver [GUIA_CPU_WEBRTC.md](GUIA_CPU_WEBRTC.md) |



