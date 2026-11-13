package com.shoron.mono.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoron.mono.MonoApplication
import com.shoron.mono.data.LockSettings
import com.shoron.mono.data.WeatherPlace
import com.shoron.mono.data.WidgetPreferences
import com.shoron.mono.data.WidgetSettings
import com.shoron.mono.lockscreen.LockScreenController
import com.shoron.mono.lockscreen.LockWallpaperSync
import com.shoron.mono.wallpaper.WallpaperRepository
import com.shoron.mono.widgets.weather.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the "Clock & widgets" and "Lock screen" settings pages. */
class WidgetSettingsViewModel(
    private val preferences: WidgetPreferences,
    private val weather: WeatherRepository,
    private val wallpapers: WallpaperRepository,
    private val lockSync: LockWallpaperSync,
    private val lockController: LockScreenController,
) : ViewModel() {

    val widgets: StateFlow<WidgetSettings?> =
        preferences.widgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lock: StateFlow<LockSettings?> =
        preferences.lock.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _results = MutableStateFlow<List<WeatherPlace>?>(null)
    val results: StateFlow<List<WeatherPlace>?> = _results.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    fun updateWidgets(transform: (WidgetSettings) -> WidgetSettings) {
        viewModelScope.launch { preferences.updateWidgets(transform) }
    }

    fun updateLock(transform: (LockSettings) -> LockSettings) {
        viewModelScope.launch { preferences.updateLock(transform) }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _searching.value = true
            _results.value = weather.search(query)
            _searching.value = false
        }
    }

    fun choosePlace(place: WeatherPlace) {
        _results.value = null
        updateWidgets { it.copy(weatherPlace = place, weatherEnabled = true) }
    }

    fun canShowLockFromBackground(): Boolean = lockController.canShowFromBackground()

    suspend fun syncLockWallpaper(): Boolean {
        val lockDim = preferences.lock.first().dim
        return lockSync.apply(wallpapers.settings.first(), lockDim)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonoApplication
                val c = app.container
                WidgetSettingsViewModel(
                    c.widgetPreferences,
                    c.weatherRepository,
                    c.wallpaperRepository,
                    c.lockWallpaperSync,
                    c.lockScreenController,
                )
            }
        }
    }
}
