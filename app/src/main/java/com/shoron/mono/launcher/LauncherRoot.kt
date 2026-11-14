package com.shoron.mono.launcher

import android.app.ActivityOptions
import android.content.Context
import android.graphics.Rect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoron.mono.R
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.data.PinList
import com.shoron.mono.data.PinResult
import com.shoron.mono.data.SwipeDownAction
import com.shoron.mono.data.WidgetSettings
import com.shoron.mono.launcher.drawer.DrawerScreen
import com.shoron.mono.launcher.drawer.DrawerViewModel
import com.shoron.mono.launcher.home.HomeActions
import com.shoron.mono.launcher.home.HomeScreen
import com.shoron.mono.launcher.home.HomeViewModel
import com.shoron.mono.launcher.home.HomeWidgets
import com.shoron.mono.launcher.quick.QuickActions
import com.shoron.mono.launcher.quick.QuickPanel
import com.shoron.mono.wallpaper.WallpaperLayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private val DrawerSpring = spring<Float>(dampingRatio = 0.92f, stiffness = 420f)

@Composable
fun LauncherRoot(
    homePresses: Flow<Unit>,
    onOpenSettings: () -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val drawerViewModel: DrawerViewModel = viewModel(factory = DrawerViewModel.Factory)

    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    // Drawer position: 0 = closed, 1 = open. Driven by the finger while dragging, then a spring.
    val drawer = remember { Animatable(0f) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var quickOpen by rememberSaveable { mutableStateOf(false) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var focusSearchToken by remember { mutableIntStateOf(0) }
    val heightPx = remember { floatArrayOf(1f) } // plain holder: no recomposition on resize

    fun settleDrawer(open: Boolean) {
        drawerOpen = open
        scope.launch { drawer.animateTo(if (open) 1f else 0f, DrawerSpring) }
    }

    fun closeAll() {
        settleDrawer(false)
        quickOpen = false
        editMode = false
    }

    LaunchedEffect(Unit) { if (drawerOpen) drawer.snapTo(1f) }
    LaunchedEffect(homePresses) { homePresses.collect { closeAll() } }
    LaunchedEffect(drawerOpen) {
        if (!drawerOpen) {
            keyboard?.hide()
            focusManager.clearFocus()
            drawerViewModel.clearQuery()
        }
    }
    // HOME screen: back closes overlays, never leaves the launcher.
    BackHandler(enabled = true) { closeAll() }

    val clock by homeViewModel.clock.collectAsStateWithLifecycle()
    val home by homeViewModel.uiState.collectAsStateWithLifecycle()
    val wallpaper by homeViewModel.wallpaper.collectAsStateWithLifecycle()
    val widgetSettings by homeViewModel.widgetSettings.collectAsStateWithLifecycle()
    val battery by homeViewModel.battery.collectAsStateWithLifecycle()
    val weather by homeViewModel.weather.collectAsStateWithLifecycle()
    val layout by homeViewModel.homeLayout.collectAsStateWithLifecycle()
    val canRestore by homeViewModel.canRestore.collectAsStateWithLifecycle()
    // Kept as State (not unwrapped) so second ticks are read only by the clock's draw phase.
    val time = homeViewModel.time.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        homeViewModel.onVisible()
        onPauseOrDispose { }
    }

    /** Launch with a clip-reveal from the tapped row; a subtle press scale precedes it. */
    val launch: (AppEntry, Rect?) -> Unit = { entry, bounds ->
        val options = bounds?.let {
            runCatching {
                ActivityOptions.makeClipRevealAnimation(view, it.left, it.top, it.width(), it.height()).toBundle()
            }.getOrNull()
        }
        if (!homeViewModel.launch(entry, bounds, options)) context.toast(context.getString(R.string.toast_launch_failed))
    }
    val appInfo: (AppEntry) -> Unit = { entry ->
        if (!homeViewModel.openAppInfo(entry)) context.toast(context.getString(R.string.toast_launch_failed))
    }

    val actions = remember(homeViewModel) {
        HomeActions(
            onLaunch = { entry, bounds -> launch(entry, bounds) },
            onAppInfo = { appInfo(it) },
            onUnpin = homeViewModel::unpin,
            onReorder = homeViewModel::reorder,
            onRestore = homeViewModel::restoreLastRemoved,
            onEnterEdit = { editMode = true },
            onExitEdit = { editMode = false },
            onAddApps = {
                editMode = false
                settleDrawer(true)
            },
            onDrawerDrag = { delta ->
                scope.launch { drawer.snapTo((drawer.value - delta / heightPx[0]).coerceIn(0f, 1f)) }
            },
            onDrawerRelease = { velocity ->
                val flingOpen = velocity < -900f
                val flingClose = velocity > 900f
                settleDrawer(flingOpen || (!flingClose && drawer.value > 0.3f))
            },
            onSwipeDown = {
                when (homeViewModel.homeLayout.value.swipeDownAction) {
                    SwipeDownAction.QuickActions -> quickOpen = true
                    SwipeDownAction.Search -> {
                        settleDrawer(true)
                        focusSearchToken++
                    }
                    SwipeDownAction.Notifications -> if (!QuickActions.expandNotifications(context)) quickOpen = true
                    SwipeDownAction.None -> Unit
                }
            },
            onOpenSettings = onOpenSettings,
            onRefreshWeather = homeViewModel::refreshWeatherNow,
        )
    }

    // Only crosses 0 → visible recompose; per-frame motion is applied in graphicsLayer.
    val drawerVisible by remember { derivedStateOf { drawer.value > 0.001f } }
    val drawerSlidePx = with(density) { 96.dp.toPx() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        heightPx[0] = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        wallpaper?.let { WallpaperLayer(it) }

        HomeScreen(
            widgets = HomeWidgets(
                settings = widgetSettings ?: WidgetSettings(),
                clock = clock,
                time = time,
                battery = battery,
                weather = weather,
            ),
            state = home,
            layout = layout,
            editMode = editMode,
            canRestore = canRestore,
            actions = actions,
            modifier = Modifier.graphicsLayer {
                val p = drawer.value
                alpha = 1f - p
                val s = 1f - 0.04f * p
                scaleX = s
                scaleY = s
                translationY = -drawerSlidePx * 0.4f * p
            },
        )

        if (drawerVisible) {
            val state by drawerViewModel.uiState.collectAsStateWithLifecycle()
            val recents by drawerViewModel.recents.collectAsStateWithLifecycle()
            DrawerScreen(
                state = state,
                recents = recents,
                focusSearchToken = focusSearchToken,
                query = drawerViewModel.query,
                onQueryChange = drawerViewModel::onQueryChange,
                onLaunch = { entry, bounds ->
                    launch(entry, bounds)
                    settleDrawer(false)
                },
                onTogglePin = { entry ->
                    scope.launch {
                        val message = when (drawerViewModel.togglePin(entry)) {
                            PinResult.Pinned -> context.getString(R.string.toast_pinned)
                            PinResult.Unpinned -> context.getString(R.string.toast_unpinned)
                            PinResult.Full -> context.getString(R.string.toast_pins_full, PinList.MAX_PINS)
                        }
                        context.toast(message)
                    }
                },
                onAppInfo = appInfo,
                onClose = { settleDrawer(false) },
                modifier = Modifier
                    // Claim touches so nothing reaches the home screen underneath.
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                    .graphicsLayer {
                    val p = drawer.value
                    alpha = p
                    translationY = (1f - p) * drawerSlidePx * 2.5f
                },
            )
        }

        // Swipe-down quick panel with a dimmed backdrop.
        AnimatedVisibility(visible = quickOpen, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        quickOpen = false
                    },
            )
        }
        AnimatedVisibility(
            visible = quickOpen,
            enter = slideInVertically(spring(dampingRatio = 0.9f, stiffness = 500f)) { -it } + fadeIn(),
            exit = slideOutVertically(tween(180)) { -it } + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            val recents by homeViewModel.recents.collectAsStateWithLifecycle()
            QuickPanel(
                recents = recents,
                onSearch = {
                    quickOpen = false
                    settleDrawer(true)
                    focusSearchToken++
                },
                onLaunch = { entry, bounds ->
                    quickOpen = false
                    launch(entry, bounds)
                },
                onOpenMonoSettings = onOpenSettings,
                onNotifications = {
                    if (QuickActions.expandNotifications(context)) quickOpen = false
                },
                onClose = { quickOpen = false },
            )
        }
    }
}

private fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
