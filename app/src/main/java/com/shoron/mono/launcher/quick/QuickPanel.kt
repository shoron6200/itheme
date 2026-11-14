package com.shoron.mono.launcher.quick

import android.graphics.Rect
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.R
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppIcon
import com.shoron.mono.core.design.LaunchBounds
import com.shoron.mono.core.design.MonoChip
import com.shoron.mono.core.design.pressScale
import com.shoron.mono.core.design.trackBounds
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit

private val PanelShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
private val ChipShape = RoundedCornerShape(50)

/** Swipe-down panel: search entry, recent apps and quick system actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPanel(
    recents: List<AppEntry>,
    onSearch: () -> Unit,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onOpenMonoSettings: () -> Unit,
    onNotifications: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val torchId = remember { QuickActions.torchCameraId(context) }
    var torchOn by remember { mutableStateOf(false) }

    // Track the real torch state only while the panel is visible.
    DisposableEffect(torchId) {
        val manager = context.getSystemService(CameraManager::class.java)
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == torchId) torchOn = enabled
            }
        }
        if (torchId != null) manager?.registerTorchCallback(callback, null)
        onDispose { if (torchId != null) manager?.unregisterTorchCallback(callback) }
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(MonoColors.Ink.copy(alpha = 0.94f))
            .border(1.dp, MonoColors.Hairline, PanelShape)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 20.dp),
    ) {
        // Search entry (opens the drawer with the keyboard).
        Row(
            Modifier
                .fillMaxWidth()
                .clip(ChipShape)
                .background(MonoColors.Charcoal)
                .clickable(onClick = onSearch)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MonoColors.Faint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.quick_search), color = MonoColors.Faint, fontFamily = Outfit, fontSize = 15.sp)
        }

        if (recents.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.drawer_recent).uppercase(),
                color = MonoColors.Faint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                recents.take(5).forEach { entry ->
                    val interaction = remember(entry.key) { MutableInteractionSource() }
                    val bounds = remember(entry.key) { LaunchBounds() }
                    Column(
                        Modifier
                            .width(56.dp)
                            .trackBounds(bounds)
                            .pressScale(interaction)
                            .clickable(interactionSource = interaction, indication = null) { onLaunch(entry, bounds.rect) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIcon(entry, 32.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(entry.label, color = MonoColors.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MonoChip(stringResource(R.string.quick_internet)) { QuickActions.openInternet(context); onClose() }
            MonoChip(stringResource(R.string.quick_bluetooth)) { QuickActions.openBluetooth(context); onClose() }
            if (torchId != null) {
                MonoChip(
                    stringResource(if (torchOn) R.string.quick_torch_on else R.string.quick_torch_off),
                    active = torchOn,
                ) { QuickActions.setTorch(context, !torchOn) }
            }
            MonoChip(stringResource(R.string.quick_camera)) { QuickActions.openCamera(context); onClose() }
            MonoChip(stringResource(R.string.quick_notifications)) { onNotifications() }
            MonoChip(stringResource(R.string.quick_settings)) { QuickActions.openSettings(context); onClose() }
            MonoChip(stringResource(R.string.quick_mono)) { onOpenMonoSettings(); onClose() }
        }
    }
}
