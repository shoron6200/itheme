package com.shoron.mono.icons

import androidx.compose.runtime.compositionLocalOf
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppRepository
import com.shoron.mono.apps.DefaultAppResolver
import com.shoron.mono.icons.generated.IconManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Resolves which installed apps have a MONO Thin Line icon.
 *
 * Recomputed only when the installed-app list changes (event driven, no polling); the result is
 * a small map from app key to VectorDrawable resource. Apps without an entry keep their original
 * (desaturated) icon.
 */
class IconRepository(
    appRepository: AppRepository,
    private val defaultApps: DefaultAppResolver,
    scope: CoroutineScope,
) {
    val specs: List<IconSpec> = IconManifest.icons

    /** App key → drawable resource of its thin-line icon. */
    val themedIcons: StateFlow<Map<String, Int>> = appRepository.apps
        .filterNotNull()
        .map { apps -> resolve(apps, runCatching { defaultApps.rolePackages() }.getOrDefault(emptyMap())) }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun resolve(apps: List<AppEntry>, roles: Map<String, String?>): Map<String, Int> {
        val perPackage = apps.groupingBy { "${it.packageName}|${it.userSerial}" }.eachCount()
        val result = HashMap<String, Int>()
        apps.forEach { app ->
            IconMatcher.match(
                packageName = app.packageName,
                className = app.className,
                label = app.label,
                activitiesInPackage = perPackage["${app.packageName}|${app.userSerial}"] ?: 1,
                specs = specs,
                rolePackages = roles,
            )?.let { result[app.key] = it.drawable }
        }
        return result
    }
}

/** Thin-line icon resources for the current screen; empty when the user prefers original icons. */
val LocalThemedIcons = compositionLocalOf<Map<String, Int>> { emptyMap() }
