package com.shoron.mono.launcher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import android.graphics.Rect
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectDragGestures
import com.shoron.mono.core.design.LaunchBounds
import com.shoron.mono.core.design.MonoChip
import com.shoron.mono.core.design.TopCutout
import com.shoron.mono.core.design.pressScale
import com.shoron.mono.core.design.rememberTopCutout
import com.shoron.mono.core.design.trackBounds
import com.shoron.mono.data.HomeLayout
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoron.mono.R
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppIcon
import com.shoron.mono.core.design.LayoutMetrics
import com.shoron.mono.core.design.LegibleShadow
import com.shoron.mono.core.design.rememberLayoutMetrics
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.core.theme.Outfit
import com.shoron.mono.data.ClockStyle
import com.shoron.mono.data.WidgetSettings
import com.shoron.mono.launcher.AppAction
import com.shoron.mono.launcher.AppActionsMenu
import com.shoron.mono.widgets.SystemShortcuts
import com.shoron.mono.widgets.battery.BatteryIndicator
import com.shoron.mono.widgets.battery.BatteryState
import com.shoron.mono.widgets.clock.ClockSnapshot
import com.shoron.mono.widgets.clock.RadialClock
import com.shoron.mono.widgets.clock.WordClock
import com.shoron.mono.widgets.date.DateLine
import com.shoron.mono.widgets.date.MonthTitle
import com.shoron.mono.widgets.date.WeekdayScript
import com.shoron.mono.widgets.weather.WeatherDetails
import com.shoron.mono.widgets.weather.WeatherSnapshot
import java.time.LocalTime

private val Legible = LegibleShadow

/** Widget data for the home screen. [time] stays a State so per-second ticks skip recomposition here. */
@Stable
class HomeWidgets(
    val settings: WidgetSettings,
    val clock: ClockSnapshot,
    val time: State<LocalTime>,
    val battery: BatteryState?,
    val weather: WeatherSnapshot?,
)

/**
 * Ocean Edition home screen, left-aligned editorial layout, drawn edge-to-edge with the status
 * bar hidden:
 *   top: date · battery (just below the camera cutout on their side, or at the top edge)
 *   upper: Radial Digital clock (default) — or the Editorial Word Clock further down
 *   middle: month · weather details · script weekday
 *   lower: vertical app shortcuts (long-press empty space to edit)
 * Gestures: swipe up = app drawer (follows the finger), swipe down = configurable action.
 */
@Composable
fun HomeScreen(
    widgets: HomeWidgets,
    state: HomeUiState,
    layout: HomeLayout,
    editMode: Boolean,
    canRestore: Boolean,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val current by rememberUpdatedState(actions)
    val density = LocalDensity.current
    val swipeDownThresholdPx = with(density) { 72.dp.toPx() }
    val directionSlopPx = with(density) { 6.dp.toPx() }
    val bottomEdgePx = WindowInsets.mandatorySystemGestures.getBottom(density).toFloat()
    val topEdgePx = with(density) { 12.dp.toPx() }
    val cutout = rememberTopCutout()

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .pointerInput(editMode) {
                detectTapGestures(
                    onLongPress = { if (!editMode) current.onEnterEdit() },
                    onTap = { if (editMode) current.onExitEdit() },
                )
            }
            .pointerInput(editMode) {
                if (editMode) return@pointerInput
                val tracker = VelocityTracker()
                var total = 0f
                var direction = 0 // -1 = up (drawer), 1 = down (swipe-down action)
                detectVerticalDragGestures(
                    onDragStart = { start ->
                        total = 0f
                        tracker.resetTracking()
                        // Leave Android's edges alone: the bottom navigation-gesture strip (Home /
                        // Recents) and the top edge (status bar). Those drags are ignored here.
                        direction = if (start.y < topEdgePx || start.y > size.height - bottomEdgePx) IGNORE else 0
                    },
                    onVerticalDrag = { change, amount ->
                        if (direction == IGNORE) return@detectVerticalDragGestures
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        total += amount
                        if (direction == 0 && abs(total) > directionSlopPx) direction = if (total < 0) -1 else 1
                        if (direction == -1) current.onDrawerDrag(amount)
                    },
                    onDragEnd = {
                        val velocity = tracker.calculateVelocity().y
                        when {
                            direction == -1 -> current.onDrawerRelease(velocity)
                            direction == 1 && total > swipeDownThresholdPx -> current.onSwipeDown()
                        }
                    },
                    // Cancelled (e.g. Android's navigation gesture took over): snap closed.
                    onDragCancel = { if (direction == -1) current.onDrawerRelease(Float.MAX_VALUE) },
                )
            }
            // Top is handled per side against the cutout (status bar is hidden).
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
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
                    TopRow(widgets, metrics, cutout)
                    Spacer(Modifier.height(metrics.dp(12f)))
                    EditorialBlock(widgets, metrics, actions.onOpenSettings, actions.onRefreshWeather)
                }
                Column(Modifier.weight(1f)) {
                    if (editMode) EditBar(canRestore, actions)
                    PinnedAppList(state.pinnedApps, metrics, layout, editMode, actions)
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = metrics.gutter, end = metrics.gutter / 2),
            ) {
                TopRow(widgets, metrics, cutout)
                Spacer(Modifier.weight(0.6f))
                EditorialBlock(widgets, metrics, actions.onOpenSettings, actions.onRefreshWeather)
                Spacer(Modifier.weight(1f))
                if (editMode) EditBar(canRestore, actions)
                PinnedAppList(state.pinnedApps, metrics, layout, editMode, actions)
                Spacer(Modifier.height(usableHeight * 0.05f))
            }
        }
    }
}

