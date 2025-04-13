package io.lib.pang_image.domain

data class DecodeRequest(
    val filePath: String,
    val reqWidth: Int,
    val reqHeight: Int,
    val inScale: Boolean = false,
)
