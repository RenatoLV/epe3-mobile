package com.example.epe3_moviles.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.epe3_moviles.ui.screens.ClinicasCercanasScreen
import com.example.epe3_moviles.ui.screens.HistorialClinicoScreen
import com.example.epe3_moviles.ui.screens.MiConsultaScreen
import com.example.epe3_moviles.ui.screens.VideoconsultaScreen

/**
 * Grafo de navegación principal de la aplicación.
 *
 * La pantalla inicial es "Mi Consulta". Desde allí el usuario puede navegar
 * a las otras tres pantallas mediante los botones de acceso rápido o el
 * menú inferior. Cada destino puede regresar al anterior con el botón
 * de retroceso del sistema o el botón "Volver" que incluye cada pantalla.
 *
 * @param navController Controlador de navegación provisto por rememberNavController().
 */
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.MiConsulta.route,
    ) {
        composable(Screen.MiConsulta.route) {
            MiConsultaScreen(
                onNavigateToHistorial = { navController.navigate(Screen.HistorialClinico.route) },
                onNavigateToVideoconsulta = { navController.navigate(Screen.Videoconsulta.route) },
                onNavigateToClinicas = { navController.navigate(Screen.ClinicasCercanas.route) },
            )
        }
        composable(Screen.HistorialClinico.route) {
            HistorialClinicoScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Videoconsulta.route) {
            VideoconsultaScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ClinicasCercanas.route) {
            ClinicasCercanasScreen(onBack = { navController.popBackStack() })
        }
    }
}
