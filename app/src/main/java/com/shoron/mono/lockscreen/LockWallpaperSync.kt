package com.shoron.mono.lockscreen

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.shoron.mono.wallpaper.WallpaperMath
import com.shoron.mono.wallpaper.WallpaperRepository
import com.shoron.mono.wallpaper.WallpaperSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Renders the selected MONO wallpaper (crop, focal point, dim, greyscale) at screen size and
 * sets it as Android's *lock screen* wallpaper, so the system PIN/biometric screen matches.
 * Uses the normal SET_WALLPAPER permission; runs only when the user taps the button.
 */
class LockWallpaperSync(context: Context, private val wallpapers: WallpaperRepository) {

    private val appContext = context.applicationContext

    suspend fun apply(settings: WallpaperSettings, extraDim: Float): Boolean = withContext(Dispatchers.Default) {
        runCatching {
            val (w, h) = screenSize()
            val source = wallpapers.decodeSoftware(settings, w, h) ?: return@runCatching false
            val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val scale = max(w.toFloat() / source.width, h.toFloat() / source.height)
            val dx = (w - source.width * scale) * (1f + settings.biasX) / 2f
            val dy = (h - source.height * scale) * (1f + settings.biasY) / 2f
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(
                    WallpaperMath.toneMatrix(max(settings.dim, extraDim), settings.monochrome),
                )
            }
            Canvas(output).drawBitmap(
                source,
                Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(dx, dy)
                },
                paint,
            )
            source.recycle()
            WallpaperManager.getInstance(appContext).setBitmap(output, null, true, WallpaperManager.FLAG_LOCK)
            output.recycle()
            true
        }.getOrDefault(false)
    }

    private fun screenSize(): Pair<Int, Int> {
        val wm = appContext.getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }
}
