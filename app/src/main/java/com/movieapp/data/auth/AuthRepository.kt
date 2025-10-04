package com.movieapp.data.auth

import android.util.Log
import com.movieapp.data.repository.UserProfileRepository
import com.movieapp.data.repository.SupabaseClientProvider
import com.movieapp.utils.SupabaseRetrofitInstance
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Authentication Repository
 * Handles all authentication operations using Supabase GoTrue
 * Matches decompiled version structure exactly
 * 
 * @property userProfileRepository Repository for user profile operations (optional for now)
 */
class AuthRepository(
    private val userProfileRepository: UserProfileRepository? = null
) {
    private val supabaseAuth: Auth = SupabaseClientProvider.getGotrue()
    
    companion object {
        private const val TAG = "AuthRepository"
    }
    
    init {
        // Configure token provider for API calls
        // Note: Token provider configuration can be added here if needed
    }
    
    /**
     * Observe authentication state changes
     * Returns Flow of AuthState for reactive UI updates
     */
    fun observeAuthState(): Flow<AuthState> {
        return supabaseAuth.sessionStatus
            .map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val session = status.session
                        val user = session.user
                        if (user != null) {
                            AuthState.Authenticated(
                                user = user,
                                userId = user.id
                            )
                        } else {
                            AuthState.Unauthenticated
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        AuthState.Unauthenticated
                    }
                    is SessionStatus.LoadingFromStorage -> {
                        AuthState.Loading
                    }
                    is SessionStatus.NetworkError -> {
                        AuthState.Error("Network error occurred")
                    }
                    else -> AuthState.Unauthenticated
                }
            }
            .distinctUntilChanged()
            .catch { e ->
                Log.e(TAG, "Error observing auth state", e)
                emit(AuthState.Error("Failed to observe auth state: ${e.message}", e))
            }
    }
    
    /**
     * Sign up a new user with email and password
     * Creates user profile automatically after successful signup
     * 
     * @param email User's email address
     * @param password User's password
     * @return AuthState indicating result
     */
    suspend fun signUp(email: String, password: String): AuthState {
        return try {
            Log.d(TAG, "Signing up user: $email")
            
            // Sign up with Supabase
            supabaseAuth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            
            // Get the current user after successful signup
            val userInfo = supabaseAuth.currentUserOrNull()
            val userId = userInfo?.id
            
            if (userId != null && userInfo != null) {
                Log.d(TAG, "Sign up successful for user: $userId")
                
                // Create user profile if repository is available
                userProfileRepository?.let { repo ->
                    try {
                        repo.createProfileFromAuth(userInfo)
                        Log.d(TAG, "User profile created for: $userId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create user profile", e)
                        // Don't fail signup if profile creation fails
                    }
                }
                
                AuthState.Success("Account created successfully!")
            } else {
                AuthState.Error("Sign up failed: Invalid response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            AuthState.Error(
                error = e.message ?: "Sign up failed",
                exception = e
            )
        }
    }
    
    /**
     * Sign in an existing user with email and password
     * Fetches user profile after successful signin
     * 
     * @param email User's email address
     * @param password User's password
     * @return AuthState indicating result
     */
    suspend fun signIn(email: String, password: String): AuthState {
        return try {
            Log.d(TAG, "Signing in user: $email")
            
            // Sign in with Supabase
            supabaseAuth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            // Get the current user after successful signin
            val userInfo = supabaseAuth.currentUserOrNull()
            val userId = userInfo?.id
            
            if (userId != null && userInfo != null) {
                Log.d(TAG, "Sign in successful for user: $userId")
                
                // Fetch or create user profile if repository is available
                userProfileRepository?.let { repo ->
                    try {
                        val existingProfile = repo.getProfileFromLocal(userId)
                        if (existingProfile == null) {
                            // Profile doesn't exist locally, try to create from auth
                            repo.createProfileFromAuth(userInfo)
                            Log.d(TAG, "User profile created for: $userId")
                        } else {
                            // Profile exists, refresh from remote
                            repo.refreshProfile(userId)
                            Log.d(TAG, "User profile refreshed for: $userId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to handle user profile", e)
                        // Don't fail signin if profile handling fails
                    }
                }
                
                AuthState.Success("Signed in successfully!")
            } else {
                AuthState.Error("Sign in failed: Invalid response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            AuthState.Error(
                error = e.message ?: "Sign in failed",
                exception = e
            )
        }
    }
    
    /**
     * Sign out the current user
     * Clears session and optionally clears local data
     * 
     * @return AuthState indicating result
     */
    suspend fun signOut(): AuthState {
        return try {
            Log.d(TAG, "Signing out user")
            
            supabaseAuth.signOut()
            
            // Note: We don't clear local profiles as they might be needed offline
            // The UserProfileRepository can handle this if needed
            
            Log.d(TAG, "Sign out successful")
            AuthState.Success("Signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            AuthState.Error(
                error = e.message ?: "Sign out failed",
                exception = e
            )
        }
    }
    
    /**
     * Get current authenticated user
     * 
     * @return UserInfo if user is authenticated, null otherwise
     */
    suspend fun getCurrentUser(): UserInfo? {
        return try {
            supabaseAuth.currentUserOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current user", e)
            null
        }
    }
    
    /**
     * Check if user is currently authenticated
     * 
     * @return true if user has valid session
     */
    fun isAuthenticated(): Boolean {
        return supabaseAuth.currentAccessTokenOrNull() != null
    }
    
    /**
     * Get current user ID if authenticated
     * 
     * @return User ID or null
     */
    fun getCurrentUserId(): String? {
        return try {
            supabaseAuth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current user ID", e)
            null
        }
    }
    
    /**
     * Send password reset email
     * 
     * @param email User's email address
     * @return AuthState indicating result
     */
    suspend fun resetPassword(email: String): AuthState {
        return try {
            Log.d(TAG, "Sending password reset email to: $email")
            
            supabaseAuth.resetPasswordForEmail(email)
            
            AuthState.Success("Password reset email sent!")
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            AuthState.Error(
                error = e.message ?: "Password reset failed",
                exception = e
            )
        }
    }
    
    /**
     * Update user password
     * 
     * @param newPassword New password
     * @return AuthState indicating result
     */
    suspend fun updatePassword(newPassword: String): AuthState {
        return try {
            Log.d(TAG, "Updating user password")
            
            supabaseAuth.modifyUser {
                password = newPassword
            }
            
            AuthState.Success("Password updated successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Password update failed", e)
            AuthState.Error(
                error = e.message ?: "Password update failed",
                exception = e
            )
        }
    }
    
    /**
     * Update user email
     * 
     * @param newEmail New email address
     * @return AuthState indicating result
     */
    suspend fun updateEmail(newEmail: String): AuthState {
        return try {
            Log.d(TAG, "Updating user email to: $newEmail")
            
            supabaseAuth.modifyUser {
                email = newEmail
            }
            
            AuthState.Success("Email updated successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Email update failed", e)
            AuthState.Error(
                error = e.message ?: "Email update failed",
                exception = e
            )
        }
    }
}
