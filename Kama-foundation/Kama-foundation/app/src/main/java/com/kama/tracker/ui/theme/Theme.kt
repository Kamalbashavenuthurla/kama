package com.kama.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val KamaOrange = Color(0xFFFF7A1A)
val KamaGreen = Color(0xFF2ECC71)
val KamaRed = Color(0xFFE5484D)

private val DarkColors = darkColorScheme(
    primary = KamaOrange,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3A2410),
    onPrimaryContainer = Color(0xFFFFD2AE),
    secondary = KamaGreen,
    onSecondary = Color.Black,
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFA8A8A8),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1B1B1B),
    surfaceContainerHighest = Color(0xFF222222),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A),
    error = KamaRed,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE8660A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3CF),
    onPrimaryContainer = Color(0xFF4A2000),
    secondary = Color(0xFF1E9E54),
    onSecondary = Color.White,
    background = Color(0xFFFAF8F6),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEFEBE7),
    onSurfaceVariant = Color(0xFF5E5A56),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF5F1ED),
    surfaceContainerHighest = Color(0xFFEFEBE7),
    outline = Color(0xFFC9C3BD),
    outlineVariant = Color(0xFFE2DDD8),
    error = Color(0xFFD6333A),
)

private val KamaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** [mode] is DARK, LIGHT or SYSTEM. Dark is the default. */
@Composable
fun KamaTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemInDarkTheme()
        else -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = KamaShapes,
        content = content,
    )
}
