package io.lib.pang_image.interceptor

import android.graphics.Bitmap
import android.util.Log
import io.lib.pang_image.decoder.PangDecoder
import io.lib.pang_image.disk.DiskCache
import io.lib.pang_image.domain.DecodeRequest
import io.lib.pang_image.domain.PangRequest
import io.lib.pang_image.downloader.PangDownloader
import io.lib.pang_image.exception.PangException
import io.lib.pang_image.memory.MemoryCache
import io.lib.pang_image.utils.keygen.CacheKey
import kotlin.coroutines.cancellation.CancellationException

object PangInterceptor {
    private const val TAG = "PangInterceptor"

    suspend fun interceptor(request: PangRequest): Result<Bitmap> = retryIfNotCoroutineException(
        maxAttempts = request.retry,
    ) {
        runCatching {
            val width = request.imageWidth
            val height = request.imageHeight

            val diskCacheKey = CacheKey.disk(request.url)
            val memoryCacheKey = CacheKey.memory(request.url, width, height)

            // 1. 메모리 캐시 체크
            MemoryCache.get(memoryCacheKey)?.let {
                Log.d(TAG, "Memory Hit")
                return@runCatching it
            }

            // 2. 디스크 체크
            DiskCache.get(request.cachePath, diskCacheKey)?.let {
                Log.d(TAG, "Disk Hit")
                val decoded =
                    PangDecoder.decodeFromFile(
                        DecodeRequest(
                            request.cachePath + "/" + diskCacheKey,
                            request.imageWidth,
                            request.imageHeight,
                            request.inScale,
                        ),
                    ).getOrElse { throw it }
                        ?: throw PangException.PangDecodeException // 발생할 수 있음

                MemoryCache.set(memoryCacheKey, decoded)
                return@runCatching decoded
            }

            // 3. 다운로드
            PangDownloader.saveImage(request, diskCacheKey)
                    .getOrElse {
                        throw it
                    }
                    ?: throw IllegalStateException("Downloaded file is null")

            // 4. 디스크 저장
            DiskCache.set(request.cachePath, diskCacheKey)

            // 5. 디코딩
            val bitmap =
                PangDecoder.decodeFromFile(
                    DecodeRequest(request.cachePath + "/" + diskCacheKey, width, height, request.inScale),
                ).getOrElse { throw it }
                    ?: throw PangException.PangDecodeException // 발생할 수 있음

            // 6. 메모리 캐시에 저장
            MemoryCache.set(memoryCacheKey, bitmap)

            return@runCatching bitmap
        }
    }

    private suspend fun <T> retryIfNotCoroutineException(
        maxAttempts: Int = 3,
        block: suspend () -> Result<T>,
    ): Result<T> {
        var last: Throwable? = null

        repeat(maxAttempts) { attempt ->
            val result = block()
            if (result.isSuccess) return result

            val ex = result.exceptionOrNull()
            if (ex is CancellationException) return Result.failure(ex)
            last = ex
        }

        return Result.failure(last ?: Exception("Failed after $maxAttempts attempts"))
    }
}
