package io.lib.pang_image

import android.util.Log
import android.view.View
import android.widget.ImageView
import io.lib.pang_image.domain.PangRequest
import io.lib.pang_image.interceptor.PangInterceptor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object PangImageLoader {
    private const val TAG = "PangImageLoader"
    private val jobKey = R.id.pang

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

        (getTag(jobKey) as? Job)?.cancel()

        addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    val job = loadInternalStart(this@load, url)
                    setTag(jobKey, job)
                }

                override fun onViewDetachedFromWindow(v: View) {
                    (getTag(jobKey) as? Job)?.cancel()
                    setTag(jobKey, null)
                    removeOnAttachStateChangeListener(this)
                }
            },
        )
    }

    private fun loadInternalStart(
        imageView: ImageView,
        url: String,
    ): Job {
        return scope.launch {
            val request =
                PangRequest(
                    imageWidth = imageView.width,
                    imageHeight = imageView.height,
                    url = url,
                    cachePath = imageView.context.cacheDir.toString(),
                )

            PangInterceptor.interceptor(request)
                .onSuccess {
                    imageView.setImageBitmap(it)
                }
                .onFailure {
                    Log.e(TAG, "Exception: ${it.message}", it)
                }
        }
    }
}
