package com.movieapp.utils

import com.movieapp.BuildConfig
import com.movieapp.data.api.SupabaseApiInterface
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Supabase Retrofit Instance singleton for database operations
 * Follows the same pattern as TMDB RetrofitInstance
 * Uses centralized API configuration from ApiConfig
 */
object SupabaseRetrofitInstance {
    
    // HTTP logging interceptor for debugging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // Interceptor that injects Supabase headers into every request
    private val supabaseHeadersInterceptor = Interceptor { chain ->
        val original = chain.request()
        val newReq = original.newBuilder()
            .header("apikey", getApiKeyHeader())
            .header("Authorization", getAuthorizationHeader())
            .build()
        chain.proceed(newReq)
    }

    // OkHttpClient with Supabase specific configurations
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(supabaseHeadersInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    // Retrofit instance for Supabase REST API
    private val retrofit: Retrofit by lazy {
        ensureConfig()
        Retrofit.Builder()
            .baseUrl(getSupabaseRestUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // Lazily initialized Supabase API interface
    val apiInterface: SupabaseApiInterface by lazy {
        retrofit.create(SupabaseApiInterface::class.java)
    }
    
    /**
     * Get Supabase REST API URL
     * @return Complete Supabase REST API endpoint
     */
    private fun getSupabaseRestUrl(): String = "${BuildConfig.SUPABASE_URL}/rest/v1/"
    
    /**
     * Get authorization header value
     * @return Bearer token for API calls
     */
    fun getAuthorizationHeader(): String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    
    /**
     * Get API key header value
     * @return Supabase anon key
     */
    fun getApiKeyHeader(): String = BuildConfig.SUPABASE_ANON_KEY

    /**
     * Validate required BuildConfig values to avoid cryptic runtime crashes
     * when Retrofit is initialized with an empty base URL or missing headers.
     */
    private fun ensureConfig() {
        val url = BuildConfig.SUPABASE_URL?.trim().orEmpty()
        val key = BuildConfig.SUPABASE_ANON_KEY?.trim().orEmpty()
        require(url.isNotEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
            "SUPABASE_URL is missing or invalid. Set SUPABASE_URL in local.properties (e.g., https://<project>.supabase.co)."
        }
        require(key.isNotEmpty()) {
            "SUPABASE_ANON_KEY is missing. Set SUPABASE_ANON_KEY in local.properties (Settings > API > anon key)."
        }
    }
}