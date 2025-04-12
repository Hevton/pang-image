package io.lib.pang_image.downloader

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
    private const val ERROR_BAD_REQUEST = "Bad Request (400)"
    private const val ERROR_UNAUTHORIZED = "Unauthorized (401)"
    private const val ERROR_FORBIDDEN = "Forbidden (403)"
    private const val ERROR_NOT_FOUND = "Not Found (404)"
    private const val ERROR_INTERNET_SERVER = "Internal Server Error (500)"
    private const val ERROR_UNKNOWN = "Unknown HTTP error code: %d"
    private const val ERROR_BODY_NULL = "Response body is null"

    private val mutexMap = ConcurrentHashMap<String, Mutex>()

    private fun getLock(key: String): Mutex =
        mutexMap.getOrPut(key) { Mutex() }

    private val defaultExceptionInterceptor =
        Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (!response.isSuccessful) {
                when (response.code()) {
                    400 -> throw IllegalArgumentException(ERROR_BAD_REQUEST)
                    401 -> throw SecurityException(ERROR_UNAUTHORIZED)
                    403 -> throw SecurityException(ERROR_FORBIDDEN)
                    404 -> throw NoSuchElementException(ERROR_NOT_FOUND)
                    500 -> throw IOException(ERROR_INTERNET_SERVER)
                    else -> throw Exception(ERROR_UNKNOWN.format(response.code()))
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
            } ?: throw IOException(ERROR_BODY_NULL)
            file
        }
    }
}
