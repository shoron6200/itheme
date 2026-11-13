package com.shoron.mono.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Watches active media sessions from any app (music or video) using MediaSessionManager.
 * Cold flow: listeners exist only while a visible MONO screen collects it. No playback service.
 * Emits null when there is no usable session or notification access is not granted.
 */
class MediaSessionRepository(context: Context) {

    private val appContext = context.applicationContext
    private val listener = ComponentName(appContext, MediaAccessService::class.java)

    @Volatile
    private var active: MediaController? = null

    fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(appContext).contains(appContext.packageName)

    val media: Flow<MediaInfo?> = callbackFlow {
        val manager = appContext.getSystemService(MediaSessionManager::class.java)
        if (manager == null || !hasAccess()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val handler = Handler(Looper.getMainLooper())
        var controller: MediaController? = null

        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) { trySend(controller?.let(::snapshot)) }
            override fun onMetadataChanged(metadata: MediaMetadata?) { trySend(controller?.let(::snapshot)) }
            override fun onSessionDestroyed() {
                select(runCatching { manager.getActiveSessions(listener) }.getOrDefault(emptyList()))
            }
        }

        fun attach(next: MediaController?) {
            if (next?.sessionToken == controller?.sessionToken && next != null) return
            controller?.unregisterCallback(callback)
            controller = next
            active = next
            next?.registerCallback(callback, handler)
            trySend(next?.let(::snapshot))
        }

        // Prefer a playing session, then a paused one; ignore stopped/none sessions.
        fun choose(list: List<MediaController>): MediaController? =
            list.firstOrNull { it.playbackState?.state in PLAYING_STATES }
                ?: list.firstOrNull { it.playbackState?.state in PAUSED_STATES }

        val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
            attach(choose(list.orEmpty()))
        }
        selectRef = { list -> attach(choose(list)) }

        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, listener, handler)
            attach(choose(manager.getActiveSessions(listener)))
        } catch (_: SecurityException) {
            trySend(null) // access revoked
        }

        awaitClose {
            runCatching { manager.removeOnActiveSessionsChangedListener(sessionsListener) }
            controller?.unregisterCallback(callback)
            controller = null
            active = null
            selectRef = null
        }
    }.conflate().distinctUntilChanged()

    @Volatile
    private var selectRef: ((List<MediaController>) -> Unit)? = null

    private fun select(list: List<MediaController>) {
        selectRef?.invoke(list)
    }

    // ------------------------------------------------------------------ transport controls

    fun playPause() {
        val c = active ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() { active?.transportControls?.skipToNext() }

    fun previous() { active?.transportControls?.skipToPrevious() }

    fun seekTo(positionMs: Long) { active?.transportControls?.seekTo(positionMs) }

    // ------------------------------------------------------------------ mapping

    private fun snapshot(controller: MediaController): MediaInfo? {
        val state = controller.playbackState ?: return null
        if (state.state !in PLAYING_STATES && state.state !in PAUSED_STATES) return null
        val meta = controller.metadata ?: return null
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: return null
        val subtitle = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val art = meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val actions = state.actions
        return MediaInfo(
            key = "${controller.packageName}|$title|$subtitle",
            packageName = controller.packageName,
            title = title,
            subtitle = subtitle,
            artwork = art?.let(::downscale),
            isPlaying = state.state == PlaybackState.STATE_PLAYING || state.state == PlaybackState.STATE_BUFFERING,
            positionMs = state.position.coerceAtLeast(0L),
            positionUpdatedAtMs = state.lastPositionUpdateTime,
            speed = if (state.playbackSpeed == 0f) 1f else state.playbackSpeed,
            durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L),
            canSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L,
            canSkipPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
        )
    }

    private var artSource: Bitmap? = null
    private var artScaled: Bitmap? = null

    /** Keeps artwork small (the card shows it at ~56dp) and reuses it across state updates. */
    @Synchronized
    private fun downscale(bitmap: Bitmap): Bitmap {
        if (bitmap === artSource) artScaled?.let { return it }
        artSource = bitmap
        artScaled = scaleDown(bitmap)
        return artScaled!!
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val max = 256
        if (bitmap.width <= max && bitmap.height <= max) return bitmap
        val scale = max.toFloat() / maxOf(bitmap.width, bitmap.height)
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        val PLAYING_STATES = setOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_CONNECTING,
        )
        val PAUSED_STATES = setOf(PlaybackState.STATE_PAUSED)
    }
}
