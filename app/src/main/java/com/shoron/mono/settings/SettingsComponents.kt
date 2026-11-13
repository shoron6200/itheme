package com.shoron.mono.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.R
import com.shoron.mono.core.theme.MonoColors

/** Shared building blocks for MONO settings pages (one look, no duplicated widgets). */

@Composable
internal fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                tint = MonoColors.Foreground,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = title, color = MonoColors.Foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun SettingsSectionTitle(text: String) {
    Column {
        Spacer(Modifier.height(20.dp))
        Text(
            text = text.uppercase(),
            color = MonoColors.Faint,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun SettingsNote(text: String, modifier: Modifier = Modifier) {
    Text(text = text, color = MonoColors.Faint, fontSize = 12.sp, modifier = modifier)
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MonoColors.Foreground, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MonoColors.Ink,
                checkedTrackColor = MonoColors.Foreground,
                uncheckedThumbColor = MonoColors.Muted,
                uncheckedTrackColor = MonoColors.Graphite,
                uncheckedBorderColor = MonoColors.Hairline,
            ),
        )
    }
}

@Composable
internal fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MonoColors.Foreground,
                unselectedColor = MonoColors.Muted,
            ),
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = MonoColors.Foreground, fontSize = 15.sp)
    }
}

/** Slider that edits locally while dragging and persists once on release (fewer disk writes). */
@Composable
internal fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onCommit: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MonoColors.Muted, fontSize = 14.sp, modifier = Modifier.width(132.dp))
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MonoColors.Foreground,
                activeTrackColor = MonoColors.Foreground,
                inactiveTrackColor = MonoColors.Hairline,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}
