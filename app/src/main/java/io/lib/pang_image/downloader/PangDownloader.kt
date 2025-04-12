package io.lib.pang_image.downloader

import android.content.Context
import com.example.imageloader.pang.util.domain.PangRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object PangDownloader {

    private val mutexMap = ConcurrentHashMap<String, Mutex>()

    private fun getLock(key: String): Mutex =
        mutexMap.getOrPut(key) { Mutex() }


    private val defaultExceptionInterceptor =
        Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (!response.isSuccessful) {
                when (response.code()) {
                    400 -> throw IllegalArgumentException("Bad Request (400)")
                    401 -> throw SecurityException("Unauthorized (401)")
                    403 -> throw SecurityException("Forbidden (403)")
                    404 -> throw NoSuchElementException("Not Found (404)")
                    500 -> throw IOException("Internal Server Error (500)")
                    else -> throw Exception("Unknown HTTP error code: $response.code")
                }
            }
            response
        }

    private val client = OkHttpClient.Builder()
        .addInterceptor(defaultExceptionInterceptor)
        .dispatcher(Dispatcher().apply {
            maxRequests = 128
            maxRequestsPerHost = 20
        })
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .build()


    suspend fun saveImage(
        request: PangRequest,
        diskCacheKey: String
    ): Result<File?> = download(diskCacheKey, request.url, request.cachePath)

    private suspend fun download(
        cacheKey: String,
        url: String,
        path: String,
        downloadDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Result<File?>  = runCatching {
        val request = Request.Builder().url(url).build()
        withContext(downloadDispatcher) {
            val response = client.newCall(request).execute()
            val file = File(path, cacheKey)

            response.body()?.byteStream()?.apply {
                getLock(path).withLock {
                    file.outputStream().use { output ->
                        this.copyTo(output)
                    }
                }
            } ?: throw IOException("Response body is null")
            file
        }
    }
}
