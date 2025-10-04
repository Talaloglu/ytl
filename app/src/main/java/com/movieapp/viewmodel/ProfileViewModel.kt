package com.movieapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.auth.AuthRepository
import com.movieapp.data.local.UserProfileEntity
import com.movieapp.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Profile ViewModel
 * Manages user profile state and operations
 * 
 * Features:
 * - Profile state management
 * - Profile updates (display name, avatar)
 * - Profile sync with remote
 * - Error handling
 * - Loading states
 */
class ProfileViewModel : ViewModel() {
    
    private val authRepository = AuthRepository()
    private val profileRepository = UserProfileRepository()
    
    companion object {
        private const val TAG = "ProfileViewModel"
    }
    
    // Profile state
    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()
    
    // Action result state
    private val _actionResult = MutableSharedFlow<ProfileActionResult>()
    val actionResult: SharedFlow<ProfileActionResult> = _actionResult.asSharedFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadProfile()
    }
    
    /**
     * Load user profile
     * Fetches current user's profile and observes changes
     */
    fun loadProfile() {
        viewModelScope.launch {
            try {
                _profileState.value = ProfileUiState.Loading
                
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _profileState.value = ProfileUiState.NotAuthenticated
                    return@launch
                }
                
                // Observe profile changes
                profileRepository.getProfile(userId)
                    .collect { profile ->
                        if (profile != null) {
                            _profileState.value = ProfileUiState.Success(profile)
                        } else {
                            // Profile doesn't exist locally, try to fetch from remote
                            refreshProfile()
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load profile", e)
                _profileState.value = ProfileUiState.Error("Failed to load profile: ${e.message}")
            }
        }
    }
    
    /**
     * Refresh profile from remote
     * Forces a sync with Supabase
     */
    fun refreshProfile() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _actionResult.emit(ProfileActionResult.Error("Not authenticated"))
                    return@launch
                }
                
                val profile = profileRepository.refreshProfile(userId)
                if (profile == null) {
                    _actionResult.emit(ProfileActionResult.Error("Profile not found on server"))
                } else {
                    _actionResult.emit(ProfileActionResult.Success("Profile refreshed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh profile", e)
                _actionResult.emit(ProfileActionResult.Error("Failed to refresh: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update display name
     * 
     * @param displayName New display name
     */
    fun updateDisplayName(displayName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _actionResult.emit(ProfileActionResult.Error("Not authenticated"))
                    return@launch
                }
                
                if (displayName.isBlank()) {
                    _actionResult.emit(ProfileActionResult.Error("Display name cannot be empty"))
                    return@launch
                }
                
                profileRepository.updateDisplayName(userId, displayName)
                _actionResult.emit(ProfileActionResult.Success("Display name updated"))
                
                Log.d(TAG, "Display name updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update display name", e)
                _actionResult.emit(ProfileActionResult.Error("Failed to update display name: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update avatar URL
     * 
     * @param avatarUrl New avatar URL
     */
    fun updateAvatarUrl(avatarUrl: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _actionResult.emit(ProfileActionResult.Error("Not authenticated"))
                    return@launch
                }
                
                if (avatarUrl.isBlank()) {
                    _actionResult.emit(ProfileActionResult.Error("Avatar URL cannot be empty"))
                    return@launch
                }
                
                profileRepository.updateAvatarUrl(userId, avatarUrl)
                _actionResult.emit(ProfileActionResult.Success("Avatar updated"))
                
                Log.d(TAG, "Avatar URL updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update avatar URL", e)
                _actionResult.emit(ProfileActionResult.Error("Failed to update avatar: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update profile
     * Generic update for multiple fields
     * 
     * @param profile Updated profile entity
     */
    fun updateProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                profileRepository.updateProfile(profile)
                _actionResult.emit(ProfileActionResult.Success("Profile updated"))
                
                Log.d(TAG, "Profile updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update profile", e)
                _actionResult.emit(ProfileActionResult.Error("Failed to update profile: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Sign out
     * Clears profile state and signs out user
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                authRepository.signOut()
                _profileState.value = ProfileUiState.NotAuthenticated
                _actionResult.emit(ProfileActionResult.Success("Signed out successfully"))
                
                Log.d(TAG, "User signed out")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sign out", e)
                _actionResult.emit(ProfileActionResult.Error("Failed to sign out: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get current profile
     * Returns the current profile from state
     * 
     * @return UserProfileEntity or null
     */
    fun getCurrentProfile(): UserProfileEntity? {
        return when (val state = _profileState.value) {
            is ProfileUiState.Success -> state.profile
            else -> null
        }
    }
    
    /**
     * Get current user ID
     * 
     * @return User ID or null if not authenticated
     */
    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }
}

/**
 * Profile UI State
 * Represents different states of profile loading
 */
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    object NotAuthenticated : ProfileUiState()
    data class Success(val profile: UserProfileEntity) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

/**
 * Profile Action Result
 * Represents results of profile actions
 */
sealed class ProfileActionResult {
    data class Success(val message: String) : ProfileActionResult()
    data class Error(val message: String) : ProfileActionResult()
}
