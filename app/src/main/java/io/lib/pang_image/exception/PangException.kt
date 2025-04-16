package io.lib.pang_image.exception

sealed class PangException(message: String?) : Exception(message) {
    class PangDecodeException : PangException("Decoded bitmap is null")
}
