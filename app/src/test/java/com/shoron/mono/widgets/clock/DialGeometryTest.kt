package com.shoron.mono.widgets.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class DialGeometryTest {

    @Test fun currentMinuteSitsAtAnchor() {
        assertEquals(180f, DialGeometry.angleOf(mark = 38, minute = 38), 1e-4f)
        assertEquals(180f, DialGeometry.angleOf(mark = 0, minute = 0), 1e-4f)
    }

    @Test fun laterMinutesAreClockwiseAndWrap() {
        assertEquals(210f, DialGeometry.angleOf(mark = 43, minute = 38), 1e-4f)
        assertEquals(150f, DialGeometry.angleOf(mark = 33, minute = 38), 1e-4f)
        // 30 minutes away is directly opposite.
        assertEquals(0f, DialGeometry.angleOf(mark = 8, minute = 38), 1e-4f)
        assertEquals(354f, DialGeometry.angleOf(mark = 7, minute = 38), 1e-3f)
    }

    @Test fun secondsRotateTheDialSmoothly() {
        // Half a minute later the current mark has moved 3 degrees counter-clockwise.
        assertEquals(177f, DialGeometry.angleOf(mark = 38, minute = 38, second = 30), 1e-4f)
    }

    @Test fun pointOnCircle() {
        val (x, y) = DialGeometry.point(100f, 100f, 50f, 180f)
        assertEquals(50f, x, 1e-3f)
        assertEquals(100f, y, 1e-3f)
        val (x2, y2) = DialGeometry.point(0f, 0f, 10f, 90f)
        assertEquals(0f, x2, 1e-3f)
        assertEquals(10f, y2, 1e-3f)
    }
}
