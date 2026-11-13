package com.shoron.mono.wallpaper

import androidx.compose.runtime.Immutable

/**
 * What the home screen draws behind its content.
 *
 * @param sourceId a built-in id (see [WallpaperCatalog]) or [CUSTOM_SOURCE].
 * @param biasX horizontal focal point of the centre-crop, -1 (left) .. 1 (right).
 * @param biasY vertical focal point, -1 (top) .. 1 (bottom).
 * @param dim 0 (none) .. [MAX_DIM]; darkens the image for legibility.
 * @param monochrome render the image in greyscale (useful for colour photos).
 * @param customVersion identifies the imported photo file; 0 when none.
 */
@Immutable
data class WallpaperSettings(
    val sourceId: String = WallpaperCatalog.DEFAULT_ID,
    val biasX: Float = 0f,
    val biasY: Float = 0f,
    val dim: Float = 0.10f,
    val monochrome: Boolean = true,
    val customVersion: Long = 0L,
) {
    val isCustom: Boolean get() = sourceId == CUSTOM_SOURCE

    companion object {
        const val CUSTOM_SOURCE = "custom"
        const val MAX_DIM = 0.7f
    }
}
