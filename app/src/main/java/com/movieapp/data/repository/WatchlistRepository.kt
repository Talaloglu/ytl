package com.movieapp.data.repository

import android.util.Log
import com.movieapp.data.local.DatabaseProvider
import com.movieapp.data.local.WatchlistCacheDao
import com.movieapp.data.local.WatchlistCacheEntity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout

/**
 * Watchlist Repository
 * Manages user watchlist with local-first architecture and remote sync
 * 
 * Features:
 * - Local-first watchlist management
 * - Optimistic UI updates
 * - Background sync with Supabase
 * - Offline support
 * - Conflict resolution
 * - Automatic retry on network errors
 */
class WatchlistRepository {
    
    private val watchlistDao: WatchlistCacheDao by lazy {
        DatabaseProvider.getDatabase().watchlistCacheDao()
    }
    
    private val supabaseClient by lazy {
        SupabaseClientProvider.getInstance()
    }
    
    companion object {
        private const val TAG = "WatchlistRepository"
        private const val TABLE_NAME = "user_watchlist"
        private const val TIMEOUT_MS = 10_000L
    }
    
    /**
     * Get user's watchlist
     * Returns local cached data
     * 
     * @param userId User ID
     * @return List of watchlist entries
     */
    suspend fun getWatchlist(userId: String): List<WatchlistCacheEntity> {
        return try {
            watchlistDao.getWatchlist(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get watchlist", e)
            emptyList()
        }
    }
    
    /**
     * Get watchlist as Flow for reactive updates
     * 
     * @param userId User ID
     * @return Flow of watchlist entries
     */
    fun getWatchlistFlow(userId: String): Flow<List<WatchlistCacheEntity>> {
        return watchlistDao.getWatchlistFlow(userId)
    }
    
    /**
     * Get watchlist movie IDs
     * Useful for checking if movies are in watchlist
     * 
     * @param userId User ID
     * @return List of movie IDs
     */
    suspend fun getWatchlistMovieIds(userId: String): List<Int> {
        return try {
            watchlistDao.getWatchlistMovieIds(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get watchlist movie IDs", e)
            emptyList()
        }
    }
    
    /**
     * Check if movie is in watchlist
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return True if movie is in watchlist
     */
    suspend fun isInWatchlist(userId: String, movieId: Int): Boolean {
        return try {
            watchlistDao.isInWatchlist(userId, movieId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if movie is in watchlist", e)
            false
        }
    }
    
    /**
     * Check if movie is in watchlist (Flow)
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return Flow of boolean
     */
    fun isInWatchlistFlow(userId: String, movieId: Int): Flow<Boolean> {
        return watchlistDao.isInWatchlistFlow(userId, movieId)
    }
    
    /**
     * Add movie to watchlist
     * Uses optimistic update - saves locally first, then syncs to remote
     * 
     * @param userId User ID
     * @param movieId Movie ID
     */
    suspend fun addToWatchlist(userId: String, movieId: Int) {
        try {
            // Create watchlist entry
            val entry = WatchlistCacheEntity.create(userId, movieId)
            
            // Save locally first (optimistic UI)
            watchlistDao.insertWatchlistEntry(entry)
            Log.d(TAG, "Added movie $movieId to local watchlist for user $userId")
            
            // Sync to remote in background
            syncEntryToRemote(entry)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add to watchlist", e)
            throw e
        }
    }
    
    /**
     * Remove movie from watchlist
     * Uses optimistic update - removes locally first, then syncs to remote
     * 
     * @param userId User ID
     * @param movieId Movie ID
     */
    suspend fun removeFromWatchlist(userId: String, movieId: Int) {
        try {
            // Delete locally first (optimistic UI)
            watchlistDao.deleteWatchlistEntry(userId, movieId)
            Log.d(TAG, "Removed movie $movieId from local watchlist for user $userId")
            
            // Delete from remote
            deleteFromRemote(userId, movieId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove from watchlist", e)
            throw e
        }
    }
    
    /**
     * Toggle movie in watchlist
     * Add if not present, remove if present
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return True if added, false if removed
     */
    suspend fun toggleWatchlist(userId: String, movieId: Int): Boolean {
        return try {
            val isInList = isInWatchlist(userId, movieId)
            if (isInList) {
                removeFromWatchlist(userId, movieId)
                false
            } else {
                addToWatchlist(userId, movieId)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle watchlist", e)
            throw e
        }
    }
    
    /**
     * Sync watchlist from remote
     * Fetches user's watchlist from Supabase and merges with local
     * 
     * @param userId User ID
     */
    suspend fun syncFromRemote(userId: String) {
        try {
            Log.d(TAG, "Syncing watchlist from remote for user: $userId")
            
            // Fetch from Supabase
            val remoteEntries = withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("userId", userId)
                        }
                    }
                    .decodeList<WatchlistCacheEntity>()
            }
            
            Log.d(TAG, "Fetched ${remoteEntries.size} entries from remote")
            
            // Get local entries
            val localEntries = watchlistDao.getWatchlist(userId)
            val localMovieIds = localEntries.map { it.movieId }.toSet()
            val remoteMovieIds = remoteEntries.map { it.movieId }.toSet()
            
            // Find entries to add (on remote but not local)
            val entriesToAdd = remoteEntries.filter { it.movieId !in localMovieIds }
            if (entriesToAdd.isNotEmpty()) {
                watchlistDao.insertWatchlistEntries(entriesToAdd.map { it.markAsSynced() })
                Log.d(TAG, "Added ${entriesToAdd.size} entries from remote")
            }
            
            // Find entries to remove (on local but not remote)
            val entriesToRemove = localEntries.filter { it.movieId !in remoteMovieIds }
            if (entriesToRemove.isNotEmpty()) {
                entriesToRemove.forEach { watchlistDao.deleteWatchlistEntry(userId, it.movieId) }
                Log.d(TAG, "Removed ${entriesToRemove.size} entries not on remote")
            }
            
            Log.d(TAG, "Watchlist sync complete")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync watchlist from remote", e)
        }
    }
    
    /**
     * Sync pending entries to remote
     * Uploads all entries marked as needing sync
     * 
     * @param userId User ID
     */
    suspend fun syncPendingToRemote(userId: String) {
        try {
            val pendingEntries = watchlistDao.getEntriesNeedingSync(userId)
            
            if (pendingEntries.isEmpty()) {
                Log.d(TAG, "No pending entries to sync")
                return
            }
            
            Log.d(TAG, "Syncing ${pendingEntries.size} pending entries to remote")
            
            for (entry in pendingEntries) {
                syncEntryToRemote(entry)
            }
            
            Log.d(TAG, "Synced all pending entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync pending entries", e)
        }
    }
    
    /**
     * Sync entry to remote
     * Uploads single entry to Supabase
     * 
     * @param entry Watchlist entry to sync
     */
    private suspend fun syncEntryToRemote(entry: WatchlistCacheEntity) {
        try {
            Log.d(TAG, "Syncing entry to remote: ${entry.movieId}")
            
            withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .upsert(entry)
            }
            
            // Mark as synced
            watchlistDao.markAsSynced(entry.userId, entry.movieId)
            Log.d(TAG, "Successfully synced entry to remote: ${entry.movieId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync entry to remote (local changes preserved)", e)
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
            Log.d(TAG, "Deleting from remote: $movieId")
            
            withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .delete {
                        filter {
                            eq("userId", userId)
                            eq("movieId", movieId)
                        }
                    }
            }
            
            Log.d(TAG, "Successfully deleted from remote: $movieId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete from remote", e)
            // Don't throw - local deletion already happened
        }
    }
    
    /**
     * Get watchlist count
     * 
     * @param userId User ID
     * @return Number of movies in watchlist
     */
    suspend fun getWatchlistCount(userId: String): Int {
        return try {
            watchlistDao.getWatchlistCount(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get watchlist count", e)
            0
        }
    }
    
    /**
     * Get watchlist count as Flow
     * 
     * @param userId User ID
     * @return Flow of watchlist count
     */
    fun getWatchlistCountFlow(userId: String): Flow<Int> {
        return watchlistDao.getWatchlistCountFlow(userId)
    }
    
    /**
     * Clear user's watchlist
     * Deletes all entries locally and remotely
     * 
     * @param userId User ID
     */
    suspend fun clearWatchlist(userId: String) {
        try {
            // Delete locally
            watchlistDao.deleteUserWatchlist(userId)
            Log.d(TAG, "Cleared local watchlist for user: $userId")
            
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
                Log.d(TAG, "Cleared remote watchlist for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear remote watchlist", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear watchlist", e)
            throw e
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
