package com.shoron.mono.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.shoron.mono.R

/** Outfit (SIL OFL 1.1) — geometric sans used for all UI and editorial type. */
val Outfit = FontFamily(
    Font(R.font.outfit_light, FontWeight.Light),
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

/** Sacramento (SIL OFL 1.1) — thin monoline script for the weekday accent. */
val Sacramento = FontFamily(Font(R.font.sacramento_regular, FontWeight.Normal))

private fun TextStyle.outfit() = copy(fontFamily = Outfit)

internal val MonoTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.outfit(),
        displayMedium = displayMedium.outfit(),
        displaySmall = displaySmall.outfit(),
        headlineLarge = headlineLarge.outfit(),
        headlineMedium = headlineMedium.outfit(),
        headlineSmall = headlineSmall.outfit(),
        titleLarge = titleLarge.outfit(),
        titleMedium = titleMedium.outfit(),
        titleSmall = titleSmall.outfit(),
        bodyLarge = bodyLarge.outfit(),
        bodyMedium = bodyMedium.outfit(),
        bodySmall = bodySmall.outfit(),
        labelLarge = labelLarge.outfit(),
        labelMedium = labelMedium.outfit(),
        labelSmall = labelSmall.outfit(),
    )
}
