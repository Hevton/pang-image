package io.lib.pang_image.disk

import io.lib.pang_image.utils.dispatchers.DispatchersHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicBoolean

object DiskCache {
    private const val MAXSIZE = 100 * 1024 * 1024L // 100MB

    private val cacheMutex = Mutex()
    private val isInitialized = AtomicBoolean(false)

    private lateinit var fileQueue: PriorityQueue<File> // 항상 cacheMutex 안에서만 접근
    private var totalSize = 0L // 항상 cacheMutex 안에서만 접근

    private val diskDispatcher: CoroutineDispatcher = DispatchersHelper.diskDispatcher

    private fun ensureInitialized(cachePath: String) { // 항상 cacheMutex 안에서만 접근
        if (!isInitialized.get()) {
            val dir = File(cachePath)
            val files = dir.listFiles()?.toMutableList() ?: mutableListOf()
            fileQueue =
                PriorityQueue<File>(files.size) { a, b ->
                    a.lastModified().compareTo(b.lastModified())
                }.apply {
                    addAll(files)
                }
            totalSize = files.sumOf { it.length() }
            isInitialized.set(true)
        }
    }

    suspend fun get(
        cachePath: String,
        key: String,
    ): File? =
        withContext(diskDispatcher) {
            cacheMutex.withLock {
                val file = File(cachePath, key)
                if (file.exists()) file else null
            }
        }

    suspend fun set(
        cachePath: String,
        file: File,
    ) = withContext(diskDispatcher) {
        cacheMutex.withLock {
            ensureInitialized(cachePath)

            val newFileSize = file.length()
            while (totalSize + newFileSize > MAXSIZE) {
                val oldest = fileQueue.poll() ?: break
                val size = oldest.length()
                if (oldest.delete()) {
                    totalSize -= size
                }
            }

            file.setLastModified(System.currentTimeMillis())
            fileQueue.add(file)
            totalSize += newFileSize
        }
    }
}
