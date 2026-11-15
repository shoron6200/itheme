package com.shoron.mono.settings

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoron.mono.BuildConfig
import com.shoron.mono.R
import com.shoron.mono.apps.AppIcon
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.data.PinList
import com.shoron.mono.power.Diagnostics
import com.shoron.mono.power.ScreenControl

private enum class SettingsPage { Main, Wallpaper, Widgets, Lock, Home, Icons }

/** Two-page settings flow without a navigation library: main list and wallpaper editor. */
@Composable
fun SettingsNavHost(onExit: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Main) }
    BackHandler(enabled = page != SettingsPage.Main) { page = SettingsPage.Main }
    when (page) {
        SettingsPage.Main -> SettingsScreen(
            onBack = onExit,
            onOpenWallpaper = { page = SettingsPage.Wallpaper },
            onOpenWidgets = { page = SettingsPage.Widgets },
            onOpenLock = { page = SettingsPage.Lock },
            onOpenHome = { page = SettingsPage.Home },
            onOpenIcons = { page = SettingsPage.Icons },
        )
        SettingsPage.Wallpaper -> WallpaperScreen(onBack = { page = SettingsPage.Main })
        SettingsPage.Widgets -> WidgetsScreen(onBack = { page = SettingsPage.Main })
        SettingsPage.Lock -> LockSettingsScreen(onBack = { page = SettingsPage.Main })
        SettingsPage.Home -> HomeSettingsScreen(onBack = { page = SettingsPage.Main })
        SettingsPage.Icons -> IconsScreen(onBack = { page = SettingsPage.Main })
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenLock: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenIcons: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val pinned by viewModel.pinnedApps.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isDefault by remember { mutableStateOf(DefaultHome.isDefault(context)) }
    LifecycleResumeEffect(Unit) {
        isDefault = DefaultHome.isDefault(context)
        onPauseOrDispose { }
    }
    val roleRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        isDefault = DefaultHome.isDefault(context)
        if (!isDefault && result.resultCode != Activity.RESULT_OK) {
            DefaultHome.openSystemHomeSettings(context)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoColors.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                        tint = MonoColors.Foreground,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_title),
                    color = MonoColors.Foreground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            SectionTitle(stringResource(R.string.settings_default_home))
            Text(
                text = stringResource(
                    if (isDefault) R.string.settings_default_home_active
                    else R.string.settings_default_home_inactive,
                ),
                color = MonoColors.Muted,
                fontSize = 14.sp,
            )
            if (!isDefault) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    val request = DefaultHome.roleRequestIntent(context)
                    if (request != null) roleRequest.launch(request)
                    else DefaultHome.openSystemHomeSettings(context)
                }) {
                    Text(stringResource(R.string.settings_default_home_button), color = MonoColors.Foreground)
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        item {
            SectionTitle(stringResource(R.string.settings_appearance))
            NavRow(stringResource(R.string.settings_wallpaper), onOpenWallpaper)
            NavRow(stringResource(R.string.settings_widgets), onOpenWidgets)
            NavRow(stringResource(R.string.settings_lock), onOpenLock)
            NavRow(stringResource(R.string.settings_home_gestures), onOpenHome)
            NavRow(stringResource(R.string.icons_title), onOpenIcons)
            Spacer(Modifier.height(24.dp))
        }

        item {
            SectionTitle(stringResource(R.string.settings_home_apps))
            Text(
                text = stringResource(R.string.settings_home_apps_hint, PinList.MAX_PINS),
                color = MonoColors.Muted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            if (pinned.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_home_apps_empty),
                    color = MonoColors.Faint,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }

        itemsIndexed(pinned, key = { _, entry -> entry.key }) { index, entry ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(entry, 28.dp)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = entry.label,
                    color = MonoColors.Foreground,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.move(entry, -1) }, enabled = index > 0) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.settings_move_up),
                        tint = if (index > 0) MonoColors.Foreground else MonoColors.Hairline,
                    )
                }
                IconButton(onClick = { viewModel.move(entry, 1) }, enabled = index < pinned.lastIndex) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.settings_move_down),
                        tint = if (index < pinned.lastIndex) MonoColors.Foreground else MonoColors.Hairline,
                    )
                }
                IconButton(onClick = { viewModel.unpin(entry) }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_unpin),
                        tint = MonoColors.Muted,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            SectionTitle(stringResource(R.string.settings_about))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                color = MonoColors.Muted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            DiagnosticsBlock()
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_licenses),
                color = MonoColors.Faint,
                fontSize = 12.sp,
            )
        }
    }
}

/** On-device measurements for battery/lifecycle investigation. */
@Composable
private fun DiagnosticsBlock() {
    val context = LocalContext.current
    val lines = remember {
        buildList {
            add("MONO process starts (all time): ${Diagnostics.persistedProcessStarts}")
            add("Process uptime: ${Diagnostics.uptimeMinutes()} min")
            add("Lock screen shown: ${Diagnostics.value(Diagnostics.LOCK_SHOWN)}")
            add("Unlock checks: ${Diagnostics.value(Diagnostics.LOCK_UNLOCK_CHECKS)}")
            add("Screen-off requests: ${Diagnostics.value(Diagnostics.SCREEN_OFF_REQUESTS)}")
            add("Pocket detections: ${Diagnostics.value(Diagnostics.POCKET_EVENTS)}")
            add("Seconds ticks: ${Diagnostics.value(Diagnostics.SECOND_TICKS)}")
            add("Weather requests: ${Diagnostics.value(Diagnostics.WEATHER_REQUESTS)}")
            add("Wallpaper decodes: ${Diagnostics.value(Diagnostics.WALLPAPER_DECODES)}")
            add("Battery optimisation exempt: ${ScreenControl.isIgnoringBatteryOptimizations(context)}")
            add("Screen-off access: ${ScreenControl.isAccessibilityEnabled(context)}")
        }
    }
    Text(
        stringResource(R.string.settings_diagnostics).uppercase(),
        color = MonoColors.Faint,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
    )
    Spacer(Modifier.height(6.dp))
    lines.forEach { Text(it, color = MonoColors.Muted, fontSize = 12.sp) }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = MonoColors.Foreground, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MonoColors.Muted)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
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
