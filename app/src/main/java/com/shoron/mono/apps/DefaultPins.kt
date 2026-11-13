package com.shoron.mono.apps

/** Chooses the first-run home shortcuts from apps that are actually installed. */
object DefaultPins {

    const val DEFAULT_COUNT = 6

    fun choose(
        apps: List<AppEntry>,
        preferredPackages: List<String>,
        count: Int = DEFAULT_COUNT,
    ): List<String> {
        val personal = apps.filterNot { it.isWorkProfile }
        val preferred = preferredPackages
            .distinct()
            .mapNotNull { pkg -> personal.firstOrNull { it.packageName == pkg }?.key }
        val filler = personal.map { it.key }.filterNot { it in preferred }
        return (preferred + filler).take(count)
    }
}
