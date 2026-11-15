package com.shoron.mono.launcher

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class FloatingBoundsTest {
    @Test fun compactWindowIsCentredHorizontallyAndInUpperThird() {
        // Realme 9 SE: 1080 x 2412.
        assertArrayEquals(intArrayOf(108, 402, 972, 1608), FloatingBounds.compactValues(1080, 2412))
    }
}
