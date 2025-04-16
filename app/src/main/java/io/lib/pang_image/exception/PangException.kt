package io.lib.pang_image.exception

sealed class PangException(message: String?) : Exception(message) {
    data object PangDecodeException : PangException("Decoded bitmap is null")
}
