package com.movieapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Viewing History Cache operations
 * Provides methods to access and modify viewing history data
 */
@Dao
interface ViewingHistoryCacheDao {
    
    /**
     * Insert viewing history entry
     * 
     * @param entry History entry to insert
     * @return ID of inserted entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: ViewingHistoryCacheEntity): Long
    
    /**
     * Insert multiple history entries
     * 
     * @param entries List of entries to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntries(entries: List<ViewingHistoryCacheEntity>)
    
    /**
     * Get all viewing history for a user
     * 
     * @param userId User ID
     * @return List of history entries ordered by viewed date
     */
    @Query("SELECT * FROM viewing_history_cache WHERE userId = :userId ORDER BY viewedAt DESC")
    suspend fun getHistory(userId: String): List<ViewingHistoryCacheEntity>
    
    /**
     * Get viewing history as Flow
     * 
     * @param userId User ID
     * @return Flow of history entries
     */
    @Query("SELECT * FROM viewing_history_cache WHERE userId = :userId ORDER BY viewedAt DESC")
    fun getHistoryFlow(userId: String): Flow<List<ViewingHistoryCacheEntity>>
    
    /**
     * Get viewing history for a specific movie
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return List of history entries for the movie
     */
    @Query("SELECT * FROM viewing_history_cache WHERE userId = :userId AND movieId = :movieId ORDER BY viewedAt DESC")
    suspend fun getMovieHistory(userId: String, movieId: Int): List<ViewingHistoryCacheEntity>
    
    /**
     * Get recent viewing history (last N entries)
     * 
     * @param userId User ID
     * @param limit Maximum number of entries
     * @return List of recent history entries
     */
    @Query("SELECT * FROM viewing_history_cache WHERE userId = :userId ORDER BY viewedAt DESC LIMIT :limit")
    suspend fun getRecentHistory(userId: String, limit: Int = 20): List<ViewingHistoryCacheEntity>
    
    /**
     * Get recent history as Flow
     * 
     * @param userId User ID
     * @param limit Maximum number of entries
     * @return Flow of recent history entries
     */
    @Query("SELECT * FROM viewing_history_cache WHERE userId = :userId ORDER BY viewedAt DESC LIMIT :limit")
    fun getRecentHistoryFlow(userId: String, limit: Int = 20): Flow<List<ViewingHistoryCacheEntity>>
    
    /**
     * Get viewing history within time range
     * 
     * @param userId User ID
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of history entries in range
     */
    @Query("SELECT * FROM viewing_history_cache WHERE userId = :userId AND viewedAt >= :startTime AND viewedAt <= :endTime ORDER BY viewedAt DESC")
    suspend fun getHistoryInRange(userId: String, startTime: Long, endTime: Long): List<ViewingHistoryCacheEntity>
    
    /**
     * Get completed views only
     * 
     * @param userId User ID
     * @return List of completed viewing entries
     */
    @Query("SELECT * FROM viewing_history_cache WHERE userId = :userId AND completedView = 1 ORDER BY viewedAt DESC")
    suspend fun getCompletedViews(userId: String): List<ViewingHistoryCacheEntity>
    
    /**
     * Get unique movies from history (deduplicated by movieId)
     * 
     * @param userId User ID
     * @return List of unique movie IDs
     */
    @Query("SELECT DISTINCT movieId FROM viewing_history_cache WHERE userId = :userId ORDER BY viewedAt DESC")
    suspend fun getUniqueMovieIds(userId: String): List<Int>
    
    /**
     * Get last viewed date for a movie
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return Timestamp of last view or null
     */
    @Query("SELECT MAX(viewedAt) FROM viewing_history_cache WHERE userId = :userId AND movieId = :movieId")
    suspend fun getLastViewedDate(userId: String, movieId: Int): Long?
    
    /**
     * Update history entry
     * 
     * @param entry Updated entry
     */
    @Update
    suspend fun updateHistoryEntry(entry: ViewingHistoryCacheEntity)
    
    /**
     * Delete history entry
     * 
     * @param id Entry ID to delete
     */
    @Query("DELETE FROM viewing_history_cache WHERE id = :id")
    suspend fun deleteHistoryEntry(id: Long)
    
    /**
     * Delete all history for a specific movie
     * 
     * @param userId User ID
     * @param movieId Movie ID
     */
    @Query("DELETE FROM viewing_history_cache WHERE userId = :userId AND movieId = :movieId")
    suspend fun deleteMovieHistory(userId: String, movieId: Int)
    
    /**
     * Delete all history for a user
     * 
     * @param userId User ID
     */
    @Query("DELETE FROM viewing_history_cache WHERE userId = :userId")
    suspend fun deleteAllHistory(userId: String)
    
    /**
     * Delete all history entries
     */
    @Query("DELETE FROM viewing_history_cache")
    suspend fun deleteAllHistoryEntries()
    
    /**
     * Delete old history entries (older than specified timestamp)
     * 
     * @param userId User ID
     * @param olderThan Timestamp threshold
     */
    @Query("DELETE FROM viewing_history_cache WHERE userId = :userId AND viewedAt < :olderThan")
    suspend fun deleteOldHistory(userId: String, olderThan: Long)
    
    /**
     * Get entries that need syncing
     * 
     * @param userId User ID
     * @return List of entries needing sync
     */
    @Query("SELECT * FROM viewing_history_cache WHERE userId = :userId AND needsSync = 1")
    suspend fun getEntriesNeedingSync(userId: String): List<ViewingHistoryCacheEntity>
    
    /**
     * Mark as synced
     * 
     * @param id Entry ID
     * @param syncedAt Sync timestamp
     */
    @Query("UPDATE viewing_history_cache SET syncedAt = :syncedAt, needsSync = 0 WHERE id = :id")
    suspend fun markAsSynced(id: Long, syncedAt: Long = System.currentTimeMillis())
    
    /**
     * Get history count
     * 
     * @param userId User ID
     * @return Number of history entries
     */
    @Query("SELECT COUNT(*) FROM viewing_history_cache WHERE userId = :userId")
    suspend fun getHistoryCount(userId: String): Int
    
    /**
     * Get history count as Flow
     * 
     * @param userId User ID
     * @return Flow of history count
     */
    @Query("SELECT COUNT(*) FROM viewing_history_cache WHERE userId = :userId")
    fun getHistoryCountFlow(userId: String): Flow<Int>
    
    /**
     * Get unique movies count
     * 
     * @param userId User ID
     * @return Number of unique movies watched
     */
    @Query("SELECT COUNT(DISTINCT movieId) FROM viewing_history_cache WHERE userId = :userId")
    suspend fun getUniqueMoviesCount(userId: String): Int
}
