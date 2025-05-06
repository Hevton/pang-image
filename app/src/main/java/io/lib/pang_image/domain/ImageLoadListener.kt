package io.lib.pang_image.domain

import android.graphics.Bitmap

interface ImageLoadListener {
    fun onSuccess(bitmap: Bitmap)
    fun onFailure(error: Throwable)
} 