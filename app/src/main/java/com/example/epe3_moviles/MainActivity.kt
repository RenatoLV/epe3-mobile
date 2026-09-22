package com.example.epe3_moviles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.epe3_moviles.ui.navigation.AppNavHost
import com.example.epe3_moviles.ui.theme.EPE3_MovilesTheme

/**
 * Actividad principal de la aplicación de consultas médicas remotas (EPE3).
 *
 * Configura el tema Material 3, el soporte edge-to-edge y el grafo de
 * navegación Compose que une las cuatro pantallas:
 * Mi Consulta → Historial Clínico / Videoconsulta / Clínicas cercanas.
 *
 * No se inicializa ningún componente costoso en onCreate en esta fase.
 * La inicialización diferida (Room, WebRTC) se agrega en fases posteriores.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EPE3_MovilesTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}