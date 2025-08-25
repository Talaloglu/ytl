package com.movieapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Simplified Supabase Movie model for streaming integration
 * Only fetches essential data: title (for TMDB matching) and videourl (for streaming)
 * All other metadata comes from TMDB for better performance and reduced bandwidth
 */
data class SupabaseMovie(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("videourl")
    val videoUrl: String
) {
    /**
     * Check if movie has valid video URL for streaming
     * Enhanced validation to ensure streaming capability
     */
    fun hasValidVideoUrl(): Boolean {
        return videoUrl.isNotBlank() && 
               videoUrl.trim().isNotEmpty() && 
               (videoUrl.startsWith("http://") || videoUrl.startsWith("https://"))
    }
}

/**
 * Combined movie data with TMDB info and Supabase video URL
 * TMDB provides all movie metadata, Supabase provides only the video URL
 */
data class CombinedMovie(
    val tmdbMovie: Movie,
    val supabaseMovie: SupabaseMovie,
    val tmdbMovieDetails: MovieDetails? = null,
    // Supabase-enriched extras
    val runtimeMinutes: Int? = null,
    val genreNames: List<String>? = null,
    val companyNames: List<String>? = null
) {
    val id: Int get() = tmdbMovie.id
    val title: String get() = tmdbMovie.title
    val overview: String get() = tmdbMovie.overview
    val posterPath: String? get() = tmdbMovie.posterPath
    val backdropPath: String? get() = tmdbMovie.backdropPath
    val releaseDate: String get() = tmdbMovie.releaseDate
    val voteAverage: Double get() = tmdbMovie.voteAverage
    val voteCount: Int get() = tmdbMovie.voteCount
    
    // Supabase data - only video URL is used from Supabase
    val videoUrl: String get() = supabaseMovie.videoUrl
    // Use provided runtime minutes if available; fallback to TMDB details if present
    val duration: String get() = getFormattedRuntime()

    val category: String get() = "Movie" // Default category since we get this from TMDB genres
    val hasVideo: Boolean get() = supabaseMovie.hasValidVideoUrl()
    
    /**
     * Additional validation to ensure this movie is truly streamable
     * This acts as a final safeguard in the UI layer
     */
    val isStreamable: Boolean get() = hasVideo && videoUrl.isNotBlank() && 
                                      (videoUrl.startsWith("http://") || videoUrl.startsWith("https://"))
    
    /**
     * Get full poster URL using TMDB path
     */
    fun getFullPosterUrl(): String? {
        return tmdbMovie.getFullPosterUrl()
    }
    
    /**
     * Get full backdrop URL using TMDB path
     */
    fun getFullBackdropUrl(): String? {
        return tmdbMovie.getFullBackdropUrl()
    }

    /**
     * Format runtime in minutes to "Xh Ym"; returns "Unknown" if not available.
     */
    fun getFormattedRuntime(): String {
        val minutes = runtimeMinutes ?: tmdbMovieDetails?.runtime
        return if (minutes != null && minutes > 0) {
            val h = minutes / 60
            val m = minutes % 60
            if (h > 0) "${h}h ${m}m" else "${m}m"
        } else {
            "Unknown"
        }
    }

    /**
     * Join genre names for display if available.
     */
    fun getGenresString(): String {
        return genreNames?.joinToString(", ")?.takeIf { it.isNotBlank() } ?: "Unknown"
    }

    /**
     * Join production company names for display if available.
     */
    fun getCompaniesString(): String {
        return companyNames?.joinToString(", ")?.takeIf { it.isNotBlank() } ?: "Unknown"
    }
}

/**
 * Response model for Supabase movies query
 */
data class SupabaseMovieResponse(
    @SerializedName("data")
    val data: List<SupabaseMovie>? = null,
    
    @SerializedName("error")
    val error: SupabaseError? = null,
    
    @SerializedName("count")
    val count: Int? = null
)

/**
 * Supabase error model
 */
data class SupabaseError(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("details")
    val details: String? = null,
    
    @SerializedName("hint")
    val hint: String? = null,
    
    @SerializedName("code")
    val code: String? = null
)