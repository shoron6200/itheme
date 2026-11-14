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
 * The top "camera band" with the status bar hidden, from the real display-cutout geometry
 * (e.g. the Realme 9 SE's top-left punch-hole camera).
 *
 * @param top / [height] vertical band occupied by the top cutout(s); height is 0 when none.
 * @param leftEnd distance from the left screen edge to the right side of a cutout in the left
 *   third of the screen (0 if none) — content on that side starts after it.
 * @param rightStart distance from the right screen edge to the left side of a cutout in the
 *   right third (0 if none).
 */
@Immutable
data class TopCutout(val top: Dp, val height: Dp, val leftEnd: Dp, val rightStart: Dp) {
    /** Vertical band for a status-bar-like row: aligned with the camera, or a slim default. */
    val bandTop: Dp get() = if (height > 0.dp) top else 8.dp
    val bandHeight: Dp get() = if (height > 0.dp) maxOf(height, 22.dp) else 24.dp
}

/** Pure classification of cutout rectangles (px) into a [TopCutout]-like result (JVM-tested). */
object CutoutMath {
    data class Band(val top: Int, val height: Int, val leftEnd: Int, val rightStart: Int)

    /** [rects] as (left, top, right, bottom). Only cutouts touching the top area count. */
    fun band(rects: List<IntArray>, screenWidth: Int): Band {
        val topRects = rects.filter { it[1] < (it[3] - it[1]).coerceAtLeast(1) * 2 }
        if (topRects.isEmpty() || screenWidth <= 0) return Band(0, 0, 0, 0)
        val top = topRects.minOf { it[1] }
        val bottom = topRects.maxOf { it[3] }
        var leftEnd = 0
        var rightStart = 0
        topRects.forEach { r ->
            if (r[0] < screenWidth / 3) leftEnd = maxOf(leftEnd, r[2])
            if (r[2] > screenWidth * 2 / 3) rightStart = maxOf(rightStart, screenWidth - r[0])
        }
        return Band(top, bottom - top, leftEnd, rightStart)
    }
}

@Composable
fun rememberTopCutout(): TopCutout {
    val view = LocalView.current
    val density = LocalDensity.current
    // Reading the inset subscribes to cutout/rotation changes.
    val cutoutTop = WindowInsets.displayCutout.getTop(density)
    return remember(cutoutTop, view.width) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@remember TopCutout(0.dp, 0.dp, 0.dp, 0.dp)
        val rects = view.rootWindowInsets?.displayCutout?.boundingRects.orEmpty()
            .map { intArrayOf(it.left, it.top, it.right, it.bottom) }
        val width = if (view.width > 0) view.width else view.resources.displayMetrics.widthPixels
        val b = CutoutMath.band(rects, width)
        with(density) { TopCutout(b.top.toDp(), b.height.toDp(), b.leftEnd.toDp(), b.rightStart.toDp()) }
    }
}
