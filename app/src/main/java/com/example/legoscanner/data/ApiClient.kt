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
    private const val ROBOFLOW_BASE_URL = "https://serverless.roboflow.com/"
    private const val TIMEOUT_SECONDS = 45L

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val rebrickableAuth = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "key ${Config.rebrickableApiKey}")
            .addHeader("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    private fun httpClient(vararg interceptors: Interceptor): OkHttpClient =
        OkHttpClient.Builder()
            .apply { interceptors.forEach { addInterceptor(it) } }
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    val rebrickable: RebrickableApi = Retrofit.Builder()
        .baseUrl(REBRICKABLE_BASE_URL)
        .client(httpClient(rebrickableAuth))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RebrickableApi::class.java)

    val roboflow: RoboflowApi = Retrofit.Builder()
        .baseUrl(ROBOFLOW_BASE_URL)
        .client(httpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RoboflowApi::class.java)
}
