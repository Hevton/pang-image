package io.lib.pang_image.memory

import android.graphics.Bitmap
import android.util.LruCache

object StrongMemoryCache {

    private val defaultMemoryCacheSize: Int
        get() {
            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            return maxMemory / 8
        }

    private val cache = object : LruCache<String, Bitmap>(defaultMemoryCacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }

        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) =
            WeakMemoryCache.set(key, oldValue)
    }

    val size: Long get() = cache.size().toLong()

    fun get(key: String): Bitmap? = cache[key]

    fun set(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            cache.put(key, bitmap)
        }
    }

    fun remove(key: String): Boolean = cache.remove(key) != null

    fun clear() = cache.evictAll()
}
