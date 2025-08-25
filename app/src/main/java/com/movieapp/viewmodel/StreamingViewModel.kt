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
class StreamingViewModel : ViewModel() {
    
    private val repository = CombinedMovieRepository()
    
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
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to load streaming movies"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
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
                
                val result = repository.getMovieWithStreamDetails(tmdbId)
                
                result.fold(
                    onSuccess = { movie ->
                        _currentMovie.value = movie
                        if (movie == null) {
                            _errorMessage.value = "This movie is not available for streaming"
                        }
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to load movie details"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
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
            return
        }
        
        if (isCurrentlySearching) return
        
        viewModelScope.launch {
            try {
                _isSearching.value = true
                isCurrentlySearching = true
                _searchError.value = null
                
                val result = repository.searchAvailableMovies(query)
                
                result.fold(
                    onSuccess = { movies ->
                        _searchResults.value = movies
                    },
                    onFailure = { exception ->
                        _searchError.value = exception.message ?: "Search failed"
                        _searchResults.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                _searchError.value = "Search error: ${e.message}"
                _searchResults.value = emptyList()
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
    }
    
    /**
     * Clear current movie
     */
    fun clearCurrentMovie() {
        _currentMovie.value = null
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