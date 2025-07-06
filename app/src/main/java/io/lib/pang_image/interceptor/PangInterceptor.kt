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

                decodeAndCache(request, memoryCacheKey, diskCacheKey)
                    .onSuccess { bitmap ->
                        return@runCatching bitmap
                    }
                    .onFailure { exception ->
                        // 코루틴 취소인 경우 예외를 다시 던져서 종료
                        if (exception is CancellationException) {
                            throw exception
                        }
                        // 디코딩 실패인 경우 3단계 다운로드부터 진행
                    }
            }

            // 3. 다운로드 & 디스크 캐시에 저장
            downloadAndCache(request, diskCacheKey)

            // 4. 디코딩 & 메모리 캐시에 저장
            val bitmap = decodeAndCache(request, memoryCacheKey, diskCacheKey)
                .getOrElse { throw it }

            return@runCatching bitmap
        }
    }

    // 다운로드 & 디스크 캐시 저장
    private suspend fun downloadAndCache(request: PangRequest, diskCacheKey: String) {
        val file = PangDownloader.saveImage(request, diskCacheKey)
            .getOrElse {
                throw it
            }
            ?: throw IllegalStateException("Downloaded file is null")

        DiskCache.set(request.cachePath, file)
    }

    // 디코딩 & 메모리 캐시 저장
    private suspend fun decodeAndCache(request: PangRequest, memoryCacheKey: String, diskCacheKey: String): Result<Bitmap> = runCatching {
        val bitmap = PangDecoder.decodeFromFile(
            DecodeRequest(
                request.cachePath + "/" + diskCacheKey,
                request.imageWidth,
                request.imageHeight,
                request.inScale,
            ),
        ).getOrElse { throw it }
            ?: throw PangException.PangDecodeException

        MemoryCache.set(memoryCacheKey, bitmap)

        bitmap
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
