package com.shoron.mono.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

interface WallpaperLoader {
    /** Decodes the wallpaper at (no larger than) the size needed to cover [widthPx]x[heightPx]. */
    suspend fun load(settings: WallpaperSettings, widthPx: Int, heightPx: Int): ImageBitmap?
}

val LocalWallpaperLoader = staticCompositionLocalOf<WallpaperLoader> {
    error("No WallpaperLoader provided")
}

/**
 * Loads built-in and user-imported wallpapers.
 *
 * - Decodes straight to the display size (hardware bitmaps where possible, so pixels live in
 *   GPU memory rather than the Java heap).
 * - Keeps a handful of recent decodes (home, settings preview, thumbnails).
 * - Imported photos are downscaled and re-encoded as WebP in app-private storage, so no
 *   storage permission or persistable URI grant is needed.
 */
class WallpaperRepository(
    context: Context,
    private val preferences: WallpaperPreferences,
) : WallpaperLoader {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "wallpapers")
    private val cache = LruCache<String, ImageBitmap>(CACHE_ENTRIES)

    val settings: Flow<WallpaperSettings> = preferences.settings

    override suspend fun load(settings: WallpaperSettings, widthPx: Int, heightPx: Int): ImageBitmap? {
        if (widthPx <= 0 || heightPx <= 0) return null
        val key = "${sourceKey(settings)}@${widthPx}x$heightPx"
        cache.get(key)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching { decode(sourceOf(settings), widthPx, heightPx) }.getOrNull()
        } ?: return null
        return bitmap.asImageBitmap().also { cache.put(key, it) }
    }

    /** Software (CPU-readable) decode for compositing, e.g. the system lock-screen wallpaper. */
    suspend fun decodeSoftware(settings: WallpaperSettings, widthPx: Int, heightPx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching { decode(sourceOf(settings), widthPx, heightPx, software = true) }.getOrNull()
        }

    suspend fun save(settings: WallpaperSettings) {
        preferences.save(settings)
        withContext(Dispatchers.IO) { deleteCustomFilesExcept(settings.customVersion.takeIf { settings.isCustom }) }
    }

    /**
     * Imports a photo chosen with the system photo picker. Returns the new custom version,
     * or null if the image could not be read. Keeps the currently applied custom file.
     */
    suspend fun importCustom(uri: Uri): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val applied = preferences.settings.first()
            val version = System.currentTimeMillis()
            val bitmap = decodeForImport(uri) ?: return@runCatching null
            dir.mkdirs()
            val target = customFile(version)
            FileOutputStream(target).use { out ->
                @Suppress("DEPRECATION")
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(format, IMPORT_QUALITY, out)
            }
            bitmap.recycle()
            deleteCustomFilesExcept(applied.customVersion.takeIf { applied.isCustom }, version)
            version
        }.getOrNull()
    }

    // ------------------------------------------------------------------ decoding

    private sealed interface Source {
        data class Resource(val resId: Int) : Source
        data class FileSource(val file: File) : Source
    }

    private fun sourceKey(settings: WallpaperSettings): String =
        if (settings.isCustom) "custom:${settings.customVersion}" else settings.sourceId

    private fun sourceOf(settings: WallpaperSettings): Source {
        if (settings.isCustom) {
            val file = customFile(settings.customVersion)
            if (file.exists()) return Source.FileSource(file)
        }
        val builtIn = WallpaperCatalog.find(settings.sourceId) ?: WallpaperCatalog.default()
        return Source.Resource(builtIn.resId)
    }

    private fun decode(source: Source, widthPx: Int, heightPx: Int, software: Boolean = false): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decoderSource = when (source) {
                is Source.Resource -> ImageDecoder.createSource(appContext.resources, source.resId)
                is Source.FileSource -> ImageDecoder.createSource(source.file)
            }
            decodeScaled(decoderSource, software) { w, h -> WallpaperMath.coverSize(w, h, widthPx, heightPx) }
        } else {
            decodeLegacy(widthPx, heightPx, hardware = !software) {
                when (source) {
                    is Source.Resource -> appContext.resources.openRawResource(source.resId)
                    is Source.FileSource -> source.file.inputStream()
                }
            }
        }

    private fun decodeForImport(uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(appContext.contentResolver, uri)
            decodeScaled(source, software = true) { w, h -> WallpaperMath.fitSize(w, h, IMPORT_MAX_W, IMPORT_MAX_H) }
        } else {
            decodeLegacy(IMPORT_MAX_W / 2, IMPORT_MAX_H / 2, hardware = false) {
                appContext.contentResolver.openInputStream(uri)
            }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeScaled(
        source: ImageDecoder.Source,
        software: Boolean = false,
        size: (Int, Int) -> Pair<Int, Int>,
    ): Bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        // ImageDecoder also applies EXIF orientation, so camera photos come out upright.
        val (w, h) = size(info.size.width, info.size.height)
        decoder.setTargetSize(w, h)
        if (software) decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }

    private fun decodeLegacy(
        targetW: Int,
        targetH: Int,
        hardware: Boolean,
        open: () -> InputStream?,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = WallpaperMath.sampleSize(bounds.outWidth, bounds.outHeight, targetW, targetH)
            inPreferredConfig = if (hardware) Bitmap.Config.HARDWARE else Bitmap.Config.ARGB_8888
        }
        return open()?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    // ------------------------------------------------------------------ files

    private fun customFile(version: Long) = File(dir, "custom_$version.webp")

    private fun deleteCustomFilesExcept(vararg keep: Long?) {
        val keepNames = keep.filterNotNull().map { customFile(it).name }.toSet()
        dir.listFiles()
            ?.filter { it.name.startsWith("custom_") && it.name !in keepNames }
            ?.forEach { it.delete() }
    }

    private companion object {
        const val IMPORT_MAX_W = 1800
        const val IMPORT_MAX_H = 3600
        const val IMPORT_QUALITY = 88

        // Home + settings preview + a few thumbnails. Hardware bitmaps live in GPU memory.
        const val CACHE_ENTRIES = 6
    }
}
