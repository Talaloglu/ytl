package com.movieapp.data.repository

import android.util.Log
import com.movieapp.data.local.DatabaseProvider
import com.movieapp.data.local.WatchProgressCacheDao
import com.movieapp.data.local.WatchProgressCacheEntity
import com.movieapp.data.model.WatchProgress
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout

/**
 * Watch Progress Repository
 * Manages video playback progress with local-first architecture and remote sync
 * 
 * Features:
 * - Auto-save playback progress
 * - Resume playback from last position
 * - Continue watching list
 * - Completion tracking
 * - Background sync with Supabase
 * - Offline support
 */
class WatchProgressRepository {
    
    private val progressDao: WatchProgressCacheDao by lazy {
        DatabaseProvider.getDatabase().watchProgressCacheDao()
    }
    
    private val supabaseClient by lazy {
        SupabaseClientProvider.getInstance()
    }
    
    companion object {
        private const val TAG = "WatchProgressRepository"
        private const val TABLE_NAME = "user_watch_progress"
        private const val TIMEOUT_MS = 10_000L
        private const val AUTO_SAVE_INTERVAL_MS = 10_000L // Save every 10 seconds
    }
    
    /**
     * Get watch progress for a movie
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return Watch progress entity or null
     */
    suspend fun getProgress(userId: String, movieId: Int): WatchProgressCacheEntity? {
        return try {
            progressDao.getProgress(userId, movieId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get progress", e)
            null
        }
    }
    
    /**
     * Get watch progress as Flow
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return Flow of watch progress
     */
    fun getProgressFlow(userId: String, movieId: Int): Flow<WatchProgressCacheEntity?> {
        return progressDao.getProgressFlow(userId, movieId)
    }
    
    /**
     * Get all progress for user
     * 
     * @param userId User ID
     * @return List of all progress entries
     */
    suspend fun getAllProgress(userId: String): List<WatchProgressCacheEntity> {
        return try {
            progressDao.getAllProgress(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all progress", e)
            emptyList()
        }
    }
    
    /**
     * Get in-progress movies (continue watching)
     * Returns movies that are partially watched (>5%, <95%)
     * 
     * @param userId User ID
     * @return List of in-progress entries
     */
    suspend fun getInProgressMovies(userId: String): List<WatchProgressCacheEntity> {
        return try {
            progressDao.getInProgressMovies(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get in-progress movies", e)
            emptyList()
        }
    }
    
    /**
     * Get in-progress movies as Flow
     * 
     * @param userId User ID
     * @return Flow of in-progress entries
     */
    fun getInProgressMoviesFlow(userId: String): Flow<List<WatchProgressCacheEntity>> {
        return progressDao.getInProgressMoviesFlow(userId)
    }
    
    /**
     * Get completed movies
     * 
     * @param userId User ID
     * @return List of completed entries
     */
    suspend fun getCompletedMovies(userId: String): List<WatchProgressCacheEntity> {
        return try {
            progressDao.getCompletedMovies(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get completed movies", e)
            emptyList()
        }
    }
    
    /**
     * Get recently watched movies
     * 
     * @param userId User ID
     * @param daysBack Number of days to look back (default 30)
     * @return List of recent entries
     */
    suspend fun getRecentlyWatched(userId: String, daysBack: Int = 30): List<WatchProgressCacheEntity> {
        return try {
            val since = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
            progressDao.getRecentlyWatched(userId, since)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recently watched", e)
            emptyList()
        }
    }
    
    /**
     * Save watch progress
     * Saves locally first (optimistic), then syncs to remote
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @param currentPositionMs Current playback position
     * @param durationMs Total duration
     */
    suspend fun saveProgress(
        userId: String,
        movieId: Int,
        currentPositionMs: Long,
        durationMs: Long
    ) {
        try {
            // Get existing progress or create new
            val existing = progressDao.getProgress(userId, movieId)
            
            val updatedProgress = if (existing != null) {
                existing.updateProgress(currentPositionMs, durationMs)
            } else {
                WatchProgressCacheEntity.createOffline(userId, movieId, durationMs)
                    .updateProgress(currentPositionMs, durationMs)
            }
            
            // Save locally first (optimistic UI)
            progressDao.insertProgress(updatedProgress)
            Log.d(TAG, "Saved progress locally: movie=$movieId, position=$currentPositionMs")
            
            // Sync to remote in background
            syncProgressToRemote(updatedProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save progress", e)
            throw e
        }
    }
    
    /**
     * Update progress
     * Convenience method that wraps saveProgress
     * 
     * @param progress Watch progress entity
     */
    suspend fun updateProgress(progress: WatchProgressCacheEntity) {
        try {
            progressDao.updateProgress(progress)
            Log.d(TAG, "Updated progress: movie=${progress.movieId}")
            
            // Sync to remote
            syncProgressToRemote(progress)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update progress", e)
            throw e
        }
    }
    
    /**
     * Mark movie as completed
     * 
     * @param userId User ID
     * @param movieId Movie ID
     */
    suspend fun markCompleted(userId: String, movieId: Int) {
        try {
            val existing = progressDao.getProgress(userId, movieId)
            
            if (existing != null) {
                val completed = existing.markCompleted()
                progressDao.updateProgress(completed)
                Log.d(TAG, "Marked as completed: movie=$movieId")
                
                // Sync to remote
                syncProgressToRemote(completed)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark as completed", e)
            throw e
        }
    }
    
    /**
     * Delete progress
     * 
     * @param userId User ID
     * @param movieId Movie ID
     */
    suspend fun deleteProgress(userId: String, movieId: Int) {
        try {
            // Delete locally
            progressDao.deleteProgress(userId, movieId)
            Log.d(TAG, "Deleted progress locally: movie=$movieId")
            
            // Delete from remote
            deleteFromRemote(userId, movieId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete progress", e)
            throw e
        }
    }
    
    /**
     * Clear all progress for user
     * 
     * @param userId User ID
     */
    suspend fun clearAllProgress(userId: String) {
        try {
            // Delete locally
            progressDao.deleteAllProgress(userId)
            Log.d(TAG, "Cleared all progress locally")
            
            // Delete from remote
            try {
                withTimeout(TIMEOUT_MS) {
                    supabaseClient.from(TABLE_NAME)
                        .delete {
                            filter {
                                eq("userId", userId)
                            }
                        }
                }
                Log.d(TAG, "Cleared all progress from remote")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear remote progress", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all progress", e)
            throw e
        }
    }
    
    /**
     * Check if progress exists
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return True if progress exists
     */
    suspend fun progressExists(userId: String, movieId: Int): Boolean {
        return try {
            progressDao.progressExists(userId, movieId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if progress exists", e)
            false
        }
    }
    
    /**
     * Get progress count
     * 
     * @param userId User ID
     * @return Total number of progress entries
     */
    suspend fun getProgressCount(userId: String): Int {
        return try {
            progressDao.getProgressCount(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get progress count", e)
            0
        }
    }
    
    /**
     * Get in-progress count
     * 
     * @param userId User ID
     * @return Number of movies in progress
     */
    suspend fun getInProgressCount(userId: String): Int {
        return try {
            progressDao.getInProgressCount(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get in-progress count", e)
            0
        }
    }
    
    /**
     * Get in-progress count as Flow
     * 
     * @param userId User ID
     * @return Flow of in-progress count
     */
    fun getInProgressCountFlow(userId: String): Flow<Int> {
        return progressDao.getInProgressCountFlow(userId)
    }
    
    /**
     * Sync progress from remote
     * Fetches user's progress from Supabase and merges with local
     * 
     * @param userId User ID
     */
    suspend fun syncFromRemote(userId: String) {
        try {
            Log.d(TAG, "Syncing progress from remote for user: $userId")
            
            // Fetch from Supabase
            val remoteEntries = withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("userId", userId)
                        }
                    }
                    .decodeList<WatchProgressCacheEntity>()
            }
            
            Log.d(TAG, "Fetched ${remoteEntries.size} entries from remote")
            
            // Get local entries
            val localEntries = progressDao.getAllProgress(userId)
            val localMap = localEntries.associateBy { it.movieId }
            
            // Merge: keep most recent for each movie
            val toUpdate = mutableListOf<WatchProgressCacheEntity>()
            for (remote in remoteEntries) {
                val local = localMap[remote.movieId]
                
                if (local == null) {
                    // Only on remote, add to local
                    toUpdate.add(remote.markAsSynced())
                } else if (remote.lastUpdatedAt > local.lastUpdatedAt) {
                    // Remote is newer, update local
                    toUpdate.add(remote.markAsSynced())
                }
            }
            
            if (toUpdate.isNotEmpty()) {
                progressDao.insertProgressList(toUpdate)
                Log.d(TAG, "Updated ${toUpdate.size} entries from remote")
            }
            
            Log.d(TAG, "Progress sync complete")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync progress from remote", e)
        }
    }
    
    /**
     * Sync pending progress to remote
     * Uploads all entries marked as needing sync
     * 
     * @param userId User ID
     */
    suspend fun syncPendingToRemote(userId: String) {
        try {
            val pendingEntries = progressDao.getEntriesNeedingSync(userId)
            
            if (pendingEntries.isEmpty()) {
                Log.d(TAG, "No pending progress to sync")
                return
            }
            
            Log.d(TAG, "Syncing ${pendingEntries.size} pending progress entries to remote")
            
            for (entry in pendingEntries) {
                syncProgressToRemote(entry)
            }
            
            Log.d(TAG, "Synced all pending progress entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync pending progress", e)
        }
    }
    
    /**
     * Sync progress to remote
     * Uploads single entry to Supabase
     * 
     * @param progress Progress entry to sync
     */
    private suspend fun syncProgressToRemote(progress: WatchProgressCacheEntity) {
        try {
            Log.d(TAG, "Syncing progress to remote: movie=${progress.movieId}")
            
            withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .upsert(progress)
            }
            
            // Mark as synced
            progressDao.markAsSynced(progress.userId, progress.movieId)
            Log.d(TAG, "Successfully synced progress to remote: movie=${progress.movieId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync progress to remote (local changes preserved)", e)
            // Don't throw - local changes are preserved
        }
    }
    
    /**
     * Delete from remote
     * Removes entry from Supabase
     * 
     * @param userId User ID
     * @param movieId Movie ID
     */
    private suspend fun deleteFromRemote(userId: String, movieId: Int) {
        try {
            Log.d(TAG, "Deleting progress from remote: movie=$movieId")
            
            withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .delete {
                        filter {
                            eq("userId", userId)
                            eq("movieId", movieId)
                        }
                    }
            }
            
            Log.d(TAG, "Successfully deleted progress from remote: movie=$movieId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete from remote", e)
            // Don't throw - local deletion already happened
        }
    }
    
    /**
     * Full sync (bidirectional)
     * Syncs pending local changes to remote, then fetches remote changes
     * 
     * @param userId User ID
     */
    suspend fun fullSync(userId: String) {
        try {
            Log.d(TAG, "Starting full sync for user: $userId")
            
            // First, push local changes to remote
            syncPendingToRemote(userId)
            
            // Then, pull remote changes
            syncFromRemote(userId)
            
            Log.d(TAG, "Full sync complete")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform full sync", e)
        }
    }
}
