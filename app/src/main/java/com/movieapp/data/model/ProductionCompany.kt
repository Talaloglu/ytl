package com.movieapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a production company
 */
data class ProductionCompany(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("logo_path")
    val logoPath: String?,
    
    @SerializedName("origin_country")
    val originCountry: String
)