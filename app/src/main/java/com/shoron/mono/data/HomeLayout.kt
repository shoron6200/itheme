package com.shoron.mono.data

import androidx.compose.runtime.Immutable

enum class SwipeDownAction { QuickActions, Search, Notifications, None }

enum class IconSize(val dp: Float) { Small(26f), Medium(30f), Large(36f) }

/** Persistent home-screen layout and gesture preferences. */
@Immutable
data class HomeLayout(
    val swipeDownAction: SwipeDownAction = SwipeDownAction.QuickActions,
    val showLabels: Boolean = true,
    val iconSize: IconSize = IconSize.Medium,
    val showRecents: Boolean = true,
    val thinLineIcons: Boolean = true,
)

/** Pure most-recently-used list operations (JVM-tested). */
object RecentList {
    const val MAX = 8

    fun push(keys: List<String>, key: String, max: Int = MAX): List<String> =
        (listOf(key) + keys.filterNot { it == key }).take(max)
}
