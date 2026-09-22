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
| **4** | Room + Paging 3 con 200+ registros e índices | 🔲 Pendiente |
| **5** | Clínicas Cercanas con permisos GPS y ciclo de vida | 🔲 Pendiente |
| **6** | Videoconsulta con WebRTC real (negociación SDP) | 🔲 Pendiente |

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

