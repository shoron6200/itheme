package com.shoron.mono.apps

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

/** Memory-bounded icon cache (1/16 of the heap limit, in KiB). */
class IconCache {

    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024L / 16L).toInt()

    private val cache = object : LruCache<String, ImageBitmap>(maxKb) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4 / 1024).coerceAtLeast(1)
    }

    fun get(appKey: String, sizePx: Int): ImageBitmap? = cache.get(cacheKey(appKey, sizePx))

    fun put(appKey: String, sizePx: Int, bitmap: ImageBitmap) {
        cache.put(cacheKey(appKey, sizePx), bitmap)
    }

    fun invalidatePackage(packageName: String) {
        val prefix = "$packageName/"
        cache.snapshot().keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    fun clear() = cache.evictAll()

    private fun cacheKey(appKey: String, sizePx: Int) = "$appKey@$sizePx"
}
