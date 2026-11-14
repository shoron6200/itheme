package com.shoron.mono.core.design

import org.junit.Assert.assertEquals
import org.junit.Test

class CutoutMathTest {

    @Test fun noCutoutGivesEmptyBand() {
        assertEquals(CutoutMath.Band(0, 0, 0, 0), CutoutMath.band(emptyList(), 1080))
    }

    @Test fun topLeftPunchHoleIsALeftObstacle() {
        // Realme 9 SE style: small hole near the top-left corner.
        val band = CutoutMath.band(listOf(intArrayOf(40, 20, 120, 100)), 1080)
        assertEquals(20, band.top)
        assertEquals(80, band.height)
        assertEquals(120, band.leftEnd)
        assertEquals(0, band.rightStart)
    }

    @Test fun centredHoleBlocksNeitherSide() {
        val band = CutoutMath.band(listOf(intArrayOf(500, 20, 580, 100)), 1080)
        assertEquals(0, band.leftEnd)
        assertEquals(0, band.rightStart)
        assertEquals(80, band.height)
    }

    @Test fun topRightHoleIsARightObstacle() {
        val band = CutoutMath.band(listOf(intArrayOf(960, 20, 1040, 100)), 1080)
        assertEquals(120, band.rightStart)
        assertEquals(0, band.leftEnd)
    }

    @Test fun bottomCutoutsAreIgnored() {
        assertEquals(CutoutMath.Band(0, 0, 0, 0), CutoutMath.band(listOf(intArrayOf(500, 2300, 580, 2400)), 1080))
    }
}
