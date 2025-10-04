package com.movieapp.data.repository

import com.movieapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

/**
 * Supabase Client Provider
 * Singleton providing access to Supabase client and services
 */
object SupabaseClientProvider {
    
    @Volatile
    private var instance: SupabaseClient? = null
    
    fun getInstance(): SupabaseClient {
        return instance ?: synchronized(this) {
            instance ?: createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
            }.also { instance = it }
        }
    }
    
    fun getGotrue(): Auth {
        return getInstance().auth
    }
    
    fun getPostgrest(): Postgrest {
        return getInstance().postgrest
    }
}
