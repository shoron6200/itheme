package com.shoron.mono.widgets.battery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.shoron.mono.core.theme.MonoColors

/** Thin-line battery glyph with fill level and a bolt while charging, plus the percentage. */
@Composable
fun BatteryIndicator(
    state: BatteryState,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MonoColors.Foreground,
    showStatusLabel: Boolean = false,
) {
    val description = buildString {
        append("Battery ").append(state.percent).append('%')
        if (state.charging) append(", charging")
    }
    Row(
        modifier.semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryGlyph(state, color, Modifier.size(width = 22.dp, height = 11.dp))
        Spacer(Modifier.width(6.dp))
        val label = when {
            !showStatusLabel -> ""
            state.full -> " · full"
            state.charging -> " · charging"
            else -> ""
        }
        Text(text = "${state.percent}%$label", style = textStyle.copy(color = color))
    }
}

@Composable
fun BatteryGlyph(state: BatteryState, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.dp.toPx()
        val capW = 2.dp.toPx()
        val bodyW = size.width - capW - stroke
        val body = Size(bodyW, size.height - stroke)
        val radius = CornerRadius(2.5.dp.toPx())
        drawRoundRect(color, Offset(stroke / 2, stroke / 2), body, radius, style = Stroke(stroke))
        // Terminal cap.
        val capH = size.height * 0.42f
        drawRoundRect(
            color,
            Offset(bodyW + stroke, (size.height - capH) / 2),
            Size(capW, capH),
            CornerRadius(1.dp.toPx()),
        )
        // Fill level.
        val inset = stroke + 1.2.dp.toPx()
        val fillW = (bodyW - 2 * inset + stroke) * (state.percent / 100f)
        if (fillW > 0f) {
            drawRoundRect(
                color.copy(alpha = if (state.percent <= 15 && !state.charging) 0.55f else 0.9f),
                Offset(inset, inset),
                Size(fillW, size.height - 2 * inset),
                CornerRadius(1.dp.toPx()),
            )
        }
        if (state.charging) {
            val w = bodyW
            val h = size.height
            val bolt = Path().apply {
                moveTo(w * 0.56f, h * 0.10f)
                lineTo(w * 0.34f, h * 0.56f)
                lineTo(w * 0.50f, h * 0.56f)
                lineTo(w * 0.42f, h * 0.92f)
                lineTo(w * 0.68f, h * 0.42f)
                lineTo(w * 0.52f, h * 0.42f)
                close()
            }
            drawPath(bolt, MonoColors.Ink)
            drawPath(bolt, color, style = Stroke(0.8.dp.toPx()))
        }
    }
}
