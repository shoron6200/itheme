package com.shoron.mono.launcher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.R
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppIcon
import com.shoron.mono.core.design.LayoutMetrics
import com.shoron.mono.core.design.rememberLayoutMetrics
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit
import com.shoron.mono.core.theme.Sacramento
import com.shoron.mono.launcher.AppAction
import com.shoron.mono.launcher.AppActionsMenu
import com.shoron.mono.widgets.clock.ClockSnapshot

/**
 * Editorial, left-aligned home screen:
 *   status line · month · day · time in words · accent bar · vertical app shortcuts.
 * The wallpaper is drawn behind this screen by LauncherRoot; Phases 3–4 fill the status slots.
 */
@Composable
fun HomeScreen(
    clock: ClockSnapshot,
    state: HomeUiState,
    onLaunch: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUnpin: (AppEntry) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeUp by rememberUpdatedState(onOpenDrawer)
    val longPress by rememberUpdatedState(onOpenSettings)
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { longPress() }) }
            .pointerInput(swipeThresholdPx) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { change, amount ->
                        dragged += amount
                        change.consume()
                    },
                    onDragEnd = { if (dragged < -swipeThresholdPx) swipeUp() },
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val metrics = rememberLayoutMetrics(maxWidth, maxHeight)
        val usableHeight = maxHeight
        if (metrics.isLandscape) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = metrics.gutter / 2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    StatusLine(clock, metrics)
                    Spacer(Modifier.height(metrics.dp(16f)))
                    ClockBlock(clock, metrics)
                }
                PinnedAppList(
                    state.pinnedApps, metrics, onLaunch, onAppInfo, onUnpin, onOpenSettings,
                    Modifier.weight(1f),
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = metrics.gutter, end = metrics.gutter / 2),
            ) {
                Spacer(Modifier.height(usableHeight * 0.05f))
                StatusLine(clock, metrics)
                Spacer(Modifier.height(usableHeight * 0.08f))
                ClockBlock(clock, metrics)
                Spacer(Modifier.weight(1f))
                PinnedAppList(state.pinnedApps, metrics, onLaunch, onAppInfo, onUnpin, onOpenSettings)
                Spacer(Modifier.height(usableHeight * 0.06f))
            }
        }
    }
}

/** Soft shadow so editorial type stays legible over bright foam or user photos. */
private val Legible = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 1f), blurRadius = 8f)

@Composable
private fun StatusLine(clock: ClockSnapshot, metrics: LayoutMetrics) {
    Text(
        text = clock.dateLine,
        style = TextStyle(
            color = MonoColors.Muted,
            fontFamily = Outfit,
            fontSize = metrics.sp(11f),
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
            shadow = Legible,
        ),
    )
}

@Composable
private fun ClockBlock(clock: ClockSnapshot, metrics: LayoutMetrics) {
    Column {
        Text(
            text = clock.month,
            maxLines = 1,
            style = TextStyle(
                color = MonoColors.Foreground,
                fontFamily = Outfit,
                fontSize = metrics.sp(44f),
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                shadow = Legible,
            ),
        )
        Spacer(Modifier.height(metrics.dp(14f)))
        // Script weekday with a hairline drawn through it, editorial style.
        val strike = MonoColors.Muted
        Text(
            text = clock.dayOfWeek,
            modifier = Modifier.drawBehind {
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
                shadow = Legible,
            ),
        )
        Spacer(Modifier.height(metrics.dp(6f)))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(clock.hourWord) }
                withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append(clock.minuteWords) }
            },
            style = TextStyle(
                color = MonoColors.Foreground,
                fontFamily = Outfit,
                fontSize = metrics.sp(15f),
                shadow = Legible,
            ),
        )
        Spacer(Modifier.height(metrics.dp(20f)))
        Box(
            Modifier
                .size(width = metrics.dp(26f), height = 2.dp)
                .background(MonoColors.Foreground),
        )
    }
}

@Composable
private fun PinnedAppList(
    apps: List<AppEntry>,
    metrics: LayoutMetrics,
    onLaunch: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUnpin: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(metrics.dp(4f))) {
        apps.forEach { entry ->
            key(entry.key) {
                HomeAppRow(entry, metrics, onLaunch, onAppInfo, onUnpin, onOpenSettings)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeAppRow(
    entry: AppEntry,
    metrics: LayoutMetrics,
    onLaunch: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUnpin: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box {
        Row(
            Modifier
                .combinedClickable(
                    onClick = { onLaunch(entry) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .padding(vertical = metrics.dp(8f), horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(entry, metrics.dp(30f))
            Spacer(Modifier.width(metrics.dp(24f)))
            Text(
                text = entry.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = MonoColors.Foreground,
                    fontFamily = Outfit,
                    fontSize = metrics.sp(12.5f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    shadow = Legible,
                ),
            )
        }
        AppActionsMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            actions = listOf(
                AppAction(stringResource(R.string.action_unpin)) { onUnpin(entry) },
                AppAction(stringResource(R.string.action_app_info)) { onAppInfo(entry) },
                AppAction(stringResource(R.string.action_mono_settings), onOpenSettings),
            ),
        )
    }
}
