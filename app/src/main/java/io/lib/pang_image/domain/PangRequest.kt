package io.lib.pang_image.domain

data class PangRequest(
    val imageWidth: Int,
    val imageHeight: Int,
    val url: String,
    val cachePath: String,
    val inScale: Boolean = false,
)