/** Everything the home screen can ask the launcher to do. Created once by LauncherRoot. */
class HomeActions(
    val onLaunch: (AppEntry, Rect?) -> Unit,
    val onAppInfo: (AppEntry) -> Unit,
    val onUnpin: (AppEntry) -> Unit,
    val onReorder: (List<String>) -> Unit,
    val onRestore: () -> Unit,
    val onEnterEdit: () -> Unit,
    val onExitEdit: () -> Unit,
    val onAddApps: () -> Unit,
    val onDrawerDrag: (Float) -> Unit,
    val onDrawerRelease: (Float) -> Unit,
    val onSwipeDown: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onRefreshWeather: () -> Unit,
    /** Null when the device has no freeform-window support (then the option is hidden). */
    val onLaunchFloating: ((AppEntry) -> Unit)? = null,
)

private const val IGNORE = 99

@Composable
private fun TopRow(widgets: HomeWidgets, metrics: LayoutMetrics, cutout: TopCutout) {
    val context = LocalContext.current
    // A slim line in the camera band (where the hidden status bar was). The date starts after
    // a left punch-hole camera with clear space around it; the battery avoids a right one.
    val clearance = 14.dp
    val dateShift = (cutout.leftEnd + clearance - metrics.gutter).coerceAtLeast(0.dp)
    val batteryShift = (cutout.rightStart + clearance - metrics.gutter / 2).coerceAtLeast(0.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = cutout.bandTop)
            .height(cutout.bandHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateLine(
            widgets.clock,
            metrics,
            Modifier
                .padding(start = dateShift)
                .noRippleClick { SystemShortcuts.openCalendar(context) },
        )
        Spacer(Modifier.weight(1f))
        widgets.battery?.let { battery ->
            BatteryIndicator(
                state = battery,
                textStyle = TextStyle(
                    fontFamily = Outfit,
                    fontSize = metrics.sp(11f),
                    fontWeight = FontWeight.Medium,
                    shadow = Legible,
                ),
                modifier = Modifier
                    .padding(end = batteryShift)
                    .noRippleClick { SystemShortcuts.openBatteryUsage(context) },
            )
        }
    }
}

