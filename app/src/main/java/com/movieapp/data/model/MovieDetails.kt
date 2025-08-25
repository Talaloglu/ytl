package com.movieapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing detailed movie information
 * Contains additional fields not present in the basic Movie class
 */
data class MovieDetails(
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
    
    @SerializedName("adult")
    val adult: Boolean,
    
    @SerializedName("video")
    val video: Boolean,
    
    @SerializedName("original_language")
    val originalLanguage: String,
    
    @SerializedName("original_title")
    val originalTitle: String,
    
    // Additional fields for detailed view
    @SerializedName("budget")
    val budget: Long,
    
    @SerializedName("revenue")
    val revenue: Long,
    
    @SerializedName("runtime")
    val runtime: Int?,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("tagline")
    val tagline: String?,
    
    @SerializedName("homepage")
    val homepage: String?,
    
    @SerializedName("imdb_id")
    val imdbId: String?,
    
    @SerializedName("genres")
    val genres: List<Genre>,
    
    @SerializedName("production_companies")
    val productionCompanies: List<ProductionCompany>,
    
    @SerializedName("production_countries")
    val productionCountries: List<ProductionCountry>,
    
    @SerializedName("spoken_languages")
    val spokenLanguages: List<SpokenLanguage>
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
     * @param baseUrl The base URL for TMDB images (default: https://image.tmdb.org/t/p/w1280)
     * @return Full backdrop URL or null if backdropPath is null
     */
    fun getFullBackdropUrl(baseUrl: String = "https://image.tmdb.org/t/p/w1280"): String? {
        return backdropPath?.let { "$baseUrl$it" }
    }
    
    /**
     * Get formatted runtime string
     */
    fun getFormattedRuntime(): String? {
        return runtime?.let { minutes ->
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            when {
                hours > 0 -> "${hours}h ${remainingMinutes}m"
                else -> "${minutes}m"
            }
        }
    }
    
    /**
     * Get genre names as a comma-separated string
     */
    fun getGenreNames(): String {
        return genres.joinToString(", ") { it.name }
    }
}