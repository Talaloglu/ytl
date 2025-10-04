package com.movieapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Watchlist Cache operations
 * Provides methods to access and modify watchlist data
 */
@Dao
interface WatchlistCacheDao {
    
    /**
     * Insert or replace a watchlist entry
     * 
     * @param entry Watchlist entry to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistEntry(entry: WatchlistCacheEntity)
    
    /**
     * Insert multiple watchlist entries
     * 
     * @param entries List of entries to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistEntries(entries: List<WatchlistCacheEntity>)
    
    /**
     * Get all watchlist entries for a user
     * 
     * @param userId User ID
     * @return List of watchlist entries
     */
    @Query("SELECT * FROM watchlist_cache WHERE userId = :userId ORDER BY addedAt DESC")
    suspend fun getWatchlist(userId: String): List<WatchlistCacheEntity>
    
    /**
     * Get watchlist as Flow for reactive updates
     * 
     * @param userId User ID
     * @return Flow of watchlist entries
     */
    @Query("SELECT * FROM watchlist_cache WHERE userId = :userId ORDER BY addedAt DESC")
    fun getWatchlistFlow(userId: String): Flow<List<WatchlistCacheEntity>>
    
    /**
     * Get watchlist movie IDs for a user
     * 
     * @param userId User ID
     * @return List of movie IDs in watchlist
     */
    @Query("SELECT movieId FROM watchlist_cache WHERE userId = :userId ORDER BY addedAt DESC")
    suspend fun getWatchlistMovieIds(userId: String): List<Int>
    
    /**
     * Check if movie is in watchlist
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return True if movie is in watchlist
     */
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_cache WHERE userId = :userId AND movieId = :movieId)")
    suspend fun isInWatchlist(userId: String, movieId: Int): Boolean
    
    /**
     * Check if movie is in watchlist (Flow)
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return Flow of boolean
     */
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_cache WHERE userId = :userId AND movieId = :movieId)")
    fun isInWatchlistFlow(userId: String, movieId: Int): Flow<Boolean>
    
    /**
     * Delete watchlist entry
     * 
     * @param userId User ID
     * @param movieId Movie ID
     */
    @Query("DELETE FROM watchlist_cache WHERE userId = :userId AND movieId = :movieId")
    suspend fun deleteWatchlistEntry(userId: String, movieId: Int)
    
    /**
     * Delete all watchlist entries for a user
     * 
     * @param userId User ID
     */
    @Query("DELETE FROM watchlist_cache WHERE userId = :userId")
    suspend fun deleteUserWatchlist(userId: String)
    
    /**
     * Delete all watchlist entries
     */
    @Query("DELETE FROM watchlist_cache")
    suspend fun deleteAllWatchlist()
    
    /**
     * Get entries that need syncing
     * 
     * @param userId User ID
     * @return List of entries needing sync
     */
    @Query("SELECT * FROM watchlist_cache WHERE userId = :userId AND needsSync = 1")
    suspend fun getEntriesNeedingSync(userId: String): List<WatchlistCacheEntity>
    
    /**
     * Mark entry as synced
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @param syncedAt Sync timestamp
     */
    @Query("UPDATE watchlist_cache SET syncedAt = :syncedAt, needsSync = 0 WHERE userId = :userId AND movieId = :movieId")
    suspend fun markAsSynced(userId: String, movieId: Int, syncedAt: Long = System.currentTimeMillis())
    
    /**
     * Get watchlist count
     * 
     * @param userId User ID
     * @return Number of movies in watchlist
     */
    @Query("SELECT COUNT(*) FROM watchlist_cache WHERE userId = :userId")
    suspend fun getWatchlistCount(userId: String): Int
    
    /**
     * Get watchlist count as Flow
     * 
     * @param userId User ID
     * @return Flow of watchlist count
     */
    @Query("SELECT COUNT(*) FROM watchlist_cache WHERE userId = :userId")
    fun getWatchlistCountFlow(userId: String): Flow<Int>
}
