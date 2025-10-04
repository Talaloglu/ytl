
package com.movieapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database for the Movie App
 * Provides offline caching of movie data and user-related data
 * 
 * Version History:
 * - v1: Initial version with CachedMovieEntity
 * - v2: Added user profile, watchlist, watch progress, and viewing history
 * 
 * Note: Using version 2 instead of 4 to avoid auto-migration complexity
 * For existing v1 databases, will use destructive migration (acceptable for development)
 */
@Database(
    entities = [
        CachedMovieEntity::class,
        UserProfileEntity::class,
        WatchlistCacheEntity::class,
        WatchProgressCacheEntity::class,
        ViewingHistoryCacheEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * DAO for cached movie operations
     */
    abstract fun cachedMovieDao(): CachedMovieDao
    
    /**
     * DAO for user profile operations
     */
    abstract fun userProfileDao(): UserProfileDao
    
    /**
     * DAO for watchlist operations
     */
    abstract fun watchlistCacheDao(): WatchlistCacheDao
    
    /**
     * DAO for watch progress operations
     */
    abstract fun watchProgressCacheDao(): WatchProgressCacheDao
    
    /**
     * DAO for viewing history operations
     */
    abstract fun viewingHistoryCacheDao(): ViewingHistoryCacheDao
}
