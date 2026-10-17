package com.graberhub.companion.network

import android.util.Log
import com.graberhub.companion.BuildConfig
import com.graberhub.companion.data.prefs.PrefsManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object RetrofitClient {

    private const val TAG = "RetrofitClient"
    private var baseUrl = PrefsManager.DEFAULT_SERVER
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    /** Thread-safe store for the latest data version from the server. */
    val dataVersion = AtomicInteger(0)

    fun init(serverUrl: String) {
        val url = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        Log.d(TAG, "init() called with serverUrl=$serverUrl → baseUrl=$url")
        if (url != baseUrl || retrofit == null) {
            baseUrl = url
            retrofit = null
            apiService = null
            Log.d(TAG, "Retrofit instance reset, will rebuild on next request")
        }
    }

    /**
     * Interceptor that attaches X-Data-Version header to all POST/PUT/DELETE requests
     * so the server can reject stale writes with 409.
     */
    private val versionRequestInterceptor = Interceptor { chain ->
        val request = chain.request()
        val method = request.method.uppercase()
        if (method in listOf("POST", "PUT", "DELETE")) {
            val version = dataVersion.get()
            if (version > 0) {
                val newRequest = request.newBuilder()
                    .addHeader("X-Data-Version", version.toString())
                    .build()
                return@Interceptor chain.proceed(newRequest)
            }
        }
        chain.proceed(request)
    }

    /**
     * Interceptor that reads X-Data-Version from every response and updates our local store.
     */
    private val versionResponseInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        val versionHeader = response.header("X-Data-Version")
        if (versionHeader != null) {
            try {
                val serverVersion = versionHeader.toInt()
                // Only move forward, never backward
                dataVersion.updateAndGet { current -> maxOf(current, serverVersion) }
                Log.d(TAG, "Updated data version from header: $serverVersion")
            } catch (_: NumberFormatException) {
                Log.w(TAG, "Invalid X-Data-Version header: $versionHeader")
            }
        }
        response
    }

    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            Log.d(TAG, "Building new Retrofit instance with baseUrl=$baseUrl")
            val logging = HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(versionRequestInterceptor)
                .addInterceptor(logging)
                .addInterceptor(versionResponseInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
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
            Log.d(TAG, "testConnection() → baseUrl=$baseUrl")
            val response = getApiService().getDashboard()
            Log.d(TAG, "testConnection() → code=${response.code()}, success=${response.isSuccessful}")
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "testConnection() failed: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }
}
