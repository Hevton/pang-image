package io.lib.pang_image.disk

import io.lib.pang_image.utils.dispatchers.DispatchersHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object DiskCache {
    private const val MAX_SIZE_BYTES = 100 * 1024 * 1024L // 100MB
    private const val AVG_FILE_SIZE_BYTES = 500L * 1024
    private const val INITIAL_CAPACITY = (MAX_SIZE_BYTES / AVG_FILE_SIZE_BYTES).toInt()

    private val cacheMutex = Mutex()
    private val diskDispatcher: CoroutineDispatcher = DispatchersHelper.diskDispatcher

    // mutex 안에서만
    private var isInitialized = false
    private var currentSize = 0L
    private val cacheMap = object : LinkedHashMap<String, File>(INITIAL_CAPACITY, 0.75f, true) {
        override fun put(key: String, value: File): File? {
            val old = super.put(key, value)
            if (old != null) currentSize -= old.length()
            currentSize += value.length()
            return old
        }

        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, File>?): Boolean {
            if (eldest != null && currentSize > MAX_SIZE_BYTES) {
                val file = eldest.value
                val size = file.length()
                if (file.delete()) {
                    currentSize -= size
                }
                return true
            }
            return false
        }
    }

    private fun ensureInitialized(cachePath: String) {
        if (isInitialized) return
        val dir = File(cachePath)
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                cacheMap[file.name] = file
            }
        }
        isInitialized = true
    }

    suspend fun get(cachePath: String, key: String): File? = withContext(diskDispatcher) {
        cacheMutex.withLock {
            ensureInitialized(cachePath)

            val file = cacheMap[key] ?: return@withLock null
            file.setLastModified(System.currentTimeMillis())
            file
        }
    }

    suspend fun set(cachePath: String, key: String) = withContext(diskDispatcher) {
        cacheMutex.withLock {
            ensureInitialized(cachePath)

            val file = File(cachePath, key)
            file.setLastModified(System.currentTimeMillis())
            cacheMap[key] = file
        }
    }
}
