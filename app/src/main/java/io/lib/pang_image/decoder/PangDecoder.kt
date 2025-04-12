package com.example.imageloader.pang.decoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.lib.pang_image.domain.DecodeRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PangDecoder {
    suspend fun decodeFromFile(
        decodeRequest: DecodeRequest,
        decodeDispatcher: CoroutineDispatcher = Dispatchers.Default
    ): Result<Bitmap?> = runCatching {
        withContext(decodeDispatcher) {
            BitmapFactory.Options().run {
                inJustDecodeBounds = true // 메타 정보만
                inPreferredConfig = Bitmap.Config.RGB_565
                BitmapFactory.decodeFile(decodeRequest.filePath, this)

                inJustDecodeBounds = false
                inSampleSize = calculateInSampleSize(this, decodeRequest.reqWidth, decodeRequest.reqHeight)

                inScaled = false

                BitmapFactory.decodeFile(decodeRequest.filePath, this)
            }
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (reqWidth == 0 || reqHeight == 0) return inSampleSize

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
