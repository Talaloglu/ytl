package com.movieapp.data.local

import androidx.room.Entity
import androidx.room.Index
import com.movieapp.data.model.WatchProgress

/**
 * Watch Progress Cache Entity for Room database
 * Stores user's viewing progress with sync support
 * 
 * @property userId User ID
 * @property movieId Movie ID
 * @property currentPositionMs Current playback position in milliseconds
 * @property durationMs Total video duration in milliseconds
 * @property watchPercentage Percentage watched (0.0 to 1.0)
 * @property lastUpdatedAt Timestamp of last update
 * @property isCompleted Whether the movie has been fully watched
 * @property syncedAt Timestamp when synced with Supabase (0 if not synced)
 * @property needsSync Flag indicating if sync is needed
 */
@Entity(
    tableName = "watch_progress_cache",
    primaryKeys = ["userId", "movieId"],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["movieId"]),
        Index(value = ["lastUpdatedAt"]),
        Index(value = ["needsSync"]),
        Index(value = ["isCompleted"])
    ]
)
data class WatchProgressCacheEntity(
    val userId: String,
    val movieId: Int,
    val currentPositionMs: Long,
    val durationMs: Long,
    val watchPercentage: Float,
    val lastUpdatedAt: Long,
    val isCompleted: Boolean,
    val syncedAt: Long = 0,
    val needsSync: Boolean = true
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(movieId > 0) { "Movie ID must be positive" }
        require(currentPositionMs >= 0) { "Current position cannot be negative" }
        require(durationMs > 0) { "Duration must be positive" }
        require(watchPercentage in 0f..1f) { "Watch percentage must be between 0 and 1" }
        require(lastUpdatedAt > 0) { "Last updated timestamp must be positive" }
        require(syncedAt >= 0) { "Synced timestamp cannot be negative" }
        require(currentPositionMs <= durationMs) { "Current position cannot exceed duration" }
    }
    
    companion object {
        /**
         * Create from WatchProgress model
         * 
         * @param watchProgress Watch progress model
         * @param needsSync Whether sync is needed
         * @return New WatchProgressCacheEntity
         */
        fun fromWatchProgress(
            watchProgress: WatchProgress,
            needsSync: Boolean = false
        ): WatchProgressCacheEntity {
            val now = System.currentTimeMillis()
            return WatchProgressCacheEntity(
                userId = watchProgress.userId,
                movieId = watchProgress.movieId,
                currentPositionMs = watchProgress.currentPositionMs,
                durationMs = watchProgress.durationMs,
                watchPercentage = watchProgress.watchPercentage,
                lastUpdatedAt = watchProgress.lastUpdatedAt,
                isCompleted = watchProgress.isCompleted,
                syncedAt = if (needsSync) 0 else now,
                needsSync = needsSync
            )
        }
        
        /**
         * Create offline progress entry
         * 
         * @param userId User ID
         * @param movieId Movie ID
         * @param durationMs Video duration
         * @return New progress entity marked for sync
         */
        fun createOffline(
            userId: String,
            movieId: Int,
            durationMs: Long
        ): WatchProgressCacheEntity {
            val now = System.currentTimeMillis()
            return WatchProgressCacheEntity(
                userId = userId,
                movieId = movieId,
                currentPositionMs = 0,
                durationMs = durationMs,
                watchPercentage = 0f,
                lastUpdatedAt = now,
                isCompleted = false,
                syncedAt = 0,
                needsSync = true
            )
        }
    }
    
    /**
     * Convert to WatchProgress model
     * 
     * @return WatchProgress instance
     */
    fun toWatchProgress(): WatchProgress {
        return WatchProgress(
            userId = userId,
            movieId = movieId,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            watchPercentage = watchPercentage,
            lastUpdatedAt = lastUpdatedAt,
            isCompleted = isCompleted
        )
    }
    
    /**
     * Mark as synced
     * 
     * @return Updated entity with sync timestamp
     */
    fun markAsSynced(): WatchProgressCacheEntity {
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
    fun markForSync(): WatchProgressCacheEntity {
        return copy(needsSync = true)
    }
    
    /**
     * Update progress
     * 
     * @param newPositionMs New playback position
     * @param newDurationMs New duration (optional)
     * @return Updated entity
     */
    fun updateProgress(
        newPositionMs: Long,
        newDurationMs: Long = durationMs
    ): WatchProgressCacheEntity {
        val newPercentage = WatchProgress.calculateWatchPercentage(newPositionMs, newDurationMs)
        val shouldComplete = newPercentage >= 0.95f
        
        return copy(
            currentPositionMs = newPositionMs,
            durationMs = newDurationMs,
            watchPercentage = newPercentage,
            lastUpdatedAt = System.currentTimeMillis(),
            isCompleted = isCompleted || shouldComplete,
            needsSync = true
        )
    }
    
    /**
     * Mark as completed
     * 
     * @return Updated entity marked as complete
     */
    fun markCompleted(): WatchProgressCacheEntity {
        return copy(
            watchPercentage = 1.0f,
            lastUpdatedAt = System.currentTimeMillis(),
            isCompleted = true,
            needsSync = true
        )
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
     * Check if can resume playback
     * Position > 30s, not completed, and < 95% watched
     * 
     * @return True if can resume
     */
    fun canResume(): Boolean {
        return currentPositionMs > 30000 && 
               watchPercentage < 0.95f && 
               !isCompleted
    }
    
    /**
     * Validate entity data
     * 
     * @return True if valid
     */
    fun isValid(): Boolean {
        return userId.isNotBlank() &&
               movieId > 0 &&
               currentPositionMs >= 0 &&
               durationMs > 0 &&
               watchPercentage in 0f..1f &&
               lastUpdatedAt > 0 &&
               syncedAt >= 0 &&
               currentPositionMs <= durationMs
    }
}
