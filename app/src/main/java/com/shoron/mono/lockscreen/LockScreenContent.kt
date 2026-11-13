package com.shoron.mono.lockscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.R
import com.shoron.mono.core.design.LayoutMetrics
import com.shoron.mono.core.design.LegibleShadow
import com.shoron.mono.core.design.rememberLayoutMetrics
import com.shoron.mono.core.design.verticalText
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit
import com.shoron.mono.data.LockSettings
import com.shoron.mono.wallpaper.WallpaperImage
import com.shoron.mono.wallpaper.WallpaperSettings
import com.shoron.mono.widgets.clock.ClockFormatter
import com.shoron.mono.widgets.clock.ClockSnapshot
import com.shoron.mono.widgets.weather.WeatherSnapshot
import java.time.LocalTime

/**
 * MONO lock screen: large hour with smaller minutes and seconds, then vertical date, weather
 * and weekday typography reading bottom-to-top, over the selected ocean wallpaper.
 * Swipe up hands off to Android's own secure unlock.
 */
@Composable
fun LockScreenContent(
    wallpaper: WallpaperSettings?,
    lock: LockSettings,
    use24Hour: Boolean,
    showSeconds: Boolean,
    clock: ClockSnapshot,
    time: State<LocalTime>,
    weather: WeatherSnapshot?,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unlock by rememberUpdatedState(onUnlock)
    val thresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    Box(
        modifier
            .fillMaxSize()
            .background(MonoColors.Ink)
            .pointerInput(thresholdPx) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { change, amount ->
                        dragged += amount
                        change.consume()
                    },
                    onDragEnd = { if (dragged < -thresholdPx) unlock() },
                )
            },
    ) {
        wallpaper?.let {
            WallpaperImage(it.copy(dim = maxOf(it.dim, lock.dim)), Modifier.fillMaxSize())
        }
        // Contrast scrim behind the text column; strength follows the contrast setting.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to MonoColors.Ink.copy(alpha = 0.2f + lock.dim * 0.6f),
                        0.55f to Color.Transparent,
                    ),
                ),
        )
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val metrics = rememberLayoutMetrics(maxWidth, maxHeight)
            LockClock(
                use24Hour = use24Hour,
                showSeconds = showSeconds,
                clock = clock,
                time = time,
                weather = weather,
                metrics = metrics,
                modifier = Modifier.offset(x = maxWidth * lock.textX, y = maxHeight * lock.textY),
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MonoColors.Faint,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.lock_swipe_hint),
                    style = TextStyle(color = MonoColors.Faint, fontFamily = Outfit, fontSize = 11.sp, letterSpacing = 1.sp),
                )
            }
        }
    }
}

@Composable
private fun LockClock(
    use24Hour: Boolean,
    showSeconds: Boolean,
    clock: ClockSnapshot,
    time: State<LocalTime>,
    weather: WeatherSnapshot?,
    metrics: LayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val hour by remember(time) { derivedStateOf { time.value.hour } }
    val minute by remember(time) { derivedStateOf { time.value.minute } }
    val hourText = ClockFormatter.hour(hour, use24Hour).trimStart('0').ifEmpty { "0" }
    val minuteText = ClockFormatter.twoDigits(minute)

    Column(modifier.semantics { contentDescription = "$hourText:$minuteText, ${clock.dayMonth}" }) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = hourText,
                style = TextStyle(
                    color = MonoColors.Foreground,
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.sp(92f),
                    lineHeight = metrics.sp(92f),
                    letterSpacing = (-3).sp,
                    shadow = LegibleShadow,
                ),
            )
            Spacer(Modifier.width(metrics.dp(8f)))
            Column(Modifier.padding(top = metrics.dp(14f))) {
                Text(
                    text = minuteText,
                    style = TextStyle(
                        color = MonoColors.Foreground,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = metrics.sp(40f),
                        lineHeight = metrics.sp(40f),
                        shadow = LegibleShadow,
                    ),
                )
                if (showSeconds) LockSeconds(time, metrics)
            }
        }
        Spacer(Modifier.height(metrics.dp(10f)))
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(metrics.dp(2f)),
        ) {
            VerticalWord(clock.dayMonth, metrics.sp(34f), FontWeight.Bold, MonoColors.Foreground)
            weather?.let {
                VerticalWord(it.condition, metrics.sp(13f), FontWeight.Light, MonoColors.Muted)
            }
            VerticalWord(clock.dayShort, metrics.sp(26f), FontWeight.Bold, MonoColors.Foreground)
        }
    }
}

/** Only this tiny composable recomposes every second. */
@Composable
private fun LockSeconds(time: State<LocalTime>, metrics: LayoutMetrics) {
    Text(
        text = ClockFormatter.twoDigits(time.value.second),
        style = TextStyle(
            color = MonoColors.Muted,
            fontFamily = Outfit,
            fontWeight = FontWeight.Light,
            fontSize = metrics.sp(16f),
            shadow = LegibleShadow,
        ),
    )
}

@Composable
private fun VerticalWord(text: String, size: androidx.compose.ui.unit.TextUnit, weight: FontWeight, color: Color) {
    Text(
        text = text,
        modifier = Modifier.verticalText(),
        maxLines = 1,
        softWrap = false,
        style = TextStyle(
            color = color,
            fontFamily = Outfit,
            fontWeight = weight,
            fontSize = size,
            lineHeight = size,
            shadow = LegibleShadow,
        ),
    )
}
