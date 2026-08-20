package com.example.legoscanner.data

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface RoboflowApi {

    @POST
    suspend fun detect(
        @Url url: String,
        @Body imageBase64: RequestBody
    ): DetectionResponse
}
