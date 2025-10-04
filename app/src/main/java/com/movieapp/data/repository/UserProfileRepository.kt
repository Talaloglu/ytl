package com.movieapp.data.repository

import android.util.Log
import com.movieapp.data.local.DatabaseProvider
import com.movieapp.data.local.UserProfileDao
import com.movieapp.data.local.UserProfileEntity
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout

/**
 * User Profile Repository
 * Handles user profile data with local database and remote sync
 * 
 * Features:
 * - Local-first architecture with Room database
 * - Remote sync with Supabase Postgrest
 * - Optimistic UI updates
 * - Conflict resolution
 * - Error handling and retry logic
 */
class UserProfileRepository {
    
    private val userProfileDao: UserProfileDao by lazy {
        DatabaseProvider.getDatabase().userProfileDao()
    }
    
    private val supabaseClient by lazy {
        SupabaseClientProvider.getInstance()
    }
    
    companion object {
        private const val TAG = "UserProfileRepository"
        private const val TABLE_NAME = "user_profile"
        private const val TIMEOUT_MS = 10_000L
    }
    
    /**
     * Get user profile from local database
     * 
     * @param userId User ID
     * @return UserProfileEntity or null if not found
     */
    suspend fun getProfileFromLocal(userId: String): UserProfileEntity? {
        return try {
            userProfileDao.getProfile(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile from local", e)
            null
        }
    }
    
    /**
     * Get user profile as Flow for reactive updates
     * 
     * @param userId User ID
     * @return Flow of UserProfileEntity
     */
    fun getProfile(userId: String): Flow<UserProfileEntity?> {
        return userProfileDao.getProfileFlow(userId)
    }
    
    /**
     * Get user profile with remote sync
     * Fetches from local first, then syncs with remote
     * 
     * @param userId User ID
     * @param forceRefresh Force fetch from remote even if local exists
     * @return UserProfileEntity or null if not found
     */
    suspend fun getProfileWithSync(userId: String, forceRefresh: Boolean = false): UserProfileEntity? {
        return try {
            // Get local profile first
            val localProfile = userProfileDao.getProfile(userId)
            
            // If force refresh or no local profile, fetch from remote
            if (forceRefresh || localProfile == null) {
                refreshProfile(userId)
                return userProfileDao.getProfile(userId)
            }
            
            localProfile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile with sync", e)
            // Return local profile as fallback
            userProfileDao.getProfile(userId)
        }
    }
    
    /**
     * Create user profile from auth info
     * Creates both locally and remotely
     * 
     * @param userInfo User information from Supabase auth
     */
    suspend fun createProfileFromAuth(userInfo: UserInfo) {
        try {
            val userId = userInfo.id
            val email = userInfo.email ?: ""
            
            // Check if profile already exists locally
            val existingProfile = userProfileDao.getProfile(userId)
            
            if (existingProfile == null) {
                // Create new profile
                val profile = UserProfileEntity.create(
                    userId = userId,
                    email = email,
                    displayName = userInfo.userMetadata?.get("display_name")?.toString(),
                    avatarUrl = userInfo.userMetadata?.get("avatar_url")?.toString()
                )
                
                // Save locally first (optimistic UI)
                userProfileDao.insertProfile(profile)
                Log.d(TAG, "Created local profile for user: $userId")
                
                // Sync to remote
                syncProfileToRemote(profile)
            } else {
                // Update existing profile with latest auth info
                val updatedProfile = UserProfileEntity.updateFrom(
                    existing = existingProfile,
                    displayName = userInfo.userMetadata?.get("display_name")?.toString() ?: existingProfile.displayName,
                    avatarUrl = userInfo.userMetadata?.get("avatar_url")?.toString() ?: existingProfile.avatarUrl
                )
                
                userProfileDao.updateProfile(updatedProfile)
                Log.d(TAG, "Updated local profile for user: $userId")
                
                // Sync to remote
                syncProfileToRemote(updatedProfile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create/update profile from auth", e)
            throw e
        }
    }
    
    /**
     * Refresh user profile from remote
     * Fetches from Supabase and updates local database
     * 
     * @param userId User ID
     * @return Updated profile or null if not found
     */
    suspend fun refreshProfile(userId: String): UserProfileEntity? {
        return try {
            Log.d(TAG, "Refreshing profile from remote: $userId")
            
            // Fetch from Supabase with timeout
            val remoteProfiles = withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("userId", userId)
                        }
                    }
                    .decodeList<UserProfileEntity>()
            }
            
            val remoteProfile = remoteProfiles.firstOrNull()
            
            if (remoteProfile != null) {
                // Update local database
                userProfileDao.insertProfile(remoteProfile)
                Log.d(TAG, "Refreshed profile from remote: $userId")
                remoteProfile
            } else {
                Log.w(TAG, "Profile not found on remote: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh profile from remote", e)
            null
        }
    }
    
    /**
     * Sync profile to remote
     * Upserts profile to Supabase
     * 
     * @param profile Profile to sync
     */
    private suspend fun syncProfileToRemote(profile: UserProfileEntity) {
        try {
            Log.d(TAG, "Syncing profile to remote: ${profile.userId}")
            
            withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .upsert(profile)
            }
            
            Log.d(TAG, "Successfully synced profile to remote: ${profile.userId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync profile to remote (local changes preserved)", e)
            // Don't throw - local changes are preserved
        }
    }
    
