package io.lib.pang_image

import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.view.doOnPreDraw
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
    private val jobKey = R.id.pang_job_key
    private val urlKey = R.id.pang_url_key

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
        setImageDrawable(null)
        setTag(urlKey, url)
        (getTag(jobKey) as? Job)?.cancel()

        doOnPreDraw {
            if (getTag(urlKey) != url) return@doOnPreDraw // View 재활용 무시

            val req = PangRequest(width, height, url, context.cacheDir.path)
            val job = scope.launch {
                PangInterceptor.interceptor(req)
                    .onSuccess {
                        if (getTag(urlKey) == url) setImageBitmap(it) // 결과 도착 tag 확인
                    }
                    .onFailure {
                        Log.e(TAG, "Exception: ${it.message}", it)
                    }
            }
            setTag(jobKey, job)
        }

        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                (getTag(jobKey) as? Job)?.cancel()
                setTag(jobKey, null)
                removeOnAttachStateChangeListener(this)
            }
        })
    }
}
