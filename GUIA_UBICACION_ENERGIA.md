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
| **Intervalo mínimo** | 2 segundos | 30 segundos |
| **Ciclo de vida en pantalla** | Activo mientras la pantalla esté visible | Activo mediante `DisposableEffect` (liberado en `onDispose`) |
| **Comportamiento al salir** | Se detiene explícitamente con log de confirmación | Se detiene en `onDispose` con log de confirmación |
| **Segundo plano** | **Nunca** (ninguna variante mantiene GPS en background) | **Nunca** |

---

## 2. Preparación de la Ubicación en el Emulador

Para probar la distancia calculada a las clínicas de Santiago:
1. En el emulador, abre el panel lateral de opciones avanzadas (tres puntos `...` **Extended controls**).
2. Ve a la pestaña **Location**.
3. Ingresa coordenadas de Santiago Centro:
   * **Latitude:** `-33.4560`
   * **Longitude:** `-70.6480`
4. Haz clic en **Save Point** y presiona **Send**.
5. Las clínicas calcularán automáticamente la distancia geodésica real (fórmula de Haversine) respecto a este punto.

---

## 3. Protocolo de Captura con Logcat (Intervalos de Actualización)

### Opción A — Desde la terminal (CLI)
Con el emulador o dispositivo conectado por ADB:
```powershell
$adb = 'C:\Users\U-1133\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb logcat -v time -s EPE3_Location
```

### Opción B — Desde Android Studio
1. Abre la pestaña **Logcat** en la parte inferior de Android Studio.
2. En el cuadro de búsqueda/filtro escribe:
   ```text
   tag:EPE3_Location
   ```

---

### Prueba 1: Medición en BASELINE (2 segundos)

1. En el panel **Build Variants**, selecciona `baselineDebug` y ejecuta la app.
2. Abre "Clínicas cercanas" y acepta los permisos de ubicación.
3. Observa los registros en Logcat. Verás una línea cada **2 segundos**:
   ```text
   [BASELINE] Solicitud GPS INICIADA - Prioridad: PRIORITY_HIGH_ACCURACY, Intervalo: 2s
   [BASELINE] Actualización GPS #1 - Prioridad: PRIORITY_HIGH_ACCURACY, Intervalo solicitado: 2s, Hora: 13:10:02, Lat: -33.456, Lon: -70.648, Precisión: 5.0m
   [BASELINE] Actualización GPS #2 - Prioridad: PRIORITY_HIGH_ACCURACY, Intervalo solicitado: 2s, Hora: 13:10:04, Lat: -33.456, Lon: -70.648, Precisión: 5.0m
   [BASELINE] Actualización GPS #3 - Prioridad: PRIORITY_HIGH_ACCURACY, Intervalo solicitado: 2s, Hora: 13:10:06, Lat: -33.456, Lon: -70.648, Precisión: 5.0m
   ```
4. **Verificación de detención al salir**:
   * Presiona la flecha ← para volver a "Mi Consulta".
   * Verifica en Logcat el mensaje de detención:
     ```text
     [BASELINE] Detención confirmada (Salida de Pantalla Baseline) - Actualizaciones GPS finalizadas.
     ```
5. **Guardar evidencia**: Toma captura de Logcat mostrando los intervalos de 2s y guárdala como `evidencia_gps_baseline_logcat.png`.

---

### Prueba 2: Medición en OPTIMIZED (30 segundos)

1. En el panel **Build Variants**, selecciona `optimizedDebug` y ejecuta la app.
2. Limpia el Logcat (ícono 🗑).
3. Abre "Clínicas cercanas".
4. Observa los registros en Logcat:
   ```text
   [OPTIMIZED] Solicitud GPS INICIADA - Prioridad: PRIORITY_BALANCED_POWER_ACCURACY, Intervalo: 30s
   [OPTIMIZED] Actualización GPS #1 - Prioridad: PRIORITY_BALANCED_POWER_ACCURACY, Intervalo solicitado: 30s, Hora: 13:12:00, Lat: -33.456, Lon: -70.648, Precisión: 15.0m
   [OPTIMIZED] Actualización GPS #2 - Prioridad: PRIORITY_BALANCED_POWER_ACCURACY, Intervalo solicitado: 30s, Hora: 13:12:30, Lat: -33.456, Lon: -70.648, Precisión: 15.0m
   ```
5. **Verificación de ciclo de vida (`onDispose`)**:
   * Presiona ← para salir de la pantalla.
   * Verifica en Logcat:
     ```text
     [OPTIMIZED] Detención confirmada (DisposableEffect onDispose Optimized) - Actualizaciones GPS finalizadas.
     ```
6. **Guardar evidencia**: Toma captura de Logcat mostrando el intervalo de 30s y el log de `onDispose`, y guárdala como `evidencia_gps_optimized_logcat.png`.

---

## 4. Medición de Consumo con Energy Profiler (Dispositivos compatibles)

1. Abre **View → Tool Windows → Profiler**.
2. Conecta la app (`baselineDebug` u `optimizedDebug`).
3. Haz clic en la fila **Energy**:
   * **Baseline**: Observarás el sensor de localización ("Location") encendido de forma continua o con barra de nivel "Light/Medium" constante debido a la tasa de 2s.
   * **Optimized**: El sensor Location muestra pulsos aislados cada 30 segundos, manteniendo un nivel general "Light" la mayor parte del tiempo.
4. Toma una captura comparativa del Energy Profiler si tu dispositivo emulador soporta métricas energéticas.

---

## 5. Registro en Matriz de Métricas

Abre [`RESULTADOS_METRICAS.csv`](file:///C:/Users/U-1133/AndroidStudioProjects/EPE3_Moviles/RESULTADOS_METRICAS.csv) y anota los valores medidos en las columnas de repeticiones:

* `Actualizaciones_GPS_1_Minuto`:
  * Baseline (esperado en 1 min con 2s): ~30 actualizaciones.
  * Optimized (esperado en 1 min con 30s): ~2 actualizaciones.
* `Intervalo_GPS_Observado_Segundos`:
  * Baseline: tiempo observado entre dos logs (ej: 2.0s).
  * Optimized: tiempo observado entre dos logs (ej: 30.0s).
