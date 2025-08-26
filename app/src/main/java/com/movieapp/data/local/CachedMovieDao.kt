package com.movieapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CachedMovieDao {
    @Query("SELECT * FROM cached_movies ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaged(limit: Int, offset: Int): List<CachedMovieEntity>

    @Query("SELECT * FROM cached_movies WHERE tmdbId = :tmdbId LIMIT 1")
    suspend fun getById(tmdbId: Int): CachedMovieEntity?

    @Query("SELECT * FROM cached_movies WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun search(query: String, limit: Int, offset: Int): List<CachedMovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedMovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CachedMovieEntity)

    @Query("DELETE FROM cached_movies")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(items: List<CachedMovieEntity>) {
        clearAll()
        if (items.isNotEmpty()) upsertAll(items)
    }
}
