package com.movieapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.model.Movie
import com.movieapp.data.model.MoviesList
import com.movieapp.data.model.MovieDetails
import com.movieapp.data.model.Genre
import com.movieapp.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing movie data and UI state
 * Follows MVVM architecture pattern
 */
class MovieViewModel : ViewModel() {
    
    private val repository = MovieRepository()
    
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
    
    // UI State for movie details
    private val _movieDetails = MutableStateFlow<MovieDetails?>(null)
    val movieDetails: StateFlow<MovieDetails?> = _movieDetails.asStateFlow()
    
    // UI State for genres
    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Fetch popular movies from the API
     */
    fun fetchPopularMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getPopularMovies()
                if (response.isSuccessful) {
                    _popularMovies.value = response.body()?.results ?: emptyList()
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
     * Fetch top rated movies from the API
     */
    fun fetchTopRatedMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getTopRatedMovies()
                if (response.isSuccessful) {
                    _topRatedMovies.value = response.body()?.results ?: emptyList()
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
     * Fetch now playing movies from the API
     */
    fun fetchNowPlayingMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getNowPlayingMovies()
                if (response.isSuccessful) {
                    _nowPlayingMovies.value = response.body()?.results ?: emptyList()
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
     * Fetch upcoming movies from the API
     */
    fun fetchUpcomingMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getUpcomingMovies()
                if (response.isSuccessful) {
                    _upcomingMovies.value = response.body()?.results ?: emptyList()
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
     * Fetch movie details by ID using the new MovieDetails model
     * @param movieId The ID of the movie to fetch
     */
    fun fetchMovieDetails(movieId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getDetailsById(movieId)
                if (response.isSuccessful) {
                    _movieDetails.value = response.body()
                } else {
                    _errorMessage.value = "Failed to fetch movie details: ${response.message()}"
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
                    _searchResults.value = response.body()?.results ?: emptyList()
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
     * Fetch similar movies for a given movie ID
     * @param movieId The movie ID to find similar movies for
     * @param page The page number for pagination
     */
    fun fetchSimilarMovies(movieId: Int, page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.getSimilarMovies(movieId, page)
                if (response.isSuccessful) {
                    _searchResults.value = response.body()?.results ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to fetch similar movies: ${response.message()}"
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
     */
    fun searchMovies(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val response = repository.searchMovies(query)
                if (response.isSuccessful) {
                    _searchResults.value = response.body()?.results ?: emptyList()
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
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}