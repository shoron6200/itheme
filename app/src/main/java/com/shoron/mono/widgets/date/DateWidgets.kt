package com.shoron.mono.widgets.date

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.core.design.LayoutMetrics
import com.shoron.mono.core.design.LegibleShadow
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit
import com.shoron.mono.core.theme.Sacramento
import com.shoron.mono.widgets.clock.ClockSnapshot

/** Small spaced date line, e.g. "FRI  13 NOV 2026". */
@Composable
fun DateLine(clock: ClockSnapshot, metrics: LayoutMetrics, modifier: Modifier = Modifier) {
    Text(
        text = clock.dateLine,
        modifier = modifier,
        style = TextStyle(
            color = MonoColors.Muted,
            fontFamily = Outfit,
            fontSize = metrics.sp(11f),
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
            shadow = LegibleShadow,
        ),
    )
}

/** Large bold month name, the editorial headline. */
@Composable
fun MonthTitle(clock: ClockSnapshot, metrics: LayoutMetrics, modifier: Modifier = Modifier) {
    Text(
        text = clock.month,
        modifier = modifier,
        maxLines = 1,
        style = TextStyle(
            color = MonoColors.Foreground,
            fontFamily = Outfit,
            fontSize = metrics.sp(44f),
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
            shadow = LegibleShadow,
        ),
    )
}

/** Script weekday with a hairline drawn through it. */
@Composable
fun WeekdayScript(clock: ClockSnapshot, metrics: LayoutMetrics, modifier: Modifier = Modifier) {
    val strike = MonoColors.Muted
    Text(
        text = clock.dayOfWeek,
        modifier = modifier.drawBehind {
            val y = size.height * 0.58f
            drawLine(
                color = strike,
                start = Offset(-size.width * 0.08f, y),
                end = Offset(size.width * 1.04f, y),
                strokeWidth = 1.dp.toPx(),
            )
        },
        style = TextStyle(
            color = MonoColors.Foreground,
            fontFamily = Sacramento,
            fontSize = metrics.sp(40f),
            shadow = LegibleShadow,
        ),
    )
}
