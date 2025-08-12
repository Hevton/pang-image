package io.lib.pang_image.memory

import android.graphics.Bitmap
import java.lang.ref.WeakReference

object WeakMemoryCache {
    private const val MAX_SIZE = 100
    private const val CLEAN_UP_INTERVAL = 10

    private val cache = object : LinkedHashMap<String, WeakReference<Bitmap>>(
        16, 0.75f, true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, WeakReference<Bitmap>>?
        ): Boolean {
            return size > MAX_SIZE
        }
    }

    private var operationsSinceCleanUp = 0

    fun get(key: String): Bitmap? {
        val ref = cache[key] ?: return null
        val bitmap = ref.get()

        if (bitmap == null) {
            cache.remove(key)
        }

        cleanUpIfNecessary()
        return bitmap
    }

    fun set(key: String, bitmap: Bitmap) {
        if (bitmap.isRecycled) return

        cache[key] = WeakReference(bitmap)

        cleanUpIfNecessary()
    }

    fun remove(key: String): Boolean = cache.remove(key) != null

    fun clear() {
        operationsSinceCleanUp = 0
        cache.clear()
    }

    fun size(): Int = cache.size

    private fun cleanUpIfNecessary() {
        if (operationsSinceCleanUp++ >= CLEAN_UP_INTERVAL) {
            cleanUp()
        }
    }

    private fun cleanUp() {
        operationsSinceCleanUp = 0

        val iterator = cache.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null) {
                iterator.remove()
            }
        }
    }
}
