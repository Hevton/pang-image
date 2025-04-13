package io.lib.pang_image.memory

import android.graphics.Bitmap
import android.util.LruCache

object MemoryCache {
    private val defaultMemoryCacheSize: Int
        get() {
            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            return maxMemory / 8 // 전체 메모리의 1/8 사용
        }

    private val cache =
        object : LruCache<String, Bitmap>(defaultMemoryCacheSize) {
            override fun sizeOf(
                key: String,
                value: Bitmap,
            ): Int {
                return value.byteCount / 1024 // KB 단위
            }
        }

    fun get(key: String): Bitmap? = cache.get(key)

    fun set(
        key: String,
        bitmap: Bitmap,
    ) {
        cache.put(key, bitmap)
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.evictAll()
    }
}
