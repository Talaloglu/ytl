package com.movieapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.auth.AuthRepository
import com.movieapp.data.local.WatchlistCacheEntity
import com.movieapp.data.repository.WatchlistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Watchlist ViewModel
 * Manages watchlist state and operations
 * 
 * Features:
 * - Watchlist management (add/remove/toggle)
 * - Reactive watchlist updates
 * - Background sync with remote
 * - Loading states
 * - Error handling
 */
class WatchlistViewModel : ViewModel() {
    
    private val authRepository = AuthRepository()
    private val watchlistRepository = WatchlistRepository()
    
    companion object {
        private const val TAG = "WatchlistViewModel"
    }
    
    // Watchlist state
    private val _watchlistState = MutableStateFlow<WatchlistUiState>(WatchlistUiState.Loading)
    val watchlistState: StateFlow<WatchlistUiState> = _watchlistState.asStateFlow()
    
    // Watchlist count
    private val _watchlistCount = MutableStateFlow(0)
    val watchlistCount: StateFlow<Int> = _watchlistCount.asStateFlow()
    
    // Action result
    private val _actionResult = MutableSharedFlow<WatchlistActionResult>()
    val actionResult: SharedFlow<WatchlistActionResult> = _actionResult.asSharedFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Syncing state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    init {
        loadWatchlist()
    }
    
    /**
     * Load user's watchlist
     * Observes watchlist changes from local database
     */
    fun loadWatchlist() {
        viewModelScope.launch {
            try {
                _watchlistState.value = WatchlistUiState.Loading
                
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _watchlistState.value = WatchlistUiState.NotAuthenticated
                    return@launch
                }
                
                // Observe watchlist changes
                watchlistRepository.getWatchlistFlow(userId)
                    .collect { entries ->
                        _watchlistState.value = WatchlistUiState.Success(entries)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load watchlist", e)
                _watchlistState.value = WatchlistUiState.Error("Failed to load watchlist: ${e.message}")
            }
        }
        
        // Observe watchlist count
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                watchlistRepository.getWatchlistCountFlow(userId)
                    .collect { count ->
                        _watchlistCount.value = count
                    }
            }
        }
    }
    
    /**
     * Add movie to watchlist
     * 
     * @param movieId Movie ID to add
     */
    fun addToWatchlist(movieId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _actionResult.emit(WatchlistActionResult.Error("Not authenticated"))
                    return@launch
                }
                
                watchlistRepository.addToWatchlist(userId, movieId)
                _actionResult.emit(WatchlistActionResult.Success("Added to watchlist"))
                
                Log.d(TAG, "Added movie $movieId to watchlist")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add to watchlist", e)
                _actionResult.emit(WatchlistActionResult.Error("Failed to add: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Remove movie from watchlist
     * 
     * @param movieId Movie ID to remove
     */
    fun removeFromWatchlist(movieId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _actionResult.emit(WatchlistActionResult.Error("Not authenticated"))
                    return@launch
                }
                
                watchlistRepository.removeFromWatchlist(userId, movieId)
                _actionResult.emit(WatchlistActionResult.Success("Removed from watchlist"))
                
                Log.d(TAG, "Removed movie $movieId from watchlist")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove from watchlist", e)
                _actionResult.emit(WatchlistActionResult.Error("Failed to remove: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle movie in watchlist
     * Add if not present, remove if present
     * 
     * @param movieId Movie ID to toggle
     */
    fun toggleWatchlist(movieId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _actionResult.emit(WatchlistActionResult.Error("Not authenticated"))
                    return@launch
                }
                
                val added = watchlistRepository.toggleWatchlist(userId, movieId)
                val message = if (added) "Added to watchlist" else "Removed from watchlist"
                _actionResult.emit(WatchlistActionResult.Success(message))
                
                Log.d(TAG, "Toggled movie $movieId in watchlist: added=$added")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle watchlist", e)
                _actionResult.emit(WatchlistActionResult.Error("Failed to update: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Check if movie is in watchlist
     * 
     * @param movieId Movie ID to check
     * @return Flow of boolean indicating if movie is in watchlist
     */
    fun isInWatchlist(movieId: Int): Flow<Boolean> {
        val userId = authRepository.getCurrentUserId()
        return if (userId != null) {
            watchlistRepository.isInWatchlistFlow(userId, movieId)
        } else {
            flowOf(false)
        }
    }
    
    /**
     * Sync watchlist with remote
     * Performs full bidirectional sync
     */
    fun syncWatchlist() {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _actionResult.emit(WatchlistActionResult.Error("Not authenticated"))
                    return@launch
                }
                
                watchlistRepository.fullSync(userId)
                _actionResult.emit(WatchlistActionResult.Success("Watchlist synced"))
                
                Log.d(TAG, "Watchlist synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync watchlist", e)
                _actionResult.emit(WatchlistActionResult.Error("Sync failed: ${e.message}"))
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * Clear entire watchlist
     */
    fun clearWatchlist() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _actionResult.emit(WatchlistActionResult.Error("Not authenticated"))
                    return@launch
                }
                
                watchlistRepository.clearWatchlist(userId)
                _actionResult.emit(WatchlistActionResult.Success("Watchlist cleared"))
                
                Log.d(TAG, "Watchlist cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear watchlist", e)
                _actionResult.emit(WatchlistActionResult.Error("Failed to clear: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get current watchlist entries
     * 
     * @return List of watchlist entries or empty list
     */
    fun getCurrentWatchlist(): List<WatchlistCacheEntity> {
        return when (val state = _watchlistState.value) {
            is WatchlistUiState.Success -> state.entries
            else -> emptyList()
        }
    }
    
    /**
     * Get current watchlist movie IDs
     * 
     * @return List of movie IDs in watchlist
     */
    fun getCurrentWatchlistMovieIds(): List<Int> {
        return getCurrentWatchlist().map { it.movieId }
    }
}

/**
 * Watchlist UI State
 * Represents different states of watchlist loading
 */
sealed class WatchlistUiState {
    object Loading : WatchlistUiState()
    object NotAuthenticated : WatchlistUiState()
    data class Success(val entries: List<WatchlistCacheEntity>) : WatchlistUiState()
    data class Error(val message: String) : WatchlistUiState()
}

/**
 * Watchlist Action Result
 * Represents results of watchlist actions
 */
sealed class WatchlistActionResult {
    data class Success(val message: String) : WatchlistActionResult()
    data class Error(val message: String) : WatchlistActionResult()
}
