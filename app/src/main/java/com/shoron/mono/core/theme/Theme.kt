package com.shoron.mono.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MonoColorScheme = darkColorScheme(
    primary = MonoColors.Foreground,
    onPrimary = MonoColors.Ink,
    secondary = MonoColors.Muted,
    onSecondary = MonoColors.Ink,
    background = MonoColors.Ink,
    onBackground = MonoColors.Foreground,
    surface = MonoColors.Charcoal,
    onSurface = MonoColors.Foreground,
    surfaceVariant = MonoColors.Graphite,
    onSurfaceVariant = MonoColors.Muted,
    surfaceContainer = MonoColors.Graphite,
    outline = MonoColors.Hairline,
)

private val MonoTypography = Typography()

@Composable
fun MonoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonoColorScheme,
        typography = MonoTypography,
        content = content,
    )
}
