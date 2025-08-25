package com.movieapp.utils

import com.movieapp.data.api.ApiInterface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Retrofit Instance singleton object as specified
 * Initializes Retrofit with base URL and Gson converter factory
 * Lazily initializes the ApiInterface instance
 * Uses centralized API configuration
 */
object RetrofitInstance {
    
    // HTTP logging interceptor for debugging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // OkHttpClient with timeout configurations and interceptors
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    // Retrofit instance with base URL and Gson converter factory
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // Lazily initialized ApiInterface instance as specified
    val apiInterface: ApiInterface by lazy {
        retrofit.create(ApiInterface::class.java)
    }
    
    /**
     * Get the base URL for building image URLs
     * @return The TMDB base URL
     */
    fun getBaseUrl(): String = ApiConfig.BASE_URL
    
    /**
     * Get the image base URL for TMDB images
     * @param size The image size (w500, w780, w1280, original, etc.)
     * @return Complete image base URL
     */
    fun getImageBaseUrl(size: String = "w500"): String = "${ApiConfig.IMAGE_BASE_URL}$size"
}