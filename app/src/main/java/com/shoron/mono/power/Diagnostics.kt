package com.shoron.mono.power

import android.content.Context
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight on-device counters so battery/lifecycle behaviour can be measured on the real
 * phone (Settings → About → Diagnostics). Counters are in memory since the process started,
 * except [PROCESS_STARTS], which is persisted to reveal process death (e.g. OEM freezing).
 * Incrementing is a single atomic add — no logging, no I/O on hot paths.
 */
object Diagnostics {
    const val PROCESS_STARTS = "process_starts"
    const val LOCK_SHOWN = "lock_shown"
    const val LOCK_UNLOCK_CHECKS = "lock_unlock_checks"
    const val SCREEN_OFF_REQUESTS = "screen_off_requests"
    const val POCKET_EVENTS = "pocket_events"
    const val WEATHER_REQUESTS = "weather_requests"
    const val WALLPAPER_DECODES = "wallpaper_decodes"
    const val SECOND_TICKS = "second_ticks"

    private val counters = ConcurrentHashMap<String, AtomicLong>()
    val processStartElapsed: Long = SystemClock.elapsedRealtime()
    @Volatile var persistedProcessStarts: Long = 0
        private set

    fun count(name: String) {
        counters.getOrPut(name) { AtomicLong() }.incrementAndGet()
    }

    fun value(name: String): Long = counters[name]?.get() ?: 0L

    /** Called once from Application.onCreate. */
    fun onProcessStart(context: Context) {
        val prefs = context.getSharedPreferences("diagnostics", Context.MODE_PRIVATE)
        persistedProcessStarts = prefs.getLong(PROCESS_STARTS, 0) + 1
        prefs.edit().putLong(PROCESS_STARTS, persistedProcessStarts).apply()
    }

    fun uptimeMinutes(): Long = (SystemClock.elapsedRealtime() - processStartElapsed) / 60_000L
}
