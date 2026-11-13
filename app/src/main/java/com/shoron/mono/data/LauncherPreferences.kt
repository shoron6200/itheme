package com.shoron.mono.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher")

/** Persistent launcher settings (DataStore). */
class LauncherPreferences(context: Context) {

    private val store = context.applicationContext.launcherDataStore

    /** Ordered pinned app keys, or `null` before first-run defaults have been chosen. */
    val pinnedKeys: Flow<List<String>?> = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            if (prefs[PINS_INITIALIZED] == true) PinList.decode(prefs[PINNED].orEmpty()) else null
        }
        .distinctUntilChanged()

    suspend fun initializePins(keys: List<String>) {
        store.edit { prefs ->
            if (prefs[PINS_INITIALIZED] != true) {
                prefs[PINNED] = PinList.encode(keys)
                prefs[PINS_INITIALIZED] = true
            }
        }
    }

    suspend fun setPinned(keys: List<String>) {
        store.edit { prefs ->
            prefs[PINNED] = PinList.encode(keys.distinct())
            prefs[PINS_INITIALIZED] = true
        }
    }

    /**
     * Pins or unpins [key]. Keys of apps that are no longer installed ([validKeys]) are pruned
     * first so they do not count against the limit.
     */
    suspend fun togglePin(key: String, validKeys: Set<String>): PinResult {
        var result = PinResult.Unpinned
        store.edit { prefs ->
            val current = PinList.decode(prefs[PINNED].orEmpty()).filter { it in validKeys || it == key }
            val (next, outcome) = PinList.toggle(current, key)
            result = outcome
            prefs[PINNED] = PinList.encode(next)
            prefs[PINS_INITIALIZED] = true
        }
        return result
    }

    suspend fun unpin(key: String) {
        store.edit { prefs ->
            prefs[PINNED] = PinList.encode(PinList.decode(prefs[PINNED].orEmpty()) - key)
            prefs[PINS_INITIALIZED] = true
        }
    }

    private companion object {
        val PINNED = stringPreferencesKey("pinned_apps")
        val PINS_INITIALIZED = booleanPreferencesKey("pins_initialized")
    }
}
