package com.movieapp.data.model

import com.google.gson.annotations.SerializedName
import com.movieapp.utils.ApiConfig

/**
 * Supabase-enriched movie model.
 * Maps to columns added to the `movies` table via the TMDB metadata migration.
 * Includes the streamable `videourl` and common TMDB fields so we can fetch
 * everything from Supabase at runtime (no TMDB API calls needed).
 */
data class SupabaseEnrichedMovie(
    // Primary key in Supabase `movies` table (TEXT)
    @SerializedName("id")
    val id: String,

    // Numeric TMDB id persisted in Supabase (used for lookups and navigation)
    @SerializedName("tmdb_id")
    val tmdbId: Int? = null,

    @SerializedName("title")
    val title: String,

    // Stream URL
    @SerializedName("videourl")
    val videoUrl: String?,

    // TMDB metadata persisted into Supabase
    @SerializedName("overview")
    val overview: String? = null,

    @SerializedName("poster_path")
    val posterPath: String? = null,

    @SerializedName("backdrop_path")
    val backdropPath: String? = null,

    @SerializedName("release_date")
    val releaseDate: String? = null,

    @SerializedName("vote_average")
    val voteAverage: Double? = null,

    @SerializedName("vote_count")
    val voteCount: Int? = null,

    @SerializedName("popularity")
    val popularity: Double? = null,

    @SerializedName("original_language")
    val originalLanguage: String? = null,

    @SerializedName("original_title")
    val originalTitle: String? = null,

    // Stored as int[] in Postgres; exposed as JSON array by PostgREST
    @SerializedName("genre_ids")
    val genreIds: List<Int>? = null,

    // Full genre objects persisted for display
    @SerializedName("genres_json")
    val genresJson: List<GenreItem>? = null,

    // Runtime in minutes (from TMDB), if available
    @SerializedName("runtime")
    val runtime: Int? = null,

    // Production companies from TMDB persisted for display
    @SerializedName("companies_json")
    val companiesJson: List<CompanyItem>? = null,

    @SerializedName("publishedat")
    val publishedAt: String? = null
) {
    /**
     * Validates if the movie has a playable URL.
     */
    fun hasValidVideoUrl(): Boolean {
        val url = videoUrl?.trim().orEmpty()
        return url.isNotEmpty() && (url.startsWith("http://") || url.startsWith("https://"))
    }

/**
 * TMDB Genre item persisted as JSON in Supabase.
 */
data class GenreItem(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

/**
 * TMDB Production company item persisted as JSON in Supabase.
 */
data class CompanyItem(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

    /**
     * Build full poster URL from TMDB path using the configured base.
     */
    fun getFullPosterUrl(): String? {
        return posterPath?.let { path ->
            if (path.startsWith("http")) path else ApiConfig.IMAGE_BASE_URL + "w500" + path
        }
    }

    /**
     * Build full backdrop URL from TMDB path using the configured base.
     */
    fun getFullBackdropUrl(): String? {
        return backdropPath?.let { path ->
            if (path.startsWith("http")) path else ApiConfig.IMAGE_BASE_URL + "w780" + path
        }
    }

    /**
     * Validate that we have at least one thumbnail (poster or backdrop).
     * This helps the UI avoid rendering items with missing images.
     */
    fun hasPosterOrBackdrop(): Boolean {
        val poster = posterPath?.trim().orEmpty()
        val backdrop = backdropPath?.trim().orEmpty()
        return poster.isNotEmpty() || backdrop.isNotEmpty()
    }
}
