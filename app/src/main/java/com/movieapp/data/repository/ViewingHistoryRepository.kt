package com.movieapp.data.repository

import android.util.Log
import com.movieapp.data.local.DatabaseProvider
import com.movieapp.data.local.ViewingHistoryCacheDao
import com.movieapp.data.local.ViewingHistoryCacheEntity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout

/**
 * Viewing History Repository
 * Manages user viewing history with local-first architecture and remote sync
 * 
 * Features:
 * - Track viewing sessions
 * - Viewing analytics (most watched, recent, etc.)
 * - Time-based history queries
 * - Continue watching support
 * - Background sync with Supabase
 * - Offline support
 */
class ViewingHistoryRepository {
    
    private val historyDao: ViewingHistoryCacheDao by lazy {
        DatabaseProvider.getDatabase().viewingHistoryCacheDao()
    }
    
    private val supabaseClient by lazy {
        SupabaseClientProvider.getInstance()
    }
    
    companion object {
        private const val TAG = "ViewingHistoryRepo"
        private const val TABLE_NAME = "user_viewing_history"
        private const val TIMEOUT_MS = 10_000L
    }
    
    /**
     * Add viewing history entry
     * Records a viewing session
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @param viewDurationMs Duration of viewing session
     * @param completedView Whether viewing was completed
     */
    suspend fun addHistoryEntry(
        userId: String,
        movieId: Int,
        viewDurationMs: Long = 0,
        completedView: Boolean = false
    ) {
        try {
            val entry = ViewingHistoryCacheEntity.create(
                userId = userId,
                movieId = movieId,
                viewDurationMs = viewDurationMs,
                completedView = completedView
            )
            
            // Save locally first
            val entryId = historyDao.insertHistoryEntry(entry)
            Log.d(TAG, "Added history entry: movie=$movieId, id=$entryId")
            
            // Sync to remote
            syncEntryToRemote(entry.copy(id = entryId))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add history entry", e)
            throw e
        }
    }
    
