package com.shoron.mono.wallpaper

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.wallpaperDataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper")

class WallpaperPreferences(context: Context) {

    private val store = context.applicationContext.wallpaperDataStore

    val settings: Flow<WallpaperSettings> = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { p ->
            val defaults = WallpaperSettings()
            WallpaperSettings(
                sourceId = p[SOURCE] ?: defaults.sourceId,
                biasX = p[BIAS_X] ?: defaults.biasX,
                biasY = p[BIAS_Y] ?: defaults.biasY,
                dim = p[DIM] ?: defaults.dim,
                monochrome = p[MONOCHROME] ?: defaults.monochrome,
                customVersion = p[CUSTOM_VERSION] ?: defaults.customVersion,
            )
        }
        .distinctUntilChanged()

    suspend fun save(settings: WallpaperSettings) {
        store.edit { p ->
            p[SOURCE] = settings.sourceId
            p[BIAS_X] = settings.biasX
            p[BIAS_Y] = settings.biasY
            p[DIM] = settings.dim
            p[MONOCHROME] = settings.monochrome
            p[CUSTOM_VERSION] = settings.customVersion
        }
    }

    private companion object {
        val SOURCE = stringPreferencesKey("source")
        val BIAS_X = floatPreferencesKey("bias_x")
        val BIAS_Y = floatPreferencesKey("bias_y")
        val DIM = floatPreferencesKey("dim")
        val MONOCHROME = booleanPreferencesKey("monochrome")
        val CUSTOM_VERSION = longPreferencesKey("custom_version")
    }
}
