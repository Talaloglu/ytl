package com.movieapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.auth.AuthRepository
import com.movieapp.data.auth.AuthState
import com.movieapp.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for handling authentication operations
 * Updated to match decompiled version structure
 */
class AuthViewModel : ViewModel() {
    
    // Initialize repositories
    private val userProfileRepository = UserProfileRepository()
    private val authRepository = AuthRepository(userProfileRepository)
    
    // Observe auth state from repository
    val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Idle
        )
    
    // Action results (for one-time events like success/error messages)
    private val _actionResult = MutableStateFlow<AuthState?>(null)
    val actionResult: StateFlow<AuthState?> = _actionResult.asStateFlow()
    
    /**
     * Sign in with email and password
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            _actionResult.value = result
        }
    }
    
    /**
     * Sign up with email and password
     */
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.signUp(email, password)
            _actionResult.value = result
        }
    }
    
    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            val result = authRepository.signOut()
            _actionResult.value = result
        }
    }
    
    /**
     * Reset password for email
     */
    fun resetPassword(email: String) {
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            _actionResult.value = result
        }
    }
    
    /**
     * Update user password
     */
    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            val result = authRepository.updatePassword(newPassword)
            _actionResult.value = result
        }
    }
    
    /**
     * Update user email
     */
    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            val result = authRepository.updateEmail(newEmail)
            _actionResult.value = result
        }
    }
    
    /**
     * Clear action result (after showing message)
     */
    fun clearActionResult() {
        _actionResult.value = null
    }
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return authRepository.isAuthenticated()
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }
    
}