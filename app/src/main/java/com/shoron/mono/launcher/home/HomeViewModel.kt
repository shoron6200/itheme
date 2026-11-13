package com.shoron.mono.launcher.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoron.mono.MonoApplication
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppLauncher
import com.shoron.mono.apps.AppRepository
import com.shoron.mono.apps.PinnedApps
import com.shoron.mono.data.LauncherPreferences
import com.shoron.mono.widgets.clock.ClockSnapshot
import com.shoron.mono.widgets.clock.TimeTicker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Immutable
data class HomeUiState(val pinnedApps: List<AppEntry> = emptyList())

class HomeViewModel(
    appRepository: AppRepository,
    private val preferences: LauncherPreferences,
    private val appLauncher: AppLauncher,
    timeTicker: TimeTicker,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(appRepository.apps, preferences.pinnedKeys) { apps, keys ->
            HomeUiState(PinnedApps.resolve(apps, keys))
        }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    /** Ticks once per minute only while the home screen is at least STARTED. */
    val clock: StateFlow<ClockSnapshot> = timeTicker.now
        .map { ClockSnapshot.from(it) }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            ClockSnapshot.from(LocalDateTime.now()),
        )

    fun launch(entry: AppEntry): Boolean = appLauncher.launch(entry)

    fun openAppInfo(entry: AppEntry): Boolean = appLauncher.openAppInfo(entry)

    fun unpin(entry: AppEntry) {
        viewModelScope.launch { preferences.unpin(entry.key) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonoApplication
                val c = app.container
                HomeViewModel(c.appRepository, c.launcherPreferences, c.appLauncher, c.timeTicker)
            }
        }
    }
}
