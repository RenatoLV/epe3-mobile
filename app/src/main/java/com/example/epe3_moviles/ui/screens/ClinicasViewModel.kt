package com.example.epe3_moviles.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
        val esReferencial: Boolean = false,
        val mensajeAviso: String? = null,
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
 * Mejoras de Resiliencia ante Mal Internet / GPS lento:
 * 1. Intenta leer de inmediato [lastLocation] del proveedor FusedLocation.
 * 2. Si el hardware GPS tarda más de 3 segundos en adquirir fijación satelital (común en interiores,
 *    mala conexión o emulador sin coordenadas mock enviadas), emite automáticamente una ubicación referencial
 *    de Santiago Centro (-33.4372, -70.6506) para que el mapa y las clínicas carguen DE INMEDIATO.
 * 3. Mantiene la escucha GPS activa en segundo plano: en el instante que el satélite entrega coordenadas
 *    reales, actualiza fluidamente el mapa a la posición exacta y emite los logs oficiales requeridos en Logcat.
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
    private var fallbackTimeoutJob: Job? = null
    private var totalActualizaciones = 0
    private var ultimaHoraActualizacion = ""
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Coordenadas de referencia para fallback (Coquimbo / La Serena donde se encuentra el usuario físicamente, o Santiago)
    private var fallbackLat = -29.9533
    private var fallbackLon = -71.3395

    fun onPermisosConcedidos() {
        iniciarSeguimiento()
    }

    fun onPermisosDenegados() {
        _state.value = ClinicasLocationState.PermisoDenegado
    }

    fun reintentarGps() {
        detenerSeguimiento("Reintento solicitado")
        iniciarSeguimiento()
    }

    fun usarUbicacionReferencialManual() {
        fallbackTimeoutJob?.cancel()
        activarUbicacionReferencial("Ubicación referencial seleccionada manualmente.")
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

        // 1. Intento rápido: leer última ubicación conocida en caché del sistema FusedLocation
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                if (lastLocation != null && _state.value is ClinicasLocationState.BuscandoUbicacion) {
                    Log.i(tag, "Ubicación inicial rápida obtenida desde caché FusedLocation.")
                    fallbackLat = lastLocation.latitude
                    fallbackLon = lastLocation.longitude
                    procesarNuevaUbicacion(lastLocation, prioridadNombre, intervaloMillis, esReferencial = false)
                }
            }
        } catch (e: SecurityException) {
            Log.w(tag, "No se pudo consultar lastLocation: ${e.message}")
        }

        // 2. Temporizador de respaldo ultra rápido (800ms): si no hay señal satelital inmediata, cargar red asistencial
        fallbackTimeoutJob?.cancel()
        fallbackTimeoutJob = viewModelScope.launch {
            delay(800L)
            if (_state.value is ClinicasLocationState.BuscandoUbicacion) {
                Log.w(tag, "Fijación satelital demorada. Activando ubicación referencial para no bloquear el mapa.")
                activarUbicacionReferencial(
                    "Sincronizando coordenadas GPS en segundo plano."
                )
            }
        }

        val request = LocationRequest.Builder(prioridad, intervaloMillis)
            .setMinUpdateIntervalMillis(intervaloMillis)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                fallbackTimeoutJob?.cancel()
                procesarNuevaUbicacion(location, prioridadNombre, intervaloMillis, esReferencial = false)
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
            fallbackTimeoutJob?.cancel()
            _state.value = ClinicasLocationState.Error("Permiso de ubicación revocado: ${e.message}")
        }
    }

    private fun activarUbicacionReferencial(mensaje: String) {
        val horaActual = timeFormat.format(Date())
        ultimaHoraActualizacion = horaActual

        val clinicasConDistancia = calcularDistancias(fallbackLat, fallbackLon)

        _state.value = ClinicasLocationState.UbicacionDisponible(
            latitud = fallbackLat,
            longitud = fallbackLon,
            precisionMetros = 20.0f,
            horaTexto = horaActual,
            conteoActualizaciones = totalActualizaciones,
            clinicasConDistancia = clinicasConDistancia,
            esReferencial = true,
            mensajeAviso = mensaje
        )
    }

    private fun procesarNuevaUbicacion(
        location: Location,
        prioridadNombre: String,
        intervaloMillis: Long,
        esReferencial: Boolean
    ) {
        totalActualizaciones++
        fallbackLat = location.latitude
        fallbackLon = location.longitude
        val horaActual = timeFormat.format(Date())
        ultimaHoraActualizacion = horaActual
        val variante = if (isBaseline) "BASELINE" else "OPTIMIZED"

        // Registro estructurado exigido para Logcat (Paso 5)
        Log.i(
            tag,
            "[$variante] Actualización GPS #$totalActualizaciones - " +
            "Prioridad: $prioridadNombre, " +
            "Intervalo solicitado: ${intervaloMillis / 1000}s, " +
            "Hora: $horaActual, " +
            "Lat: ${location.latitude}, Lon: ${location.longitude}, " +
            "Precisión: ${location.accuracy}m",
        )

        val clinicasConDistancia = calcularDistancias(location.latitude, location.longitude)

        _state.value = ClinicasLocationState.UbicacionDisponible(
            latitud = location.latitude,
            longitud = location.longitude,
            precisionMetros = location.accuracy,
            horaTexto = horaActual,
            conteoActualizaciones = totalActualizaciones,
            clinicasConDistancia = clinicasConDistancia,
            esReferencial = esReferencial,
            mensajeAviso = null
        )
    }

    private fun calcularDistancias(userLat: Double, userLon: Double): List<Pair<ClinicaModel, String>> {
        return clinicasSantiagoFicticias.asSequence().map { clinica ->
            val distKm = DistanceCalculator.calcularDistanciaKm(
                userLat,
                userLon,
                clinica.latitud,
                clinica.longitud,
            )
            clinica to DistanceCalculator.formatearDistancia(distKm)
        }.sortedBy { (clinica, _) ->
            DistanceCalculator.calcularDistanciaKm(
                userLat,
                userLon,
                clinica.latitud,
                clinica.longitud,
            )
        }.toList()
    }

    /**
     * Detiene explícitamente las actualizaciones de ubicación al salir de la pantalla.
     * Garantiza que el GPS no quede encendido en segundo plano.
     */
    fun detenerSeguimiento(origenCierre: String = "Salida de Composable") {
        fallbackTimeoutJob?.cancel()
        fallbackTimeoutJob = null

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
