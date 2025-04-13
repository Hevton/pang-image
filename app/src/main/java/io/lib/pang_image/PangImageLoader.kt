package io.lib.pang_image

import android.util.Log
import android.widget.ImageView
import io.lib.pang_image.domain.PangRequest
import io.lib.pang_image.interceptor.PangInterceptor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object PangImageLoader {
    private const val TAG = "PangImageLoader"

    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            // 예상 불가능한 에러
            Log.e(TAG, "Unhandled coroutine exception: ${throwable.message}", throwable)
            throw throwable
        }
    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main.immediate +
                exceptionHandler,
        )

    fun ImageView.load(url: String) {
        this.setImageDrawable(null)
        scope.launch {
            val request =
                PangRequest(
                    imageWidth = this@load.width,
                    imageHeight = this@load.height,
                    url = url,
                    cachePath = this@load.context.cacheDir.toString(),
                )

            PangInterceptor.interceptor(request)
                .onSuccess {
                    this@load.setImageBitmap(it)
                }
                .onFailure {
                    Log.e(TAG, "Exception: ${it.message}", it)
                }
        }
    }
}
