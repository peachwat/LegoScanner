package com.example.legoscanner.data

import com.example.legoscanner.Config
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val REBRICKABLE_BASE_URL = "https://rebrickable.com/api/v3/"
    private const val TIMEOUT_SECONDS = 30L

    private val rebrickableAuth = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "key ${Config.rebrickableApiKey}")
            .addHeader("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val rebrickableClient = OkHttpClient.Builder()
        .addInterceptor(rebrickableAuth)
        .addInterceptor(logging)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    val rebrickable: RebrickableApi = Retrofit.Builder()
        .baseUrl(REBRICKABLE_BASE_URL)
        .client(rebrickableClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RebrickableApi::class.java)
}
