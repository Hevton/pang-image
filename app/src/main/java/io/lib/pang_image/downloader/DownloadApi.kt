package io.lib.pang_image.downloader

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface DownloadApi {

    @GET
    @Streaming
    suspend fun get(@Url url: String): Response<ResponseBody>
}
