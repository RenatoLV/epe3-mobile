package com.example.epe3_moviles.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.epe3_moviles.BuildConfig
import com.example.epe3_moviles.data.local.AppDatabase
import com.example.epe3_moviles.data.local.ConsultaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * ViewModel para el Historial Clínico que gestiona la carga de datos desde Room.
 *
 * Implementa dos estrategias según la variante activa:
 * - BASELINE: Carga monolítica de todos los registros (>200) en memoria en una sola lista,
 *   midiendo el tiempo de consulta para fines didácticos.
 * - OPTIMIZED: Flujo reactivo con Paging 3 en páginas de 20 elementos, cargados bajo demanda.
 */
class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val dao = database.consultaDao()

    @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
    val isBaseline: Boolean = BuildConfig.FLAVOR == "baseline"

    // Estado para BASELINE: lista completa en memoria
    private val _baselineConsultas = MutableStateFlow<List<ConsultaEntity>>(emptyList())
    val baselineConsultas: StateFlow<List<ConsultaEntity>> = _baselineConsultas.asStateFlow()

    // Tiempo medido de la consulta completa en baseline
    private val _tiempoConsultaBaselineMs = MutableStateFlow<Long?>(null)
    val tiempoConsultaBaselineMs: StateFlow<Long?> = _tiempoConsultaBaselineMs.asStateFlow()

    // Conteo total de registros en la base de datos
    private val _totalRegistros = MutableStateFlow(0)
    val totalRegistros: StateFlow<Int> = _totalRegistros.asStateFlow()

    // Estado de carga inicial
    private val _isLoading = MutableStateFlow(value = true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Flujo para la búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Flujo para OPTIMIZED: Paging 3 con páginas de 20 elementos
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pagingConsultasFlow: Flow<PagingData<ConsultaEntity>> = _searchQuery
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    prefetchDistance = 5,
                    enablePlaceholders = false,
                ),
                pagingSourceFactory = { 
                    if (query.isBlank()) dao.getPagingConsultas() else dao.searchPagingConsultas(query) 
                },
            ).flow
        }.cachedIn(viewModelScope)

    init {
        cargarDatos()
    }

    fun updateSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
        if (isBaseline) {
            cargarDatos() // Reloads baseline to filter
        }
    }

    @Suppress("unused")
    fun recargar() {
        cargarDatos()
    }

    private fun cargarDatos() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            // Asegurar que existan los 220 registros ficticios
            AppDatabase.asegurarDatosIniciales(database)
            val count = dao.getCount()
            _totalRegistros.value = count

            if (isBaseline) {
                // Medir tiempo de consulta completa de todos los registros
                val startTime = System.currentTimeMillis()
                val items = if (_searchQuery.value.isBlank()) {
                    dao.getAllConsultas()
                } else {
                    dao.searchAllConsultas(_searchQuery.value)
                }
                val elapsed = System.currentTimeMillis() - startTime
                _tiempoConsultaBaselineMs.value = elapsed
                _baselineConsultas.value = items
            }

            _isLoading.value = false
        }
    }
}
