package com.movieapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a movie genre
 */
data class Genre(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String
)