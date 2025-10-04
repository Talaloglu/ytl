package com.movieapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.model.Movie
import com.movieapp.data.model.MovieDetails
import com.movieapp.data.model.Genre
import com.movieapp.data.repository.SupabaseMovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing movie data and UI state
 * Uses Supabase-first approach with CombinedMovieRepository
 * All movie data comes from Supabase (which contains TMDB metadata)
 */
class MovieViewModel : ViewModel() {
    
    // Repository for movie data - Direct Supabase access
    private val repository = SupabaseMovieRepository()
    
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
     * Fetch popular movies from Supabase (TMDB metadata-rich content)
     */
    fun fetchPopularMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = repository.getPopularMovies(page = 1)
                
                if (result.isSuccess) {
                    _popularMovies.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to load popular movies from Supabase"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch top rated movies from Supabase (TMDB metadata-rich content)
     */
    fun fetchTopRatedMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = repository.getTopRatedMovies(page = 1)
                
                if (result.isSuccess) {
                    _topRatedMovies.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to load top rated movies from Supabase"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch now playing movies from Supabase (TMDB metadata-rich content)
     * Filters for recent movies from the last year
     */
    fun fetchNowPlayingMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Get all movies and filter recent ones
                val result = repository.getAllMovies()
                
                if (result.isSuccess) {
                    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    val recentMovies = result.getOrNull()
                        ?.filter { movie ->
                            val year = movie.releaseDate.take(4).toIntOrNull() ?: 0
                            year >= currentYear - 1 // Movies from last year and this year
                        }
                        ?.sortedByDescending { it.releaseDate }
                        ?.take(20)
                    
                    _nowPlayingMovies.value = recentMovies ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to load now playing movies from Supabase"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch upcoming movies from Supabase (TMDB metadata-rich content)
     * Sorts by release date (newest first)
     */
    fun fetchUpcomingMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Get all movies and sort by release date
                val result = repository.getAllMovies()
                
                if (result.isSuccess) {
                    val upcomingMovies = result.getOrNull()
                        ?.sortedByDescending { it.releaseDate }
                        ?.take(20)
                    
                    _upcomingMovies.value = upcomingMovies ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to load upcoming movies from Supabase"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch movie details by ID
     * Note: This still uses the old approach - consider updating to fetch from Supabase
     * @param movieId The ID of the movie to fetch
     */
    fun fetchMovieDetails(movieId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Get from all movies
                val result = repository.getAllMovies()
                
                if (result.isSuccess) {
                    val movie = result.getOrNull()?.find { it.id == movieId }
                    
                    if (movie != null) {
                        // Convert Movie to MovieDetails
                        _movieDetails.value = MovieDetails(
                            id = movie.id,
                            title = movie.title,
                            overview = movie.overview,
                            posterPath = movie.posterPath,
                            backdropPath = movie.backdropPath,
                            releaseDate = movie.releaseDate,
                            voteAverage = movie.voteAverage,
                            voteCount = movie.voteCount,
                            runtime = 0,
                            genres = emptyList(),
                            productionCompanies = emptyList(),
                            productionCountries = emptyList(),
                            spokenLanguages = emptyList(),
                            budget = 0,
                            revenue = 0,
                            status = "",
                            tagline = "",
                            homepage = "",
                            imdbId = "",
                            originalLanguage = movie.originalLanguage,
                            originalTitle = movie.originalTitle,
                            popularity = movie.popularity,
                            video = movie.video,
                            adult = movie.adult
                        )
                    } else {
                        _errorMessage.value = "Movie not found in database"
                    }
                } else {
                    _errorMessage.value = "Failed to fetch movie details from Supabase"
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
     * Note: Consider getting genres from Supabase metadata
     */
    fun fetchGenres() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Extract unique genres from all movies
                val result = repository.getAllMovies()
                
                if (result.isSuccess) {
                    val allMovies = result.getOrNull() ?: emptyList()
                    val genreIds = allMovies.flatMap { it.genreIds }.distinct()
                    
                    // Create basic Genre objects (would be better to have a genre name mapping)
                    _genres.value = genreIds.map { id ->
                        Genre(id = id, name = "Genre $id") // Placeholder names
                    }
                } else {
                    _errorMessage.value = "Failed to fetch genres from Supabase"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch movies by genre from Supabase
     * @param genreId The genre ID to filter by
     * @param page The page number for pagination
     */
    fun fetchMoviesByGenre(genreId: Int, page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = repository.getMoviesByGenre(genreId, page)
                
                if (result.isSuccess) {
                    _searchResults.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to fetch movies by genre from Supabase"
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
     * Note: Uses genre-based similarity from Supabase
     * @param movieId The movie ID to find similar movies for
     * @param page The page number for pagination
     */
    fun fetchSimilarMovies(movieId: Int, page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Get the movie's genres, then fetch movies with same genres
                val result = repository.getAllMovies()
                
                if (result.isSuccess) {
                    val allMovies = result.getOrNull() ?: emptyList()
                    val targetMovie = allMovies.find { it.id == movieId }
                    
                    if (targetMovie != null) {
                        val similarMovies = allMovies
                            .filter { movie ->
                                movie.id != movieId && // Exclude the target movie
                                movie.genreIds.any { it in targetMovie.genreIds }
                            }
                            .sortedByDescending { it.popularity }
                            .take(20)
                        
                        _searchResults.value = similarMovies
                    } else {
                        _searchResults.value = emptyList()
                    }
                } else {
                    _errorMessage.value = "Failed to fetch similar movies from Supabase"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Search movies by query in Supabase (TMDB metadata-rich content)
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
                // Get all movies and filter by title
                val result = repository.getAllMovies()
                
                if (result.isSuccess) {
                    val queryLower = query.lowercase()
                    val searchResults = result.getOrNull()
                        ?.filter { movie ->
                            movie.title.lowercase().contains(queryLower) ||
                            movie.originalTitle.lowercase().contains(queryLower)
                        }
                        ?.sortedByDescending { it.popularity }
                        ?.take(50)
                    
                    _searchResults.value = searchResults ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to search movies in Supabase"
                    _searchResults.value = emptyList()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                _searchResults.value = emptyList()
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