    /**
     * Get all viewing history for user
     * 
     * @param userId User ID
     * @return List of history entries
     */
    suspend fun getHistory(userId: String): List<ViewingHistoryCacheEntity> {
        return try {
            historyDao.getHistory(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get history", e)
            emptyList()
        }
    }
    
    /**
     * Get viewing history as Flow
     * 
     * @param userId User ID
     * @return Flow of history entries
     */
    fun getHistoryFlow(userId: String): Flow<List<ViewingHistoryCacheEntity>> {
        return historyDao.getHistoryFlow(userId)
    }
    
    /**
     * Get viewing history for specific movie
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return List of history entries for the movie
     */
    suspend fun getMovieHistory(userId: String, movieId: Int): List<ViewingHistoryCacheEntity> {
        return try {
            historyDao.getMovieHistory(userId, movieId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get movie history", e)
            emptyList()
        }
    }
    
    /**
     * Get recent viewing history
     * 
     * @param userId User ID
     * @param limit Maximum number of entries (default 20)
     * @return List of recent history entries
     */
    suspend fun getRecentHistory(userId: String, limit: Int = 20): List<ViewingHistoryCacheEntity> {
        return try {
            historyDao.getRecentHistory(userId, limit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent history", e)
            emptyList()
        }
    }
    
    /**
     * Get recent history as Flow
     * 
     * @param userId User ID
     * @param limit Maximum number of entries
     * @return Flow of recent history entries
     */
    fun getRecentHistoryFlow(userId: String, limit: Int = 20): Flow<List<ViewingHistoryCacheEntity>> {
        return historyDao.getRecentHistoryFlow(userId, limit)
    }
    
    /**
     * Get viewing history within time range
     * 
     * @param userId User ID
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of history entries in range
     */
    suspend fun getHistoryInRange(
        userId: String,
        startTime: Long,
        endTime: Long
    ): List<ViewingHistoryCacheEntity> {
        return try {
            historyDao.getHistoryInRange(userId, startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get history in range", e)
            emptyList()
        }
    }
    
    /**
     * Get history for last N days
     * 
     * @param userId User ID
     * @param days Number of days to look back
     * @return List of history entries
     */
    suspend fun getHistoryForLastDays(userId: String, days: Int): List<ViewingHistoryCacheEntity> {
        val startTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val endTime = System.currentTimeMillis()
        return getHistoryInRange(userId, startTime, endTime)
    }
    
    /**
     * Get completed views only
     * 
     * @param userId User ID
     * @return List of completed viewing entries
     */
    suspend fun getCompletedViews(userId: String): List<ViewingHistoryCacheEntity> {
        return try {
            historyDao.getCompletedViews(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get completed views", e)
            emptyList()
        }
    }
    
    /**
     * Get unique movies from history
     * Returns deduplicated list of movie IDs
     * 
     * @param userId User ID
     * @return List of unique movie IDs
     */
    suspend fun getUniqueMovieIds(userId: String): List<Int> {
        return try {
            historyDao.getUniqueMovieIds(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unique movie IDs", e)
            emptyList()
        }
    }
    
    /**
     * Get last viewed date for movie
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return Timestamp of last view or null
     */
    suspend fun getLastViewedDate(userId: String, movieId: Int): Long? {
        return try {
            historyDao.getLastViewedDate(userId, movieId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last viewed date", e)
            null
        }
    }
    
    /**
     * Check if movie was recently watched
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @param withinHours Number of hours to check (default 24)
     * @return True if watched within specified hours
     */
    suspend fun wasRecentlyWatched(
        userId: String,
        movieId: Int,
        withinHours: Int = 24
    ): Boolean {
        val lastViewed = getLastViewedDate(userId, movieId) ?: return false
        val threshold = System.currentTimeMillis() - (withinHours * 60 * 60 * 1000L)
        return lastViewed >= threshold
    }
    
    /**
     * Delete history entry
     * 
     * @param entryId Entry ID to delete
     */
    suspend fun deleteHistoryEntry(entryId: Long) {
        try {
            historyDao.deleteHistoryEntry(entryId)
            Log.d(TAG, "Deleted history entry: $entryId")
            
            // Note: Remote deletion by ID not implemented as remote table might use different ID
            // Full sync will reconcile differences
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete history entry", e)
            throw e
        }
    }
    
    /**
     * Delete all history for specific movie
     * 
     * @param userId User ID
     * @param movieId Movie ID
     */
    suspend fun deleteMovieHistory(userId: String, movieId: Int) {
        try {
            historyDao.deleteMovieHistory(userId, movieId)
            Log.d(TAG, "Deleted history for movie: $movieId")
            
            // Delete from remote
            deleteMovieHistoryFromRemote(userId, movieId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete movie history", e)
            throw e
        }
    }
    
    /**
     * Delete all history for user
     * 
     * @param userId User ID
     */
    suspend fun deleteAllHistory(userId: String) {
        try {
            historyDao.deleteAllHistory(userId)
            Log.d(TAG, "Deleted all history for user")
            
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
                Log.d(TAG, "Deleted all history from remote")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete from remote", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all history", e)
            throw e
        }
    }
    
    /**
     * Delete old history entries
     * Removes entries older than specified days
     * 
     * @param userId User ID
     * @param olderThanDays Delete entries older than this many days
     */
    suspend fun deleteOldHistory(userId: String, olderThanDays: Int) {
        try {
            val threshold = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            historyDao.deleteOldHistory(userId, threshold)
            Log.d(TAG, "Deleted history older than $olderThanDays days")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete old history", e)
            throw e
        }
    }
    
    /**
     * Get history count
     * 
     * @param userId User ID
     * @return Total number of history entries
     */
    suspend fun getHistoryCount(userId: String): Int {
        return try {
            historyDao.getHistoryCount(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get history count", e)
            0
        }
    }
    
    /**
     * Get history count as Flow
     * 
     * @param userId User ID
     * @return Flow of history count
     */
    fun getHistoryCountFlow(userId: String): Flow<Int> {
        return historyDao.getHistoryCountFlow(userId)
    }
    
    /**
     * Get unique movies count
     * 
     * @param userId User ID
     * @return Number of unique movies watched
     */
    suspend fun getUniqueMoviesCount(userId: String): Int {
        return try {
            historyDao.getUniqueMoviesCount(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unique movies count", e)
            0
        }
    }
    
    /**
     * Sync history from remote
     * Fetches user's history from Supabase and adds to local
     * 
     * @param userId User ID
     */
    suspend fun syncFromRemote(userId: String) {
        try {
            Log.d(TAG, "Syncing history from remote for user: $userId")
            
            // Fetch from Supabase
            val remoteEntries = withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("userId", userId)
                        }
                    }
                    .decodeList<ViewingHistoryCacheEntity>()
            }
            
            Log.d(TAG, "Fetched ${remoteEntries.size} entries from remote")
            
            if (remoteEntries.isNotEmpty()) {
                // Add remote entries to local (they have their own IDs)
                historyDao.insertHistoryEntries(remoteEntries.map { it.markAsSynced() })
                Log.d(TAG, "Added ${remoteEntries.size} entries from remote")
            }
            
            Log.d(TAG, "History sync complete")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync history from remote", e)
        }
    }
    
    /**
     * Sync pending history to remote
     * Uploads all entries marked as needing sync
     * 
     * @param userId User ID
     */
    suspend fun syncPendingToRemote(userId: String) {
        try {
            val pendingEntries = historyDao.getEntriesNeedingSync(userId)
            
            if (pendingEntries.isEmpty()) {
                Log.d(TAG, "No pending history to sync")
                return
            }
            
            Log.d(TAG, "Syncing ${pendingEntries.size} pending history entries to remote")
            
            for (entry in pendingEntries) {
                syncEntryToRemote(entry)
            }
            
            Log.d(TAG, "Synced all pending history entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync pending history", e)
        }
    }
    
    /**
     * Sync entry to remote
     * Uploads single entry to Supabase
     * 
     * @param entry History entry to sync
     */
    private suspend fun syncEntryToRemote(entry: ViewingHistoryCacheEntity) {
        try {
            Log.d(TAG, "Syncing entry to remote: id=${entry.id}, movie=${entry.movieId}")
            
            withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .insert(entry)
            }
            
            // Mark as synced
            historyDao.markAsSynced(entry.id)
            Log.d(TAG, "Successfully synced entry to remote: id=${entry.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync entry to remote (local changes preserved)", e)
            // Don't throw - local changes are preserved
        }
    }
    
    /**
     * Delete movie history from remote
     * 
     * @param userId User ID
     * @param movieId Movie ID
     */
    private suspend fun deleteMovieHistoryFromRemote(userId: String, movieId: Int) {
        try {
            Log.d(TAG, "Deleting movie history from remote: movie=$movieId")
            
            withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .delete {
                        filter {
                            eq("userId", userId)
                            eq("movieId", movieId)
                        }
                    }
            }
            
            Log.d(TAG, "Successfully deleted movie history from remote: movie=$movieId")
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
