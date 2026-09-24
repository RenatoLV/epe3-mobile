package com.example.epe3_moviles.util

import android.content.Context
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.example.epe3_moviles.R
import com.example.epe3_moviles.config.NetworkConfig

/**
 * Utilidad centralizada para la carga segura y optimizada de fotografías médicas.
 *
 * Características:
 * 1. Proporciona recursos drawables locales (medico_1 a medico_8) como respaldo garantizado (offline/fallback).
 * 2. Si el servidor HTTP local está encendido, Coil descarga la imagen por red (permitiendo auditoría con Network Inspector).
 * 3. Si no hay conexión o el servidor está inactivo, carga inmediatamente la foto local del médico,
 *    asegurando que NUNCA quede un cuadro vacío ni falle la experiencia de usuario.
 */
object DoctorImageHelper {

    /**
     * Retorna el recurso drawable local correspondiente al ID del médico.
     */
    fun getDoctorDrawableRes(medicoId: Int): Int {
        return when (medicoId) {
            1 -> R.drawable.medico_1
            2 -> R.drawable.medico_2
            3 -> R.drawable.medico_3
            4 -> R.drawable.medico_4
            5 -> R.drawable.medico_5
            6 -> R.drawable.medico_6
            7 -> R.drawable.medico_7
            8 -> R.drawable.medico_8
            else -> R.drawable.medico_1
        }
    }

    /**
     * Extrae las dos primeras iniciales de un nombre médico para fallback tipográfico.
     * Ejemplo: "Dra. Isabel Fuentes" -> "IF"
     */
    fun getIniciales(nombre: String): String {
        return nombre
            .replace("Dr. ", "")
            .replace("Dra. ", "")
            .trim()
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "MD" }
    }

    /**
     * Construye un [ImageRequest] de Coil configurado con la estrategia de la variante activa:
     * - [isBaseline]: Sin caché de memoria ni disco, para que en Network Inspector se refleje
     *   la descarga completa repetida de imágenes de alta resolución.
     * - [OPTIMIZED]: Con caché de disco y memoria, WebP 480x480 y transición suave.
     *
     * En ambos casos, incluye como fallback y error el recurso drawable local del médico,
     * garantizando que la imagen siempre cargue incluso con mala conexión o servidor apagado.
     */
    fun buildDoctorImageRequest(
        context: Context,
        medicoId: Int,
        isBaseline: Boolean,
        targetSizePx: Int = 480
    ): ImageRequest {
        val url = NetworkConfig.getFotoMedicoUrl(medicoId)
        val fallbackRes = getDoctorDrawableRes(medicoId)

        val builder = ImageRequest.Builder(context)
            .data(url)
            .placeholder(fallbackRes)
            .error(fallbackRes)
            .fallback(fallbackRes)

        return if (isBaseline) {
            builder
                .crossfade(false)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        } else {
            builder
                .crossfade(true)
                .size(Size(targetSizePx, targetSizePx))
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }
}
