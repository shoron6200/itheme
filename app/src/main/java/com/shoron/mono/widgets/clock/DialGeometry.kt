package com.shoron.mono.widgets.clock

import kotlin.math.cos
import kotlin.math.sin

/**
 * Geometry of the radial minute dial. The dial rotates so the current minute always sits at
 * [ANCHOR_DEGREES] (9 o'clock, next to the hour numeral); other minutes fan out around it.
 * Angles are in degrees, 0 = 3 o'clock, increasing clockwise (screen coordinates).
 */
object DialGeometry {

    const val ANCHOR_DEGREES = 180f
    private const val DEGREES_PER_MINUTE = 6f

    /** Angle at which minute [mark] (0..59) is drawn when the current time is [minute]+[second]. */
    fun angleOf(mark: Int, minute: Int, second: Int = 0): Float {
        val current = minute + second / 60f
        val raw = ANCHOR_DEGREES + (mark - current) * DEGREES_PER_MINUTE
        return ((raw % 360f) + 360f) % 360f
    }

    /** Point on a circle of [radius] around ([cx], [cy]) at [degrees]. */
    fun point(cx: Float, cy: Float, radius: Float, degrees: Float): Pair<Float, Float> {
        val rad = Math.toRadians(degrees.toDouble())
        return (cx + radius * cos(rad)).toFloat() to (cy + radius * sin(rad)).toFloat()
    }
}
