package com.shoron.mono.launcher.drawer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RailMathTest {

    @Test fun bendPeaksAtFingerAndFadesToZero() {
        assertEquals(1f, RailMath.bend(100f, 100f, 50f), 1e-6f)
        assertEquals(0f, RailMath.bend(200f, 100f, 50f), 1e-6f)
        val near = RailMath.bend(110f, 100f, 50f)
        val far = RailMath.bend(140f, 100f, 50f)
        assertTrue(near > far && far > 0f)
        assertEquals(0f, RailMath.bend(0f, 0f, 0f), 1e-6f)
    }

    @Test fun indexAtMapsEqualSlotsAndClamps() {
        assertEquals(0, RailMath.indexAt(0f, 260f, 26))
        assertEquals(6, RailMath.indexAt(65f, 260f, 26))
        assertEquals(25, RailMath.indexAt(999f, 260f, 26))
        assertEquals(0, RailMath.indexAt(-5f, 260f, 26))
    }

    @Test fun zigzagAlternates() {
        assertEquals(-1f, RailMath.zigzagSign(0), 0f)
        assertEquals(1f, RailMath.zigzagSign(1), 0f)
        assertEquals(-1f, RailMath.zigzagSign(2), 0f)
    }
}
