package com.movieapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for User Profile operations
 * Provides methods to access and modify user profile data
 */
@Dao
interface UserProfileDao {
    
    /**
     * Insert or replace a user profile
     * 
     * @param profile User profile to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)
    
    /**
     * Get user profile by user ID
     * 
     * @param userId User ID to query
     * @return User profile or null if not found
     */
    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    suspend fun getProfile(userId: String): UserProfileEntity?
    
    /**
     * Get user profile as Flow for reactive updates
     * 
     * @param userId User ID to query
     * @return Flow of user profile (null if not found)
     */
    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    fun getProfileFlow(userId: String): Flow<UserProfileEntity?>
    
    /**
     * Update user profile
     * 
     * @param profile Updated profile
     */
    @Update
    suspend fun updateProfile(profile: UserProfileEntity)
    
    /**
     * Delete user profile
     * 
     * @param userId User ID to delete
     */
    @Query("DELETE FROM user_profile WHERE userId = :userId")
    suspend fun deleteProfile(userId: String)
    
    /**
     * Delete all user profiles (for logout/cleanup)
     */
    @Query("DELETE FROM user_profile")
    suspend fun deleteAllProfiles()
    
    /**
     * Check if profile exists
     * 
     * @param userId User ID to check
     * @return True if profile exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM user_profile WHERE userId = :userId)")
    suspend fun profileExists(userId: String): Boolean
    
    /**
     * Update display name
     * 
     * @param userId User ID
     * @param displayName New display name
     */
    @Query("UPDATE user_profile SET displayName = :displayName, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun updateDisplayName(userId: String, displayName: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Update avatar URL
     * 
     * @param userId User ID
     * @param avatarUrl New avatar URL
     */
    @Query("UPDATE user_profile SET avatarUrl = :avatarUrl, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun updateAvatarUrl(userId: String, avatarUrl: String, timestamp: Long = System.currentTimeMillis())
}
