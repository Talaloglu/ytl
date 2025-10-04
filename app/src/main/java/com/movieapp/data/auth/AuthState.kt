package com.movieapp.data.auth

import io.github.jan.supabase.gotrue.user.UserInfo

/**
 * Sealed class hierarchy representing authentication states
 * Matches the decompiled version exactly
 */
sealed class AuthState {
    /**
     * Initial state - no authentication attempt yet
     */
    object Idle : AuthState()
    
    /**
     * Authentication operation in progress
     */
    object Loading : AuthState()
    
    /**
     * User is authenticated with session
     * 
     * @property user User information from Supabase
     * @property userId User's unique ID
     */
    data class Authenticated(
        val user: UserInfo,
        val userId: String
    ) : AuthState()
    
    /**
     * User is not authenticated (logged out or session expired)
     */
    object Unauthenticated : AuthState()
    
    /**
     * Authentication operation succeeded
     * 
     * @property message Success message
     */
    data class Success(
        val message: String
    ) : AuthState()
    
    /**
     * Authentication operation failed
     * 
     * @property error Error message
     * @property exception Optional exception for debugging
     */
    data class Error(
        val error: String,
        val exception: Throwable? = null
    ) : AuthState()
    
    /**
     * Check if current state is authenticated
     */
    fun isAuthenticated(): Boolean = this is Authenticated
    
    /**
     * Check if current state is loading
     */
    fun isLoading(): Boolean = this is Loading
    
    /**
     * Check if current state is error
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Get user info if authenticated
     */
    fun getUserOrNull(): UserInfo? = (this as? Authenticated)?.user
    
    /**
     * Get user ID if authenticated
     */
    fun getUserIdOrNull(): String? = (this as? Authenticated)?.userId
}
