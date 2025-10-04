package com.movieapp.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * Watchlist Cache Entity for Room database
 * Stores user's watchlist with sync support
 * 
 * @property userId User ID who added the movie
 * @property movieId TMDB movie ID
 * @property addedAt Timestamp when movie was added to watchlist
 * @property syncedAt Timestamp when synced with Supabase (0 if not synced)
 * @property needsSync Flag indicating if sync is needed
 */
@Entity(
    tableName = "watchlist_cache",
    primaryKeys = ["userId", "movieId"],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["movieId"]),
        Index(value = ["addedAt"]),
        Index(value = ["needsSync"])
    ]
)
data class WatchlistCacheEntity(
    val userId: String,
    val movieId: Int,
    val addedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long = 0,
    val needsSync: Boolean = true
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(movieId > 0) { "Movie ID must be positive" }
        require(addedAt > 0) { "Added timestamp must be positive" }
        require(syncedAt >= 0) { "Synced timestamp cannot be negative" }
    }
    
    companion object {
        /**
         * Create a new watchlist entry
         * 
         * @param userId User ID
         * @param movieId Movie ID
         * @return New WatchlistCacheEntity marked for sync
         */
        fun create(
            userId: String,
            movieId: Int
        ): WatchlistCacheEntity {
            return WatchlistCacheEntity(
                userId = userId,
                movieId = movieId,
                addedAt = System.currentTimeMillis(),
                syncedAt = 0,
                needsSync = true
            )
        }
    }
    
    /**
     * Mark as synced with Supabase
     * 
     * @return Updated entity with sync timestamp
     */
    fun markAsSynced(): WatchlistCacheEntity {
        return copy(
            syncedAt = System.currentTimeMillis(),
            needsSync = false
        )
    }
    
    /**
     * Mark as needing sync
     * 
     * @return Updated entity marked for sync
     */
    fun markForSync(): WatchlistCacheEntity {
        return copy(needsSync = true)
    }
    
    /**
     * Check if entry is stale (not synced in 1 hour)
     * 
     * @return True if entry needs re-sync
     */
    fun isStale(): Boolean {
        return needsSync || (System.currentTimeMillis() - syncedAt > 3600000)
    }
}
