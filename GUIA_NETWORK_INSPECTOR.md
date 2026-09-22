# Guía de Captura: Network Inspector (Android Studio 2025.3.4)

Esta guía explica el procedimiento paso a paso para medir con **evidencias reales**
la diferencia de transferencia de datos por red entre la variante didáctica `baseline`
y la variante `optimized` en la pantalla **Historial Clínico**.

---

## 1. Preparación del Entorno

### Paso 1.1: Generar los archivos de prueba (si no se han generado)
En la terminal del proyecto, ejecuta:
```powershell
node scripts/generate_images.js
```
Esto creará:
- `scripts/public/images/original/medico_[1-8].jpg`: 8 imágenes originales de alta resolución (~2.4 MB c/u, ~19.2 MB total).
- `scripts/public/images/optimized/medico_[1-8].webp`: 8 imágenes WebP optimizadas a 480 px (~80-190 KB c/u, ~0.9 MB total).

### Paso 1.2: Iniciar el servidor HTTP local
En una ventana de terminal dedicada, ejecuta:
```powershell
node scripts/image_server.js
```
Verifica que aparezca el mensaje:
```
================================================================
 Servidor de imágenes iniciado en el puerto 8085
================================================================
 • Host local (PC):         http://localhost:8085
 • Emulador Android (AVD):  http://10.0.2.2:8085
 • Teléfono físico (Wi-Fi): -> http://192.168.X.X:8085
```
> **Nota de puerto**: Por defecto el servidor utiliza el puerto `8085` para evitar conflictos con otros servicios locales (como Postgres Enterprise Manager o Apache en 8080).
> Si deseas especificar un puerto manual: `node scripts/image_server.js 8080`.

---

## 2. Configuración de Host según el Dispositivo

