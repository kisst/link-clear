package app.linkclear.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF218A5E)
private val GreenLight = Color(0xFF4FC48C)

private val LightColors = lightColorScheme(primary = Green, secondary = Green)
private val DarkColors = darkColorScheme(primary = GreenLight, secondary = GreenLight)

@Composable
fun LinkClearTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
