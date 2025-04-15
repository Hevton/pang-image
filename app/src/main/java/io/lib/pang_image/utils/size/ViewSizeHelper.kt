package io.lib.pang_image.utils.size

import android.view.View
import android.view.ViewTreeObserver
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ViewSizeHelper(private val view: View) {

    suspend fun awaitSize(): ViewSize = suspendCancellableCoroutine { cont ->
        if (view.width > 0 && view.height > 0) {
            cont.resume(ViewSize(view.width, view.height))
            return@suspendCancellableCoroutine
        }

        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (view.width > 0 && view.height > 0) {
                    view.viewTreeObserver.removeOnPreDrawListener(this)
                    if (cont.isActive) {
                        cont.resume(ViewSize(view.width, view.height))
                    }
                    return true
                }
                return true
            }
        }

        view.viewTreeObserver.addOnPreDrawListener(listener)

        // 코루틴 취소 시 메모리 누수 방지
        cont.invokeOnCancellation {
            view.viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }
}
