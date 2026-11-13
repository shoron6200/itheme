package com.shoron.mono.data

import androidx.compose.runtime.Immutable

enum class ClockStyle { RadialDigital, EditorialWords }

enum class TemperatureUnit { Celsius, Fahrenheit }

/** A place chosen for weather (from Open-Meteo geocoding). */
@Immutable
data class WeatherPlace(
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
)

/** Home-screen widget configuration, shared by the home screen and the lock screen. */
@Immutable
data class WidgetSettings(
    val clockStyle: ClockStyle = ClockStyle.RadialDigital,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val showBattery: Boolean = true,
    val weatherEnabled: Boolean = true,
    val showForecast: Boolean = false,
    val weatherPlace: WeatherPlace? = null,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.Celsius,
)

/** MONO lock screen (glance) configuration. Positions are fractions of the screen. */
@Immutable
data class LockSettings(
    val enabled: Boolean = false,
    val textX: Float = DEFAULT_X,
    val textY: Float = DEFAULT_Y,
    val dim: Float = 0.25f,
) {
    companion object {
        const val DEFAULT_X = 0.10f
        const val DEFAULT_Y = 0.22f
        val X_RANGE = 0.04f..0.45f
        val Y_RANGE = 0.08f..0.50f
    }
}
