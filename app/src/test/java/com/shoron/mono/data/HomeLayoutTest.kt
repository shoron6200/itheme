package com.shoron.mono.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutTest {

    @Test fun recentListMovesToFrontDedupesAndCaps() {
        assertEquals(listOf("a"), RecentList.push(emptyList(), "a"))
        assertEquals(listOf("b", "a"), RecentList.push(listOf("a"), "b"))
        assertEquals(listOf("a", "b"), RecentList.push(listOf("b", "a"), "a"))
        assertEquals(listOf("z", "a", "b"), RecentList.push(listOf("a", "b", "c"), "z", max = 3))
    }

    @Test fun restoreReinsertsAtOldPosition() {
        assertEquals(listOf("a", "x", "b"), PinList.restore(listOf("a", "b"), "x", 1))
        assertEquals(listOf("a", "b", "x"), PinList.restore(listOf("a", "b"), "x", 9))
        assertEquals(listOf("a", "b"), PinList.restore(listOf("a", "b"), "a", 0))
        assertEquals(listOf("a", "b"), PinList.restore(listOf("a", "b"), "x", 0, max = 2))
    }
}
