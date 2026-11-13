package com.shoron.mono.wallpaper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import com.shoron.mono.core.theme.MonoColors

/**
 * Draws a wallpaper centre-cropped to its bounds, honouring focal point, dim and greyscale.
 * The bitmap is decoded once per source/size; changing bias, dim or greyscale only redraws.
 */
@Composable
fun WallpaperImage(
    settings: WallpaperSettings,
    modifier: Modifier = Modifier,
    onImageSize: (IntSize) -> Unit = {},
) {
    val loader = LocalWallpaperLoader.current
    val currentOnImageSize by rememberUpdatedState(onImageSize)
    BoxWithConstraints(modifier) {
        val widthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
        val heightPx = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
        val sourceKey = if (settings.isCustom) "custom:${settings.customVersion}" else settings.sourceId
        val bitmap by produceState<ImageBitmap?>(null, sourceKey, widthPx, heightPx) {
            value = loader.load(settings, widthPx, heightPx) ?: value
        }
        val alpha by animateFloatAsState(
            targetValue = if (bitmap != null) 1f else 0f,
            animationSpec = tween(durationMillis = 240),
            label = "wallpaperFade",
        )
        val filter = remember(settings.dim, settings.monochrome) {
            ColorFilter.colorMatrix(ColorMatrix(WallpaperMath.toneMatrix(settings.dim, settings.monochrome)))
        }
        val current = bitmap
        if (current != null) {
            LaunchedEffect(current) { currentOnImageSize(IntSize(current.width, current.height)) }
            Image(
                bitmap = current,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha },
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(settings.biasX, settings.biasY),
                colorFilter = filter,
            )
        }
    }
}

/** Full-screen home wallpaper plus a soft left-side scrim that keeps editorial text legible. */
@Composable
fun WallpaperLayer(settings: WallpaperSettings, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        WallpaperImage(settings, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to MonoColors.Ink.copy(alpha = 0.45f),
                        0.6f to Color.Transparent,
                    ),
                ),
        )
    }
}
