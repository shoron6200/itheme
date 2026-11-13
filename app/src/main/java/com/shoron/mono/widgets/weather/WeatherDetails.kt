package com.shoron.mono.widgets.weather

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.roundToInt

fun formatTemperature(value: Double): String = if (value.isNaN()) "—" else "${value.roundToInt()}°"

/** Editorial weather lines: "8° · clear sky", "Dhaka · humidity 66% · wind 7 km/h", forecast. */
@Composable
fun WeatherDetails(
    weather: WeatherSnapshot,
    showForecast: Boolean,
    primary: TextStyle,
    secondary: TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text("${formatTemperature(weather.temperature)} · ${weather.condition}", style = primary)
        Text(
            "${weather.placeName} · humidity ${weather.humidity}% · wind ${weather.windSpeed.roundToInt()} ${weather.windUnit}",
            style = secondary,
            maxLines = 1,
        )
        if (showForecast && weather.daily.size > 1) {
            Text(forecastLine(weather.daily.drop(1).take(3)), style = secondary, maxLines = 1)
        }
    }
}

private fun forecastLine(days: List<DailyForecast>): String = days.joinToString("   ") { day ->
    val name = runCatching {
        LocalDate.parse(day.date).dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase()
    }.getOrDefault(day.date)
    "$name ${formatTemperature(day.max)}/${formatTemperature(day.min)}"
}
