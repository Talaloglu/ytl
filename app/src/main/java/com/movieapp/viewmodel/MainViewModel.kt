package com.movieapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.model.Movie
import com.movieapp.data.model.MoviesList
import com.movieapp.data.model.MovieDetails
import com.movieapp.data.model.Genre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MainViewModel class as specified
 * Inherits from ViewModel and manages UI-related data and business logic
 * Uses Repository as single source of truth for data
 * Enhanced with pagination support for infinite loading
 */
class MainViewModel : ViewModel() {
    
    // Private instance of the Repository as specified
    private val repository = Repository()
    
    // Mutable state variables to hold the UI state as specified
    
    // UI State for movies list (main list) - now supports pagination
    private val _moviesList = MutableStateFlow<List<Movie>>(emptyList())
    val moviesList: StateFlow<List<Movie>> = _moviesList.asStateFlow()
    
    // UI State for movie details
    private val _movieDetails = MutableStateFlow<MovieDetails?>(null)
    val movieDetails: StateFlow<MovieDetails?> = _movieDetails.asStateFlow()
    
    // UI State for popular movies
    private val _popularMovies = MutableStateFlow<List<Movie>>(emptyList())
    val popularMovies: StateFlow<List<Movie>> = _popularMovies.asStateFlow()
    
    // UI State for top rated movies
    private val _topRatedMovies = MutableStateFlow<List<Movie>>(emptyList())
    val topRatedMovies: StateFlow<List<Movie>> = _topRatedMovies.asStateFlow()
    
    // UI State for now playing movies
    private val _nowPlayingMovies = MutableStateFlow<List<Movie>>(emptyList())
    val nowPlayingMovies: StateFlow<List<Movie>> = _nowPlayingMovies.asStateFlow()
    
    // UI State for upcoming movies
    private val _upcomingMovies = MutableStateFlow<List<Movie>>(emptyList())
    val upcomingMovies: StateFlow<List<Movie>> = _upcomingMovies.asStateFlow()
    
    // UI State for search results
    private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
    val searchResults: StateFlow<List<Movie>> = _searchResults.asStateFlow()
    
    // UI State for genres
    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Pagination loading state (for loading next page)
    private val _isPaginationLoading = MutableStateFlow(false)
    val isPaginationLoading: StateFlow<Boolean> = _isPaginationLoading.asStateFlow()
    
    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Pagination state management
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()
    
    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()
    
    // Track if we're currently loading to prevent duplicate requests
    private var isCurrentlyLoading = false
    private var isCurrentlyPaginationLoading = false
    
