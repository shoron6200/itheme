package com.shoron.mono.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit

private val ChipShape = RoundedCornerShape(50)

/** Outlined pill button used by the quick panel and home edit mode. */
@Composable
fun MonoChip(label: String, active: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = TextStyle(
            color = if (active) MonoColors.Ink else MonoColors.Foreground,
            fontFamily = Outfit,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        ),
        modifier = modifier
            .pressScale(interaction)
            .clip(ChipShape)
            .background(if (active) MonoColors.Foreground else MonoColors.Charcoal.copy(alpha = 0.9f))
            .border(1.dp, MonoColors.Hairline, ChipShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}
