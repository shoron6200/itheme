package com.shoron.mono.wallpaper

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoron.mono.MonoApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds an editable draft; nothing changes on the home screen until [apply]. */
class WallpaperViewModel(private val repository: WallpaperRepository) : ViewModel() {

    private val _draft = MutableStateFlow<WallpaperSettings?>(null)
    val draft: StateFlow<WallpaperSettings?> = _draft.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    /** The custom photo version available to pick (applied or freshly imported). */
    private val _customVersion = MutableStateFlow(0L)
    val customVersion: StateFlow<Long> = _customVersion.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.settings.first()
            _draft.value = saved
            if (saved.isCustom) _customVersion.value = saved.customVersion
        }
    }

    private fun edit(transform: (WallpaperSettings) -> WallpaperSettings) {
        _draft.update { it?.let(transform) }
    }

    fun selectBuiltIn(id: String) = edit { it.copy(sourceId = id, biasX = 0f, biasY = 0f, monochrome = true) }

    fun selectCustom() {
        val version = _customVersion.value
        if (version > 0) edit { it.copy(sourceId = WallpaperSettings.CUSTOM_SOURCE, customVersion = version) }
    }

    fun setDim(value: Float) = edit { it.copy(dim = value.coerceIn(0f, WallpaperSettings.MAX_DIM)) }

    fun setMonochrome(value: Boolean) = edit { it.copy(monochrome = value) }

    fun setBias(x: Float, y: Float) = edit { it.copy(biasX = x.coerceIn(-1f, 1f), biasY = y.coerceIn(-1f, 1f)) }

    fun resetPosition() = edit { it.copy(biasX = 0f, biasY = 0f) }

    /** Imports a picked photo; returns false if it could not be read. */
    suspend fun importPhoto(uri: Uri): Boolean {
        _importing.value = true
        try {
            val version = repository.importCustom(uri) ?: return false
            _customVersion.value = version
            edit {
                it.copy(
                    sourceId = WallpaperSettings.CUSTOM_SOURCE,
                    customVersion = version,
                    biasX = 0f,
                    biasY = 0f,
                    monochrome = true,
                    dim = maxOf(it.dim, 0.2f),
                )
            }
            return true
        } finally {
            _importing.value = false
        }
    }

    suspend fun apply() {
        _draft.value?.let { repository.save(it) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonoApplication
                WallpaperViewModel(app.container.wallpaperRepository)
            }
        }
    }
}
