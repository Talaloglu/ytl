package com.movieapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing date information for movie releases
 * Used in now_playing and upcoming movie responses
 */
data class Dates(
    @SerializedName("maximum")
    val maximum: String,
    
    @SerializedName("minimum")
    val minimum: String
)