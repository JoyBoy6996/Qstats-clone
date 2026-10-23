package dev.pulsekit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Color(0xFF7CC4FF),
    onPrimary = Color(0xFF002B4A),
    secondary = Color(0xFF9AD5B0),
    background = Color(0xFF0F1115),
    surface = Color(0xFF181B21),
    surfaceVariant = Color(0xFF22262E),
    onBackground = Color(0xFFE6E8EC),
    onSurface = Color(0xFFE6E8EC),
    onSurfaceVariant = Color(0xFF9CA3AF),
    error = Color(0xFFFF8A80)
)

@Composable
fun PulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