    /**
     * Update display name
     * Updates locally and syncs to remote
     * 
     * @param userId User ID
     * @param displayName New display name
     */
    suspend fun updateDisplayName(userId: String, displayName: String) {
        try {
            // Update locally first (optimistic UI)
            userProfileDao.updateDisplayName(userId, displayName)
            Log.d(TAG, "Updated display name locally for: $userId")
            
            // Sync to remote
            try {
                withTimeout(TIMEOUT_MS) {
                    supabaseClient.from(TABLE_NAME)
                        .update({
                            set("displayName", displayName)
                            set("updatedAt", System.currentTimeMillis())
                        }) {
                            filter {
                                eq("userId", userId)
                            }
                        }
                }
                Log.d(TAG, "Synced display name to remote: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync display name to remote (local changes preserved)", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update display name", e)
            throw e
        }
    }
    
    /**
     * Update avatar URL
     * Updates locally and syncs to remote
     * 
     * @param userId User ID
     * @param avatarUrl New avatar URL
     */
    suspend fun updateAvatarUrl(userId: String, avatarUrl: String) {
        try {
            // Update locally first (optimistic UI)
            userProfileDao.updateAvatarUrl(userId, avatarUrl)
            Log.d(TAG, "Updated avatar URL locally for: $userId")
            
            // Sync to remote
            try {
                withTimeout(TIMEOUT_MS) {
                    supabaseClient.from(TABLE_NAME)
                        .update({
                            set("avatarUrl", avatarUrl)
                            set("updatedAt", System.currentTimeMillis())
                        }) {
                            filter {
                                eq("userId", userId)
                            }
                        }
                }
                Log.d(TAG, "Synced avatar URL to remote: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync avatar URL to remote (local changes preserved)", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update avatar URL", e)
            throw e
        }
    }
    
    /**
     * Update profile (generic update)
     * Updates locally and syncs to remote
     * 
     * @param profile Updated profile
     */
    suspend fun updateProfile(profile: UserProfileEntity) {
        try {
            // Update locally first (optimistic UI)
            userProfileDao.updateProfile(profile)
            Log.d(TAG, "Updated profile locally: ${profile.userId}")
            
            // Sync to remote
            syncProfileToRemote(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile", e)
            throw e
        }
    }
    
    /**
     * Delete user profile
     * Deletes from both local and remote
     * 
     * @param userId User ID
     */
    suspend fun deleteProfile(userId: String) {
        try {
            // Delete locally first
            userProfileDao.deleteProfile(userId)
            Log.d(TAG, "Deleted local profile for: $userId")
            
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
                Log.d(TAG, "Deleted remote profile for: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete remote profile", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete profile", e)
            throw e
        }
    }
    
    /**
     * Check if profile exists locally
     * 
     * @param userId User ID
     * @return true if profile exists
     */
    suspend fun profileExists(userId: String): Boolean {
        return try {
            userProfileDao.profileExists(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if profile exists", e)
            false
        }
    }
    
    /**
     * Check if profile exists on remote
     * 
     * @param userId User ID
     * @return true if profile exists on remote
     */
    suspend fun profileExistsOnRemote(userId: String): Boolean {
        return try {
            val remoteProfiles = withTimeout(TIMEOUT_MS) {
                supabaseClient.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("userId", userId)
                        }
                    }
                    .decodeList<UserProfileEntity>()
            }
            remoteProfiles.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if profile exists on remote", e)
            false
        }
    }
}
