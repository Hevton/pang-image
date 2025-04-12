package io.lib.pang_image.disk

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object DiskCache {
    private const val MAXSIZE = 100 * 1024 * 1024L // 100MB

    private val cacheMutex = Mutex()
    private val isInitialized = AtomicBoolean(false)

    private lateinit var files: MutableList<File> // 항상 cacheMutex 안에서만 접근
    private var totalSize = 0L // 항상 cacheMutex 안에서만 접근

    private fun ensureInitialized(context: Context) { // 항상 cacheMutex 안에서만 접근
        if (!isInitialized.get()) {
            val dir = context.cacheDir
            files = dir.listFiles()?.toMutableList() ?: mutableListOf()
            files.sortBy { it.lastModified() }
            totalSize = files.sumOf { it.length() }
            isInitialized.set(true)
        }
    }

    suspend fun clear(context: Context, newFileSize: Long) {
        cacheMutex.withLock {
            ensureInitialized(context)

            if (totalSize + newFileSize > MAXSIZE) {
                files.sortBy { it.lastModified() }

                while (totalSize + newFileSize > MAXSIZE) {
                    val file = files.firstOrNull() ?: break
                    val size = file.length()
                    if (file.delete()) {
                        totalSize -= size
                        files.removeAt(0)
                    }
                }
            }
        }
    }
}
