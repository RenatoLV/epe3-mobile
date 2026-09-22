package com.example.epe3_moviles.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object (DAO) para la entidad [ConsultaEntity].
 *
 * Expone dos métodos de consulta funcionalmente equivalentes:
 * 1. [getAllConsultas]: Utilizado en la variante BASELINE para cargar todos los
 *    registros en memoria de forma monolítica (escenario didáctico de medición).
 * 2. [getPagingConsultas]: Utilizado en la variante OPTIMIZED para paginación incremental
 *    con Paging 3, aprovechando el índice en la columna 'fecha'.
 */
@Dao
interface ConsultaDao {

    /**
     * Inserción masiva de consultas clínicas (reproducible para la población inicial de datos).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(consultas: List<ConsultaEntity>)

    /**
     * Cuenta el total de consultas almacenadas en la base de datos.
     */
    @Query("SELECT COUNT(*) FROM consultas")
    suspend fun getCount(): Int

    /**
     * Consulta BASELINE (Didáctica):
     * Carga la totalidad de los registros de una sola vez a la memoria RAM.
     * Permite medir el impacto de traer 200+ entidades completas sin paginación.
     */
    @Query("SELECT * FROM consultas ORDER BY fecha DESC")
    suspend fun getAllConsultas(): List<ConsultaEntity>

    /**
     * Consulta OPTIMIZED (Paging 3):
     * Retorna una fuente paginada [PagingSource] que carga los datos en bloques pequeños
     * (páginas de 20 elementos) según el desplazamiento de la interfaz del usuario.
     */
    @Query("SELECT * FROM consultas ORDER BY fecha DESC")
    fun getPagingConsultas(): PagingSource<Int, ConsultaEntity>

    /**
     * Limpia la tabla para permitir reinicios limpios en pruebas de rendimiento.
     */
    @Query("DELETE FROM consultas")
    suspend fun deleteAll()
}
