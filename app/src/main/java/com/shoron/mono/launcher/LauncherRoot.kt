package com.shoron.mono.launcher

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoron.mono.R
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.data.PinList
import com.shoron.mono.data.PinResult
import com.shoron.mono.launcher.drawer.DrawerScreen
import com.shoron.mono.launcher.drawer.DrawerViewModel
import com.shoron.mono.data.WidgetSettings
import com.shoron.mono.launcher.home.HomeScreen
import com.shoron.mono.launcher.home.HomeWidgets
import com.shoron.mono.launcher.home.HomeViewModel
import com.shoron.mono.wallpaper.WallpaperLayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun LauncherRoot(
    homePresses: Flow<Unit>,
    onOpenSettings: () -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val drawerViewModel: DrawerViewModel = viewModel(factory = DrawerViewModel.Factory)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    var drawerOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(homePresses) {
        homePresses.collect { drawerOpen = false }
    }
    LaunchedEffect(drawerOpen) {
        if (!drawerOpen) {
            keyboard?.hide()
            focusManager.clearFocus()
            drawerViewModel.clearQuery()
        }
    }
    // HOME screen: back only ever closes the drawer.
    BackHandler(enabled = true) { drawerOpen = false }

    val clock by homeViewModel.clock.collectAsStateWithLifecycle()
    val home by homeViewModel.uiState.collectAsStateWithLifecycle()
    val wallpaper by homeViewModel.wallpaper.collectAsStateWithLifecycle()
    val widgetSettings by homeViewModel.widgetSettings.collectAsStateWithLifecycle()
    val battery by homeViewModel.battery.collectAsStateWithLifecycle()
    val weather by homeViewModel.weather.collectAsStateWithLifecycle()
    // Kept as State (not unwrapped) so second ticks are read only by the clock's draw phase.
    val time = homeViewModel.time.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        homeViewModel.onVisible()
        onPauseOrDispose { }
    }

    val launch: (AppEntry) -> Unit = { entry ->
        if (!homeViewModel.launch(entry)) context.toast(context.getString(R.string.toast_launch_failed))
    }
    val appInfo: (AppEntry) -> Unit = { entry ->
        if (!homeViewModel.openAppInfo(entry)) context.toast(context.getString(R.string.toast_launch_failed))
    }

    Box(Modifier.fillMaxSize()) {
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
            onLaunch = launch,
            onAppInfo = appInfo,
            onUnpin = homeViewModel::unpin,
            onOpenDrawer = { drawerOpen = true },
            onOpenSettings = onOpenSettings,
            onRefreshWeather = homeViewModel::refreshWeatherNow,
        )

        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn(tween(180)) + slideInVertically(tween(240)) { it / 8 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 8 },
        ) {
            val drawer by drawerViewModel.uiState.collectAsStateWithLifecycle()
            DrawerScreen(
                state = drawer,
                query = drawerViewModel.query,
                onQueryChange = drawerViewModel::onQueryChange,
                onLaunch = { entry ->
                    launch(entry)
                    drawerOpen = false
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
                onClose = { drawerOpen = false },
            )
        }
    }
}

private fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
