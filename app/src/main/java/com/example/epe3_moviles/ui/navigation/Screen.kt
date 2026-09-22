package com.example.epe3_moviles.ui.navigation

/**
 * Define las rutas de navegación de la aplicación.
 * Cada objeto representa una pantalla accesible desde el grafo de navegación.
 */
sealed class Screen(val route: String) {
    object MiConsulta : Screen("mi_consulta")
    object HistorialClinico : Screen("historial_clinico")
    object Videoconsulta : Screen("videoconsulta")
    object ClinicasCercanas : Screen("clinicas_cercanas")
}
