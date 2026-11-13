package com.shoron.mono.launcher.drawer

import com.shoron.mono.apps.testApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerModelTest {

    private val apps = listOf(
        testApp("2048"),
        testApp("Camera"),
        testApp("Chrome"),
        testApp("Phone"),
    )

    @Test fun nullAppsMeansLoading() {
        assertTrue(DrawerModel.build(null, "", emptySet()).isLoading)
    }

    @Test fun browsingBuildsLetterSections() {
        val state = DrawerModel.build(apps, "", emptySet())
        assertEquals(listOf("#", "C", "P"), state.letters)
        assertEquals(apps.size + 3, state.rows.size)
        assertEquals(DrawerRow.Header("C"), state.rows[state.letterIndex.getValue("C")])
        assertEquals("2048", state.firstApp?.label)
    }

    @Test fun searchingReturnsFlatResultsWithoutHeaders() {
        val state = DrawerModel.build(apps, "ch", emptySet())
        assertEquals(listOf("Chrome"), state.rows.map { (it as DrawerRow.App).entry.label })
        assertTrue(state.letters.isEmpty())
    }
}
