package com.shoron.mono.launcher.drawer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoron.mono.MonoApplication
import com.shoron.mono.apps.AppEntry
import com.shoron.mono.apps.AppRepository
import com.shoron.mono.apps.PinnedApps
import com.shoron.mono.data.LauncherPreferences
import com.shoron.mono.data.PinResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class DrawerViewModel(
    private val appRepository: AppRepository,
    private val preferences: LauncherPreferences,
) : ViewModel() {

    /** Compose state so the text field updates synchronously (no cursor jumps). */
    var query by mutableStateOf("")
        private set

    val uiState: StateFlow<DrawerUiState> = combine(
        appRepository.apps,
        snapshotFlow { query },
        preferences.pinnedKeys,
    ) { apps, q, pins ->
        DrawerModel.build(apps, q, pins.orEmpty().toSet())
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerUiState())

    /** Recently launched apps for the drawer's top row (empty when disabled in settings). */
    val recents: StateFlow<List<AppEntry>> = combine(
        appRepository.apps,
        preferences.recentKeys,
        preferences.homeLayout,
    ) { apps, keys, layout ->
        if (layout.showRecents) PinnedApps.resolve(apps, keys).take(6) else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        query = value
    }

    fun clearQuery() {
        query = ""
    }

    suspend fun togglePin(entry: AppEntry): PinResult {
        val valid = appRepository.apps.value.orEmpty().mapTo(HashSet()) { it.key }
        return preferences.togglePin(entry.key, valid)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonoApplication
                DrawerViewModel(app.container.appRepository, app.container.launcherPreferences)
            }
        }
    }
}
