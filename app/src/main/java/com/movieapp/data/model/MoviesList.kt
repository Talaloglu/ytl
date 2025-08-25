package com.movieapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Main data class representing movies list response with pagination metadata
 * This represents the standard TMDB API response structure
 */
data class MoviesList(
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("results")
    val results: List<Movie>,
    
    @SerializedName("total_pages")
    val totalPages: Int,
    
    @SerializedName("total_results")
    val totalResults: Int,
    
    @SerializedName("dates")
    val dates: Dates? = null // Optional, used for now_playing and upcoming endpoints
) {
    /**
     * Check if there are more pages available
     */
    fun hasMorePages(): Boolean = page < totalPages
    
    /**
     * Get the next page number, or null if no more pages
     */
    fun getNextPage(): Int? = if (hasMorePages()) page + 1 else null
    
    /**
     * Check if the results list is empty
     */
    fun isEmpty(): Boolean = results.isEmpty()
    
    /**
     * Get total number of results
     */
    fun getTotalCount(): Int = totalResults
}