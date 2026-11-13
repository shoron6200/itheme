package com.shoron.mono.launcher.drawer

import androidx.compose.runtime.Immutable
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppSearch

sealed interface DrawerRow {
    val rowKey: String

    data class Header(val letter: String) : DrawerRow {
        override val rowKey: String get() = "h:$letter"
    }

    data class App(val entry: AppEntry) : DrawerRow {
        override val rowKey: String get() = "a:${entry.key}"
    }
}

@Immutable
data class DrawerUiState(
    val rows: List<DrawerRow> = emptyList(),
    val letters: List<String> = emptyList(),
    val letterIndex: Map<String, Int> = emptyMap(),
    val pinnedKeys: Set<String> = emptySet(),
    val isLoading: Boolean = true,
) {
    val firstApp: AppEntry?
        get() = rows.firstNotNullOfOrNull { (it as? DrawerRow.App)?.entry }
}

object DrawerModel {

    /** Builds drawer rows: lettered sections when browsing, a flat ranked list when searching. */
    fun build(apps: List<AppEntry>?, query: String, pinnedKeys: Set<String>): DrawerUiState {
        if (apps == null) return DrawerUiState(pinnedKeys = pinnedKeys, isLoading = true)

        if (query.isNotBlank()) {
            return DrawerUiState(
                rows = AppSearch.filter(apps, query).map { DrawerRow.App(it) },
                pinnedKeys = pinnedKeys,
                isLoading = false,
            )
        }

        val rows = ArrayList<DrawerRow>(apps.size + 27)
        val letters = ArrayList<String>()
        val index = LinkedHashMap<String, Int>()
        var current: String? = null
        apps.forEach { app ->
            val letter = AppSearch.sectionOf(app.label)
            if (letter != current && letter !in index) {
                index[letter] = rows.size
                letters += letter
                rows += DrawerRow.Header(letter)
            }
            current = letter
            rows += DrawerRow.App(app)
        }
        return DrawerUiState(rows, letters, index, pinnedKeys, isLoading = false)
    }
}
