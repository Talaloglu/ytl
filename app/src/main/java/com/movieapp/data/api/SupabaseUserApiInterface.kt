package com.movieapp.data.api

import com.movieapp.data.local.UserProfileEntity
import com.movieapp.data.local.WatchlistCacheEntity
import com.movieapp.data.local.WatchProgressCacheEntity
import com.movieapp.data.local.ViewingHistoryCacheEntity
import retrofit2.Response
import retrofit2.http.*

/**
 * Supabase User API Interface
 * Handles user-specific endpoints for profiles, watchlist, progress, and history
 * 
 * These endpoints interact with user-specific tables in Supabase:
 * - user_profile: User profiles and settings
 * - user_watchlist: User's movie watchlist
 * - user_watch_progress: Video playback progress
 * - user_viewing_history: Viewing history and analytics
 */
interface SupabaseUserApiInterface {
    
    // =================== USER PROFILE ENDPOINTS ===================
    
    /**
     * Get user profile by user ID
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter (eq.<userId>)
     * @param select Fields to select
     */
    @GET("user_profile")
    suspend fun getUserProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String, // Format: "eq.<userId>"
        @Query("select") select: String = "*"
    ): Response<List<UserProfileEntity>>
    
    /**
     * Create or update user profile
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param prefer Return preference (return=representation)
     * @param profile User profile data
     */
    @POST("user_profile")
    suspend fun upsertUserProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation,resolution=merge-duplicates",
        @Body profile: UserProfileEntity
    ): Response<List<UserProfileEntity>>
    
    /**
     * Update user profile
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter
     * @param profile Updated profile data
     */
    @PATCH("user_profile")
    suspend fun updateUserProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String, // Format: "eq.<userId>"
        @Body profile: Map<String, Any?>
    ): Response<List<UserProfileEntity>>
    
    /**
     * Delete user profile
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter
     */
    @DELETE("user_profile")
    suspend fun deleteUserProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String // Format: "eq.<userId>"
    ): Response<Void>
    
    // =================== WATCHLIST ENDPOINTS ===================
    
    /**
     * Get user's watchlist
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter
     * @param select Fields to select
     * @param order Order by field
     */
    @GET("user_watchlist")
    suspend fun getWatchlist(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "addedAt.desc"
    ): Response<List<WatchlistCacheEntity>>
    
    /**
     * Add movie to watchlist
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param prefer Return preference
     * @param watchlistItem Watchlist item to add
     */
    @POST("user_watchlist")
    suspend fun addToWatchlist(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation,resolution=merge-duplicates",
        @Body watchlistItem: WatchlistCacheEntity
    ): Response<List<WatchlistCacheEntity>>
    
    /**
     * Remove movie from watchlist
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter
     * @param movieId Movie ID filter
     */
    @DELETE("user_watchlist")
    suspend fun removeFromWatchlist(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String,
        @Query("movieId") movieId: String
    ): Response<Void>
    
    /**
     * Get watchlist with pagination
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param range Range for pagination
     * @param userId User ID filter
     * @param select Fields to select
     * @param order Order by field
     */
    @GET("user_watchlist")
    suspend fun getWatchlistPaginated(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Range") range: String,
        @Query("userId") userId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "addedAt.desc"
    ): Response<List<WatchlistCacheEntity>>
    
    // =================== WATCH PROGRESS ENDPOINTS ===================
    
    /**
     * Get user's watch progress
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter
     * @param select Fields to select
     */
    @GET("user_watch_progress")
    suspend fun getWatchProgress(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String,
        @Query("select") select: String = "*"
    ): Response<List<WatchProgressCacheEntity>>
    
    /**
     * Get watch progress for specific movie
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter
     * @param movieId Movie ID filter
     * @param select Fields to select
     */
    @GET("user_watch_progress")
    suspend fun getMovieProgress(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String,
        @Query("movieId") movieId: String,
        @Query("select") select: String = "*"
    ): Response<List<WatchProgressCacheEntity>>
    
    /**
     * Save or update watch progress
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param prefer Return preference
     * @param progress Watch progress data
     */
    @POST("user_watch_progress")
    suspend fun saveWatchProgress(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation,resolution=merge-duplicates",
        @Body progress: WatchProgressCacheEntity
    ): Response<List<WatchProgressCacheEntity>>
    
    /**
     * Delete watch progress
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter
     * @param movieId Movie ID filter
     */
    @DELETE("user_watch_progress")
    suspend fun deleteWatchProgress(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String,
        @Query("movieId") movieId: String
    ): Response<Void>
    
    // =================== VIEWING HISTORY ENDPOINTS ===================
    
    /**
     * Get user's viewing history
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter
     * @param select Fields to select
     * @param order Order by field
     */
    @GET("user_viewing_history")
    suspend fun getViewingHistory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "lastViewedAt.desc"
    ): Response<List<ViewingHistoryCacheEntity>>
    
    /**
     * Get viewing history with pagination
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param range Range for pagination
     * @param userId User ID filter
     * @param select Fields to select
     * @param order Order by field
     */
    @GET("user_viewing_history")
    suspend fun getViewingHistoryPaginated(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Range") range: String,
        @Query("userId") userId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "lastViewedAt.desc"
    ): Response<List<ViewingHistoryCacheEntity>>
    
    /**
     * Add or update viewing history entry
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param prefer Return preference
     * @param historyEntry Viewing history entry
     */
    @POST("user_viewing_history")
    suspend fun addViewingHistory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation,resolution=merge-duplicates",
        @Body historyEntry: ViewingHistoryCacheEntity
    ): Response<List<ViewingHistoryCacheEntity>>
    
    /**
     * Delete viewing history entry
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter
     * @param movieId Movie ID filter
     */
    @DELETE("user_viewing_history")
    suspend fun deleteViewingHistory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String,
        @Query("movieId") movieId: String
    ): Response<Void>
    
    /**
     * Clear all viewing history for user
     * 
     * @param apiKey Supabase anon key
     * @param authorization Bearer token
     * @param userId User ID filter
     */
    @DELETE("user_viewing_history")
    suspend fun clearViewingHistory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String
    ): Response<Void>
}
