package com.shoron.mono.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaInfoTest {

    private fun info(playing: Boolean, position: Long = 10_000, updated: Long = 1_000, speed: Float = 1f, duration: Long = 60_000) =
        MediaInfo(
            key = "k", packageName = "p", title = "t", subtitle = "s", artwork = null,
            isPlaying = playing, positionMs = position, positionUpdatedAtMs = updated, speed = speed,
            durationMs = duration, canSeek = true, canSkipPrevious = true, canSkipNext = true,
        )

    @Test fun extrapolatesWhilePlaying() {
        assertEquals(15_000L, info(playing = true).positionAt(6_000))
        assertEquals(20_000L, info(playing = true, speed = 2f).positionAt(6_000))
    }

    @Test fun holdsWhilePausedAndClampsToDuration() {
        assertEquals(10_000L, info(playing = false).positionAt(99_000))
        assertEquals(60_000L, info(playing = true).positionAt(500_000))
    }

    @Test fun formatsTime() {
        assertEquals("0:00", MediaFormat.time(0))
        assertEquals("3:07", MediaFormat.time(187_000))
        assertEquals("1:02:45", MediaFormat.time(3_765_000))
    }
}
