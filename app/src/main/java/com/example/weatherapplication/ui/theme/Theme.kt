package com.example.weatherapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme optimized for weather app.
 * Uses deep blues and contrasting accents for night-time viewing.
 */
private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = DeepBlue,
    primaryContainer = SkyBlueDark,
    onPrimaryContainer = SurfaceLight,
    secondary = SunnyYellow,
    onSecondary = DeepBlue,
    secondaryContainer = CardDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = TempMild,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorRed,
    onError = SurfaceLight
)

/**
 * Light color scheme for bright, daytime viewing.
 * Clean whites with sky blue accents.
 */
private val LightColorScheme = lightColorScheme(
    primary = DeepBlue,
    onPrimary = SurfaceLight,
    primaryContainer = SkyBlueLight,
    onPrimaryContainer = DeepBlue,
    secondary = SunnyYellow,
    onSecondary = DeepBlue,
    secondaryContainer = SurfaceLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = RainyBlue,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondaryLight,
    error = ErrorRed,
    onError = SurfaceLight
)

/**
 * Main theme composable for Weather Application.
 * Supports dynamic colors on Android 12+ and falls back to custom scheme.
 */
@Composable
fun WeatherApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use our custom weather theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Set status bar color to match theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