@Composable
private fun EditorialBlock(
    widgets: HomeWidgets,
    metrics: LayoutMetrics,
    onOpenSettings: () -> Unit,
    onRefreshWeather: () -> Unit,
) {
    val context = LocalContext.current
    val settings = widgets.settings
    Column {
        if (settings.clockStyle == ClockStyle.RadialDigital) {
            RadialClock(
                time = widgets.time,
                use24Hour = settings.use24Hour,
                showSeconds = settings.showSeconds,
                metrics = metrics,
                modifier = Modifier.noRippleClick { SystemShortcuts.openAlarms(context) },
            )
            Spacer(Modifier.height(metrics.dp(8f)))
        }
        MonthTitle(widgets.clock, metrics, Modifier.noRippleClick { SystemShortcuts.openCalendar(context) })
        WeatherSlot(widgets, metrics, onOpenSettings, onRefreshWeather)
        Spacer(Modifier.height(metrics.dp(10f)))
        WeekdayScript(widgets.clock, metrics)
        if (settings.clockStyle == ClockStyle.EditorialWords) {
            Spacer(Modifier.height(metrics.dp(6f)))
            WordClock(widgets.clock, metrics, Modifier.noRippleClick { SystemShortcuts.openAlarms(context) })
        }
        Spacer(Modifier.height(metrics.dp(18f)))
        Box(
            Modifier
                .size(width = metrics.dp(26f), height = 2.dp)
                .background(MonoColors.Foreground),
        )
    }
}

@Composable
private fun WeatherSlot(
    widgets: HomeWidgets,
    metrics: LayoutMetrics,
    onOpenSettings: () -> Unit,
    onRefreshWeather: () -> Unit,
) {
    val settings = widgets.settings
    if (!settings.weatherEnabled) return
    val secondary = TextStyle(
        color = MonoColors.Muted,
        fontFamily = Outfit,
        fontWeight = FontWeight.Light,
        fontSize = metrics.sp(11f),
        shadow = Legible,
    )
    val weather = widgets.weather
    when {
        settings.weatherPlace == null -> Text(
            text = stringResource(R.string.weather_set_location),
            style = secondary,
            modifier = Modifier.noRippleClick(onOpenSettings),
        )
        weather != null -> WeatherDetails(
            weather = weather,
            showForecast = settings.showForecast,
            primary = TextStyle(
                color = MonoColors.Foreground,
                fontFamily = Outfit,
                fontWeight = FontWeight.Medium,
                fontSize = metrics.sp(13f),
                shadow = Legible,
            ),
            secondary = secondary,
            modifier = Modifier.noRippleClick(onRefreshWeather),
        )
        else -> Text(
            text = stringResource(R.string.weather_loading),
            style = secondary,
            modifier = Modifier.noRippleClick(onRefreshWeather),
        )
    }
}

@Composable
private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditBar(canRestore: Boolean, actions: HomeActions) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(
            text = stringResource(R.string.home_edit_hint),
            style = TextStyle(color = MonoColors.Muted, fontFamily = Outfit, fontSize = 12.sp, shadow = Legible),
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MonoChip(stringResource(R.string.home_edit_add), onClick = actions.onAddApps)
            if (canRestore) MonoChip(stringResource(R.string.home_edit_restore), onClick = actions.onRestore)
            MonoChip(stringResource(R.string.home_edit_settings), onClick = actions.onOpenSettings)
            MonoChip(stringResource(R.string.home_edit_done), active = true, onClick = actions.onExitEdit)
        }
    }
}

private val EditRowShape = RoundedCornerShape(14.dp)

/**
 * Pinned shortcuts. Normal mode: tap to launch (with a clip-reveal launch animation and press
 * feedback), long-press for actions. Edit mode: drag a row to reorder, × to remove.
 */
