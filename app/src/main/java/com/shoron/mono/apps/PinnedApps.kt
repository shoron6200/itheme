package com.shoron.mono.apps

/** Resolves stored pin keys against installed apps, keeping pin order and skipping missing apps. */
object PinnedApps {
    fun resolve(apps: List<AppEntry>?, pinnedKeys: List<String>?): List<AppEntry> {
        if (apps.isNullOrEmpty() || pinnedKeys.isNullOrEmpty()) return emptyList()
        val byKey = apps.associateBy { it.key }
        return pinnedKeys.mapNotNull { byKey[it] }
    }
}
