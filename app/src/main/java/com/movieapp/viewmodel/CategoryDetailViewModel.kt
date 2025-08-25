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
 * ViewModel for Category Detail Screen
 * Manages movie lists for specific categories with pagination support
 * Now handles CombinedMovie objects to ensure streaming availability
 */
class CategoryDetailViewModel : ViewModel() {
    
    private val repository = CombinedMovieRepository()
    
    // UI State - updated to use CombinedMovie
    private val _movies = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val movies: StateFlow<List<CombinedMovie>> = _movies.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Pagination state
    private var currentPage = 1
    private var currentCategoryType = ""
    private var isCurrentlyLoading = false
    
    // Genre constants (same as in CategorizedHomeViewModel)
    companion object {
        const val GENRE_ACTION = 28
        const val GENRE_COMEDY = 35
        const val GENRE_DRAMA = 18
        const val GENRE_HORROR = 27
        const val GENRE_ROMANCE = 10749
        const val GENRE_THRILLER = 53
        const val GENRE_SCIENCE_FICTION = 878
        const val GENRE_FANTASY = 14
    }
    
    /**
     * Load movies based on category type
     */
    fun loadMovies(categoryType: String) {
        if (categoryType == currentCategoryType) return // Already loaded
        
        currentCategoryType = categoryType
        currentPage = 1
        _hasMorePages.value = true
        _movies.value = emptyList()
        
        loadMoviesInternal(categoryType, currentPage, isInitialLoad = true)
    }
    
    /**
     * Load next page of movies
     */
    fun loadNextPage() {
        if (!_hasMorePages.value || isCurrentlyLoading) return
        
        currentPage++
        loadMoviesInternal(currentCategoryType, currentPage, isInitialLoad = false)
    }
    
    /**
     * Internal method to load movies based on category and page
     */
    private fun loadMoviesInternal(categoryType: String, page: Int, isInitialLoad: Boolean) {
        if (isCurrentlyLoading) return
        
        viewModelScope.launch {
            try {
                isCurrentlyLoading = true
                
                if (isInitialLoad) {
                    _isLoading.value = true
                } else {
                    _isLoadingMore.value = true
                }
                
                _errorMessage.value = null
                
                val result = when (categoryType) {
                    "trending" -> repository.getTrendingMovies("week")
                    "popular" -> repository.getPopularMovies(page)
                    "top_rated" -> repository.getTopRatedMovies(page)
                    "year_2024" -> repository.getMoviesByYear(2024, page)
                    "year_2023" -> repository.getMoviesByYear(2023, page)
                    "year_2022" -> repository.getMoviesByYear(2022, page)
                    "year_2021" -> repository.getMoviesByYear(2021, page)
                    "genre_horror" -> repository.getMoviesByGenre(GENRE_HORROR, page)
                    "genre_action" -> repository.getMoviesByGenre(GENRE_ACTION, page)
                    "genre_comedy" -> repository.getMoviesByGenre(GENRE_COMEDY, page)
                    "genre_drama" -> repository.getMoviesByGenre(GENRE_DRAMA, page)
                    "genre_romance" -> repository.getMoviesByGenre(GENRE_ROMANCE, page)
                    "genre_thriller" -> repository.getMoviesByGenre(GENRE_THRILLER, page)
                    "genre_scifi" -> repository.getMoviesByGenre(GENRE_SCIENCE_FICTION, page)
                    "genre_fantasy" -> repository.getMoviesByGenre(GENRE_FANTASY, page)
                    else -> {
                        _errorMessage.value = "Unknown category type: $categoryType"
                        return@launch
                    }
                }
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (isInitialLoad) {
                            _movies.value = newMovies
                        } else {
                            val currentMovies = _movies.value.toMutableList()
                            currentMovies.addAll(newMovies)
                            _movies.value = currentMovies
                        }
                        
                        // Check if we have more pages (if we got fewer than 20 movies, probably no more)
                        _hasMorePages.value = newMovies.size >= 20
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to load movies"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
                _isLoadingMore.value = false
                isCurrentlyLoading = false
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
     * Refresh the current category
     */
    fun refreshMovies() {
        if (currentCategoryType.isNotEmpty()) {
            currentPage = 1
            _hasMorePages.value = true
            _movies.value = emptyList()
            loadMoviesInternal(currentCategoryType, currentPage, isInitialLoad = true)
        }
    }
}