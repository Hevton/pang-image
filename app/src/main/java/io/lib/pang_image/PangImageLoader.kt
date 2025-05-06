package io.lib.pang_image

import android.util.Log
import android.widget.ImageView
import io.lib.pang_image.domain.PangOptionBuilder
import io.lib.pang_image.domain.PangRequest
import io.lib.pang_image.interceptor.PangInterceptor
import io.lib.pang_image.utils.size.ViewSizeHelper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object PangImageLoader {
    private const val TAG = "PangImageLoader"
    private val jobKey = R.id.pang_job_key

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

    fun ImageView.load(
        url: String,
        options: PangOptionBuilder.() -> Unit = {},
        onSuccess: ((android.graphics.Bitmap) -> Unit)? = null,
        onFailure: ((Throwable) -> Unit)? = null,
    ) {
        (getTag(jobKey) as? Job)?.cancel()
        setImageDrawable(null)

        val config = PangOptionBuilder().apply(options)

        scope.launch {
            setTag(jobKey, this.coroutineContext[Job])
            val size = ViewSizeHelper(this@load).awaitSize()

            val request = PangRequest(
                imageWidth = size.width,
                imageHeight = size.height,
                url = url,
                cachePath = context.cacheDir.path,
                inScale = config.inScale,
                retry = config.retry,
            )

            PangInterceptor.interceptor(request)
                .onSuccess { bitmap ->
                    setImageBitmap(bitmap)
                    onSuccess?.invoke(bitmap)
                }
                .onFailure {
                    Log.e(TAG, "Exception: ${it.message}", it)
                    onFailure?.invoke(it)
                }
        }
    }
}
