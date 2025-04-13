package io.lib.pang_image.utils.keygen

import java.math.BigInteger
import java.security.MessageDigest

object CacheKey {
    private val messageDigest: MessageDigest = MessageDigest.getInstance("MD5")

    fun disk(url: String): String {
        return BigInteger(1, messageDigest.digest(url.toByteArray())).toString(16).padStart(32, '0')
    }

    fun memory(
        url: String,
        width: Int,
        height: Int,
    ): String {
        return BigInteger(1, messageDigest.digest("$url{$width}x$height".toByteArray())).toString(
            16,
        ).padStart(32, '0')
    }
}
