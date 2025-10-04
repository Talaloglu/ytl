package com.movieapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Watch Progress Cache operations
 * Provides methods to access and modify watch progress data
 */
@Dao
interface WatchProgressCacheDao {
    
    /**
     * Insert or replace watch progress
     * 
     * @param progress Watch progress to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: WatchProgressCacheEntity)
    
    /**
     * Insert multiple progress entries
     * 
     * @param progressList List of progress entries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<WatchProgressCacheEntity>)
    
    /**
     * Get watch progress for a specific movie
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return Watch progress or null
     */
    @Query("SELECT * FROM watch_progress_cache WHERE userId = :userId AND movieId = :movieId")
    suspend fun getProgress(userId: String, movieId: Int): WatchProgressCacheEntity?
    
    /**
     * Get watch progress as Flow
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return Flow of watch progress
     */
    @Query("SELECT * FROM watch_progress_cache WHERE userId = :userId AND movieId = :movieId")
    fun getProgressFlow(userId: String, movieId: Int): Flow<WatchProgressCacheEntity?>
    
    /**
     * Get all watch progress for a user
     * 
     * @param userId User ID
     * @return List of watch progress entries
     */
    @Query("SELECT * FROM watch_progress_cache WHERE userId = :userId ORDER BY lastUpdatedAt DESC")
    suspend fun getAllProgress(userId: String): List<WatchProgressCacheEntity>
    
    /**
     * Get all in-progress movies (not completed, > 5% watched)
     * 
     * @param userId User ID
     * @return List of in-progress entries
     */
    @Query("SELECT * FROM watch_progress_cache WHERE userId = :userId AND isCompleted = 0 AND watchPercentage >= 0.05 ORDER BY lastUpdatedAt DESC")
    suspend fun getInProgressMovies(userId: String): List<WatchProgressCacheEntity>
    
    /**
     * Get in-progress movies as Flow
     * 
     * @param userId User ID
     * @return Flow of in-progress entries
     */
    @Query("SELECT * FROM watch_progress_cache WHERE userId = :userId AND isCompleted = 0 AND watchPercentage >= 0.05 ORDER BY lastUpdatedAt DESC")
    fun getInProgressMoviesFlow(userId: String): Flow<List<WatchProgressCacheEntity>>
    
    /**
     * Get completed movies
     * 
     * @param userId User ID
     * @return List of completed entries
     */
    @Query("SELECT * FROM watch_progress_cache WHERE userId = :userId AND isCompleted = 1 ORDER BY lastUpdatedAt DESC")
    suspend fun getCompletedMovies(userId: String): List<WatchProgressCacheEntity>
    
    /**
     * Get recently watched movies (last 30 days)
     * 
     * @param userId User ID
     * @param since Timestamp to filter from
     * @return List of recent entries
     */
    @Query("SELECT * FROM watch_progress_cache WHERE userId = :userId AND lastUpdatedAt >= :since ORDER BY lastUpdatedAt DESC")
    suspend fun getRecentlyWatched(userId: String, since: Long): List<WatchProgressCacheEntity>
    
    /**
     * Update watch progress
     * 
     * @param progress Updated progress
     */
    @Update
    suspend fun updateProgress(progress: WatchProgressCacheEntity)
    
    /**
     * Delete watch progress
     * 
     * @param userId User ID
     * @param movieId Movie ID
     */
    @Query("DELETE FROM watch_progress_cache WHERE userId = :userId AND movieId = :movieId")
    suspend fun deleteProgress(userId: String, movieId: Int)
    
    /**
     * Delete all progress for a user
     * 
     * @param userId User ID
     */
    @Query("DELETE FROM watch_progress_cache WHERE userId = :userId")
    suspend fun deleteAllProgress(userId: String)
    
    /**
     * Delete all progress entries
     */
    @Query("DELETE FROM watch_progress_cache")
    suspend fun deleteAllProgressEntries()
    
    /**
     * Get entries that need syncing
     * 
     * @param userId User ID
     * @return List of entries needing sync
     */
    @Query("SELECT * FROM watch_progress_cache WHERE userId = :userId AND needsSync = 1")
    suspend fun getEntriesNeedingSync(userId: String): List<WatchProgressCacheEntity>
    
    /**
     * Mark as synced
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @param syncedAt Sync timestamp
     */
    @Query("UPDATE watch_progress_cache SET syncedAt = :syncedAt, needsSync = 0 WHERE userId = :userId AND movieId = :movieId")
    suspend fun markAsSynced(userId: String, movieId: Int, syncedAt: Long = System.currentTimeMillis())
    
    /**
     * Check if progress exists
     * 
     * @param userId User ID
     * @param movieId Movie ID
     * @return True if exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM watch_progress_cache WHERE userId = :userId AND movieId = :movieId)")
    suspend fun progressExists(userId: String, movieId: Int): Boolean
    
    /**
     * Get progress count
     * 
     * @param userId User ID
     * @return Number of progress entries
     */
    @Query("SELECT COUNT(*) FROM watch_progress_cache WHERE userId = :userId")
    suspend fun getProgressCount(userId: String): Int
    
    /**
     * Get in-progress count
     * 
     * @param userId User ID
     * @return Number of in-progress movies
     */
    @Query("SELECT COUNT(*) FROM watch_progress_cache WHERE userId = :userId AND isCompleted = 0 AND watchPercentage >= 0.05")
    suspend fun getInProgressCount(userId: String): Int
    
    /**
     * Get in-progress count as Flow
     * 
     * @param userId User ID
     * @return Flow of in-progress count
     */
    @Query("SELECT COUNT(*) FROM watch_progress_cache WHERE userId = :userId AND isCompleted = 0 AND watchPercentage >= 0.05")
    fun getInProgressCountFlow(userId: String): Flow<Int>
}
