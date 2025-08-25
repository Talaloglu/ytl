package com.movieapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a spoken language in a movie
 */
data class SpokenLanguage(
    @SerializedName("iso_639_1")
    val iso6391: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("english_name")
    val englishName: String
)