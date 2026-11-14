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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppRepository
import com.shoron.mono.data.HomeLayout
import com.shoron.mono.icons.IconRepository
import kotlinx.coroutines.flow.combine
import com.shoron.mono.data.IconSize
import com.shoron.mono.data.LauncherPreferences
import com.shoron.mono.data.SwipeDownAction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeSettingsViewModel(
    private val preferences: LauncherPreferences,
    appRepository: AppRepository,
    iconRepository: IconRepository,
) : ViewModel() {

    /** Installed apps without a MONO Thin Line icon (missing-icon detection). */
    val missingIcons: StateFlow<List<AppEntry>> =
        combine(appRepository.apps, iconRepository.themedIcons) { apps, themed ->
            apps.orEmpty().filter { it.key !in themed }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val themedCount: StateFlow<Int> = iconRepository.themedIcons.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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
                HomeSettingsViewModel(
                    app.container.launcherPreferences,
                    app.container.appRepository,
                    app.container.iconRepository,
                )
            }
        }
    }
}

/** Swipe-down action, labels, icon size and recent apps (persisted in DataStore). */
@Composable
fun HomeSettingsScreen(onBack: () -> Unit) {
    val viewModel: HomeSettingsViewModel = viewModel(factory = HomeSettingsViewModel.Factory)
    val state by viewModel.layout.collectAsStateWithLifecycle()
    val missing by viewModel.missingIcons.collectAsStateWithLifecycle()
    val themedCount by viewModel.themedCount.collectAsStateWithLifecycle()
    var showMissing by rememberSaveable { mutableStateOf(false) }
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

            SettingsSectionTitle(stringResource(R.string.icons_title))
            SwitchRow(stringResource(R.string.icons_thin_line), layout.thinLineIcons) { on ->
                viewModel.update { it.copy(thinLineIcons = on) }
            }
            Text(
                text = stringResource(R.string.icons_summary, themedCount, missing.size),
                color = MonoColors.Muted,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { showMissing = !showMissing }
                    .padding(vertical = 6.dp),
            )
            if (showMissing) {
                missing.forEach { app ->
                    Text(
                        text = "${app.label}  ·  ${app.packageName}",
                        color = MonoColors.Faint,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
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
