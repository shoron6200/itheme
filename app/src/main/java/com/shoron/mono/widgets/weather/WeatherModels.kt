package com.shoron.mono.widgets.weather

import androidx.compose.runtime.Immutable
import com.shoron.mono.data.TemperatureUnit

@Immutable
data class DailyForecast(
    val date: String, // ISO yyyy-MM-dd in the place's time zone
    val code: Int,
    val max: Double,
    val min: Double,
)

@Immutable
data class WeatherSnapshot(
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val unit: TemperatureUnit,
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val code: Int,
    val isDay: Boolean,
    val daily: List<DailyForecast>,
    val fetchedAtMillis: Long,
) {
    val condition: String get() = WeatherCodes.describe(code)
    val windUnit: String get() = if (unit == TemperatureUnit.Fahrenheit) "mph" else "km/h"
}

/** WMO weather interpretation codes as used by Open-Meteo. */
object WeatherCodes {
    fun describe(code: Int): String = when (code) {
        0 -> "clear sky"
        1 -> "mainly clear"
        2 -> "partly cloudy"
        3 -> "overcast"
        45, 48 -> "fog"
        51, 53, 55 -> "drizzle"
        56, 57 -> "freezing drizzle"
        61, 63 -> "rain"
        65 -> "heavy rain"
        66, 67 -> "freezing rain"
        71, 73 -> "snow"
        75 -> "heavy snow"
        77 -> "snow grains"
        80, 81 -> "rain showers"
        82 -> "violent showers"
        85, 86 -> "snow showers"
        95 -> "thunderstorm"
        96, 99 -> "thunderstorm, hail"
        else -> "—"
    }
}
