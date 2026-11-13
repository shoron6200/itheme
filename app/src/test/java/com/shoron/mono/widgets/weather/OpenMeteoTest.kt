package com.shoron.mono.widgets.weather

import com.shoron.mono.data.TemperatureUnit
import com.shoron.mono.data.WeatherPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoTest {

    private val dhaka = WeatherPlace("Dhaka", "Dhaka Division, Bangladesh", 23.7104, 90.40744)

    private val forecastJson = """
        {"latitude":23.71,"longitude":90.41,"generationtime_ms":0.1,"utc_offset_seconds":21600,
         "timezone":"Asia/Dhaka","timezone_abbreviation":"GMT+6","elevation":9.0,
         "current_units":{"time":"iso8601","interval":"seconds","temperature_2m":"°C",
           "relative_humidity_2m":"%","weather_code":"wmo code","wind_speed_10m":"km/h","is_day":""},
         "current":{"time":"2026-11-13T22:15","interval":900,"temperature_2m":24.6,
           "relative_humidity_2m":66,"weather_code":0,"wind_speed_10m":7.2,"is_day":0},
         "daily_units":{"time":"iso8601","weather_code":"wmo code","temperature_2m_max":"°C","temperature_2m_min":"°C"},
         "daily":{"time":["2026-11-13","2026-11-14","2026-11-15","2026-11-16"],
           "weather_code":[0,2,61,3],
           "temperature_2m_max":[29.1,28.4,26.0,27.2],
           "temperature_2m_min":[19.8,19.1,20.3,18.9]}}
    """.trimIndent()

    @Test fun parsesCurrentAndDaily() {
        val w = OpenMeteo.parseForecast(forecastJson, dhaka, TemperatureUnit.Celsius, fetchedAtMillis = 42L)
        assertEquals(24.6, w.temperature, 1e-9)
        assertEquals(66, w.humidity)
        assertEquals(7.2, w.windSpeed, 1e-9)
        assertEquals("clear sky", w.condition)
        assertEquals(false, w.isDay)
        assertEquals(4, w.daily.size)
        assertEquals(DailyForecast("2026-11-15", 61, 26.0, 20.3), w.daily[2])
        assertEquals("Dhaka", w.placeName)
        assertEquals(42L, w.fetchedAtMillis)
    }

    @Test fun cacheRoundTrip() {
        val w = OpenMeteo.parseForecast(forecastJson, dhaka, TemperatureUnit.Celsius, 42L)
        assertEquals(w, OpenMeteo.decode(OpenMeteo.encode(w)))
        assertEquals(null, OpenMeteo.decode("not json"))
    }

    @Test fun parsesGeocodingResults() {
        val json = """
            {"results":[{"id":1185241,"name":"Dhaka","latitude":23.7104,"longitude":90.40744,
              "country":"Bangladesh","admin1":"Dhaka Division"},
             {"id":2,"name":"Dhaka","latitude":26.0,"longitude":85.0,"country":"India"}],
             "generationtime_ms":0.5}
        """.trimIndent()
        val places = OpenMeteo.parseSearch(json)
        assertEquals(2, places.size)
        assertEquals(dhaka, places[0])
        assertEquals("India", places[1].region)
        assertTrue(OpenMeteo.parseSearch("""{"generationtime_ms":0.2}""").isEmpty())
    }

    @Test fun buildsUrls() {
        val url = OpenMeteo.forecastUrl(23.7104, 90.40744, TemperatureUnit.Fahrenheit)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?latitude=23.7104&longitude=90.4074"))
        assertTrue(url.contains("temperature_unit=fahrenheit"))
        assertTrue(url.contains("wind_speed_unit=mph"))
        assertTrue(url.contains("current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day"))
        assertEquals(
            "https://geocoding-api.open-meteo.com/v1/search?name=New+York&count=8&language=en&format=json",
            OpenMeteo.searchUrl(" New York "),
        )
    }

    @Test fun describesWeatherCodes() {
        assertEquals("overcast", WeatherCodes.describe(3))
        assertEquals("thunderstorm", WeatherCodes.describe(95))
        assertEquals("—", WeatherCodes.describe(1234))
    }
}
