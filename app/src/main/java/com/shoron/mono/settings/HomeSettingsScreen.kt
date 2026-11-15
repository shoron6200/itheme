package com.shoron.mono.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoron.mono.MonoApplication
import com.shoron.mono.R
import com.shoron.mono.core.theme.MonoColors
import com.shoron.mono.data.HomeLayout
import com.shoron.mono.data.IconSize
import com.shoron.mono.data.LauncherPreferences
import com.shoron.mono.data.SwipeDownAction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeSettingsViewModel(private val preferences: LauncherPreferences) : ViewModel() {

    val layout: StateFlow<HomeLayout?> = preferences.homeLayout
        .map<HomeLayout, HomeLayout?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (HomeLayout) -> HomeLayout) {
        viewModelScope.launch { preferences.updateHomeLayout(transform) }
    }

    fun clearRecents() {
        viewModelScope.launch { preferences.clearRecents() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonoApplication
                HomeSettingsViewModel(app.container.launcherPreferences)
            }
        }
    }
}

/** Swipe-down action, labels, icon size and recent apps (persisted in DataStore). */
@Composable
fun HomeSettingsScreen(onBack: () -> Unit) {
    val viewModel: HomeSettingsViewModel = viewModel(factory = HomeSettingsViewModel.Factory)
    val state by viewModel.layout.collectAsStateWithLifecycle()
    val layout = state ?: return

    Column(
        Modifier
            .fillMaxSize()
            .background(MonoColors.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 24.dp, bottom = 32.dp),
    ) {
        SettingsTopBar(stringResource(R.string.settings_home_gestures), onBack)
        Column(Modifier.padding(start = 12.dp)) {
            SettingsSectionTitle(stringResource(R.string.home_swipe_down))
            listOf(
                SwipeDownAction.QuickActions to R.string.home_swipe_quick,
                SwipeDownAction.Search to R.string.home_swipe_search,
                SwipeDownAction.Notifications to R.string.home_swipe_notifications,
                SwipeDownAction.None to R.string.home_swipe_none,
            ).forEach { (action, label) ->
                RadioRow(stringResource(label), layout.swipeDownAction == action) {
                    viewModel.update { it.copy(swipeDownAction = action) }
                }
            }

            SettingsSectionTitle(stringResource(R.string.settings_home_apps))
            SwitchRow(stringResource(R.string.home_show_labels), layout.showLabels) { on ->
                viewModel.update { it.copy(showLabels = on) }
            }
            Text(stringResource(R.string.home_icon_size), color = MonoColors.Muted, modifier = Modifier.padding(top = 8.dp))
            listOf(
                IconSize.Small to R.string.home_icon_small,
                IconSize.Medium to R.string.home_icon_medium,
                IconSize.Large to R.string.home_icon_large,
            ).forEach { (size, label) ->
                RadioRow(stringResource(label), layout.iconSize == size) {
                    viewModel.update { it.copy(iconSize = size) }
                }
            }

            SettingsSectionTitle(stringResource(R.string.drawer_recent))
            SwitchRow(stringResource(R.string.home_show_recents), layout.showRecents) { on ->
                viewModel.update { it.copy(showRecents = on) }
            }
            TextButton(onClick = viewModel::clearRecents) {
                Text(stringResource(R.string.home_clear_recents), color = MonoColors.Muted)
            }
            Spacer(Modifier.height(16.dp))
            SettingsNote(stringResource(R.string.home_gestures_note))
        }
    }
}
