package com.shoron.mono.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Proportional layout tokens derived from the real available size (after system insets).
 * Nothing in MONO hard-codes a screen size: every measurement scales from [width]/[height].
 */
@Immutable
data class LayoutMetrics(val width: Dp, val height: Dp) {

    /** 1.0 on a ~392dp wide phone; clamped so tiny or huge screens stay balanced. */
    val scale: Float = (width / ReferenceWidth).coerceIn(0.85f, 1.25f)

    /** Left editorial margin. */
    val gutter: Dp = width * 0.12f

    val isLandscape: Boolean get() = width > height

    fun dp(value: Float): Dp = (value * scale).dp
    fun sp(value: Float): TextUnit = (value * scale).sp

    private companion object {
        val ReferenceWidth = 392.dp
    }
}

@Composable
fun rememberLayoutMetrics(width: Dp, height: Dp): LayoutMetrics =
    remember(width, height) { LayoutMetrics(width, height) }
