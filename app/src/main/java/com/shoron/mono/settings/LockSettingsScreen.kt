package com.shoron.mono.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoron.mono.R
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.data.LockSettings
import com.shoron.mono.lockscreen.LockScreenActivity
import kotlinx.coroutines.launch

/** MONO lock screen: enable, permission status, position, contrast, preview, wallpaper sync. */
@Composable
fun LockSettingsScreen(onBack: () -> Unit) {
    val viewModel: WidgetSettingsViewModel = viewModel(factory = WidgetSettingsViewModel.Factory)
    val lockState by viewModel.lock.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overlayAllowed by remember { mutableStateOf(viewModel.canShowLockFromBackground()) }
    var syncing by remember { mutableStateOf(false) }
    var mediaAllowed by remember { mutableStateOf(viewModel.hasMediaAccess()) }
    LifecycleResumeEffect(Unit) {
        overlayAllowed = viewModel.canShowLockFromBackground()
        mediaAllowed = viewModel.hasMediaAccess()
        onPauseOrDispose { }
    }
    val lock = lockState ?: return

    Column(
        Modifier
            .fillMaxSize()
            .background(MonoColors.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 24.dp, bottom = 32.dp),
    ) {
        SettingsTopBar(stringResource(R.string.settings_lock), onBack)
        Column(Modifier.padding(start = 12.dp)) {
            SwitchRow(stringResource(R.string.lock_enable), lock.enabled) { on ->
                viewModel.updateLock { it.copy(enabled = on) }
            }
            SettingsNote(stringResource(R.string.lock_enable_hint))

            SettingsSectionTitle(stringResource(R.string.lock_overlay))
            Text(
                text = stringResource(if (overlayAllowed) R.string.lock_overlay_granted else R.string.lock_overlay_missing),
                color = if (overlayAllowed) MonoColors.Muted else MonoColors.Foreground,
                fontSize = 14.sp,
                modifier = Modifier.clickable(enabled = !overlayAllowed) { openOverlaySettings(context) },
            )

            SettingsSectionTitle(stringResource(R.string.lock_media))
            Text(
                text = stringResource(if (mediaAllowed) R.string.lock_media_granted else R.string.lock_media_missing),
                color = if (mediaAllowed) MonoColors.Muted else MonoColors.Foreground,
                fontSize = 14.sp,
                modifier = Modifier.clickable { openNotificationAccess(context) },
            )

            SettingsSectionTitle(stringResource(R.string.settings_appearance))
            SliderRow(stringResource(R.string.lock_position_x), lock.textX, LockSettings.X_RANGE) { v ->
                viewModel.updateLock { it.copy(textX = v) }
            }
            SliderRow(stringResource(R.string.lock_position_y), lock.textY, LockSettings.Y_RANGE) { v ->
                viewModel.updateLock { it.copy(textY = v) }
            }
            SliderRow(stringResource(R.string.lock_contrast), lock.dim, 0f..0.8f) { v ->
                viewModel.updateLock { it.copy(dim = v) }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Button(
                    onClick = { context.startActivity(LockScreenActivity.previewIntent(context)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MonoColors.Foreground,
                        contentColor = MonoColors.Ink,
                    ),
                ) { Text(stringResource(R.string.lock_preview), fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = {
                    viewModel.updateLock {
                        it.copy(textX = LockSettings.DEFAULT_X, textY = LockSettings.DEFAULT_Y)
                    }
                }) { Text(stringResource(R.string.wallpaper_reset), color = MonoColors.Muted) }
            }

            SettingsSectionTitle(stringResource(R.string.settings_wallpaper))
            OutlinedButton(
                enabled = !syncing,
                onClick = {
                    syncing = true
                    scope.launch {
                        val ok = viewModel.syncLockWallpaper()
                        syncing = false
                        Toast.makeText(
                            context,
                            if (ok) R.string.lock_sync_done else R.string.lock_sync_failed,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            ) { Text(stringResource(R.string.lock_sync_wallpaper), color = MonoColors.Foreground) }

            Spacer(Modifier.height(24.dp))
            SettingsNote(stringResource(R.string.lock_limits))
        }
    }
}

private fun openNotificationAccess(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
