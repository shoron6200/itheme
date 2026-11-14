package com.shoron.mono.core.design

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How far display cutouts (e.g. the Realme 9 SE punch-hole camera) reach down from the top
 * edge on the left and right halves of the screen. With the status bar hidden, top-corner
 * indicators sit just below a cutout on their side, or right at the top edge when there is none.
 */
@Immutable
data class TopCutout(val left: Dp, val right: Dp)

@Composable
fun rememberTopCutout(): TopCutout {
    val view = LocalView.current
    val density = LocalDensity.current
    // Reading the inset subscribes to cutout/rotation changes.
    val cutoutTop = WindowInsets.displayCutout.getTop(density)
    return remember(cutoutTop, view.width) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@remember TopCutout(0.dp, 0.dp)
        val rects = view.rootWindowInsets?.displayCutout?.boundingRects.orEmpty()
        val half = (if (view.width > 0) view.width else view.resources.displayMetrics.widthPixels) / 2
        var left = 0
        var right = 0
        rects.filter { it.top <= 0 || it.top < it.height() }.forEach { r ->
            if (r.left < half) left = maxOf(left, r.bottom)
            if (r.right > half) right = maxOf(right, r.bottom)
        }
        with(density) { TopCutout(left.toDp(), right.toDp()) }
    }
}
