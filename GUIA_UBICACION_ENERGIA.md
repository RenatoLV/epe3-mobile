# Guía de Medición: Geolocalización y Consumo de Energía (Paso 5)

Esta guía explica el procedimiento paso a paso para medir y comparar con **evidencias reales**
el comportamiento del sensor de ubicación y su impacto energético entre la variante didáctica
`baseline` y la variante `optimized` en la pantalla **Clínicas cercanas**.

---

## 1. Fundamento Técnico de la Comparativa

| Parámetro | Variante BASELINE (Didáctica) | Variante OPTIMIZED |
| :--- | :--- | :--- |
| **Prioridad de ubicación** | `Priority.PRIORITY_HIGH_ACCURACY` (GPS satelital activo) | `Priority.PRIORITY_BALANCED_POWER_ACCURACY` (Red / Wi-Fi) |
| **Intervalo solicitado** | **2 segundos** (2.000 ms) | **30 segundos** (30.000 ms) |
| **Intervalo mínimo solicitado** | 2 segundos | 30 segundos |
| **Intervalo observado** | Variable según hardware, Doze mode y latencia satelital | Variable según disponibilidad de antenas/red y políticas del SO |
| **Ciclo de vida en pantalla** | Activo mientras la pantalla esté visible | Activo mediante `DisposableEffect` (liberado en `onDispose`) |
| **Comportamiento al salir** | Se detiene explícitamente con log de confirmación | Se detiene en `onDispose` con log de confirmación |
| **Segundo plano** | **Nunca** (ninguna variante mantiene GPS en background) | **Nunca** |

> [!IMPORTANT]
> **Comportamiento real del sistema operativo Android:**
> Las solicitudes de ubicación a través de `FusedLocationProviderClient` son directrices para el sistema operativo, **no garantías de reloj estricto**. El sistema Android puede **limitar (throttling), retrasar o agrupar (batching)** las actualizaciones de ubicación dependiendo de:
> - El estado de batería del dispositivo y modos de ahorro de energía.
> - El tiempo necesario para la adquisición inicial de satélites (TTFF - Time to First Fix) en emulador o dispositivo real.
> - Las restricciones de Doze Mode y políticas del gestor de localización de Google Play Services.
> Por lo tanto, siempre se debe distinguir entre el **intervalo solicitado** (2s en baseline, 30s en optimized) y el **intervalo observado** en los registros de Logcat.

---

## 2. Preparación de la Ubicación en el Emulador

Para simular coordenadas y calcular distancias respecto a las clínicas ficticias de Santiago:
1. En el emulador, abre el panel lateral de opciones avanzadas (tres puntos `...` **Extended controls**).
2. Ve a la pestaña **Location**.
3. Ingresa coordenadas de Santiago Centro:
   * **Latitude:** `-33.4560`
   * **Longitude:** `-70.6480`
4. Haz clic en **Save Point** y presiona **Send**.
5. Las clínicas calcularán automáticamente la distancia geodésica real (fórmula de Haversine) respecto a este punto recibido.

---

## 3. Protocolo de Captura con Logcat (Evidencia Primaria de Frecuencia)

Logcat es la **evidencia concluyente y reproducible** para verificar la tasa de muestreo y la liberación del sensor.

### Filtrado en Logcat

- **Desde Android Studio:**
  En la pestaña inferior **Logcat**, introduce en el cuadro de búsqueda:
  ```text
  tag:EPE3_Location
  ```
- **Desde la terminal (CLI):**
  ```powershell
  $adb = 'C:\Users\U-1133\AppData\Local\Android\Sdk\platform-tools\adb.exe'
  & $adb logcat -v time -s EPE3_Location
  ```

---

### Prueba 1: Medición en BASELINE (Intervalo solicitado: 2s)

1. En el panel **Build Variants**, selecciona `baselineDebug` y ejecuta la app.
2. Abre la sección "Clínicas cercanas" y acepta los permisos de ubicación.
3. Observa los registros en Logcat. Cada línea reportará el intervalo solicitado y la estampa de tiempo real:
   ```text
   [BASELINE] Solicitud GPS INICIADA - Prioridad: PRIORITY_HIGH_ACCURACY, Intervalo: 2s
   [BASELINE] Actualización GPS #1 - Prioridad: PRIORITY_HIGH_ACCURACY, Intervalo solicitado: 2s, Hora: 13:10:02, Lat: -33.456, Lon: -70.648, Precisión: 5.0m
   [BASELINE] Actualización GPS #2 - Prioridad: PRIORITY_HIGH_ACCURACY, Intervalo solicitado: 2s, Hora: 13:10:04, Lat: -33.456, Lon: -70.648, Precisión: 5.0m
   [BASELINE] Actualización GPS #3 - Prioridad: PRIORITY_HIGH_ACCURACY, Intervalo solicitado: 2s, Hora: 13:10:06, Lat: -33.456, Lon: -70.648, Precisión: 5.0m
   ```
