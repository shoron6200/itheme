package com.shoron.mono.media

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class MediaInfo(
    /** Identifies the session + track, used to remember a user dismissal. */
    val key: String,
    val packageName: String,
    val title: String,
    val subtitle: String,
    val artwork: Bitmap?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val positionUpdatedAtMs: Long, // SystemClock.elapsedRealtime() base
    val speed: Float,
    val durationMs: Long,
    val canSeek: Boolean,
    val canSkipPrevious: Boolean,
    val canSkipNext: Boolean,
) {
    /** Current position extrapolated from the last playback-state update. */
    fun positionAt(elapsedRealtimeMs: Long): Long {
        val base = if (isPlaying) positionMs + ((elapsedRealtimeMs - positionUpdatedAtMs) * speed).toLong() else positionMs
        return if (durationMs > 0) base.coerceIn(0L, durationMs) else base.coerceAtLeast(0L)
    }
}

object MediaFormat {
    /** "3:07" / "1:02:45". */
    fun time(ms: Long): String {
        val total = (ms.coerceAtLeast(0L) / 1000L)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
