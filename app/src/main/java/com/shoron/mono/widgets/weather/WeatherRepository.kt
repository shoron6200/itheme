package com.shoron.mono.widgets.weather

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shoron.mono.data.WidgetPreferences
import com.shoron.mono.data.WidgetSettings
import com.shoron.mono.monoContainer
import com.shoron.mono.power.Diagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit

private val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather")

/**
 * Weather from Open-Meteo with a persistent cache.
 *
 * Network use is limited to: one request when the location/unit changes, an hourly
 * WorkManager job (only with network, only while weather is configured), and an on-demand
 * refresh when a MONO screen becomes visible and the cache is older than [STALE_MS].
 */
class WeatherRepository(
    context: Context,
    private val preferences: WidgetPreferences,
) {
    private val appContext = context.applicationContext
    private val store = appContext.weatherDataStore
    private val mutex = Mutex()

    val snapshot: Flow<WeatherSnapshot?> = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> prefs[SNAPSHOT]?.let(OpenMeteo::decode) }
        .distinctUntilChanged()

    /** Fetches if forced or the cache is stale / for another place or unit. Returns success. */
    suspend fun refresh(force: Boolean = false): Boolean = mutex.withLock {
        val settings = preferences.widgets.first()
        val place = settings.weatherPlace ?: return false
        if (!settings.weatherEnabled) return false
        val cached = snapshot.first()
        val now = System.currentTimeMillis()
        val cacheValid = cached != null &&
            cached.latitude == place.latitude && cached.longitude == place.longitude &&
            cached.unit == settings.temperatureUnit &&
            now - cached.fetchedAtMillis < STALE_MS
        if (!force && cacheValid) return true
        Diagnostics.count(Diagnostics.WEATHER_REQUESTS)
        val result = runCatching {
            val body = httpGet(OpenMeteo.forecastUrl(place.latitude, place.longitude, settings.temperatureUnit))
            OpenMeteo.parseForecast(body, place, settings.temperatureUnit, now)
        }.getOrNull() ?: return false
        store.edit { it[SNAPSHOT] = OpenMeteo.encode(result) }
        true
    }

    suspend fun search(query: String): List<com.shoron.mono.data.WeatherPlace> {
        if (query.isBlank()) return emptyList()
        return runCatching {
            OpenMeteo.parseSearch(httpGet(OpenMeteo.searchUrl(query, Locale.getDefault().language.ifBlank { "en" })))
        }.getOrDefault(emptyList())
    }

    /** Keeps the hourly background refresh in line with the settings. */
    fun syncSchedule(settings: WidgetSettings) {
        val work = WorkManager.getInstance(appContext)
        if (settings.weatherEnabled && settings.weatherPlace != null) {
            val request = PeriodicWorkRequestBuilder<WeatherWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()
            work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            work.cancelUniqueWork(WORK_NAME)
        }
    }

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val STALE_MS = 30 * 60 * 1000L
        private const val WORK_NAME = "mono-weather-refresh"
        private val SNAPSHOT = stringPreferencesKey("snapshot")
    }
}

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ok = applicationContext.monoContainer.weatherRepository.refresh(force = false)
        return if (ok) Result.success() else Result.retry()
    }
}
