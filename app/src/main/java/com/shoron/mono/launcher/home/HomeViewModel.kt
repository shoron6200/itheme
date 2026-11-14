package com.shoron.mono.launcher.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoron.mono.MonoApplication
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppLauncher
import com.shoron.mono.apps.AppRepository
import com.shoron.mono.apps.PinnedApps
import android.graphics.Rect
import android.os.Bundle
import com.shoron.mono.data.HomeLayout
import com.shoron.mono.data.LauncherPreferences
import com.shoron.mono.data.PinList
import com.shoron.mono.data.WidgetSettings
import com.shoron.mono.wallpaper.WallpaperRepository
import com.shoron.mono.wallpaper.WallpaperSettings
import com.shoron.mono.widgets.WidgetEngine
import com.shoron.mono.widgets.battery.BatteryState
import com.shoron.mono.widgets.clock.ClockSnapshot
import com.shoron.mono.widgets.weather.WeatherRepository
import com.shoron.mono.widgets.weather.WeatherSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

@Immutable
data class HomeUiState(val pinnedApps: List<AppEntry> = emptyList())

class HomeViewModel(
    appRepository: AppRepository,
    private val preferences: LauncherPreferences,
    private val appLauncher: AppLauncher,
    wallpaperRepository: WallpaperRepository,
    widgets: WidgetEngine,
    private val weatherRepository: WeatherRepository,
) : ViewModel() {

    private val whileVisible = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS)

    val uiState: StateFlow<HomeUiState> =
        combine(appRepository.apps, preferences.pinnedKeys) { apps, keys ->
            HomeUiState(PinnedApps.resolve(apps, keys))
        }
            .distinctUntilChanged()
            .stateIn(viewModelScope, whileVisible, HomeUiState())

    /** `null` until stored settings are read, so the default never flashes before a custom photo. */
    val wallpaper: StateFlow<WallpaperSettings?> = wallpaperRepository.settings
        .map<WallpaperSettings, WallpaperSettings?> { it }
        .stateIn(viewModelScope, whileVisible, null)

    val widgetSettings: StateFlow<WidgetSettings?> = widgets.settings
        .map<WidgetSettings, WidgetSettings?> { it }
        .stateIn(viewModelScope, whileVisible, null)

    val clock: StateFlow<ClockSnapshot> =
        widgets.clock.stateIn(viewModelScope, whileVisible, ClockSnapshot.from(LocalDateTime.now()))

    /** Per-second only while seconds are enabled and the home screen is visible. */
    val time: StateFlow<LocalTime> = widgets.time.stateIn(viewModelScope, whileVisible, LocalTime.now())

    val battery: StateFlow<BatteryState?> = widgets.battery.stateIn(viewModelScope, whileVisible, null)

    val weather: StateFlow<WeatherSnapshot?> = widgets.weather.stateIn(viewModelScope, whileVisible, null)

    /** Called when the home screen becomes visible: refresh weather if the cache is stale. */
    fun onVisible() {
        viewModelScope.launch { weatherRepository.refresh() }
    }

    fun refreshWeatherNow() {
        viewModelScope.launch { weatherRepository.refresh(force = true) }
    }

    val homeLayout: StateFlow<HomeLayout> = preferences.homeLayout.stateIn(viewModelScope, whileVisible, HomeLayout())

    val recents: StateFlow<List<AppEntry>> = combine(appRepository.apps, preferences.recentKeys) { apps, keys ->
        PinnedApps.resolve(apps, keys).take(5)
    }.stateIn(viewModelScope, whileVisible, emptyList())

    /** Shortcuts removed in this session (key to old index), newest last, for "Undo remove". */
    private val removed = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val canRestore: StateFlow<Boolean> = removed.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun launch(entry: AppEntry, sourceBounds: Rect? = null, options: Bundle? = null): Boolean {
        val ok = appLauncher.launch(entry, sourceBounds, options)
        if (ok) viewModelScope.launch { preferences.recordLaunch(entry.key) }
        return ok
    }

    fun reorder(keys: List<String>) {
        viewModelScope.launch { preferences.setPinned(keys) }
    }

    fun restoreLastRemoved() {
        val last = removed.value.lastOrNull() ?: return
        removed.value = removed.value.dropLast(1)
        viewModelScope.launch { preferences.restorePin(last.first, last.second) }
    }

    fun openAppInfo(entry: AppEntry): Boolean = appLauncher.openAppInfo(entry)

    fun unpin(entry: AppEntry) {
        val index = uiState.value.pinnedApps.indexOfFirst { it.key == entry.key }
        if (index >= 0) removed.value = (removed.value + (entry.key to index)).takeLast(PinList.MAX_PINS)
        viewModelScope.launch { preferences.unpin(entry.key) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonoApplication
                val c = app.container
                HomeViewModel(
                    appRepository = c.appRepository,
                    preferences = c.launcherPreferences,
                    appLauncher = c.appLauncher,
                    wallpaperRepository = c.wallpaperRepository,
                    widgets = c.widgetEngine,
                    weatherRepository = c.weatherRepository,
                )
            }
        }
    }
}