4. **Verificación de detención al salir**:
   * Presiona la flecha ← para regresar a "Mi Consulta".
   * Verifica en Logcat el mensaje de detención que certifica que el sensor no queda activo:
     ```text
     [BASELINE] Detención confirmada (Salida de Pantalla Baseline) - Actualizaciones GPS finalizadas. Total capturadas en esta sesión: X
     ```
5. **Captura de evidencia**: Guarda una captura de pantalla de Logcat mostrando la secuencia temporal y el log de detención como `evidencia_gps_baseline_logcat.png`.

---

### Prueba 2: Medición en OPTIMIZED (Intervalo solicitado: 30s)

1. En el panel **Build Variants**, selecciona `optimizedDebug` y ejecuta la app.
2. Limpia el Logcat (ícono de papelera 🗑).
3. Abre "Clínicas cercanas".
4. Observa los registros en Logcat:
   ```text
   [OPTIMIZED] Solicitud GPS INICIADA - Prioridad: PRIORITY_BALANCED_POWER_ACCURACY, Intervalo: 30s
   [OPTIMIZED] Actualización GPS #1 - Prioridad: PRIORITY_BALANCED_POWER_ACCURACY, Intervalo solicitado: 30s, Hora: 13:12:00, Lat: -33.456, Lon: -70.648, Precisión: 15.0m
   [OPTIMIZED] Actualización GPS #2 - Prioridad: PRIORITY_BALANCED_POWER_ACCURACY, Intervalo solicitado: 30s, Hora: 13:12:30, Lat: -33.456, Lon: -70.648, Precisión: 15.0m
   ```
5. **Verificación de ciclo de vida (`onDispose`)**:
   * Presiona ← para salir de la pantalla.
   * Verifica en Logcat la liberación del recurso por Compose:
     ```text
     [OPTIMIZED] Detención confirmada (DisposableEffect onDispose Optimized) - Actualizaciones GPS finalizadas. Total capturadas en esta sesión: Y
     ```
6. **Captura de evidencia**: Guarda una captura de pantalla de Logcat mostrando el espaciamiento temporal y la liberación por `onDispose` como `evidencia_gps_optimized_logcat.png`.

---

## 4. Medición Energética con Profiler (Condicional al Entorno)

> [!WARNING]
> **Disponibilidad del Energy/Power Profiler:**
> La herramienta **Energy Profiler** (o **Power Profiler**) depende estrictamente de:
> 1. La versión y compilación específica de Android Studio (en ciertas versiones recientes la pestaña Energy fue reestructurada o integrada en System Trace).
> 2. El tipo de dispositivo: muchos emuladores AVD genéricos o arquitecturas x86 no exponen los sensores de corriente/batería a nivel de hardware hacia el Profiler.
>
> **Criterio metodológico:**
> - Si el Energy Profiler está disponible en tu dispositivo/versión: abre **View → Tool Windows → Profiler → Energy** e inspecciona si el subsistema "Location" permanece continuo (baseline) o en pulsos (optimized).
> - Si el Energy Profiler **NO está disponible** o tu emulador no reporta datos de energía: **no inventes cifras ni porcentajes de batería**. Deja los campos de energía como `PENDIENTE` en la tabla de métricas y utiliza **Logcat** como la evidencia técnica objetiva que demuestra la diferencia de frecuencia de invocación y la liberación garantizada del hardware.

---

## 5. Registro en Matriz de Métricas

Abre [`RESULTADOS_METRICAS.csv`](file:///C:/Users/U-1133/AndroidStudioProjects/EPE3_Moviles/RESULTADOS_METRICAS.csv) y completa únicamente los valores medidos observados:

* `Actualizaciones_GPS_1_Minuto`: Conteo real de eventos recibidos en Logcat durante 60 segundos con la pantalla activa.
* `Intervalo_GPS_Observado_Segundos`: Diferencia de segundos entre estampas de tiempo consecutivas registradas en Logcat.
* `Consumo_Bateria_GPS` / `Nivel_Energia_Profiler_GPS`: Si el entorno soporta el Profiler energético, anota el nivel cualitativo (`Light`/`Medium`/`Heavy`). En caso contrario, mantener como `PENDIENTE`.
