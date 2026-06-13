package dev.sudoloser.sunset.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Shapes

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFF808080),
    tertiary = NetflixRed,
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
    primary = Color(0xFF1A1A1A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF666666),
    tertiary = Color(0xFFE50914),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = Color(0xFFB00020)
)

private val ClassicDarkColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = Color.White,
    secondary = Color(0xFF1E88E5),
    tertiary = Color(0xFF43A047),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF444444),
    error = Color(0xFFCF6679)
)

private val ClassicLightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    secondary = Color(0xFF689F38),
    tertiary = Color(0xFFF57C00),
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F0),
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
    onSurfaceVariant = Color(0xFF616161),
    outline = Color(0xFFBDBDBD),
    error = Color(0xFFB00020)
)

object SunsetTheme {
    val isDarkMode: Boolean
        @Composable get() = isSystemInDarkTheme()
}

@Composable
fun SunsetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useMaterial3: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useMaterial3 && darkTheme -> DarkColorScheme
        useMaterial3 && !darkTheme -> LightColorScheme
        !useMaterial3 && darkTheme -> ClassicDarkColorScheme
        else -> ClassicLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SunsetTypography,
        content = content
    )
}
