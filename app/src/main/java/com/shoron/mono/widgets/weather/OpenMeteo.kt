package com.shoron.mono.widgets.weather

import com.shoron.mono.data.TemperatureUnit
import com.shoron.mono.data.WeatherPlace
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

/**
 * Open-Meteo (free, no API key) request building and response parsing.
 * Pure functions over org.json so they run in JVM unit tests.
 */
object OpenMeteo {

    private const val FORECAST = "https://api.open-meteo.com/v1/forecast"
    private const val GEOCODING = "https://geocoding-api.open-meteo.com/v1/search"

    fun forecastUrl(latitude: Double, longitude: Double, unit: TemperatureUnit): String {
        val temp = if (unit == TemperatureUnit.Fahrenheit) "fahrenheit" else "celsius"
        val wind = if (unit == TemperatureUnit.Fahrenheit) "mph" else "kmh"
        return FORECAST +
            "?latitude=" + coord(latitude) +
            "&longitude=" + coord(longitude) +
            "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&temperature_unit=$temp&wind_speed_unit=$wind&timezone=auto&forecast_days=4"
    }

    fun searchUrl(query: String, language: String = "en"): String =
        GEOCODING + "?name=" + URLEncoder.encode(query.trim(), "UTF-8") +
            "&count=8&language=" + language + "&format=json"

    fun parseForecast(json: String, place: WeatherPlace, unit: TemperatureUnit, fetchedAtMillis: Long): WeatherSnapshot {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.optJSONObject("daily")
        val days = mutableListOf<DailyForecast>()
        if (daily != null) {
            val dates = daily.getJSONArray("time")
            val codes = daily.getJSONArray("weather_code")
            val max = daily.getJSONArray("temperature_2m_max")
            val min = daily.getJSONArray("temperature_2m_min")
            for (i in 0 until dates.length()) {
                days += DailyForecast(
                    date = dates.getString(i),
                    code = codes.optInt(i, -1),
                    max = max.optDouble(i, Double.NaN),
                    min = min.optDouble(i, Double.NaN),
                )
            }
        }
        return WeatherSnapshot(
            placeName = place.name,
            latitude = place.latitude,
            longitude = place.longitude,
            unit = unit,
            temperature = current.getDouble("temperature_2m"),
            humidity = current.optInt("relative_humidity_2m", 0),
            windSpeed = current.optDouble("wind_speed_10m", 0.0),
            code = current.optInt("weather_code", -1),
            isDay = current.optInt("is_day", 1) == 1,
            daily = days,
            fetchedAtMillis = fetchedAtMillis,
        )
    }

    fun parseSearch(json: String): List<WeatherPlace> {
        val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            WeatherPlace(
                name = r.getString("name"),
                region = listOfNotNull(
                    r.optString("admin1").takeIf { it.isNotBlank() },
                    r.optString("country").takeIf { it.isNotBlank() },
                ).joinToString(", "),
                latitude = r.getDouble("latitude"),
                longitude = r.getDouble("longitude"),
            )
        }
    }

    // ------------------------------------------------------------- cache codec

    fun encode(snapshot: WeatherSnapshot): String = JSONObject().apply {
        put("place", snapshot.placeName)
        put("lat", snapshot.latitude)
        put("lon", snapshot.longitude)
        put("unit", snapshot.unit.name)
        put("temp", snapshot.temperature)
        put("humidity", snapshot.humidity)
        put("wind", snapshot.windSpeed)
        put("code", snapshot.code)
        put("day", snapshot.isDay)
        put("fetched", snapshot.fetchedAtMillis)
        put("daily", JSONArray().apply {
            snapshot.daily.forEach { d ->
                put(JSONObject().apply {
                    put("date", d.date)
                    put("code", d.code)
                    put("max", d.max)
                    put("min", d.min)
                })
            }
        })
    }.toString()

    fun decode(json: String): WeatherSnapshot? = runCatching {
        val o = JSONObject(json)
        val daily = o.optJSONArray("daily") ?: JSONArray()
        WeatherSnapshot(
            placeName = o.getString("place"),
            latitude = o.getDouble("lat"),
            longitude = o.getDouble("lon"),
            unit = TemperatureUnit.valueOf(o.getString("unit")),
            temperature = o.getDouble("temp"),
            humidity = o.getInt("humidity"),
            windSpeed = o.getDouble("wind"),
            code = o.getInt("code"),
            isDay = o.getBoolean("day"),
            daily = (0 until daily.length()).map { i ->
                val d = daily.getJSONObject(i)
                DailyForecast(d.getString("date"), d.getInt("code"), d.getDouble("max"), d.getDouble("min"))
            },
            fetchedAtMillis = o.getLong("fetched"),
        )
    }.getOrNull()

    private fun coord(value: Double) = String.format(Locale.US, "%.4f", value)
}
