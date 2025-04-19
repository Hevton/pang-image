package io.lib.pang_image.disk

import io.lib.pang_image.utils.dispatchers.DispatchersHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicBoolean

object DiskCache {
    private const val MAX_SIZE_BYTES = 100 * 1024 * 1024L // 100MB

    private val cacheMutex = Mutex()

    // 항상 cacheMutex 안에서만 접근
    private val diskDispatcher: CoroutineDispatcher = DispatchersHelper.diskDispatcher
    private var isInitialized = false
    private var currentSize = 0L

    // accessOrder=true -> get/put 시에 최근 접근 순서로 정렬
    private val cacheMap = object : LinkedHashMap<String, File>(128, 0.75f, true) {
        override fun put(key: String, value: File): File? {
            val old = super.put(key, value) // key에 대응되는 이전 항목이 있으면 참조
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
                return true // 가장 오래된(eldest) 엔트리를 맵에서 제거(evict)하라
            }
            return false // 용량 충분하면 맵 그대로
        }
    }

    // cacheMap에서 가져오거나, 파일 직접 접근하거나 선택
    suspend fun get(cachePath: String, key: String): File? = withContext(diskDispatcher) {
        cacheMutex.withLock {
            val file = File(cachePath, key)
            if (!file.exists()) return@withLock null
            // accessOrder 갱신을 위해 LinkedHashMap get / put 호출해야함
            cacheMap[key] = file
            file
        }
    }

    suspend fun set(cachePath: String, key: String) = withContext(diskDispatcher) {
        cacheMutex.withLock {
            ensureInitialized(cachePath)
            cacheMap[key] = File(cachePath, key)
        }
    }

    // 항상 cacheMutex 안에서만 접근
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

}
