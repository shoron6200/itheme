package com.shoron.mono.core.design

import android.graphics.Rect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/** Plain holder for a view's on-screen bounds (no recomposition when it changes). */
class LaunchBounds {
    var rect: Rect? = null
}

/** Records the element's window bounds into [holder] for launch transitions. */
fun Modifier.trackBounds(holder: LaunchBounds): Modifier = onGloballyPositioned { coords ->
    val r = coords.boundsInWindow()
    holder.rect = Rect(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
}

/** Subtle press-in scale (GPU layer only). */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.95f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
