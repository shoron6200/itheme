package com.shoron.mono.wallpaper

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperMathTest {

    @Test fun coverSizeScalesDownToCoverAndNeverUpscales() {
        // 1600x3040 source on a 1080x2412 screen: height decides.
        assertEquals(1269 to 2412, WallpaperMath.coverSize(1600, 3040, 1080, 2412))
        // Small source stays as is.
        assertEquals(400 to 800, WallpaperMath.coverSize(400, 800, 1080, 2412))
    }

    @Test fun fitSizeKeepsAspectInsideBounds() {
        assertEquals(1800 to 1350, WallpaperMath.fitSize(4000, 3000, 1800, 3600))
        assertEquals(1000 to 500, WallpaperMath.fitSize(1000, 500, 1800, 3600))
    }

    @Test fun sampleSizeIsLargestPowerOfTwoThatStillCovers() {
        assertEquals(4, WallpaperMath.sampleSize(4000, 8000, 1000, 2000))
        assertEquals(2, WallpaperMath.sampleSize(4000, 8000, 1001, 2000))
        assertEquals(1, WallpaperMath.sampleSize(1000, 2000, 1080, 2412))
    }

    @Test fun overflowOfCentreCrop() {
        val (ox, oy) = WallpaperMath.overflow(1600, 3040, 1080f, 2412f)
        assertEquals(1269.47f, ox + 1080f, 0.5f)
        assertEquals(0f, oy, 0.001f)
    }

    @Test fun draggingMovesBiasOppositeToFingerAndClamps() {
        // Dragging right reveals more of the left side -> bias decreases.
        assertEquals(-0.5f, WallpaperMath.dragBias(0f, 50f, 200f), 1e-6f)
        assertEquals(-1f, WallpaperMath.dragBias(-0.9f, 500f, 200f), 1e-6f)
        assertEquals(0f, WallpaperMath.dragBias(0.7f, 10f, 0f), 1e-6f)
    }

    @Test fun toneMatrixDimsAndDesaturates() {
        val none = WallpaperMath.toneMatrix(0f, monochrome = false)
        assertArrayEquals(
            floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f),
            none,
            1e-6f,
        )
        val mono = WallpaperMath.toneMatrix(0.5f, monochrome = true)
        // Each colour row sums to the brightness factor, so white stays neutral grey.
        for (row in 0..2) assertEquals(0.5f, mono[row * 5] + mono[row * 5 + 1] + mono[row * 5 + 2], 1e-4f)
        // Dim is clamped to MAX_DIM.
        val clamped = WallpaperMath.toneMatrix(5f, monochrome = false)
        assertEquals(1f - WallpaperSettings.MAX_DIM, clamped[0], 1e-6f)
    }
}
