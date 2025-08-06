package com.rizilab.averroes.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API client configuration
 */
object ApiClient {
    private const val COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3/"
    
    /**
     * OkHttp client with logging
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "Averroes-Android-App/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    /**
     * Retrofit instance for CoinGecko API
     */
    private val coinGeckoRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(COINGECKO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * CoinGecko API service
     */
    val coinGeckoApi: CoinGeckoApi by lazy {
        coinGeckoRetrofit.create(CoinGeckoApi::class.java)
    }
}

/**
 * API response wrapper
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
}

/**
 * Extension function to handle API responses
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> retrofit2.Response<T>): ApiResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            response.body()?.let { body ->
                ApiResult.Success(body)
            } ?: ApiResult.Error("Empty response body")
        } else {
            ApiResult.Error(
                message = response.message() ?: "Unknown error",
                code = response.code()
            )
        }
    } catch (e: java.net.UnknownHostException) {
        ApiResult.NetworkError
    } catch (e: java.net.SocketTimeoutException) {
        ApiResult.Error("Request timeout")
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unknown error occurred")
    }
}
