package com.shoron.mono

import android.content.Context
import com.shoron.mono.apps.AppLauncher
import com.shoron.mono.apps.AppRepository
import com.shoron.mono.apps.DefaultAppResolver
import com.shoron.mono.apps.DefaultPins
import com.shoron.mono.apps.IconCache
import com.shoron.mono.data.LauncherPreferences
import com.shoron.mono.wallpaper.WallpaperPreferences
import com.shoron.mono.wallpaper.WallpaperRepository
import com.shoron.mono.widgets.clock.TimeTicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manual dependency container. One instance per process, owned by [MonoApplication].
 * Kept deliberately small: no DI framework, no reflection, fast cold start.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val iconCache = IconCache()
    val launcherPreferences = LauncherPreferences(appContext)
    val appRepository = AppRepository(appContext, applicationScope, iconCache)
    val appLauncher = AppLauncher(appContext)
    val timeTicker = TimeTicker(appContext)
    val wallpaperRepository = WallpaperRepository(appContext, WallpaperPreferences(appContext))
    private val defaultAppResolver = DefaultAppResolver(appContext)

    init {
        // First run: choose an initial set of home shortcuts from apps that are really installed.
        applicationScope.launch {
            val apps = appRepository.apps.filterNotNull().first()
            if (launcherPreferences.pinnedKeys.first() == null) {
                val preferred = runCatching { defaultAppResolver.preferredPackages() }.getOrDefault(emptyList())
                launcherPreferences.initializePins(DefaultPins.choose(apps, preferred))
            }
        }
    }
}
