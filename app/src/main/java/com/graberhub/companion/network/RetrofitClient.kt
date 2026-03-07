package com.graberhub.companion.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var baseUrl = "http://192.168.86.43/"
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    fun init(serverUrl: String) {
        val url = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        if (url != baseUrl || retrofit == null) {
            baseUrl = url
            retrofit = null
            apiService = null
        }
    }

    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(
                    OkHttpClient.Builder()
                        .addInterceptor(logging)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                )
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    fun getApiService(): ApiService {
        if (apiService == null) {
            apiService = getRetrofit().create(ApiService::class.java)
        }
        return apiService!!
    }

    suspend fun testConnection(): Boolean {
        return try {
            val response = getApiService().getDashboard()
            response.isSuccessful || response.code() in 200 until 500
        } catch (e: Exception) {
            false
        }
    }
}
