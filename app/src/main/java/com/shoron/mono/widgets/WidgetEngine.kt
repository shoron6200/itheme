package com.shoron.mono.widgets

import com.shoron.mono.data.WidgetPreferences
import com.shoron.mono.data.WidgetSettings
import com.shoron.mono.widgets.battery.BatteryMonitor
import com.shoron.mono.widgets.battery.BatteryState
import com.shoron.mono.widgets.clock.ClockSnapshot
import com.shoron.mono.widgets.clock.SecondTicker
import com.shoron.mono.widgets.clock.TimeTicker
import com.shoron.mono.widgets.weather.WeatherRepository
import com.shoron.mono.widgets.weather.WeatherSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalTime

/**
 * Single source for all widget data, shared by the home screen and the lock screen so no
 * widget logic is duplicated. Every flow is cold: it does work only while a visible screen
 * collects it (via collectAsStateWithLifecycle), and stops shortly after.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetEngine(
    preferences: WidgetPreferences,
    timeTicker: TimeTicker,
    batteryMonitor: BatteryMonitor,
    weatherRepository: WeatherRepository,
) {
    val settings: Flow<WidgetSettings> = preferences.widgets

    /** Minute-resolution date/clock text (TIME_TICK; handles time and zone changes). */
    val clock: Flow<ClockSnapshot> = timeTicker.now.map { ClockSnapshot.from(it) }.distinctUntilChanged()

    /** Wall time at second resolution when seconds are shown, otherwise once per minute. */
    val time: Flow<LocalTime> = settings.map { it.showSeconds }.distinctUntilChanged().flatMapLatest { seconds ->
        if (seconds) SecondTicker.ticks else timeTicker.now.map { it.toLocalTime().withSecond(0).withNano(0) }
    }

    val battery: Flow<BatteryState?> = settings.map { it.showBattery }.distinctUntilChanged().flatMapLatest { show ->
        if (show) batteryMonitor.state else flowOf(null)
    }

    val weather: Flow<WeatherSnapshot?> = combine(settings, weatherRepository.snapshot) { s, snapshot ->
        val place = s.weatherPlace
        if (!s.weatherEnabled || place == null || snapshot == null) null
        else snapshot.takeIf { it.latitude == place.latitude && it.longitude == place.longitude }
    }.distinctUntilChanged()
}
