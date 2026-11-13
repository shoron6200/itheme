package com.shoron.mono.settings

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
import com.shoron.mono.data.PinList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    appRepository: AppRepository,
    private val preferences: LauncherPreferences,
) : ViewModel() {

    val pinnedApps: StateFlow<List<AppEntry>> =
        combine(appRepository.apps, preferences.pinnedKeys) { apps, keys -> PinnedApps.resolve(apps, keys) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun move(entry: AppEntry, delta: Int) {
        val keys = pinnedApps.value.map { it.key }
        viewModelScope.launch { preferences.setPinned(PinList.move(keys, entry.key, delta)) }
    }

    fun unpin(entry: AppEntry) {
        viewModelScope.launch { preferences.unpin(entry.key) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonoApplication
                SettingsViewModel(app.container.appRepository, app.container.launcherPreferences)
            }
        }
    }
}
