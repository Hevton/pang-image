package io.lib.pang_image.memory
import android.graphics.Bitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object MemoryCache {
    private val mutex = Mutex()

    suspend fun get(key: String): Bitmap? = mutex.withLock {
        val bitmap = StrongMemoryCache.get(key) ?: WeakMemoryCache.get(key)

        return@withLock bitmap
    }


    suspend fun set(key: String, bitmap: Bitmap) = mutex.withLock {
        StrongMemoryCache.set(key, bitmap)
    }

    suspend fun remove(key: String): Boolean = mutex.withLock {
        val removedStrong = StrongMemoryCache.remove(key)
        val removedWeak = WeakMemoryCache.remove(key)
        return removedStrong || removedWeak
    }

    suspend fun clear() = mutex.withLock {
        StrongMemoryCache.clear()
        WeakMemoryCache.clear()
    }
}

