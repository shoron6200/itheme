package com.shoron.mono.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widgets")

/** DataStore for clock, weather, battery and lock-screen preferences. */
class WidgetPreferences(context: Context) {

    private val store = context.applicationContext.widgetDataStore

    private val data: Flow<Preferences> = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    val widgets: Flow<WidgetSettings> = data.map { currentWidgets(it) }.distinctUntilChanged()

    val lock: Flow<LockSettings> = data.map { p ->
        val d = LockSettings()
        LockSettings(
            enabled = p[LOCK_ENABLED] ?: d.enabled,
            textX = p[LOCK_X] ?: d.textX,
            textY = p[LOCK_Y] ?: d.textY,
            dim = p[LOCK_DIM] ?: d.dim,
        )
    }.distinctUntilChanged()

    suspend fun updateWidgets(transform: (WidgetSettings) -> WidgetSettings) {
        store.edit { p ->
            val next = transform(currentWidgets(p))
            p[CLOCK_STYLE] = next.clockStyle.name
            p[USE_24H] = next.use24Hour
            p[SHOW_SECONDS] = next.showSeconds
            p[SHOW_BATTERY] = next.showBattery
            p[WEATHER_ENABLED] = next.weatherEnabled
            p[SHOW_FORECAST] = next.showForecast
            p[TEMP_UNIT] = next.temperatureUnit.name
            writePlace(p, next.weatherPlace)
        }
    }

    suspend fun updateLock(transform: (LockSettings) -> LockSettings) {
        store.edit { p ->
            val d = LockSettings()
            val next = transform(
                LockSettings(
                    enabled = p[LOCK_ENABLED] ?: d.enabled,
                    textX = p[LOCK_X] ?: d.textX,
                    textY = p[LOCK_Y] ?: d.textY,
                    dim = p[LOCK_DIM] ?: d.dim,
                ),
            )
            p[LOCK_ENABLED] = next.enabled
            p[LOCK_X] = next.textX.coerceIn(LockSettings.X_RANGE)
            p[LOCK_Y] = next.textY.coerceIn(LockSettings.Y_RANGE)
            p[LOCK_DIM] = next.dim.coerceIn(0f, 0.8f)
        }
    }

    private fun currentWidgets(p: Preferences): WidgetSettings {
        val d = WidgetSettings()
        return WidgetSettings(
            clockStyle = p[CLOCK_STYLE]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() } ?: d.clockStyle,
            use24Hour = p[USE_24H] ?: d.use24Hour,
            showSeconds = p[SHOW_SECONDS] ?: d.showSeconds,
            showBattery = p[SHOW_BATTERY] ?: d.showBattery,
            weatherEnabled = p[WEATHER_ENABLED] ?: d.weatherEnabled,
            showForecast = p[SHOW_FORECAST] ?: d.showForecast,
            weatherPlace = readPlace(p),
            temperatureUnit = p[TEMP_UNIT]?.let { runCatching { TemperatureUnit.valueOf(it) }.getOrNull() }
                ?: d.temperatureUnit,
        )
    }

    private fun readPlace(p: Preferences): WeatherPlace? {
        val name = p[PLACE_NAME] ?: return null
        val lat = p[PLACE_LAT] ?: return null
        val lon = p[PLACE_LON] ?: return null
        return WeatherPlace(name, p[PLACE_REGION].orEmpty(), lat, lon)
    }

    private fun writePlace(p: MutablePreferences, place: WeatherPlace?) {
        if (place == null) {
            p.remove(PLACE_NAME)
            p.remove(PLACE_REGION)
            p.remove(PLACE_LAT)
            p.remove(PLACE_LON)
        } else {
            p[PLACE_NAME] = place.name
            p[PLACE_REGION] = place.region
            p[PLACE_LAT] = place.latitude
            p[PLACE_LON] = place.longitude
        }
    }

    private companion object {
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val USE_24H = booleanPreferencesKey("use_24h")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val SHOW_BATTERY = booleanPreferencesKey("show_battery")
        val WEATHER_ENABLED = booleanPreferencesKey("weather_enabled")
        val SHOW_FORECAST = booleanPreferencesKey("show_forecast")
        val TEMP_UNIT = stringPreferencesKey("temperature_unit")
        val PLACE_NAME = stringPreferencesKey("place_name")
        val PLACE_REGION = stringPreferencesKey("place_region")
        val PLACE_LAT = doublePreferencesKey("place_lat")
        val PLACE_LON = doublePreferencesKey("place_lon")
        val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        val LOCK_X = floatPreferencesKey("lock_x")
        val LOCK_Y = floatPreferencesKey("lock_y")
        val LOCK_DIM = floatPreferencesKey("lock_dim")
    }
}
