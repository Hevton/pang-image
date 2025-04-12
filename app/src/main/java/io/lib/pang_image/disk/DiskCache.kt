package io.lib.pang_image.disk

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicBoolean

object DiskCache {
    private const val MAXSIZE = 100 * 1024 * 1024L // 100MB

    private val cacheMutex = Mutex()
    private val isInitialized = AtomicBoolean(false)

    private lateinit var fileQueue: PriorityQueue<File> // 항상 cacheMutex 안에서만 접근
    private var totalSize = 0L // 항상 cacheMutex 안에서만 접근

    private fun ensureInitialized(context: Context) { // 항상 cacheMutex 안에서만 접근
        if (!isInitialized.get()) {
            val dir = context.cacheDir
            val files = dir.listFiles()?.toMutableList() ?: mutableListOf()
            fileQueue = PriorityQueue<File>(files.size) { a, b ->
                a.lastModified().compareTo(b.lastModified())
            }.apply {
                addAll(files)
            }
            totalSize = files.sumOf { it.length() }
            isInitialized.set(true)
        }
    }

    suspend fun clear(context: Context, newFileSize: Long) {
        cacheMutex.withLock {
            ensureInitialized(context)

            while (totalSize + newFileSize > MAXSIZE) {
                val file = fileQueue.poll() ?: break
                val size = file.length()
                if (file.delete()) {
                    totalSize -= size
                }
            }
        }
    }

    suspend fun add(context: Context, file: File) {
        cacheMutex.withLock {
            ensureInitialized(context)

            file.setLastModified(System.currentTimeMillis())
            fileQueue.add(file)
            totalSize += file.length()
        }
    }
}
