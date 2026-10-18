package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LumoraTealVibrant,
    onPrimary = Color.Black,
    primaryContainer = LumoraTealDark,
    onPrimaryContainer = LumoraTealLight,
    secondary = LumoraTealVibrant,
    onSecondary = Color.Black,
    tertiary = AccentAmber,
    background = StudioBgDark,
    onBackground = StudioTextPrimaryDark,
    surface = StudioSurfaceDark,
    onSurface = StudioTextPrimaryDark,
    surfaceVariant = StudioSurfaceVariantDark,
    onSurfaceVariant = StudioTextSecondaryDark,
    outline = StudioBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = LumoraTealPrimary,
    onPrimary = Color.White,
    primaryContainer = LumoraTealLight,
    onPrimaryContainer = LumoraTealDark,
    secondary = LumoraTealPrimary,
    onSecondary = Color.White,
    tertiary = AccentAmber,
    background = StudioBgLight,
    onBackground = StudioTextPrimaryLight,
    surface = StudioSurfaceLight,
    onSurface = StudioTextPrimaryLight,
    surfaceVariant = StudioSurfaceVariantLight,
    onSurfaceVariant = StudioTextSecondaryLight,
    outline = StudioBorderLight
)

@Composable
fun LumoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
