package com.shoron.mono.widgets.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.core.design.LayoutMetrics
import com.shoron.mono.core.design.LegibleShadow
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.min

/**
 * Radial Digital clock: a large hour numeral beside a rotating minute dial. The current minute
 * sits in a capsule at 9 o'clock; minute marks fan out around the ring and a seconds dot orbits
 * it. Second-by-second motion is read only inside the Canvas draw phase, so the composable
 * itself recomposes at most once a minute.
 */
@Composable
fun RadialClock(
    time: State<LocalTime>,
    use24Hour: Boolean,
    showSeconds: Boolean,
    metrics: LayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val hour by remember(time) { derivedStateOf { time.value.hour } }
    val minute by remember(time) { derivedStateOf { time.value.minute } }
    val dial: Dp = metrics.dp(150f)
    val hourText = ClockFormatter.hour(hour, use24Hour)
    val minuteText = ClockFormatter.twoDigits(minute)

    Row(
        modifier.semantics { contentDescription = "$hourText:$minuteText" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = hourText,
            style = TextStyle(
                color = MonoColors.Foreground,
                fontFamily = Outfit,
                fontWeight = FontWeight.Light,
                fontSize = metrics.sp(64f),
                letterSpacing = (-1).sp,
                shadow = LegibleShadow,
            ),
        )
        Spacer(Modifier.width(metrics.dp(2f)))
        Box(Modifier.size(dial)) {
            MinuteDial(time, showSeconds, metrics, Modifier.fillMaxSize())
            // Capsule holding the current minute, anchored on the ring at 9 o'clock.
            val ringRadius = dial * RING
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = dial / 2 - ringRadius - metrics.dp(18f))
                    .background(MonoColors.Ink.copy(alpha = 0.72f), Capsule)
                    .border(1.2.dp, MonoColors.Foreground, Capsule)
                    .padding(horizontal = metrics.dp(8f), vertical = metrics.dp(2f)),
            ) {
                Text(
                    text = minuteText,
                    style = TextStyle(
                        color = MonoColors.Foreground,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = metrics.sp(20f),
                    ),
                )
            }
        }
    }
}

private const val RING = 0.36f
private val Capsule = RoundedCornerShape(50)

@Composable
private fun MinuteDial(
    time: State<LocalTime>,
    showSeconds: Boolean,
    metrics: LayoutMetrics,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val labelStyle = TextStyle(
        color = MonoColors.Foreground,
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.sp(9f),
    )
    val secondsStyle = TextStyle(
        color = MonoColors.Muted,
        fontFamily = Outfit,
        fontWeight = FontWeight.Light,
        fontSize = metrics.sp(10f),
    )
    // Label layouts are measured once per style, not every frame.
    val labels = remember(labelStyle) {
        (0 until 60 step 5).associateWith { measurer.measure(ClockFormatter.twoDigits(it), labelStyle) }
    }

    Canvas(modifier) {
        val now = time.value // draw-phase read: per-second updates only redraw
        val minute = now.minute
        val second = if (showSeconds) now.second else 0
        val cx = size.width / 2
        val cy = size.height / 2
        val side = min(size.width, size.height)
        val ring = side * RING
        val tickOuter = side * 0.47f

        for (mark in 0 until 60) {
            val angle = DialGeometry.angleOf(mark, minute, second)
            val fade = fadeNearAnchor(angle)
            if (fade <= 0.02f) continue
            val major = mark % 5 == 0
            val inner = if (major) tickOuter - side * 0.06f else tickOuter - side * 0.03f
            val (x1, y1) = DialGeometry.point(cx, cy, inner, angle)
            val (x2, y2) = DialGeometry.point(cx, cy, tickOuter, angle)
            drawLine(
                color = MonoColors.Foreground.copy(alpha = (if (major) 0.9f else 0.35f) * fade),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = (if (major) 1.4f else 1f) * density,
                cap = StrokeCap.Round,
            )
            if (major) {
                val layout = labels.getValue(mark)
                val (lx, ly) = DialGeometry.point(cx, cy, ring, angle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(lx - layout.size.width / 2f, ly - layout.size.height / 2f),
                    alpha = fade,
                )
            }
        }

        if (showSeconds) {
            // Seconds dot orbiting from the capsule, plus small seconds digits inside the ring.
            val secAngle = DialGeometry.ANCHOR_DEGREES + now.second * 6f
            val (sx, sy) = DialGeometry.point(cx, cy, tickOuter + side * 0.025f, secAngle)
            drawCircle(MonoColors.Foreground, radius = 2.2f * density, center = Offset(sx, sy))
            val digits = measurer.measure(ClockFormatter.twoDigits(now.second), secondsStyle)
            drawText(
                textLayoutResult = digits,
                topLeft = Offset(cx - digits.size.width / 2f, cy - digits.size.height / 2f),
            )
        }
    }
}

/** 0 near the capsule (hidden behind it), rising to 1 away from it. */
private fun fadeNearAnchor(angle: Float): Float {
    val distance = abs(angle - DialGeometry.ANCHOR_DEGREES)
    return ((distance - 14f) / 22f).coerceIn(0f, 1f)
}
