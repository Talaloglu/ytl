package com.movieapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.model.CombinedMovie
import com.movieapp.data.repository.CombinedMovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * StreamingViewModel for managing movies with streaming capabilities
 * Integrates TMDB movie data with Supabase streaming URLs
 * Follows MVVM pattern with StateFlow for reactive state management
 */
class StreamingViewModel(
    private val repository: CombinedMovieRepository = CombinedMovieRepository()
) : ViewModel() {
    
    // Unified UI state for list/detail/search screens
    sealed class UiState<out T> {
        object Idle : UiState<Nothing>()
        object Loading : UiState<Nothing>()
        data class Success<T>(val data: T) : UiState<T>()
        data class Error(
            val message: String,
            val isOffline: Boolean = false,
            val canRetry: Boolean = true
        ) : UiState<Nothing>()
    }
    
    // Structured UI states (non-breaking: keep old flows too)
    private val _streamingState = MutableStateFlow<UiState<List<CombinedMovie>>>(UiState.Idle)
    val streamingState: StateFlow<UiState<List<CombinedMovie>>> = _streamingState.asStateFlow()
    
    private val _detailsState = MutableStateFlow<UiState<CombinedMovie?>>(UiState.Idle)
    val detailsState: StateFlow<UiState<CombinedMovie?>> = _detailsState.asStateFlow()
    
    private val _searchState = MutableStateFlow<UiState<List<CombinedMovie>>>(UiState.Idle)
    val searchState: StateFlow<UiState<List<CombinedMovie>>> = _searchState.asStateFlow()
    
    // UI State for streaming movies (movies available for streaming)
    private val _streamingMovies = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val streamingMovies: StateFlow<List<CombinedMovie>> = _streamingMovies.asStateFlow()
    
    // UI State for current movie being watched
    private val _currentMovie = MutableStateFlow<CombinedMovie?>(null)
    val currentMovie: StateFlow<CombinedMovie?> = _currentMovie.asStateFlow()
    
    // UI State for search results
    private val _searchResults = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val searchResults: StateFlow<List<CombinedMovie>> = _searchResults.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _isLoadingMovie = MutableStateFlow(false)
    val isLoadingMovie: StateFlow<Boolean> = _isLoadingMovie.asStateFlow()
    
    // Error states
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()
    
    // Pagination state
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()
    
    // Track loading states to prevent duplicate requests
    private var isCurrentlyLoading = false
    private var isCurrentlySearching = false
    
    // Helper to map exceptions to friendly messages and offline hint
    private fun mapError(e: Throwable): UiState.Error {
        val msg = e.message ?: "Unexpected error"
        val offline = (e is java.io.IOException) ||
            (msg.contains("timeout", ignoreCase = true)) ||
            (msg.contains("Unable to resolve host", ignoreCase = true))
        return UiState.Error(
            message = if (offline) "You're offline. Showing cached data when available." else msg,
            isOffline = offline,
            canRetry = true
        )
    }
    
    /**
     * Load available streaming movies
     * Only shows movies that exist in Supabase with valid streaming URLs
     */
    fun loadStreamingMovies(page: Int = 1, isRefresh: Boolean = false) {
        if (isCurrentlyLoading) return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                isCurrentlyLoading = true
                _errorMessage.value = null
                _streamingState.value = UiState.Loading
                
                val result = repository.getAvailableMovies(page)
                
                result.fold(
                    onSuccess = { movies ->
                        if (page == 1 || isRefresh) {
                            _streamingMovies.value = movies
                        } else {
                            val currentMovies = _streamingMovies.value.toMutableList()
                            currentMovies.addAll(movies)
                            _streamingMovies.value = currentMovies
                        }
                        
                        _currentPage.value = page
                        // For now, assume we have more pages if we got results
                        // You can enhance this based on your Supabase pagination setup
                        _hasMorePages.value = movies.isNotEmpty()
                        _streamingState.value = UiState.Success(_streamingMovies.value)
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to load streaming movies"
                        _streamingState.value = mapError(exception)
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                _streamingState.value = mapError(e)
            } finally {
                _isLoading.value = false
                isCurrentlyLoading = false
            }
        }
    }
    
    /**
     * Load next page of streaming movies
     */
    fun loadNextPage() {
        if (_hasMorePages.value && !isCurrentlyLoading) {
            val nextPage = _currentPage.value + 1
            loadStreamingMovies(nextPage)
        }
    }
    
    /**
     * Refresh streaming movies list with cache invalidation
     */
    fun refreshMovies() {
        repository.invalidateCache() // Clear cache for fresh data
        _currentPage.value = 1
        _hasMorePages.value = true
        loadStreamingMovies(1, isRefresh = true)
    }
    
    /**
     * Get movie details with streaming information
     */
    fun getMovieWithStreamDetails(tmdbId: Int) {
        viewModelScope.launch {
            try {
                _isLoadingMovie.value = true
                _errorMessage.value = null
                _detailsState.value = UiState.Loading
                
                val result = repository.getMovieWithStreamDetails(tmdbId)
                
                result.fold(
                    onSuccess = { movie ->
                        _currentMovie.value = movie
                        if (movie == null) {
                            _errorMessage.value = "This movie is not available for streaming"
                            _detailsState.value = UiState.Error("This movie is not available for streaming", canRetry = false)
                        } else {
                            _detailsState.value = UiState.Success(movie)
                        }
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to load movie details"
                        _detailsState.value = mapError(exception)
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                _detailsState.value = mapError(e)
            } finally {
                _isLoadingMovie.value = false
            }
        }
    }
    
    /**
     * Search for streamable movies by title
     */
    fun searchStreamingMovies(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchState.value = UiState.Idle
            return
        }
        
        if (isCurrentlySearching) return
        
        viewModelScope.launch {
            try {
                _isSearching.value = true
                isCurrentlySearching = true
                _searchError.value = null
                _searchState.value = UiState.Loading
                
                val result = repository.searchAvailableMovies(query)
                
                result.fold(
                    onSuccess = { movies ->
                        _searchResults.value = movies
                        _searchState.value = UiState.Success(movies)
                    },
                    onFailure = { exception ->
                        _searchError.value = exception.message ?: "Search failed"
                        _searchResults.value = emptyList()
                        _searchState.value = mapError(exception)
                    }
                )
            } catch (e: Exception) {
                _searchError.value = "Search error: ${e.message}"
                _searchResults.value = emptyList()
                _searchState.value = mapError(e)
            } finally {
                _isSearching.value = false
                isCurrentlySearching = false
            }
        }
    }
    
    /**
     * Clear search results
     */
    fun clearSearch() {
        _searchResults.value = emptyList()
        _searchError.value = null
        _searchState.value = UiState.Idle
    }
    
    /**
     * Clear current movie
     */
    fun clearCurrentMovie() {
        _currentMovie.value = null
        _detailsState.value = UiState.Idle
    }
    
    /**
     * Clear error messages
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clear search error
     */
    fun clearSearchError() {
        _searchError.value = null
    }
    
    /**
     * Set current movie for streaming
     */
    fun setCurrentMovie(movie: CombinedMovie) {
        _currentMovie.value = movie
        _detailsState.value = UiState.Success(movie)
    }
    
    /**
     * Check if a movie is available for streaming
     */
    fun isMovieAvailable(tmdbId: Int): Boolean {
        return _streamingMovies.value.any { it.id == tmdbId && it.hasVideo }
    }
    
    /**
     * Get video URL for a movie
     */
    fun getVideoUrl(tmdbId: Int): String? {
        return _streamingMovies.value
            .find { it.id == tmdbId && it.hasVideo }
            ?.videoUrl
    }
}