package com.shoron.mono.widgets.clock

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.shoron.mono.core.design.LayoutMetrics
import com.shoron.mono.core.design.LegibleShadow
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit

/** Editorial Word Clock: bold hour run into light minutes, e.g. "**four**thirty eight". */
@Composable
fun WordClock(clock: ClockSnapshot, metrics: LayoutMetrics, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(clock.hourWord) }
            withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append(clock.minuteWords) }
        },
        modifier = modifier,
        style = TextStyle(
            color = MonoColors.Foreground,
            fontFamily = Outfit,
            fontSize = metrics.sp(15f),
            shadow = LegibleShadow,
        ),
    )
}
