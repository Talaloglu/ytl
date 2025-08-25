package com.movieapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a Movie
 * Maps to the TMDB API movie object structure
 */
data class Movie(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("overview")
    val overview: String,
    
    @SerializedName("poster_path")
    val posterPath: String?,
    
    @SerializedName("backdrop_path")
    val backdropPath: String?,
    
    @SerializedName("release_date")
    val releaseDate: String,
    
    @SerializedName("vote_average")
    val voteAverage: Double,
    
    @SerializedName("vote_count")
    val voteCount: Int,
    
    @SerializedName("popularity")
    val popularity: Double,
    
    @SerializedName("genre_ids")
    val genreIds: List<Int>,
    
    @SerializedName("adult")
    val adult: Boolean,
    
    @SerializedName("video")
    val video: Boolean,
    
    @SerializedName("original_language")
    val originalLanguage: String,
    
    @SerializedName("original_title")
    val originalTitle: String
) {
    /**
     * Get the full poster URL
     * @param baseUrl The base URL for TMDB images (default: https://image.tmdb.org/t/p/w500)
     * @return Full poster URL or null if posterPath is null
     */
    fun getFullPosterUrl(baseUrl: String = "https://image.tmdb.org/t/p/w500"): String? {
        return posterPath?.let { "$baseUrl$it" }
    }
    
    /**
     * Get the full backdrop URL
     * @param baseUrl The base URL for TMDB images (default: https://image.tmdb.org/t/p/w500)
     * @return Full backdrop URL or null if backdropPath is null
     */
    fun getFullBackdropUrl(baseUrl: String = "https://image.tmdb.org/t/p/w500"): String? {
        return backdropPath?.let { "$baseUrl$it" }
    }
}