El archivo único de configuración es [`app/src/main/java/com/example/epe3_moviles/config/NetworkConfig.kt`](file:///C:/Users/U-1133/AndroidStudioProjects/EPE3_Moviles/app/src/main/java/com/example/epe3_moviles/config/NetworkConfig.kt).

### Caso A: Emulador Android (AVD)
- No requiere cambios. Usa por defecto `http://10.0.2.2:8085`.
- La IP `10.0.2.2` es el alias reservado de QEMU/Android Emulator para acceder al `localhost` de la máquina anfitriona (PC).

### Caso B: Teléfono Físico (vía Wi-Fi)
1. Conecta tu teléfono a la **misma red Wi-Fi** que tu PC.
2. Consulta la IP de tu PC en la terminal del servidor o con `ipconfig` (ej: `192.168.96.156`).
3. En `NetworkConfig.kt`, actualiza:
   ```kotlin
   var BASE_URL = "http://192.168.96.156:8085"
   ```
4. En [`app/src/main/res/xml/network_security_config.xml`](file:///C:/Users/U-1133/AndroidStudioProjects/EPE3_Moviles/app/src/main/res/xml/network_security_config.xml), agrega tu IP para permitir HTTP local:
   ```xml
   <domain includeSubdomains="false">192.168.96.156</domain>
   ```

---

## 3. Protocolo de Medición en Android Studio 2025.3.4

### Prueba 1: Variante BASELINE (Didáctico)

1. En el panel **Build Variants** de Android Studio (esquina inferior izquierda):
   - Selecciona **Active Build Variant: `baselineDebug`**.
2. Ejecuta la aplicación en el emulador (botón ▶ o `Shift+F10`).
3. Abre **App Inspection** en Android Studio:
   - Menú: **View → Tool Windows → App Inspection**.
   - Haz clic en la pestaña **Network Inspector**.
   - Asegúrate de que el proceso seleccionado sea `com.example.epe3_moviles.baseline`.
4. En el emulador, desde la pantalla **"Mi Consulta"**:
   - Presiona el botón **"Historial Clínico"**.
   - Desplázate por la lista hasta el final para que aparezcan los 8 médicos.
5. Observa el gráfico y la tabla del **Network Inspector**:
   - Verás 8 solicitudes HTTP `GET /images/original/medico_X.jpg`.
   - Cada solicitud transferirá entre 2.2 MB y 2.5 MB.
   - Volumen total recibido: **~19 MB**.
6. **Guardar evidencia**:
   - Haz clic en la primera solicitud `medico_1.jpg` y revisa la pestaña **Response Headers** (muestra `Content-Type: image/jpeg` y `Content-Length`).
   - Toma una captura de pantalla de Android Studio que incluya:
     * El gráfico de tráfico (pico alto de transferencia).
     * La lista de las 8 solicitudes con su tamaño en MB.
     * El emulador con la variante `BASELINE DIDÁCTICO` visible.
   - Guarda la imagen como `evidencia_network_baseline.png`.
7. **Repeticiones**:
   - Regresa a "Mi Consulta" y vuelve a entrar a "Historial Clínico".
   - Como la caché está desactivada en baseline, se vuelven a descargar los ~19 MB.
   - Anota el tiempo y volumen transferido en `RESULTADOS_METRICAS.csv`.

---

### Prueba 2: Variante OPTIMIZED (WebP + Caché)

1. En el panel **Build Variants**:
   - Selecciona **Active Build Variant: `optimizedDebug`**.
2. Ejecuta la aplicación en el emulador (botón ▶).
3. En **App Inspection → Network Inspector**:
   - Verifica que el proceso ahora sea `com.example.epe3_moviles.optimized`.
   - Limpia el historial de solicitudes con el ícono de papelera (🗑 Clear).
4. **Primera carga (Caché fría)**:
   - En el emulador, presiona **"Historial Clínico"** y desplázate por las 8 consultas.
   - Observa las 8 solicitudes HTTP `GET /images/optimized/medico_X.webp`.
   - Cada solicitud transferirá únicamente entre 70 KB y 195 KB.
   - Volumen total recibido: **~0.9 MB** (en lugar de ~19.2 MB).
   - Toma una captura y guárdala como `evidencia_network_optimized_fria.png`.
5. **Segunda carga (Caché caliente)**:
   - Presiona el botón "Volver a Mi Consulta".
   - Limpia el inspector de red (🗑 Clear).
   - Vuelve a entrar a **"Historial Clínico"** y desplázate por las 8 consultas.
   - **Resultado observable**: ¡0 solicitudes de red! Las 8 fotos se leen instantáneamente desde la caché de disco/memoria de Coil.
   - Toma una captura mostrando 0 bytes transferidos y guárdala como `evidencia_network_optimized_caliente.png`.

---

## 4. Registro de Datos en CSV

Abre el archivo [`RESULTADOS_METRICAS.csv`](file:///C:/Users/U-1133/AndroidStudioProjects/EPE3_Moviles/RESULTADOS_METRICAS.csv) y reemplaza los valores `PENDIENTE` en las filas correspondientes con los datos reales leídos del Network Inspector:

| Fila | Columna | Valor a registrar |
| :--- | :--- | :--- |
| `Descarga_Historial_Clinico_Bytes` (baseline) | `Repeticion_1`, `2`, `3` | Total MB leídos en Network Inspector (ej: 19.2) |
| `Descarga_Historial_Clinico_Bytes` (optimized fría) | `Repeticion_1`, `2`, `3` | Total MB leídos en Network Inspector (ej: 0.92) |
| `Descarga_Historial_Clinico_Bytes` (optimized caliente) | `Repeticion_1`, `2`, `3` | Total MB leídos en Network Inspector (ej: 0.0) |
| `Descarga_Historial_Clinico_Tiempo` (baseline) | `Repeticion_1`, `2`, `3` | Duración en segundos desde primera solicitud hasta la última |
| `Descarga_Historial_Clinico_Tiempo` (optimized) | `Repeticion_1`, `2`, `3` | Duración en segundos con WebP |

Calcula el promedio y la mejora porcentual:
$$\text{Mejora \%} = \frac{\text{Baseline} - \text{Optimized}}{\text{Baseline}} \times 100$$
