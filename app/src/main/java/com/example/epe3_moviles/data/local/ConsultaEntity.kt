package com.example.epe3_moviles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room para representar una consulta médica en el Historial Clínico.
 *
 * Cumple con el requisito técnico del informe:
 * "Room con Paging 3 e índices apropiados, usando un conjunto de datos
 * suficientemente grande y reproducible."
 *
 * Se define un índice explícito sobre la columna "fecha" para optimizar
 * las consultas con ordenamiento cronológico inverso (ORDER BY fecha DESC).
 */
@Entity(
    tableName = "consultas",
    indices = [Index(value = ["fecha"])],
)
data class ConsultaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fecha: Long, // timestamp en milisegundos para ordenamiento indexado
    val fechaTexto: String, // fecha formateada legible para el usuario
    val medicoId: Int, // 1 a 8, mapeado al avatar del servidor de imágenes
    val medicoNombre: String,
    val especialidad: String,
    val diagnostico: String,
    val tratamiento: String,
    val pacienteNombre: String = "Renato Alvarez",
)
