package com.example.epe3_moviles.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.epe3_moviles.BuildConfig
import com.example.epe3_moviles.location.ClinicaModel
import com.example.epe3_moviles.location.DistanceCalculator
import com.example.epe3_moviles.location.clinicasSantiagoFicticias
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Estados accesibles de la pantalla Clínicas Cercanas.
 */
sealed interface ClinicasLocationState {
    data object PermisoRequerido : ClinicasLocationState
    data object PermisoDenegado : ClinicasLocationState
    data object BuscandoUbicacion : ClinicasLocationState
    data class UbicacionDisponible(
        val latitud: Double,
        val longitud: Double,
        val precisionMetros: Float,
        val horaTexto: String,
        val conteoActualizaciones: Int,
        val clinicasConDistancia: List<Pair<ClinicaModel, String>>,
    ) : ClinicasLocationState
    data class Error(val mensaje: String) : ClinicasLocationState
    data class UbicacionDetenida(
        val ultimaHora: String,
        val totalActualizaciones: Int,
    ) : ClinicasLocationState
}

/**
 * ViewModel que administra el ciclo de vida y la frecuencia de actualización GPS según la variante.
 *
 * BASELINE (Didáctico para medición de energía):
 * - Prioridad: [Priority.PRIORITY_HIGH_ACCURACY] (GPS hardware continuo).
 * - Intervalo: 2 segundos (2000 ms).
 * - Estado: "Medición activa".
 * - Se detiene explícitamente al salir de la pantalla con log de confirmación.
 *
 * OPTIMIZED:
 * - Prioridad: [Priority.PRIORITY_BALANCED_POWER_ACCURACY] (torres celulares / Wi-Fi / bajo impacto).
 * - Intervalo: 30 segundos (30000 ms).
 * - Estado: "Ubicación detenida" al salir mediante DisposableEffect.
 * - Log de confirmación de liberación de recursos.
 */
class ClinicasViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "EPE3_Location"
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
    val isBaseline: Boolean = BuildConfig.FLAVOR == "baseline"

    private val _state = MutableStateFlow<ClinicasLocationState>(ClinicasLocationState.PermisoRequerido)
    val state: StateFlow<ClinicasLocationState> = _state.asStateFlow()

    private var locationCallback: LocationCallback? = null
    private var totalActualizaciones = 0
    private var ultimaHoraActualizacion = ""
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun onPermisosConcedidos() {
        iniciarSeguimiento()
    }

    fun onPermisosDenegados() {
        _state.value = ClinicasLocationState.PermisoDenegado
    }

    @SuppressLint("MissingPermission")
    private fun iniciarSeguimiento() {
        if (locationCallback != null) return // Ya está activo

        _state.value = ClinicasLocationState.BuscandoUbicacion

        val prioridad: Int
        val intervaloMillis: Long
        val prioridadNombre: String

        if (isBaseline) {
            prioridad = Priority.PRIORITY_HIGH_ACCURACY
            intervaloMillis = 2000L // 2 segundos
            prioridadNombre = "PRIORITY_HIGH_ACCURACY"
        } else {
            prioridad = Priority.PRIORITY_BALANCED_POWER_ACCURACY
            intervaloMillis = 30000L // 30 segundos
            prioridadNombre = "PRIORITY_BALANCED_POWER_ACCURACY"
        }

        val request = LocationRequest.Builder(prioridad, intervaloMillis)
            .setMinUpdateIntervalMillis(intervaloMillis)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                procesarNuevaUbicacion(location, prioridadNombre, intervaloMillis)
            }
        }

        locationCallback = callback
        try {
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            val variante = if (isBaseline) "BASELINE" else "OPTIMIZED"
            Log.i(
                tag,
                "[$variante] Solicitud GPS INICIADA - Prioridad: $prioridadNombre, " +
                "Intervalo: ${intervaloMillis / 1000}s",
            )
        } catch (e: SecurityException) {
            _state.value = ClinicasLocationState.Error("Permiso de ubicación revocado: ${e.message}")
        }
    }

    private fun procesarNuevaUbicacion(
        location: Location,
        prioridadNombre: String,
        intervaloMillis: Long,
    ) {
        totalActualizaciones++
        val horaActual = timeFormat.format(Date())
        ultimaHoraActualizacion = horaActual
        val variante = if (isBaseline) "BASELINE" else "OPTIMIZED"

        // Registro estructurado exigido para Logcat
        Log.i(
            tag,
            "[$variante] Actualización GPS #$totalActualizaciones - " +
            "Prioridad: $prioridadNombre, " +
            "Intervalo solicitado: ${intervaloMillis / 1000}s, " +
            "Hora: $horaActual, " +
            "Lat: ${location.latitude}, Lon: ${location.longitude}, " +
            "Precisión: ${location.accuracy}m",
        )

        // Calcular distancias reales respecto a las clínicas ficticias de Santiago
        val clinicasConDistancia = clinicasSantiagoFicticias.asSequence().map { clinica ->
            val distKm = DistanceCalculator.calcularDistanciaKm(
                location.latitude,
                location.longitude,
                clinica.latitud,
                clinica.longitud,
            )
            clinica to DistanceCalculator.formatearDistancia(distKm)
        }.sortedBy { (clinica, _) ->
            DistanceCalculator.calcularDistanciaKm(
                location.latitude,
                location.longitude,
                clinica.latitud,
                clinica.longitud,
            )
        }.toList()

        _state.value = ClinicasLocationState.UbicacionDisponible(
            latitud = location.latitude,
            longitud = location.longitude,
            precisionMetros = location.accuracy,
            horaTexto = horaActual,
            conteoActualizaciones = totalActualizaciones,
            clinicasConDistancia = clinicasConDistancia,
        )
    }

    /**
     * Detiene explícitamente las actualizaciones de ubicación al salir de la pantalla.
     * Garantiza que el GPS no quede encendido en segundo plano.
     */
    fun detenerSeguimiento(origenCierre: String = "Salida de Composable") {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
            val variante = if (isBaseline) "BASELINE" else "OPTIMIZED"
            Log.i(
                tag,
                "[$variante] Detención confirmada ($origenCierre) - Actualizaciones GPS finalizadas. " +
                "Total capturadas en esta sesión: $totalActualizaciones",
            )
            _state.value = ClinicasLocationState.UbicacionDetenida(
                ultimaHora = ultimaHoraActualizacion,
                totalActualizaciones = totalActualizaciones,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        detenerSeguimiento(origenCierre = "ViewModel onCleared")
    }
}
