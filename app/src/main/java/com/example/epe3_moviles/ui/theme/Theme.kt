package com.example.epe3_moviles.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Esquema de color Modo Oscuro:
 * Utiliza tonos Slate Navy profundos, Cian Médico y Menta Quirúrgica
 * para ofrecer un contraste nítido, descansado y elegante.
 */
private val DarkColorScheme = darkColorScheme(
    primary = MedicalBlueDark,
    onPrimary = Color(0xFF082F49),
    primaryContainer = MedicalBlueContainerDark,
    onPrimaryContainer = Color(0xFFE0F2FE),

    secondary = MedicalTealDark,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = MedicalTealContainerDark,
    onSecondaryContainer = Color(0xFFCCFBF1),

    tertiary = MedicalIndigoDark,
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = MedicalIndigoContainerDark,
    onTertiaryContainer = Color(0xFFEEF2FF),

    background = SlateBackgroundDark,
    onBackground = SlateTextPrimaryDark,

    surface = SlateSurfaceDark,
    onSurface = SlateTextPrimaryDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = SlateTextSecondaryDark,

    error = MedicalCoralDark,
    onError = Color(0xFF450A0A),
    errorContainer = MedicalCoralContainer,
    onErrorContainer = Color(0xFFFEE2E2),

    outline = SlateBorderDark,
    outlineVariant = Color(0xFF1E293B)
)

/**
 * Esquema de color Modo Claro:
 * Superficies limpias, blancas y nítidas con acentos azul clínico.
 */
private val LightColorScheme = lightColorScheme(
    primary = MedicalBluePrimary,
    onPrimary = Color.White,
    primaryContainer = MedicalBlueContainerLight,
    onPrimaryContainer = Color(0xFF075985),

    secondary = MedicalTealPrimary,
    onSecondary = Color.White,
    secondaryContainer = MedicalTealContainerLight,
    onSecondaryContainer = Color(0xFF134E4A),

    tertiary = MedicalIndigo,
    onTertiary = Color.White,
    tertiaryContainer = MedicalIndigoContainerLight,
    onTertiaryContainer = Color(0xFF312E81),

    background = SlateBackgroundLight,
    onBackground = SlateTextPrimaryLight,

    surface = SlateSurfaceLight,
    onSurface = SlateTextPrimaryLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = SlateTextSecondaryLight,

    error = MedicalCoral,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),

    outline = SlateBorderLight,
    outlineVariant = Color(0xFFF1F5F9)
)

@Composable
fun EPE3_MovilesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Se deshabilita dynamicColor para preservar la identidad médica uniforme
    // y evitar que la paleta del fondo de pantalla de Xiaomi/HyperOS degrade el contraste
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}