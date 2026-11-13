package com.shoron.mono.core.design

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/** Phase 1 monochrome treatment for original app icons (Thin Line icons arrive in Phase 5). */
val MonoIconFilter: ColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
