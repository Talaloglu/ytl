package com.movieapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a production country
 */
data class ProductionCountry(
    @SerializedName("iso_3166_1")
    val iso31661: String,
    
    @SerializedName("name")
    val name: String
)