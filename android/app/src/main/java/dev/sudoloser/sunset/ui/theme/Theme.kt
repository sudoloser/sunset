package dev.sudoloser.sunset.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = Color.White,
    secondary = Color(0xFF808080),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = NetflixRed,
    onPrimary = Color.White,
    secondary = Color(0xFF666666),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = Color(0xFFB00020)
)

object SunsetTheme {
    val isDarkMode: Boolean
        @Composable get() = isSystemInDarkTheme()
}

@Composable
fun SunsetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SunsetTypography,
        content = content
    )
}
