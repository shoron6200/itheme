package com.shoron.mono.apps

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

interface IconLoader {
    /** Returns a cached icon immediately, or null. Never blocks. */
    fun peek(entry: AppEntry, sizePx: Int): ImageBitmap?

    /** Loads (off the main thread) and caches the icon. */
    suspend fun load(entry: AppEntry, sizePx: Int): ImageBitmap?
}

val LocalIconLoader = staticCompositionLocalOf<IconLoader> {
    error("No IconLoader provided")
}
