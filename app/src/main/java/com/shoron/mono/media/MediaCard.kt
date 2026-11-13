package com.shoron.mono.media

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(18.dp)
private val ArtShape = RoundedCornerShape(10.dp)

/** Minimal monochrome now-playing card with thin-line transport controls. */
@Composable
fun MediaCard(
    media: MediaInfo,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MonoColors.Ink.copy(alpha = 0.62f))
            .border(1.dp, MonoColors.Hairline, CardShape)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(media)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = MonoColors.Foreground, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                )
                if (media.subtitle.isNotBlank()) {
                    Text(
                        text = media.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = MonoColors.Muted, fontFamily = Outfit, fontSize = 12.sp),
                    )
                }
            }
            GlyphButton("Dismiss", 32.dp, onDismiss) { drawClose(MonoColors.Faint) }
        }
        Spacer(Modifier.height(6.dp))
        MediaProgress(media, onSeek)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val prevColor = if (media.canSkipPrevious) MonoColors.Foreground else MonoColors.Hairline
            val nextColor = if (media.canSkipNext) MonoColors.Foreground else MonoColors.Hairline
            GlyphButton("Previous", 44.dp, onPrevious, enabled = media.canSkipPrevious) { drawSkip(prevColor, forward = false) }
            Spacer(Modifier.width(18.dp))
            GlyphButton(if (media.isPlaying) "Pause" else "Play", 52.dp, onPlayPause, ring = true) {
                if (media.isPlaying) drawPause(MonoColors.Foreground) else drawPlay(MonoColors.Foreground)
            }
            Spacer(Modifier.width(18.dp))
            GlyphButton("Next", 44.dp, onNext, enabled = media.canSkipNext) { drawSkip(nextColor, forward = true) }
        }
    }
}

@Composable
private fun Artwork(media: MediaInfo) {
    val art = media.artwork
    val image = remember(art) { art?.asImageBitmap() }
    Box(
        Modifier
            .size(52.dp)
            .clip(ArtShape)
            .background(MonoColors.Graphite),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp))
        } else {
            Canvas(Modifier.size(20.dp)) { drawPlay(MonoColors.Faint) }
        }
    }
}

/** Progress ticks every 500 ms only while playing and the screen is started. */
@Composable
private fun MediaProgress(media: MediaInfo, onSeek: (Long) -> Unit) {
    if (media.durationMs <= 0L) return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(media.isPlaying, media.positionMs, media.positionUpdatedAtMs) {
        now = SystemClock.elapsedRealtime()
        if (!media.isPlaying) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = SystemClock.elapsedRealtime()
                delay(500)
            }
        }
    }
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = media.durationMs
    val position = media.positionAt(now)
    val fraction = dragging ?: (position.toFloat() / duration).coerceIn(0f, 1f)

    if (media.canSeek) {
        Slider(
            value = fraction,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek((it * duration).toLong()) }
                dragging = null
            },
            colors = SliderDefaults.colors(
                thumbColor = MonoColors.Foreground,
                activeTrackColor = MonoColors.Foreground,
                inactiveTrackColor = MonoColors.Hairline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(end = 6.dp),
        )
    } else {
        LinearProgressIndicator(
            progress = { fraction },
            color = MonoColors.Foreground,
            trackColor = MonoColors.Hairline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .padding(end = 6.dp)
                .height(2.dp),
        )
    }
    Row(Modifier.fillMaxWidth().padding(end = 6.dp)) {
        val style = TextStyle(color = MonoColors.Faint, fontFamily = Outfit, fontSize = 10.sp)
        Text(MediaFormat.time(dragging?.let { (it * duration).toLong() } ?: position), style = style)
        Spacer(Modifier.weight(1f))
        Text(MediaFormat.time(duration), style = style)
    }
}

@Composable
private fun GlyphButton(
    label: String,
    size: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    ring: Boolean = false,
    draw: DrawScope.() -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (ring) Modifier.border(1.2.dp, MonoColors.Foreground, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.36f)) { draw() }
    }
}

// ---------------------------------------------------------------- thin-line glyphs

private fun DrawScope.stroke() = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawPlay(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.22f, h * 0.08f)
        lineTo(w * 0.92f, h * 0.5f)
        lineTo(w * 0.22f, h * 0.92f)
        close()
    }
    drawPath(path, color, style = stroke())
}

private fun DrawScope.drawPause(color: Color) {
    val s = stroke()
    drawLine(color, Offset(size.width * 0.3f, size.height * 0.1f), Offset(size.width * 0.3f, size.height * 0.9f), s.width, StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.7f, size.height * 0.1f), Offset(size.width * 0.7f, size.height * 0.9f), s.width, StrokeCap.Round)
}

private fun DrawScope.drawSkip(color: Color, forward: Boolean) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        if (forward) {
            moveTo(w * 0.1f, h * 0.12f); lineTo(w * 0.72f, h * 0.5f); lineTo(w * 0.1f, h * 0.88f); close()
        } else {
            moveTo(w * 0.9f, h * 0.12f); lineTo(w * 0.28f, h * 0.5f); lineTo(w * 0.9f, h * 0.88f); close()
        }
    }
    drawPath(path, color, style = stroke())
    val x = if (forward) w * 0.9f else w * 0.1f
    drawLine(color, Offset(x, h * 0.12f), Offset(x, h * 0.88f), stroke().width, StrokeCap.Round)
}

private fun DrawScope.drawClose(color: Color) {
    val s = stroke().width
    drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), s, StrokeCap.Round)
    drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), s, StrokeCap.Round)
}
