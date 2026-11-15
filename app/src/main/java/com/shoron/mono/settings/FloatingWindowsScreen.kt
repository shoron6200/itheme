package com.shoron.mono.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.R
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.launcher.FloatingWindows

/** Honest status of floating-window support and how to use Realme's own feature. */
@Composable
fun FloatingWindowsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val supported = remember { FloatingWindows.isSupported(context) }
    Column(
        Modifier
            .fillMaxSize()
            .background(MonoColors.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 24.dp, bottom = 32.dp),
    ) {
        SettingsTopBar(stringResource(R.string.floating_title), onBack)
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                stringResource(if (supported) R.string.floating_supported else R.string.floating_not_supported),
                color = MonoColors.Foreground,
                fontSize = 15.sp,
            )
            SettingsSectionTitle(stringResource(R.string.floating_gesture_title))
            SettingsNote(stringResource(R.string.floating_gesture_body))
            SettingsSectionTitle(stringResource(R.string.floating_realme_title))
            SettingsNote(stringResource(R.string.floating_realme_body))
            SettingsSectionTitle(stringResource(R.string.floating_mono_title))
            SettingsNote(stringResource(if (supported) R.string.floating_mono_on else R.string.floating_mono_off))
        }
    }
}
