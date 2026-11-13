package com.shoron.mono.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {

    private val apps = listOf(
        testApp("Calculator"),
        testApp("Camera"),
        testApp("Gmail"),
        testApp("Google Maps"),
        testApp("Música"),
        testApp("Play Store"),
    )

    @Test fun blankQueryReturnsEverything() {
        assertEquals(apps, AppSearch.filter(apps, "   "))
    }

    @Test fun labelPrefixRanksBeforeSubstring() {
        val labels = AppSearch.filter(apps, "ca").map { it.label }
        assertEquals(listOf("Calculator", "Camera", "Música"), labels)
    }

    @Test fun wordPrefixRanksBeforeSubstring() {
        // "Maps" starts a word; "Gmail" only contains "ma".
        val labels = AppSearch.filter(apps, "ma").map { it.label }
        assertEquals(listOf("Google Maps", "Gmail"), labels)
    }

    @Test fun searchIgnoresCaseAndAccents() {
        assertEquals(listOf("Música"), AppSearch.filter(apps, "MUSI").map { it.label })
    }

    @Test fun sectionLetters() {
        assertEquals("C", AppSearch.sectionOf("camera"))
        assertEquals("E", AppSearch.sectionOf("Éclair"))
        assertEquals("#", AppSearch.sectionOf("2048"))
        assertEquals("#", AppSearch.sectionOf(""))
    }
}
