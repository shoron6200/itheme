package com.shoron.mono.apps

import java.text.Normalizer

/** Accent- and case-insensitive app search with prefix matches ranked first. */
object AppSearch {

    private val diacritics = Regex("\\p{Mn}+")

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(diacritics, "").lowercase()

    fun filter(apps: List<AppEntry>, query: String): List<AppEntry> {
        val q = normalize(query.trim())
        if (q.isEmpty()) return apps
        return apps
            .mapNotNull { app ->
                val label = normalize(app.label)
                when {
                    label.startsWith(q) -> app to 0
                    label.split(' ', '-', '_', '.').any { it.startsWith(q) } -> app to 1
                    label.contains(q) -> app to 2
                    else -> null
                }
            }
            .sortedBy { it.second } // stable: keeps alphabetical order within a rank
            .map { it.first }
    }

    /** Section letter used by the drawer index: A–Z, otherwise "#". */
    fun sectionOf(label: String): String {
        val first = normalize(label.trim()).firstOrNull() ?: return "#"
        val upper = first.uppercaseChar()
        return if (upper in 'A'..'Z') upper.toString() else "#"
    }
}
