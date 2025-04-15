package io.lib.pang_image.utils.dispatchers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
object DispatchersHelper {
    private val cpu = Runtime.getRuntime().availableProcessors()

    val decodeDispatcher by lazy {
        Dispatchers.Default.limitedParallelism(
            cpu.coerceAtMost(4).coerceAtLeast(2),
        )
    }

    val downloadDispatcher by lazy {
        // 네트워크 상황을 고려해 적당히
        if (cpu >= 8) {
            Dispatchers.IO.limitedParallelism(8)
        } else Dispatchers.IO.limitedParallelism(6)
    }

    val diskDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(2)
    }
}
