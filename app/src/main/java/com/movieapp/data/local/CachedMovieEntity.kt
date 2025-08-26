package com.movieapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.movieapp.data.model.CombinedMovie

/**
 * Room entity for caching streamable movies offline.
 * We persist the full CombinedMovie as JSON for resilience against
 * model evolution and to avoid heavy relational joins.
 */
@Entity(tableName = "cached_movies")
data class CachedMovieEntity(
    @PrimaryKey val tmdbId: Int,
    val title: String,
    val hasVideo: Boolean,
    val combinedJson: String,
    val updatedAt: Long
) {
    companion object {
        fun fromCombined(movie: CombinedMovie, gson: Gson = Gson(), nowUtcMillis: Long = System.currentTimeMillis()): CachedMovieEntity =
            CachedMovieEntity(
                tmdbId = movie.id,
                title = movie.title,
                hasVideo = movie.isStreamable,
                combinedJson = gson.toJson(movie),
                updatedAt = nowUtcMillis
            )
    }
}

fun CachedMovieEntity.toCombinedMovie(gson: Gson = Gson()): CombinedMovie {
    return gson.fromJson(this.combinedJson, CombinedMovie::class.java)
}