    /**
     * Function to call the Repository's getMovies suspend function as specified
     * Enhanced with pagination support for infinite loading
     * Uses viewModelScope.launch to perform asynchronous network calls
     * Updates the state variables with the data received from the Repository
     * @param page The page number for pagination
     * @param isRefresh Whether this is a refresh (clear existing data) or pagination (append data)
     */
    fun getMovies(page: Int = 1, isRefresh: Boolean = false) {
        // Prevent duplicate requests
        if (isCurrentlyLoading || (page > 1 && isCurrentlyPaginationLoading)) {
            return
        }
        
        // If trying to load beyond available pages, return
        if (page > 1 && !_hasMorePages.value) {
            return
        }
        
        viewModelScope.launch {
            try {
                // Set appropriate loading state
                if (page == 1 || isRefresh) {
                    _isLoading.value = true
                    isCurrentlyLoading = true
                    _errorMessage.value = null
                } else {
                    _isPaginationLoading.value = true
                    isCurrentlyPaginationLoading = true
                }
                
                val response = repository.getMovies(page)
                if (response.isSuccessful) {
                    val moviesList = response.body()
                    if (moviesList != null) {
                        // Update pagination metadata
                        _currentPage.value = moviesList.page
                        _totalPages.value = moviesList.totalPages
                        _hasMorePages.value = moviesList.hasMorePages()
                        
                        // Update movies list based on operation type
                        if (page == 1 || isRefresh) {
                            // Initial load or refresh - replace existing data
                            _moviesList.value = moviesList.results
                        } else {
                            // Pagination - append new data to existing list
                            val currentMovies = _moviesList.value.toMutableList()
                            currentMovies.addAll(moviesList.results)
                            _moviesList.value = currentMovies
                        }
                    } else {
                        _errorMessage.value = "No data received from server"
                    }
                } else {
                    _errorMessage.value = "Failed to fetch movies: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
                _isPaginationLoading.value = false
                isCurrentlyLoading = false
                isCurrentlyPaginationLoading = false
            }
        }
    }
    
    /**
     * Load next page of movies for pagination
     * Automatically determines the next page number
     */
    fun loadNextPage() {
        val nextPage = _currentPage.value + 1
        if (_hasMorePages.value && !isCurrentlyPaginationLoading) {
            getMovies(nextPage)
        }
    }
    
    /**
     * Refresh movies list - loads first page and replaces existing data
     */
    fun refreshMovies() {
        _currentPage.value = 1
        _hasMorePages.value = true
        getMovies(1, isRefresh = true)
    }
    
    /**
     * Function to call the Repository's getMovieDetails suspend function as specified
     * Uses viewModelScope.launch to perform asynchronous network calls
     * Updates the state variables with the data received from the Repository
     * @param id The movie ID to fetch details for
     */
    fun getMovieDetails(id: Int) {
        // Supabase-first mode: do nothing here (details come from Supabase via StreamingViewModel).
        // Keep UI state calm and avoid logging/noise.
        viewModelScope.launch { _isLoading.value = false }
    }
    
    // Additional functions for enhanced functionality
    
    /**
     * Fetch popular movies from the Repository
     * @param page The page number for pagination
     */
    fun fetchPopularMovies(page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getPopularMovies(page)
                if (response.isSuccessful) {
                    val moviesList = response.body()
                    _popularMovies.value = moviesList?.results ?: emptyList()
                    _currentPage.value = page
                } else {
                    _errorMessage.value = "Failed to fetch popular movies: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch top rated movies from the Repository
     * @param page The page number for pagination
     */
    fun fetchTopRatedMovies(page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getTopRatedMovies(page)
                if (response.isSuccessful) {
                    val moviesList = response.body()
                    _topRatedMovies.value = moviesList?.results ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to fetch top rated movies: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch now playing movies from the Repository
     * @param page The page number for pagination
     */
    fun fetchNowPlayingMovies(page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getNowPlayingMovies(page)
                if (response.isSuccessful) {
                    val moviesList = response.body()
                    _nowPlayingMovies.value = moviesList?.results ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to fetch now playing movies: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch upcoming movies from the Repository
     * @param page The page number for pagination
     */
    fun fetchUpcomingMovies(page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getUpcomingMovies(page)
                if (response.isSuccessful) {
                    val moviesList = response.body()
                    _upcomingMovies.value = moviesList?.results ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to fetch upcoming movies: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Search movies by query
     * @param query The search query
     * @param page The page number for pagination
     */
    fun searchMovies(query: String, page: Int = 1) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.searchMovies(query, page)
                if (response.isSuccessful) {
                    val moviesList = response.body()
                    _searchResults.value = moviesList?.results ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to search movies: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch movie genres list
     */
    fun fetchGenres() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getGenres()
                if (response.isSuccessful) {
                    _genres.value = response.body()?.genres ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to fetch genres: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch movies by genre
     * @param genreId The genre ID to filter by
     * @param page The page number for pagination
     */
    fun fetchMoviesByGenre(genreId: Int, page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getMoviesByGenre(genreId, page)
                if (response.isSuccessful) {
                    val moviesList = response.body()
                    _searchResults.value = moviesList?.results ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to fetch movies by genre: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clear movie details
     */
    fun clearMovieDetails() {
        _movieDetails.value = null
    }
    
    /**
     * Clear search results
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
    
}