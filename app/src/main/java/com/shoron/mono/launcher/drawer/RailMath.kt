package com.shoron.mono.launcher.drawer

/** Pure geometry for the A–Z rail deformation (JVM-tested). */
object RailMath {

    /**
     * How strongly a letter at [slotCenter] bends toward a finger at [fingerY]:
     * 1 at the finger, easing smoothly to 0 at [spread] pixels away.
     */
    fun bend(slotCenter: Float, fingerY: Float, spread: Float): Float {
        if (spread <= 0f) return 0f
        val t = (1f - kotlin.math.abs(slotCenter - fingerY) / spread).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t) // smoothstep
    }

    /** Index of the letter slot under [y] for [count] equal slots over [height]. */
    fun indexAt(y: Float, height: Float, count: Int): Int {
        if (count <= 0 || height <= 0f) return 0
        return (y / height * count).toInt().coerceIn(0, count - 1)
    }

    /** Alternating ±1 so neighbouring letters zigzag while the rail is bent. */
    fun zigzagSign(index: Int): Float = if (index % 2 == 0) -1f else 1f
}