@Composable
private fun PinnedAppList(
    apps: List<AppEntry>,
    metrics: LayoutMetrics,
    layout: HomeLayout,
    editMode: Boolean,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val rowHeight = metrics.dp(layout.iconSize.dp + 16f)
    val rowHeightPx = with(density) { rowHeight.toPx() }
    val latestApps by rememberUpdatedState(apps)

    val working = remember { mutableStateListOf<AppEntry>() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    val dragOffset = remember { mutableFloatStateOf(0f) }
    var pendingOrder by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(apps) {
        if (apps.map { it.key } == pendingOrder) pendingOrder = null
    }
    val display: List<AppEntry> = when {
        draggingKey != null -> working
        pendingOrder != null -> pendingOrder!!.mapNotNull { k -> apps.firstOrNull { it.key == k } } +
            apps.filter { it.key !in pendingOrder!! }
        else -> apps
    }

    Column(modifier) {
        display.forEach { entry ->
            key(entry.key) {
                val isDragging = entry.key == draggingKey
                val rowModifier = Modifier
                    .height(rowHeight)
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset.floatValue else 0f
                        val s = if (isDragging) 1.04f else 1f
                        scaleX = s
                        scaleY = s
                    }
                    .then(
                        if (!editMode) {
                            Modifier
                        } else {
                            Modifier.pointerInput(entry.key) {
                                detectDragGestures(
                                    onDragStart = {
                                        working.clear()
                                        working.addAll(pendingOrder?.let { order ->
                                            order.mapNotNull { k -> latestApps.firstOrNull { it.key == k } }
                                        } ?: latestApps)
                                        draggingKey = entry.key
                                        dragOffset.floatValue = 0f
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset.floatValue += amount.y
                                        val from = working.indexOfFirst { it.key == entry.key }
                                        val steps = (dragOffset.floatValue / rowHeightPx).roundToInt()
                                        if (from >= 0 && steps != 0) {
                                            val to = (from + steps).coerceIn(0, working.lastIndex)
                                            if (to != from) {
                                                working.add(to, working.removeAt(from))
                                                dragOffset.floatValue -= (to - from) * rowHeightPx
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val order = working.map { it.key }
                                        pendingOrder = order
                                        draggingKey = null
                                        dragOffset.floatValue = 0f
                                        actions.onReorder(order)
                                    },
                                    onDragCancel = {
                                        draggingKey = null
                                        dragOffset.floatValue = 0f
                                    },
                                )
                            }
                        },
                    )
                if (editMode) {
                    EditableAppRow(entry, metrics, layout, actions, rowModifier)
                } else {
                    HomeAppRow(entry, metrics, layout, actions, rowModifier)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeAppRow(
    entry: AppEntry,
    metrics: LayoutMetrics,
    layout: HomeLayout,
    actions: HomeActions,
    modifier: Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val bounds = remember { LaunchBounds() }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxHeight()
                .trackBounds(bounds)
                .pressScale(interaction)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { actions.onLaunch(entry, bounds.rect) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(entry, metrics.dp(layout.iconSize.dp))
            if (layout.showLabels) {
                Spacer(Modifier.width(metrics.dp(24f)))
                AppLabel(entry, metrics)
            }
        }
        AppActionsMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            actions = listOf(
                AppAction(stringResource(R.string.action_unpin)) { actions.onUnpin(entry) },
                AppAction(stringResource(R.string.action_app_info)) { actions.onAppInfo(entry) },
                AppAction(stringResource(R.string.action_edit_home), actions.onEnterEdit),
            ) + listOfNotNull(
                actions.onLaunchFloating?.let { floating ->
                    AppAction(stringResource(R.string.action_floating)) { floating(entry) }
                },
            ),
        )
    }
}

@Composable
private fun EditableAppRow(
    entry: AppEntry,
    metrics: LayoutMetrics,
    layout: HomeLayout,
    actions: HomeActions,
    modifier: Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(EditRowShape)
            .background(MonoColors.Ink.copy(alpha = 0.45f))
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DragHandle()
        Spacer(Modifier.width(10.dp))
        AppIcon(entry, metrics.dp(layout.iconSize.dp * 0.9f))
        Spacer(Modifier.width(metrics.dp(16f)))
        Box(Modifier.weight(1f)) { AppLabel(entry, metrics) }
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .size(40.dp)
                .pressScale(interaction, 0.85f)
                .clickable(interactionSource = interaction, indication = null) { actions.onUnpin(entry) },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(12.dp)) {
                val w = 1.6.dp.toPx()
                drawLine(MonoColors.Foreground, Offset.Zero, Offset(size.width, size.height), w, StrokeCap.Round)
                drawLine(MonoColors.Foreground, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun DragHandle() {
    Canvas(Modifier.size(width = 14.dp, height = 12.dp)) {
        val w = 1.4.dp.toPx()
        for (i in 0..2) {
            val y = size.height * (0.15f + 0.35f * i)
            drawLine(MonoColors.Muted, Offset(0f, y), Offset(size.width, y), w, StrokeCap.Round)
        }
    }
}

@Composable
private fun AppLabel(entry: AppEntry, metrics: LayoutMetrics) {
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
