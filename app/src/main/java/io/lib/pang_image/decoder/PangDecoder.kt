package io.lib.pang_image.decoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build.VERSION.SDK_INT
import io.lib.pang_image.domain.DecodeRequest
import io.lib.pang_image.utils.dispatchers.DispatchersHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

object PangDecoder {

    private val DEFAULT_BITMAP_CONFIG = if (SDK_INT >= 26) {
        Bitmap.Config.HARDWARE
    } else {
        Bitmap.Config.ARGB_8888
    }

    suspend fun decodeFromFile(
        decodeRequest: DecodeRequest,
        decodeDispatcher: CoroutineDispatcher = DispatchersHelper.decodeDispatcher,
    ): Result<Bitmap> =
        runCatching {
            withContext(decodeDispatcher) {
                BitmapFactory.Options().run {
                    inJustDecodeBounds = true // 메타 정보만
                    inPreferredConfig = DEFAULT_BITMAP_CONFIG
                    BitmapFactory.decodeFile(decodeRequest.filePath, this)

                    inJustDecodeBounds = false
                    inSampleSize = calculateInSampleSize(this, decodeRequest.reqWidth, decodeRequest.reqHeight)

                    if (decodeRequest.inScale) {
                        inScaled = true
                        if (outWidth >= outHeight) {
                            inDensity = outWidth
                            inTargetDensity = decodeRequest.reqWidth * inSampleSize
                        } else {
                            inDensity = outHeight
                            inTargetDensity = decodeRequest.reqHeight * inSampleSize
                        }
                    }

                    BitmapFactory.decodeFile(decodeRequest.filePath, this)
                }
            }
        }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
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
