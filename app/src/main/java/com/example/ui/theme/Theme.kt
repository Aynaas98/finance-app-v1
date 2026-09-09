package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkNavyColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = NavyDeep,
    primaryContainer = NavyCardElevated,
    onPrimaryContainer = TextPrimary,
    secondary = AccentIndigo,
    onSecondary = NavyDeep,
    secondaryContainer = NavySlate800,
    onSecondaryContainer = TextPrimary,
    tertiary = ProfitGreen,
    onTertiary = NavyDeep,
    background = NavyDeep,
    onBackground = TextPrimary,
    surface = NavySlate900,
    onSurface = TextPrimary,
    surfaceVariant = NavySlate800,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = NavySlate700
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Navy/Dark Slate is default
    dynamicColor: Boolean = false, // Keep intentional Navy/Slate brand aesthetics
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkNavyColorScheme,
        typography = Typography,
        content = content
    )
}
