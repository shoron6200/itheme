package com.shoron.mono.icons

import org.junit.Assert.assertEquals
import org.junit.Test

class IconLibraryStatsTest {

    private fun spec(name: String, drawable: Int, batch: Int) =
        IconSpec(name, drawable, setOf("pkg.$name"), emptySet(), null, emptyList(), batch)

    private val specs = listOf(spec("a", 1, 1), spec("b", 2, 1), spec("c", 3, 3), spec("d", 4, 3), spec("e", 5, 3))

    @Test fun countsAppsAndBatches() {
        // three apps themed; two of them share icon 3
        val c = IconLibraryStats.coverage(specs, totalApps = 10, themed = mapOf("x" to 1, "y" to 3, "z" to 3))
        assertEquals(10, c.totalApps)
        assertEquals(3, c.customApps)
        assertEquals(7, c.missingApps)
        assertEquals(30, c.percent)
        assertEquals(5, c.libraryIcons)
        assertEquals(listOf(BatchStat(1, 2, 1), BatchStat(3, 3, 1)), c.batches)
    }

    @Test fun noAppsMeansZeroCoverage() {
        val c = IconLibraryStats.coverage(specs, totalApps = 0, themed = emptyMap())
        assertEquals(0f, c.fraction)
        assertEquals(0, c.missingApps)
    }
}
