package com.shoron.mono.launcher.home

import androidx.compose.foundation.ExperimentalFoundationApi
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
 * Ocean Edition home screen, left-aligned editorial layout:
 *   top: date · battery
 *   upper: Radial Digital clock (default) — or the Editorial Word Clock further down
 *   middle: month · weather details · script weekday
 *   lower: vertical app shortcuts
 * The wallpaper is drawn behind this screen by LauncherRoot.
 */
@Composable
fun HomeScreen(
    widgets: HomeWidgets,
    state: HomeUiState,
    onLaunch: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUnpin: (AppEntry) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshWeather: () -> Unit,
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
                    TopRow(widgets, metrics)
                    Spacer(Modifier.height(metrics.dp(12f)))
                    EditorialBlock(widgets, metrics, onOpenSettings, onRefreshWeather)
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
                Spacer(Modifier.height(usableHeight * 0.03f))
                TopRow(widgets, metrics)
                Spacer(Modifier.weight(0.6f))
                EditorialBlock(widgets, metrics, onOpenSettings, onRefreshWeather)
                Spacer(Modifier.weight(1f))
                PinnedAppList(state.pinnedApps, metrics, onLaunch, onAppInfo, onUnpin, onOpenSettings)
                Spacer(Modifier.height(usableHeight * 0.05f))
            }
        }
    }
}

@Composable
private fun TopRow(widgets: HomeWidgets, metrics: LayoutMetrics) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DateLine(
            widgets.clock,
            metrics,
            Modifier.noRippleClick { SystemShortcuts.openCalendar(context) },
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
                modifier = Modifier.noRippleClick { SystemShortcuts.openBatteryUsage(context) },
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(metrics.dp(2f))) {
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
