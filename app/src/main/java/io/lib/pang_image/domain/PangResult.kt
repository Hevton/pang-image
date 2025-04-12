package com.example.imageloader.pang.util.domain

sealed class PangResult<out T: Any> {

    data class Success<out T: Any>(val data: T): PangResult<T>()
    data class Fail(val msg: String) : PangResult<Nothing>()
}

