package com.shoron.mono.lockscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoron.mono.MonoApplication
import com.shoron.mono.data.LockSettings
import com.shoron.mono.data.WidgetPreferences
import com.shoron.mono.data.WidgetSettings
import com.shoron.mono.wallpaper.WallpaperRepository
import com.shoron.mono.wallpaper.WallpaperSettings
import com.shoron.mono.widgets.WidgetEngine
import com.shoron.mono.widgets.clock.ClockSnapshot
import com.shoron.mono.widgets.weather.WeatherRepository
import com.shoron.mono.widgets.weather.WeatherSnapshot
import com.shoron.mono.media.MediaInfo
import com.shoron.mono.media.MediaSessionRepository
import com.shoron.mono.widgets.battery.BatteryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

/** Lock screen state, reusing the same widget engine and wallpaper as the home screen. */
class LockViewModel(
    widgets: WidgetEngine,
    wallpaperRepository: WallpaperRepository,
    preferences: WidgetPreferences,
    private val weatherRepository: WeatherRepository,
    private val mediaSessions: MediaSessionRepository,
) : ViewModel() {

    private val whileVisible = SharingStarted.WhileSubscribed(2_000)

    val wallpaper: StateFlow<WallpaperSettings?> = wallpaperRepository.settings
        .map<WallpaperSettings, WallpaperSettings?> { it }
        .stateIn(viewModelScope, whileVisible, null)

    val lock: StateFlow<LockSettings> = preferences.lock.stateIn(viewModelScope, whileVisible, LockSettings())

    val widgetSettings: StateFlow<WidgetSettings> =
        widgets.settings.stateIn(viewModelScope, whileVisible, WidgetSettings())

    val clock: StateFlow<ClockSnapshot> =
        widgets.clock.stateIn(viewModelScope, whileVisible, ClockSnapshot.from(LocalDateTime.now()))

    val time: StateFlow<LocalTime> = widgets.time.stateIn(viewModelScope, whileVisible, LocalTime.now())

    val weather: StateFlow<WeatherSnapshot?> = widgets.weather.stateIn(viewModelScope, whileVisible, null)

    val battery: StateFlow<BatteryState?> = widgets.battery.stateIn(viewModelScope, whileVisible, null)

    private val dismissedMedia = MutableStateFlow<String?>(null)

    /** Active media session, unless the user dismissed this track. Collected only while visible. */
    val media: StateFlow<MediaInfo?> = combine(mediaSessions.media, dismissedMedia) { info, dismissed ->
        info?.takeIf { it.key != dismissed }
    }.stateIn(viewModelScope, whileVisible, null)

    fun dismissMedia() {
        dismissedMedia.value = media.value?.key
    }

    fun playPause() = mediaSessions.playPause()
    fun next() = mediaSessions.next()
    fun previous() = mediaSessions.previous()
    fun seekTo(positionMs: Long) = mediaSessions.seekTo(positionMs)

    fun onVisible() {
        viewModelScope.launch { weatherRepository.refresh() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonoApplication
                val c = app.container
                LockViewModel(c.widgetEngine, c.wallpaperRepository, c.widgetPreferences, c.weatherRepository, c.mediaSessions)
            }
        }
    }
}
