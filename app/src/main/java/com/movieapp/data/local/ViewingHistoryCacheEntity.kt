package com.movieapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Viewing History Cache Entity for Room database
 * Stores user's viewing history with timestamps
 * 
 * @property id Auto-generated primary key
 * @property userId User ID
 * @property movieId Movie ID
 * @property viewedAt Timestamp when movie was viewed
 * @property viewDurationMs Duration of viewing session in milliseconds
 * @property completedView Whether the viewing session was completed
 * @property syncedAt Timestamp when synced with Supabase (0 if not synced)
 * @property needsSync Flag indicating if sync is needed
 */
@Entity(
    tableName = "viewing_history_cache",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["movieId"]),
        Index(value = ["viewedAt"]),
        Index(value = ["needsSync"]),
        Index(value = ["userId", "movieId", "viewedAt"])
    ]
)
data class ViewingHistoryCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val movieId: Int,
    val viewedAt: Long = System.currentTimeMillis(),
    val viewDurationMs: Long = 0,
    val completedView: Boolean = false,
    val syncedAt: Long = 0,
    val needsSync: Boolean = true
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(movieId > 0) { "Movie ID must be positive" }
        require(viewedAt > 0) { "Viewed timestamp must be positive" }
        require(viewDurationMs >= 0) { "View duration cannot be negative" }
        require(syncedAt >= 0) { "Synced timestamp cannot be negative" }
    }
    
    companion object {
        /**
         * Create a new viewing history entry
         * 
         * @param userId User ID
         * @param movieId Movie ID
         * @param viewDurationMs Duration of viewing session
         * @param completedView Whether viewing was completed
         * @return New ViewingHistoryCacheEntity
         */
        fun create(
            userId: String,
            movieId: Int,
            viewDurationMs: Long = 0,
            completedView: Boolean = false
        ): ViewingHistoryCacheEntity {
            return ViewingHistoryCacheEntity(
                userId = userId,
                movieId = movieId,
                viewedAt = System.currentTimeMillis(),
                viewDurationMs = viewDurationMs,
                completedView = completedView,
                syncedAt = 0,
                needsSync = true
            )
        }
    }
    
    /**
     * Mark as synced
     * 
     * @return Updated entity with sync timestamp
     */
    fun markAsSynced(): ViewingHistoryCacheEntity {
        return copy(
            syncedAt = System.currentTimeMillis(),
            needsSync = false
        )
    }
    
    /**
     * Mark for sync
     * 
     * @return Updated entity marked for sync
     */
    fun markForSync(): ViewingHistoryCacheEntity {
        return copy(needsSync = true)
    }
    
    /**
     * Check if entry is stale (not synced in 1 hour)
     * 
     * @return True if needs re-sync
     */
    fun isStale(): Boolean {
        return System.currentTimeMillis() - syncedAt > 3600000
    }
    
    /**
     * Check if viewing session is recent (within 24 hours)
     * 
     * @return True if recent
     */
    fun isRecent(): Boolean {
        return System.currentTimeMillis() - viewedAt < 86400000 // 24 hours
    }
}
