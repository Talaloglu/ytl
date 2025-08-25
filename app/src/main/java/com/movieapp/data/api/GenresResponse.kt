package com.movieapp.data.api

import com.movieapp.data.model.Genre
import com.google.gson.annotations.SerializedName

/**
 * Data class representing the genres list API response
 */
data class GenresResponse(
    @SerializedName("genres")
    val genres: List<Genre>
)