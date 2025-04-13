package io.lib.pang_image.interceptor

import android.graphics.Bitmap
import android.util.Log
import io.lib.pang_image.decoder.PangDecoder
import io.lib.pang_image.disk.DiskCache
import io.lib.pang_image.domain.DecodeRequest
import io.lib.pang_image.domain.PangRequest
import io.lib.pang_image.downloader.PangDownloader
import io.lib.pang_image.memory.MemoryCache
import io.lib.pang_image.utils.keygen.CacheKey

object PangInterceptor {
    private const val TAG = "PangInterceptor"

    suspend fun interceptor(request: PangRequest): Result<Bitmap> =
        runCatching {
            val width = request.imageWidth
            val height = request.imageHeight

            val diskCacheKey = CacheKey.disk(request.url)
            val memoryCacheKey = CacheKey.memory(request.url, width, height)

            // 1. 메모리 캐시 체크
            MemoryCache.get(memoryCacheKey)?.let {
                Log.d(TAG, "Memory Hit")
                return Result.success(it)
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
                    ).getOrElse { return Result.failure(it) }

                MemoryCache.set(memoryCacheKey, decoded)
                return Result.success(decoded)
            }

            // 3. 다운로드
            val file =
                PangDownloader.saveImage(request, diskCacheKey)
                    .getOrElse {
                        return Result.failure(it)
                    }
                    ?: return Result.failure(IllegalStateException("Downloaded file is null"))

            // 4. 디스크 저장
            DiskCache.set(request.cachePath, file)
            // 용량 정리
            DiskCache.clear(request.cachePath, file.length())

            // 5. 디코딩
            val bitmap =
                PangDecoder.decodeFromFile(
                    DecodeRequest(request.cachePath + "/" + diskCacheKey, width, height, request.inScale),
                ).getOrElse { return Result.failure(it) }

            // 6. 메모리 캐시에 저장
            MemoryCache.set(memoryCacheKey, bitmap)

            return Result.success(bitmap)
        }
}
