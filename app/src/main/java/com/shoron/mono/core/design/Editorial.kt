package com.shoron.mono.core.design

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

/** Soft shadow so type stays legible over bright foam or user photos. */
val LegibleShadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 1f), blurRadius = 8f)

/**
 * Lays its content out rotated 90° counter-clockwise (reads bottom-to-top), swapping width and
 * height so surrounding layout treats it as a tall, narrow column — the vertical date typography.
 */
fun Modifier.verticalText(): Modifier = layout { measurable, constraints ->
    val maxLength = if (constraints.hasBoundedHeight) constraints.maxHeight else Constraints.Infinity
    val placeable = measurable.measure(Constraints(maxWidth = maxLength))
    layout(placeable.height, placeable.width) {
        val shift = (placeable.width - placeable.height) / 2
        placeable.placeWithLayer(x = -shift, y = shift) { rotationZ = -90f }
    }
}
