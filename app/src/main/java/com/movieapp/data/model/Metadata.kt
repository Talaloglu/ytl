package com.movieapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing metadata for API responses
 * Contains pagination information and additional response metadata
 */
data class Metadata(
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("total_pages")
    val totalPages: Int,
    
    @SerializedName("total_results")
    val totalResults: Int,
    
    @SerializedName("dates")
    val dates: Dates? = null
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
     * Get the previous page number, or null if on first page
     */
    fun getPreviousPage(): Int? = if (page > 1) page - 1 else null
    
    /**
     * Check if this is the first page
     */
    fun isFirstPage(): Boolean = page == 1
    
    /**
     * Check if this is the last page
     */
    fun isLastPage(): Boolean = page == totalPages
}