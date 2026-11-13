package com.shoron.mono.wallpaper

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Pure sizing, positioning and tone maths for the wallpaper engine (JVM-tested). */
object WallpaperMath {

    /** Size that covers [dstW]x[dstH] while keeping aspect ratio; never upscales. */
    fun coverSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Pair<Int, Int> {
        if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) return srcW to srcH
        val scale = min(1f, max(dstW.toFloat() / srcW, dstH.toFloat() / srcH))
        return scaled(srcW, srcH, scale)
    }

    /** Size that fits inside [maxW]x[maxH] while keeping aspect ratio; never upscales. */
    fun fitSize(srcW: Int, srcH: Int, maxW: Int, maxH: Int): Pair<Int, Int> {
        if (srcW <= 0 || srcH <= 0) return srcW to srcH
        val scale = min(1f, min(maxW.toFloat() / srcW, maxH.toFloat() / srcH))
        return scaled(srcW, srcH, scale)
    }

    /** Largest power-of-two sample size that still covers the destination (BitmapFactory path). */
    fun sampleSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Int {
        if (dstW <= 0 || dstH <= 0) return 1
        var sample = 1
        while (srcW / (sample * 2) >= dstW && srcH / (sample * 2) >= dstH) sample *= 2
        return sample
    }

    /** How far a centre-cropped image overflows its view on each axis, in view pixels. */
    fun overflow(imageW: Int, imageH: Int, viewW: Float, viewH: Float): Pair<Float, Float> {
        if (imageW <= 0 || imageH <= 0 || viewW <= 0f || viewH <= 0f) return 0f to 0f
        val scale = max(viewW / imageW, viewH / imageH)
        return (imageW * scale - viewW).coerceAtLeast(0f) to (imageH * scale - viewH).coerceAtLeast(0f)
    }

    /** New crop bias after the user drags the image by [deltaPx] (image follows the finger). */
    fun dragBias(bias: Float, deltaPx: Float, overflowPx: Float): Float =
        if (overflowPx <= 0f) 0f else (bias - 2f * deltaPx / overflowPx).coerceIn(-1f, 1f)

    /**
     * 4x5 row-major colour matrix (Android/Compose layout) applying greyscale and dimming.
     */
    fun toneMatrix(dim: Float, monochrome: Boolean): FloatArray {
        val k = 1f - dim.coerceIn(0f, WallpaperSettings.MAX_DIM)
        return if (monochrome) {
            val r = 0.2126f * k
            val g = 0.7152f * k
            val b = 0.0722f * k
            floatArrayOf(
                r, g, b, 0f, 0f,
                r, g, b, 0f, 0f,
                r, g, b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        } else {
            floatArrayOf(
                k, 0f, 0f, 0f, 0f,
                0f, k, 0f, 0f, 0f,
                0f, 0f, k, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        }
    }

    private fun scaled(w: Int, h: Int, scale: Float): Pair<Int, Int> =
        (w * scale).roundToInt().coerceAtLeast(1) to (h * scale).roundToInt().coerceAtLeast(1)
}
