package com.shoron.mono.icons

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

class MissingIconsExportTest {

    private val items = listOf(
        MissingIcon("bKash", "com.bKash.customerapp", "com.bKash.Main", system = false),
        MissingIcon("Notes, \"Pro\"", "com.example.notes", "com.example.notes.Main", system = true),
        MissingIcon("Notes 2", "com.example.notes", "com.example.notes.Second", system = true),
    )

    @Test fun namesAndDistinctPackages() {
        assertEquals("bKash\nNotes, \"Pro\"\nNotes 2", MissingIconsExport.names(items))
        assertEquals("com.bKash.customerapp\ncom.example.notes", MissingIconsExport.packages(items))
    }

    @Test fun txtIsTabSeparated() {
        assertEquals("bKash\tcom.bKash.customerapp", MissingIconsExport.txt(items).lines().first())
    }

    @Test fun csvQuotesSpecialCharacters() {
        val lines = MissingIconsExport.csv(items).trimEnd().lines()
        assertEquals("label,package,activity,system", lines[0])
        assertEquals("\"Notes, \"\"Pro\"\"\",com.example.notes,com.example.notes.Main,true", lines[2])
    }

    @Test fun jsonIsValidAndRoundTrips() {
        val array = JSONArray(MissingIconsExport.json(items))
        assertEquals(3, array.length())
        assertEquals("Notes, \"Pro\"", array.getJSONObject(1).getString("label"))
        assertEquals(true, array.getJSONObject(1).getBoolean("system"))
        assertEquals("com.bKash.customerapp", array.getJSONObject(0).getString("package"))
    }

    @Test fun emptyExports() {
        assertEquals("", MissingIconsExport.txt(emptyList()))
        assertEquals(0, JSONArray(MissingIconsExport.json(emptyList())).length())
    }
}
