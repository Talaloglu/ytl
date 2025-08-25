package com.movieapp.utils

/**
 * API Configuration for TMDB and Supabase integration
 * 
 * TMDB API Key setup:
 * 1. Go to https://www.themoviedb.org/
 * 2. Create an account or sign in
 * 3. Go to Settings -> API
 * 4. Request an API key (it's free)
 * 5. Replace the TMDB_API_KEY value below
 * 
 * Supabase setup:
 * 1. Go to https://supabase.com/
 * 2. Create a new project
 * 3. Get your project URL and anon key from Settings -> API
 * 4. Replace the SUPABASE_* values below
 */
object ApiConfig {
    // TMDB Configuration
    const val TMDB_API_KEY = "8029f4f5cf7c962a99e4e7b82876f108"
    const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
    
    // Supabase Configuration
    // TODO: Replace with your actual Supabase project URL and key
    // Example: https://your-project-id.supabase.co
    const val SUPABASE_URL = "https://lgausczyvnnwvlqiiufc.supabase.co"
    // Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxnYXVzY3p5dm5ud3ZscWlpdWZjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDMxMTI5ODAsImV4cCI6MjA1ODY4ODk4MH0.Hb27MieZ2PN3DYiBQOVIcCsab_SYxRkE0mJ6yCkW96Y"
    
    // Backward compatibility
    const val API_KEY = TMDB_API_KEY
    const val BASE_URL = TMDB_BASE_URL
    
    // Image sizes
    const val POSTER_SIZE_W500 = "w500"
    const val BACKDROP_SIZE_W1280 = "w1280"
    
    /**
     * Get full image URL for posters
     */
    fun getPosterUrl(posterPath: String?): String? {
        return posterPath?.let { "${IMAGE_BASE_URL}${POSTER_SIZE_W500}$it" }
    }
    
    /**
     * Get full image URL for backdrops
     */
    fun getBackdropUrl(backdropPath: String?): String? {
        return backdropPath?.let { "${IMAGE_BASE_URL}${BACKDROP_SIZE_W1280}$it" }
    }
}