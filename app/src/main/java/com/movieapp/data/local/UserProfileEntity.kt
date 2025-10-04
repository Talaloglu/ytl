package com.movieapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User Profile Entity for Room database
 * Stores user profile information locally with sync support
 * 
 * @property userId Primary key - Supabase user UUID
 * @property email User's email address
 * @property displayName User's display name (optional)
 * @property avatarUrl URL to user's avatar image (optional)
 * @property createdAt Timestamp when profile was created
 * @property updatedAt Timestamp when profile was last updated
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Create a new user profile entity
         * 
         * @param userId Supabase user UUID
         * @param email User's email address
         * @param displayName Optional display name
         * @param avatarUrl Optional avatar URL
         * @return New UserProfileEntity instance
         */
        fun create(
            userId: String,
            email: String,
            displayName: String? = null,
            avatarUrl: String? = null,
            createdAt: Long = System.currentTimeMillis(),
            updatedAt: Long = System.currentTimeMillis()
        ): UserProfileEntity {
            return UserProfileEntity(
                userId = userId,
                email = email,
                displayName = displayName,
                avatarUrl = avatarUrl,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
        
        /**
         * Update existing profile with new data
         * 
         * @param existing Existing profile entity
         * @param displayName New display name (optional)
         * @param avatarUrl New avatar URL (optional)
         * @return Updated UserProfileEntity with new timestamp
         */
        fun updateFrom(
            existing: UserProfileEntity,
            displayName: String? = existing.displayName,
            avatarUrl: String? = existing.avatarUrl
        ): UserProfileEntity {
            return existing.copy(
                displayName = displayName,
                avatarUrl = avatarUrl,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
