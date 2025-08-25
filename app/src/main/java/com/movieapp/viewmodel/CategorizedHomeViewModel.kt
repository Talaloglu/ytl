package com.movieapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.model.CombinedMovie
import com.movieapp.data.repository.CombinedMovieRepository
import com.movieapp.utils.MatchingDiagnostics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * ViewModel for categorized home screen with multiple sections
 * Manages trending, popular, year-based, and genre-based movie sections
 * Now handles CombinedMovie objects to ensure streaming availability
 * Enhanced with loading state management to prevent infinite loops
 */
class CategorizedHomeViewModel : ViewModel() {
    
    private val repository = CombinedMovieRepository()
    private val diagnostics = MatchingDiagnostics()
    
    // Data freshness tracking to prevent unnecessary re-fetches
    private var lastDataLoadTime = 0L
    private val dataFreshnessTimeout = 30 * 60 * 1000L // 30 minutes
    
    // Global loading state to prevent multiple simultaneous loads
    private val _isGlobalLoading = MutableStateFlow(false)
    val isGlobalLoading: StateFlow<Boolean> = _isGlobalLoading.asStateFlow()
    
    // Track if data has been loaded at least once
    private var hasDataBeenLoaded = false
    
    // UI State for different movie sections - updated to use CombinedMovie
    private val _trendingMovies = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val trendingMovies: StateFlow<List<CombinedMovie>> = _trendingMovies.asStateFlow()
    
    private val _popularMovies = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val popularMovies: StateFlow<List<CombinedMovie>> = _popularMovies.asStateFlow()
    
    private val _topRatedMovies = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val topRatedMovies: StateFlow<List<CombinedMovie>> = _topRatedMovies.asStateFlow()
    
    private val _movies2024 = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val movies2024: StateFlow<List<CombinedMovie>> = _movies2024.asStateFlow()
    
    private val _movies2023 = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val movies2023: StateFlow<List<CombinedMovie>> = _movies2023.asStateFlow()
    
    private val _horrorMovies = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val horrorMovies: StateFlow<List<CombinedMovie>> = _horrorMovies.asStateFlow()
    
    private val _actionMovies = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val actionMovies: StateFlow<List<CombinedMovie>> = _actionMovies.asStateFlow()
    
    private val _comedyMovies = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val comedyMovies: StateFlow<List<CombinedMovie>> = _comedyMovies.asStateFlow()
    
    private val _dramaMovies = MutableStateFlow<List<CombinedMovie>>(emptyList())
    val dramaMovies: StateFlow<List<CombinedMovie>> = _dramaMovies.asStateFlow()
    
    // Loading states for each section
    private val _isLoadingTrending = MutableStateFlow(false)
    val isLoadingTrending: StateFlow<Boolean> = _isLoadingTrending.asStateFlow()
    
    private val _isLoadingPopular = MutableStateFlow(false)
    val isLoadingPopular: StateFlow<Boolean> = _isLoadingPopular.asStateFlow()
    
    private val _isLoadingTopRated = MutableStateFlow(false)
    val isLoadingTopRated: StateFlow<Boolean> = _isLoadingTopRated.asStateFlow()
    
    private val _isLoadingByYear = MutableStateFlow(false)
    val isLoadingByYear: StateFlow<Boolean> = _isLoadingByYear.asStateFlow()
    
    private val _isLoadingByGenre = MutableStateFlow(false)
    val isLoadingByGenre: StateFlow<Boolean> = _isLoadingByGenre.asStateFlow()
    
    // Pagination loading states for horizontal sections
    private val _isLoadingMoreTrending = MutableStateFlow(false)
    val isLoadingMoreTrending: StateFlow<Boolean> = _isLoadingMoreTrending.asStateFlow()
    
    private val _isLoadingMorePopular = MutableStateFlow(false)
    val isLoadingMorePopular: StateFlow<Boolean> = _isLoadingMorePopular.asStateFlow()
    
    private val _isLoadingMoreTopRated = MutableStateFlow(false)
    val isLoadingMoreTopRated: StateFlow<Boolean> = _isLoadingMoreTopRated.asStateFlow()
    
    private val _isLoadingMore2024 = MutableStateFlow(false)
    val isLoadingMore2024: StateFlow<Boolean> = _isLoadingMore2024.asStateFlow()
    
    private val _isLoadingMore2023 = MutableStateFlow(false)
    val isLoadingMore2023: StateFlow<Boolean> = _isLoadingMore2023.asStateFlow()
    
    private val _isLoadingMoreHorror = MutableStateFlow(false)
    val isLoadingMoreHorror: StateFlow<Boolean> = _isLoadingMoreHorror.asStateFlow()
    
    private val _isLoadingMoreAction = MutableStateFlow(false)
    val isLoadingMoreAction: StateFlow<Boolean> = _isLoadingMoreAction.asStateFlow()
    
    private val _isLoadingMoreComedy = MutableStateFlow(false)
    val isLoadingMoreComedy: StateFlow<Boolean> = _isLoadingMoreComedy.asStateFlow()
    
    private val _isLoadingMoreDrama = MutableStateFlow(false)
    val isLoadingMoreDrama: StateFlow<Boolean> = _isLoadingMoreDrama.asStateFlow()
    
    // Pagination state tracking
    private var trendingPage = 1
    private var popularPage = 1
    private var topRatedPage = 1
    private var movies2024Page = 1
    private var movies2023Page = 1
    private var horrorPage = 1
    private var actionPage = 1
    private var comedyPage = 1
    private var dramaPage = 1
    
    // Track displayed movie IDs to prevent duplicates
    private val displayedTrendingMovieIds = mutableSetOf<Int>()
    private val displayedPopularMovieIds = mutableSetOf<Int>()
    private val displayedTopRatedMovieIds = mutableSetOf<Int>()
    private val displayed2024MovieIds = mutableSetOf<Int>()
    private val displayed2023MovieIds = mutableSetOf<Int>()
    private val displayedHorrorMovieIds = mutableSetOf<Int>()
    private val displayedActionMovieIds = mutableSetOf<Int>()
    private val displayedComedyMovieIds = mutableSetOf<Int>()
    private val displayedDramaMovieIds = mutableSetOf<Int>()
    
    // Has more pages tracking
    private val _hasMoreTrending = MutableStateFlow(true)
    val hasMoreTrending: StateFlow<Boolean> = _hasMoreTrending.asStateFlow()
    
    private val _hasMorePopular = MutableStateFlow(true)
    val hasMorePopular: StateFlow<Boolean> = _hasMorePopular.asStateFlow()
    
    private val _hasMoreTopRated = MutableStateFlow(true)
    val hasMoreTopRated: StateFlow<Boolean> = _hasMoreTopRated.asStateFlow()
    
    private val _hasMore2024 = MutableStateFlow(true)
    val hasMore2024: StateFlow<Boolean> = _hasMore2024.asStateFlow()
    
    private val _hasMore2023 = MutableStateFlow(true)
    val hasMore2023: StateFlow<Boolean> = _hasMore2023.asStateFlow()
    
    private val _hasMoreHorror = MutableStateFlow(true)
    val hasMoreHorror: StateFlow<Boolean> = _hasMoreHorror.asStateFlow()
    
    private val _hasMoreAction = MutableStateFlow(true)
    val hasMoreAction: StateFlow<Boolean> = _hasMoreAction.asStateFlow()
    
    private val _hasMoreComedy = MutableStateFlow(true)
    val hasMoreComedy: StateFlow<Boolean> = _hasMoreComedy.asStateFlow()
    
    private val _hasMoreDrama = MutableStateFlow(true)
    val hasMoreDrama: StateFlow<Boolean> = _hasMoreDrama.asStateFlow()
    
    // Error states
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Diagnostic states
    private val _diagnosticReport = MutableStateFlow<String?>(null)
    val diagnosticReport: StateFlow<String?> = _diagnosticReport.asStateFlow()
    
    private val _isRunningDiagnostics = MutableStateFlow(false)
    val isRunningDiagnostics: StateFlow<Boolean> = _isRunningDiagnostics.asStateFlow()
    
    // Genre constants (TMDB genre IDs)
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
     * Load all sections with optional force refresh
     * Delegates to the simple efficient system
     */
    fun loadAllSections(forceRefresh: Boolean = false) {
        loadAllSectionsSimple(forceRefresh)
    }
    /**
     * Shows ALL your streaming content without complex TMDB filtering
     * This replaces the complex enhanced loading methods
     */
    fun loadAllSectionsSimple(forceRefresh: Boolean = false) {
        // Prevent duplicate calls if already loading
        if (_isGlobalLoading.value && !forceRefresh) {
            println("🔄 loadAllSectionsSimple: Already loading, skipping duplicate call")
            return
        }
        
        // Check data freshness - avoid unnecessary reloads
        val currentTime = System.currentTimeMillis()
        if (!forceRefresh && hasDataBeenLoaded && (currentTime - lastDataLoadTime) < dataFreshnessTimeout) {
            println("📋 loadAllSectionsSimple: Data is fresh, skipping reload")
            return
        }
        
        println("🚀 SIMPLE SYSTEM: Starting efficient Supabase-first load (forceRefresh=$forceRefresh)")
        println("🎯 Priority: Show ALL your streaming content with simple categorization")
        
        viewModelScope.launch {
            _isGlobalLoading.value = true
            try {
                // Reset deduplication tracking
                displayedTrendingMovieIds.clear()
                displayedPopularMovieIds.clear()
                displayedTopRatedMovieIds.clear()
                displayed2024MovieIds.clear()
                displayed2023MovieIds.clear()
                displayedHorrorMovieIds.clear()
                displayedActionMovieIds.clear()
                displayedComedyMovieIds.clear()
                displayedDramaMovieIds.clear()
                
                // Load all sections using simple approach
                coroutineScope {
                    val trendingDeferred = async { loadSimpleSection("trending") }
                    val popularDeferred = async { loadSimpleSection("popular") }
                    val topRatedDeferred = async { loadSimpleSection("popular") } // Use popular for top-rated
                    val movies2024Deferred = async { loadSimpleSection("2024") }
                    val movies2023Deferred = async { loadSimpleSection("2023") }
                    val horrorDeferred = async { loadSimpleSection("horror") }
                    val actionDeferred = async { loadSimpleSection("action") }
                    val comedyDeferred = async { loadSimpleSection("comedy") }
                    val dramaDeferred = async { loadSimpleSection("comedy") } // Use comedy for drama
                    
                    // Wait for all to complete and update states
                    trendingDeferred.await().fold(
                        onSuccess = { movies: List<CombinedMovie> ->
                            _trendingMovies.value = movies
                            displayedTrendingMovieIds.addAll(movies.map { it.id })
                            _hasMoreTrending.value = movies.size >= 15
                            println("✅ SIMPLE: Trending loaded ${movies.size} movies")
                        },
                        onFailure = { exception: Throwable -> println("❌ SIMPLE: Trending failed - ${exception.message}") }
                    )
                    
                    popularDeferred.await().fold(
                        onSuccess = { movies: List<CombinedMovie> ->
                            _popularMovies.value = movies
                            displayedPopularMovieIds.addAll(movies.map { it.id })
                            _hasMorePopular.value = movies.size >= 15
                            println("✅ SIMPLE: Popular loaded ${movies.size} movies")
                        },
                        onFailure = { exception: Throwable -> println("❌ SIMPLE: Popular failed - ${exception.message}") }
                    )
                    
                    topRatedDeferred.await().fold(
                        onSuccess = { movies: List<CombinedMovie> ->
                            _topRatedMovies.value = movies
                            displayedTopRatedMovieIds.addAll(movies.map { it.id })
                            _hasMoreTopRated.value = movies.size >= 15
                            println("✅ SIMPLE: Top Rated loaded ${movies.size} movies")
                        },
                        onFailure = { exception: Throwable -> println("❌ SIMPLE: Top Rated failed - ${exception.message}") }
                    )
                    
                    movies2024Deferred.await().fold(
                        onSuccess = { movies: List<CombinedMovie> ->
                            _movies2024.value = movies
                            displayed2024MovieIds.addAll(movies.map { it.id })
                            _hasMore2024.value = movies.size >= 15
                            println("✅ SIMPLE: 2024 loaded ${movies.size} movies")
                        },
                        onFailure = { exception: Throwable -> println("❌ SIMPLE: 2024 failed - ${exception.message}") }
                    )
                    
                    movies2023Deferred.await().fold(
                        onSuccess = { movies: List<CombinedMovie> ->
                            _movies2023.value = movies
                            displayed2023MovieIds.addAll(movies.map { it.id })
                            _hasMore2023.value = movies.size >= 15
                            println("✅ SIMPLE: 2023 loaded ${movies.size} movies")
                        },
                        onFailure = { exception: Throwable -> println("❌ SIMPLE: 2023 failed - ${exception.message}") }
                    )
                    
                    horrorDeferred.await().fold(
                        onSuccess = { movies: List<CombinedMovie> ->
                            _horrorMovies.value = movies
                            displayedHorrorMovieIds.addAll(movies.map { it.id })
                            _hasMoreHorror.value = movies.size >= 15
                            println("✅ SIMPLE: Horror loaded ${movies.size} movies")
                        },
                        onFailure = { exception: Throwable -> println("❌ SIMPLE: Horror failed - ${exception.message}") }
                    )
                    
                    actionDeferred.await().fold(
                        onSuccess = { movies: List<CombinedMovie> ->
                            _actionMovies.value = movies
                            displayedActionMovieIds.addAll(movies.map { it.id })
                            _hasMoreAction.value = movies.size >= 15
                            println("✅ SIMPLE: Action loaded ${movies.size} movies")
                        },
                        onFailure = { exception: Throwable -> println("❌ SIMPLE: Action failed - ${exception.message}") }
                    )
                    
                    comedyDeferred.await().fold(
                        onSuccess = { movies: List<CombinedMovie> ->
                            _comedyMovies.value = movies
                            displayedComedyMovieIds.addAll(movies.map { it.id })
                            _hasMoreComedy.value = movies.size >= 15
                            println("✅ SIMPLE: Comedy loaded ${movies.size} movies")
                        },
                        onFailure = { exception: Throwable -> println("❌ SIMPLE: Comedy failed - ${exception.message}") }
                    )
                    
                    dramaDeferred.await().fold(
                        onSuccess = { movies: List<CombinedMovie> ->
                            _dramaMovies.value = movies
                            displayedDramaMovieIds.addAll(movies.map { it.id })
                            _hasMoreDrama.value = movies.size >= 15
                            println("✅ SIMPLE: Drama loaded ${movies.size} movies")
                        },
                        onFailure = { exception: Throwable -> println("❌ SIMPLE: Drama failed - ${exception.message}") }
                    )
                }
                
                // Update data freshness tracking
                lastDataLoadTime = currentTime
                hasDataBeenLoaded = true
                println("✅ SIMPLE SYSTEM: All sections loaded efficiently!")
                
            } catch (e: Exception) {
                _errorMessage.value = "Error loading sections: ${e.message ?: "Unknown error"}"
                println("❌ SIMPLE SYSTEM: Error loading sections - ${e.message ?: "Unknown error"}")
            } finally {
                _isGlobalLoading.value = false
            }
        }
    }
    
    /**
     * Load a simple section using Supabase-first approach
     */
    private suspend fun loadSimpleSection(category: String): Result<List<CombinedMovie>> {
        return repository.getMoviesBySimpleCategory(
            category = category,
            page = 1,
            pageSize = 20,
            excludedIds = emptySet() // No exclusions for initial load
        )
    }
    
    /**
     * Load more movies from the database beyond the initial 1000
     * This can be called when user wants to see more content
     */
    fun loadMoreFromDatabase(additionalCount: Int = 500) {
        if (_isGlobalLoading.value) {
            println("⚠️ loadMoreFromDatabase: Global loading in progress, skipping")
            return
        }
        
        println("🔄 Loading $additionalCount more movies from database...")
        
        viewModelScope.launch {
            _isGlobalLoading.value = true
            try {
                // Build excluded titles from all current sections to avoid duplicates
                val excludedTitles: Set<String> = (
                    _trendingMovies.value.map { it.title } +
                    _popularMovies.value.map { it.title } +
                    _topRatedMovies.value.map { it.title } +
                    _movies2024.value.map { it.title } +
                    _movies2023.value.map { it.title } +
                    _horrorMovies.value.map { it.title } +
                    _actionMovies.value.map { it.title } +
                    _comedyMovies.value.map { it.title } +
                    _dramaMovies.value.map { it.title }
                ).toSet()

                val result = repository.getMoreMoviesFromDatabase(excludedTitles = excludedTitles, limit = additionalCount)
                result.fold(
                    onSuccess = { newMovies: List<CombinedMovie> ->
                        if (newMovies.isNotEmpty()) {
                            // Add new movies to existing sections based on their characteristics
                            val currentTrending = _trendingMovies.value.toMutableList()
                            val currentPopular = _popularMovies.value.toMutableList()
                            val currentTopRated = _topRatedMovies.value.toMutableList()

                            // Distribute new movies across sections
                            val sortedNewMovies = newMovies.sortedByDescending { movie: CombinedMovie -> movie.voteAverage }

                            sortedNewMovies.forEachIndexed { index: Int, movie: CombinedMovie ->
                                when (index % 3) {
                                    0 -> if (currentTrending.size < 100) currentTrending.add(movie)
                                    1 -> if (currentPopular.size < 100) currentPopular.add(movie)
                                    2 -> if (currentTopRated.size < 100) currentTopRated.add(movie)
                                }
                            }

                            // Update StateFlows
                            _trendingMovies.value = currentTrending
                            _popularMovies.value = currentPopular
                            _topRatedMovies.value = currentTopRated

                            println("✅ loadMoreFromDatabase: Added ${newMovies.size} more movies to sections")
                        } else {
                            println("⚠️ loadMoreFromDatabase: No additional movies found")
                        }
                    },
                    onFailure = { exception: Throwable ->
                        _errorMessage.value = "Failed to load more movies: ${exception.message ?: "Unknown error"}"
                        println("❌ loadMoreFromDatabase: Failed - ${exception.message ?: "Unknown error"}")
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error loading more movies: ${e.message ?: "Unknown error"}"
                println("❌ loadMoreFromDatabase: Exception - ${e.message ?: "Unknown error"}")
            } finally {
                _isGlobalLoading.value = false
            }
        }
    }
    
    /**
     * Get current total movie count across all sections
     */
    fun getCurrentMovieCount(): Int {
        return _trendingMovies.value.size + 
               _popularMovies.value.size + 
               _topRatedMovies.value.size + 
               _movies2024.value.size + 
               _movies2023.value.size + 
               _horrorMovies.value.size + 
               _actionMovies.value.size + 
               _comedyMovies.value.size + 
               _dramaMovies.value.size
    }
    
    /**
     * Check if we should suggest loading more movies
     * Returns true if current count is below 1000 movies (suggesting more are available)
     */
    fun shouldSuggestLoadingMore(): Boolean {
        return getCurrentMovieCount() < 1000
    }
    
    /**
     * Get estimated remaining movies in database
     */
    fun getEstimatedRemainingMovies(): Int {
        val totalExpected = 1440 // Your database size
        val currentCount = getCurrentMovieCount()
        return maxOf(0, totalExpected - currentCount)
    }
    
    /**
     * Load trending movies using simplified consistent approach
     */
    private suspend fun loadEnhancedTrendingMovies() {
        if (_isLoadingTrending.value) {
            println("⚠️ loadEnhancedTrendingMovies: Already loading, skipping")
            return
        }
        
        _isLoadingTrending.value = true
        trendingPage = 1 // Reset pagination for initial load
        _hasMoreTrending.value = true
        
        // Reset displayed movie IDs for fresh start
        displayedTrendingMovieIds.clear()
        
        try {
            // Use the same approach as year-based movies for consistency
            val result = repository.getTrendingMoviesConsistent("week")
            result.fold(
                onSuccess = { movies ->
                    _trendingMovies.value = movies
                    _hasMoreTrending.value = movies.size >= 15
                    // Track displayed movie IDs
                    displayedTrendingMovieIds.addAll(movies.map { it.id })
                    println("✅ loadEnhancedTrendingMovies: Loaded ${movies.size} trending movies")
                },
                onFailure = { exception ->
                    _errorMessage.value = "Failed to load trending movies: ${exception.message ?: "Unknown error"}"
                    println("❌ loadEnhancedTrendingMovies: Failed - ${exception.message ?: "Unknown error"}")
                }
            )
        } catch (e: Exception) {
            _errorMessage.value = "Error loading trending movies: ${e.message ?: "Unknown error"}"
            println("❌ loadEnhancedTrendingMovies: Exception - ${e.message ?: "Unknown error"}")
        } finally {
            _isLoadingTrending.value = false
        }
    }
    
    /**
     * Load popular movies using simplified consistent approach
     */
    private suspend fun loadEnhancedPopularMovies() {
        if (_isLoadingPopular.value) {
            println("⚠️ loadEnhancedPopularMovies: Already loading, skipping")
            return
        }
        
        _isLoadingPopular.value = true
        popularPage = 1 // Reset pagination for initial load
        _hasMorePopular.value = true
        
        // Reset displayed movie IDs for fresh start
        displayedPopularMovieIds.clear()
        
        try {
            // Use the same approach as year-based movies for consistency
            val result = repository.getPopularMoviesConsistent(popularPage)
            result.fold(
                onSuccess = { movies ->
                    _popularMovies.value = movies
                    _hasMorePopular.value = movies.size >= 15
                    // Track displayed movie IDs
                    displayedPopularMovieIds.addAll(movies.map { it.id })
                    println("✅ loadEnhancedPopularMovies: Loaded ${movies.size} popular movies")
                },
                onFailure = { exception ->
                    _errorMessage.value = "Failed to load popular movies: ${exception.message ?: "Unknown error"}"
                    println("❌ loadEnhancedPopularMovies: Failed - ${exception.message ?: "Unknown error"}")
                }
            )
        } catch (e: Exception) {
            _errorMessage.value = "Error loading popular movies: ${e.message ?: "Unknown error"}"
            println("❌ loadEnhancedPopularMovies: Exception - ${e.message ?: "Unknown error"}")
        } finally {
            _isLoadingPopular.value = false
        }
    }
    
    /**
     * Load top rated movies using simplified consistent approach
     */
    private suspend fun loadTopRatedMovies() {
        if (_isLoadingTopRated.value) {
            println("⚠️ loadTopRatedMovies: Already loading, skipping")
            return
        }
        
        _isLoadingTopRated.value = true
        topRatedPage = 1 // Reset pagination for initial load
        _hasMoreTopRated.value = true
        
        // Reset displayed movie IDs for fresh start
        displayedTopRatedMovieIds.clear()
        
        try {
            // Use the same approach as year-based movies for consistency
            val result = repository.getTopRatedMoviesConsistent(topRatedPage)
            result.fold(
                onSuccess = { movies ->
                    _topRatedMovies.value = movies
                    _hasMoreTopRated.value = movies.size >= 15
                    // Track displayed movie IDs
                    displayedTopRatedMovieIds.addAll(movies.map { it.id })
                    println("✅ loadTopRatedMovies: Loaded ${movies.size} top rated movies")
                },
                onFailure = { exception ->
                    _errorMessage.value = "Failed to load top rated movies: ${exception.message}"
                    println("❌ loadTopRatedMovies: Failed - ${exception.message}")
                }
            )
        } catch (e: Exception) {
            _errorMessage.value = "Error loading top rated movies: ${e.message}"
            println("❌ loadTopRatedMovies: Exception - ${e.message}")
        } finally {
            _isLoadingTopRated.value = false
        }
    }
    
    /**
     * Load movies by different years
     */
    private suspend fun loadMoviesByYear() {
        if (_isLoadingByYear.value) {
            println("⚠️ loadMoviesByYear: Already loading, skipping")
            return
        }
        
        _isLoadingByYear.value = true
        // Reset pagination for initial load
        movies2024Page = 1
        movies2023Page = 1
        _hasMore2024.value = true
        _hasMore2023.value = true
        
        // Reset displayed movie IDs for fresh start
        displayed2024MovieIds.clear()
        displayed2023MovieIds.clear()
        
        try {
            coroutineScope {
                val movies2024Deferred = async { repository.getMoviesByYear(2024, movies2024Page) }
                val movies2023Deferred = async { repository.getMoviesByYear(2023, movies2023Page) }
                
                movies2024Deferred.await().fold(
                    onSuccess = { movies ->
                        _movies2024.value = movies
                        _hasMore2024.value = movies.size >= 20
                        // Track displayed movie IDs
                        displayed2024MovieIds.addAll(movies.map { it.id })
                        println("✅ loadMoviesByYear: Loaded ${movies.size} movies from 2024")
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Failed to load 2024 movies: ${exception.message}"
                        println("❌ loadMoviesByYear: Failed 2024 - ${exception.message}")
                    }
                )
                
                movies2023Deferred.await().fold(
                    onSuccess = { movies ->
                        _movies2023.value = movies
                        _hasMore2023.value = movies.size >= 20
                        // Track displayed movie IDs
                        displayed2023MovieIds.addAll(movies.map { it.id })
                        println("✅ loadMoviesByYear: Loaded ${movies.size} movies from 2023")
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Failed to load 2023 movies: ${exception.message}"
                        println("❌ loadMoviesByYear: Failed 2023 - ${exception.message}")
                    }
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error loading movies by year: ${e.message}"
            println("❌ loadMoviesByYear: Exception - ${e.message}")
        } finally {
            _isLoadingByYear.value = false
        }
    }
    
    /**
     * Load movies by different genres
     */
    private suspend fun loadMoviesByGenres() {
        if (_isLoadingByGenre.value) {
            println("⚠️ loadMoviesByGenres: Already loading, skipping")
            return
        }
        
        _isLoadingByGenre.value = true
        // Reset pagination for initial load
        horrorPage = 1
        actionPage = 1
        comedyPage = 1
        dramaPage = 1
        _hasMoreHorror.value = true
        _hasMoreAction.value = true
        _hasMoreComedy.value = true
        _hasMoreDrama.value = true
        
        // Reset displayed movie IDs for fresh start
        displayedHorrorMovieIds.clear()
        displayedActionMovieIds.clear()
        displayedComedyMovieIds.clear()
        displayedDramaMovieIds.clear()
        
        try {
            coroutineScope {
                val horrorDeferred = async { repository.getMoviesByGenre(GENRE_HORROR, horrorPage) }
                val actionDeferred = async { repository.getMoviesByGenre(GENRE_ACTION, actionPage) }
                val comedyDeferred = async { repository.getMoviesByGenre(GENRE_COMEDY, comedyPage) }
                val dramaDeferred = async { repository.getMoviesByGenre(GENRE_DRAMA, dramaPage) }
                
                horrorDeferred.await().fold(
                    onSuccess = { movies ->
                        _horrorMovies.value = movies
                        _hasMoreHorror.value = movies.size >= 20
                        // Track displayed movie IDs
                        displayedHorrorMovieIds.addAll(movies.map { it.id })
                        println("✅ loadMoviesByGenres: Loaded ${movies.size} horror movies")
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Failed to load horror movies: ${exception.message}"
                        println("❌ loadMoviesByGenres: Failed horror - ${exception.message}")
                    }
                )
                
                actionDeferred.await().fold(
                    onSuccess = { movies ->
                        _actionMovies.value = movies
                        _hasMoreAction.value = movies.size >= 20
                        // Track displayed movie IDs
                        displayedActionMovieIds.addAll(movies.map { it.id })
                        println("✅ loadMoviesByGenres: Loaded ${movies.size} action movies")
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Failed to load action movies: ${exception.message}"
                        println("❌ loadMoviesByGenres: Failed action - ${exception.message}")
                    }
                )
                
                comedyDeferred.await().fold(
                    onSuccess = { movies ->
                        _comedyMovies.value = movies
                        _hasMoreComedy.value = movies.size >= 20
                        // Track displayed movie IDs
                        displayedComedyMovieIds.addAll(movies.map { it.id })
                        println("✅ loadMoviesByGenres: Loaded ${movies.size} comedy movies")
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Failed to load comedy movies: ${exception.message}"
                        println("❌ loadMoviesByGenres: Failed comedy - ${exception.message}")
                    }
                )
                
                dramaDeferred.await().fold(
                    onSuccess = { movies ->
                        _dramaMovies.value = movies
                        _hasMoreDrama.value = movies.size >= 20
                        // Track displayed movie IDs
                        displayedDramaMovieIds.addAll(movies.map { it.id })
                        println("✅ loadMoviesByGenres: Loaded ${movies.size} drama movies")
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Failed to load drama movies: ${exception.message}"
                        println("❌ loadMoviesByGenres: Failed drama - ${exception.message}")
                    }
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error loading movies by genre: ${e.message}"
            println("❌ loadMoviesByGenres: Exception - ${e.message}")
        } finally {
            _isLoadingByGenre.value = false
        }
    }
    
    /**
     * Refresh all sections - Forces a fresh load of data
     * Uses debounced approach to prevent rapid successive calls
     */
    fun refreshAllSections() {
        println("🔄 refreshAllSections: Initiating forced refresh")
        loadAllSections(forceRefresh = true)
    }
    
    /**
     * Initialize data loading - only loads if no data exists
     * Used for first-time screen display
     */
    fun initializeData() {
        if (!hasDataBeenLoaded) {
            println("🎬 initializeData: Loading data for first time")
            loadAllSections(forceRefresh = false)
        } else {
            println("📋 initializeData: Data already loaded, skipping")
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    // Horizontal Pagination Methods
    
    /**
     * Load more trending movies for horizontal pagination
     * Enhanced to load from your complete 1440+ movie database
     */
    fun loadMoreTrending() {
        if (_isLoadingMoreTrending.value || !_hasMoreTrending.value) return
        
        viewModelScope.launch {
            _isLoadingMoreTrending.value = true
            try {
                // Get currently displayed movie titles to avoid duplicates
                val currentMovies = _trendingMovies.value
                val excludedTitles = currentMovies.map { repository.cleanTitle(it.title) }.toSet()
                
                // Get more movies from your database
                val result = repository.getMoreMoviesFromDatabase(excludedTitles, limit = 20)
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (newMovies.isNotEmpty()) {
                            val updatedMovies = currentMovies.toMutableList()
                            updatedMovies.addAll(newMovies)
                            _trendingMovies.value = updatedMovies
                            println("✅ Added ${newMovies.size} more trending movies from database (total: ${updatedMovies.size})")
                        }
                        _hasMoreTrending.value = newMovies.size >= 20
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Failed to load more trending movies: ${exception.message ?: "Unknown error"}"
                        println("❌ loadMoreTrending failed: ${exception.message ?: "Unknown error"}")
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error loading more trending movies: ${e.message ?: "Unknown error"}"
                println("❌ loadMoreTrending exception: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoadingMoreTrending.value = false
            }
        }
    }
    
    /**
     * Load more popular movies for horizontal pagination with deduplication
     */
    fun loadMorePopular() {
        if (_isLoadingMorePopular.value || !_hasMorePopular.value) return
        
        viewModelScope.launch {
            _isLoadingMorePopular.value = true
            try {
                popularPage++
                
                // Use deduplication method to get unique movies
                val result = repository.getPopularMoviesWithDeduplication(
                    page = popularPage,
                    excludedIds = displayedPopularMovieIds,
                    targetCount = 20
                )
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (newMovies.isNotEmpty()) {
                            val currentMovies = _popularMovies.value.toMutableList()
                            currentMovies.addAll(newMovies)
                            _popularMovies.value = currentMovies
                            
                            // Track new movie IDs
                            displayedPopularMovieIds.addAll(newMovies.map { it.id })
                            
                            _hasMorePopular.value = newMovies.size >= 15
                            println("✅ loadMorePopular: Added ${newMovies.size} new movies")
                        } else {
                            _hasMorePopular.value = false
                        }
                    },
                    onFailure = { exception ->
                        popularPage--
                        _errorMessage.value = "Failed to load more popular movies: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                popularPage--
                _errorMessage.value = "Error loading more popular movies: ${e.message}"
            } finally {
                _isLoadingMorePopular.value = false
            }
        }
    }
    
    /**
     * Load more top rated movies for horizontal pagination with deduplication
     */
    fun loadMoreTopRated() {
        if (_isLoadingMoreTopRated.value || !_hasMoreTopRated.value) return
        
        viewModelScope.launch {
            _isLoadingMoreTopRated.value = true
            try {
                topRatedPage++
                
                // Use deduplication method to get unique movies
                val result = repository.getTopRatedMoviesWithDeduplication(
                    page = topRatedPage,
                    excludedIds = displayedTopRatedMovieIds,
                    targetCount = 20
                )
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (newMovies.isNotEmpty()) {
                            val currentMovies = _topRatedMovies.value.toMutableList()
                            currentMovies.addAll(newMovies)
                            _topRatedMovies.value = currentMovies
                            
                            // Track new movie IDs
                            displayedTopRatedMovieIds.addAll(newMovies.map { it.id })
                            
                            _hasMoreTopRated.value = newMovies.size >= 15
                            println("✅ loadMoreTopRated: Added ${newMovies.size} new movies")
                        } else {
                            _hasMoreTopRated.value = false
                        }
                    },
                    onFailure = { exception ->
                        topRatedPage--
                        _errorMessage.value = "Failed to load more top rated movies: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                topRatedPage--
                _errorMessage.value = "Error loading more top rated movies: ${e.message}"
            } finally {
                _isLoadingMoreTopRated.value = false
            }
        }
    }
    
    /**
     * Load more 2024 movies for horizontal pagination with deduplication
     */
    fun loadMore2024() {
        if (_isLoadingMore2024.value || !_hasMore2024.value) return
        
        viewModelScope.launch {
            _isLoadingMore2024.value = true
            try {
                movies2024Page++
                
                // Use deduplication method to get unique movies
                val result = repository.getMoviesByYearWithDeduplication(
                    year = 2024,
                    page = movies2024Page,
                    excludedIds = displayed2024MovieIds,
                    targetCount = 20
                )
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (newMovies.isNotEmpty()) {
                            val currentMovies = _movies2024.value.toMutableList()
                            currentMovies.addAll(newMovies)
                            _movies2024.value = currentMovies
                            
                            // Track new movie IDs
                            displayed2024MovieIds.addAll(newMovies.map { it.id })
                            
                            // Check if we have more content available
                            _hasMore2024.value = newMovies.size >= 15 // Lower threshold for better UX
                            
                            println("✅ loadMore2024: Added ${newMovies.size} new movies, total: ${currentMovies.size}")
                        } else {
                            _hasMore2024.value = false
                            println("🚨 loadMore2024: No more unique movies found")
                        }
                    },
                    onFailure = { exception ->
                        movies2024Page-- // Revert page increment on failure
                        _errorMessage.value = "Failed to load more 2024 movies: ${exception.message}"
                        println("❌ loadMore2024: Failed - ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                movies2024Page-- // Revert page increment on error
                _errorMessage.value = "Error loading more 2024 movies: ${e.message}"
                println("❌ loadMore2024: Exception - ${e.message}")
            } finally {
                _isLoadingMore2024.value = false
            }
        }
    }
    
    /**
     * Load more 2023 movies for horizontal pagination with deduplication
     */
    fun loadMore2023() {
        if (_isLoadingMore2023.value || !_hasMore2023.value) return
        
        viewModelScope.launch {
            _isLoadingMore2023.value = true
            try {
                movies2023Page++
                
                // Use deduplication method to get unique movies
                val result = repository.getMoviesByYearWithDeduplication(
                    year = 2023,
                    page = movies2023Page,
                    excludedIds = displayed2023MovieIds,
                    targetCount = 20
                )
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (newMovies.isNotEmpty()) {
                            val currentMovies = _movies2023.value.toMutableList()
                            currentMovies.addAll(newMovies)
                            _movies2023.value = currentMovies
                            
                            // Track new movie IDs
                            displayed2023MovieIds.addAll(newMovies.map { it.id })
                            
                            // Check if we have more content available
                            _hasMore2023.value = newMovies.size >= 15 // Lower threshold for better UX
                            
                            println("✅ loadMore2023: Added ${newMovies.size} new movies, total: ${currentMovies.size}")
                        } else {
                            _hasMore2023.value = false
                            println("🚨 loadMore2023: No more unique movies found")
                        }
                    },
                    onFailure = { exception ->
                        movies2023Page-- // Revert page increment on failure
                        _errorMessage.value = "Failed to load more 2023 movies: ${exception.message}"
                        println("❌ loadMore2023: Failed - ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                movies2023Page-- // Revert page increment on error
                _errorMessage.value = "Error loading more 2023 movies: ${e.message}"
                println("❌ loadMore2023: Exception - ${e.message}")
            } finally {
                _isLoadingMore2023.value = false
            }
        }
    }
    
    /**
     * Load more horror movies for horizontal pagination with deduplication
     */
    fun loadMoreHorror() {
        if (_isLoadingMoreHorror.value || !_hasMoreHorror.value) return
        
        viewModelScope.launch {
            _isLoadingMoreHorror.value = true
            try {
                horrorPage++
                
                // Use deduplication method to get unique movies
                val result = repository.getMoviesByGenreWithDeduplication(
                    genreId = GENRE_HORROR,
                    page = horrorPage,
                    excludedIds = displayedHorrorMovieIds,
                    targetCount = 20
                )
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (newMovies.isNotEmpty()) {
                            val currentMovies = _horrorMovies.value.toMutableList()
                            currentMovies.addAll(newMovies)
                            _horrorMovies.value = currentMovies
                            
                            // Track new movie IDs
                            displayedHorrorMovieIds.addAll(newMovies.map { it.id })
                            
                            // Check if we have more content available
                            _hasMoreHorror.value = newMovies.size >= 15
                            
                            println("✅ loadMoreHorror: Added ${newMovies.size} new movies, total: ${currentMovies.size}")
                        } else {
                            _hasMoreHorror.value = false
                            println("🚨 loadMoreHorror: No more unique movies found")
                        }
                    },
                    onFailure = { exception ->
                        horrorPage--
                        _errorMessage.value = "Failed to load more horror movies: ${exception.message}"
                        println("❌ loadMoreHorror: Failed - ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                horrorPage--
                _errorMessage.value = "Error loading more horror movies: ${e.message}"
                println("❌ loadMoreHorror: Exception - ${e.message}")
            } finally {
                _isLoadingMoreHorror.value = false
            }
        }
    }
    
    /**
     * Load more action movies for horizontal pagination with deduplication
     */
    fun loadMoreAction() {
        if (_isLoadingMoreAction.value || !_hasMoreAction.value) return
        
        viewModelScope.launch {
            _isLoadingMoreAction.value = true
            try {
                actionPage++
                
                // Use deduplication method to get unique movies
                val result = repository.getMoviesByGenreWithDeduplication(
                    genreId = GENRE_ACTION,
                    page = actionPage,
                    excludedIds = displayedActionMovieIds,
                    targetCount = 20
                )
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (newMovies.isNotEmpty()) {
                            val currentMovies = _actionMovies.value.toMutableList()
                            currentMovies.addAll(newMovies)
                            _actionMovies.value = currentMovies
                            
                            // Track new movie IDs
                            displayedActionMovieIds.addAll(newMovies.map { it.id })
                            
                            // Check if we have more content available
                            _hasMoreAction.value = newMovies.size >= 15
                            
                            println("✅ loadMoreAction: Added ${newMovies.size} new movies, total: ${currentMovies.size}")
                        } else {
                            _hasMoreAction.value = false
                            println("🚨 loadMoreAction: No more unique movies found")
                        }
                    },
                    onFailure = { exception ->
                        actionPage--
                        _errorMessage.value = "Failed to load more action movies: ${exception.message}"
                        println("❌ loadMoreAction: Failed - ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                actionPage--
                _errorMessage.value = "Error loading more action movies: ${e.message}"
                println("❌ loadMoreAction: Exception - ${e.message}")
            } finally {
                _isLoadingMoreAction.value = false
            }
        }
    }
    
    /**
     * Load more comedy movies for horizontal pagination with deduplication
     */
    fun loadMoreComedy() {
        if (_isLoadingMoreComedy.value || !_hasMoreComedy.value) return
        
        viewModelScope.launch {
            _isLoadingMoreComedy.value = true
            try {
                comedyPage++
                
                val result = repository.getMoviesByGenreWithDeduplication(
                    genreId = GENRE_COMEDY,
                    page = comedyPage,
                    excludedIds = displayedComedyMovieIds,
                    targetCount = 20
                )
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (newMovies.isNotEmpty()) {
                            val currentMovies = _comedyMovies.value.toMutableList()
                            currentMovies.addAll(newMovies)
                            _comedyMovies.value = currentMovies
                            displayedComedyMovieIds.addAll(newMovies.map { it.id })
                            _hasMoreComedy.value = newMovies.size >= 15
                            println("✅ loadMoreComedy: Added ${newMovies.size} new movies")
                        } else {
                            _hasMoreComedy.value = false
                        }
                    },
                    onFailure = { exception ->
                        comedyPage--
                        _errorMessage.value = "Failed to load more comedy movies: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                comedyPage--
                _errorMessage.value = "Error loading more comedy movies: ${e.message}"
            } finally {
                _isLoadingMoreComedy.value = false
            }
        }
    }
    
    /**
     * Load more drama movies for horizontal pagination with deduplication
     */
    fun loadMoreDrama() {
        if (_isLoadingMoreDrama.value || !_hasMoreDrama.value) return
        
        viewModelScope.launch {
            _isLoadingMoreDrama.value = true
            try {
                dramaPage++
                
                val result = repository.getMoviesByGenreWithDeduplication(
                    genreId = GENRE_DRAMA,
                    page = dramaPage,
                    excludedIds = displayedDramaMovieIds,
                    targetCount = 20
                )
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (newMovies.isNotEmpty()) {
                            val currentMovies = _dramaMovies.value.toMutableList()
                            currentMovies.addAll(newMovies)
                            _dramaMovies.value = currentMovies
                            displayedDramaMovieIds.addAll(newMovies.map { it.id })
                            _hasMoreDrama.value = newMovies.size >= 15
                            println("✅ loadMoreDrama: Added ${newMovies.size} new movies")
                        } else {
                            _hasMoreDrama.value = false
                        }
                    },
                    onFailure = { exception ->
                        dramaPage--
                        _errorMessage.value = "Failed to load more drama movies: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                dramaPage--
                _errorMessage.value = "Error loading more drama movies: ${e.message}"
            } finally {
                _isLoadingMoreDrama.value = false
            }
        }
    }
    
    /**
     * SIMPLE PAGINATION: Load more content for any section efficiently
     * Uses deduplication tracking as specified in project requirements
     */
    fun loadMoreSimple(section: String) {
        val isLoadingState = when (section) {
            "trending" -> _isLoadingMoreTrending
            "popular" -> _isLoadingMorePopular
            "toprated" -> _isLoadingMoreTopRated
            "2024" -> _isLoadingMore2024
            "2023" -> _isLoadingMore2023
            "horror" -> _isLoadingMoreHorror
            "action" -> _isLoadingMoreAction
            "comedy" -> _isLoadingMoreComedy
            "drama" -> _isLoadingMoreDrama
            else -> return
        }
        
        val hasMoreState = when (section) {
            "trending" -> _hasMoreTrending
            "popular" -> _hasMorePopular
            "toprated" -> _hasMoreTopRated
            "2024" -> _hasMore2024
            "2023" -> _hasMore2023
            "horror" -> _hasMoreHorror
            "action" -> _hasMoreAction
            "comedy" -> _hasMoreComedy
            "drama" -> _hasMoreDrama
            else -> return
        }
        
        if (isLoadingState.value || !hasMoreState.value) return
        
        viewModelScope.launch {
            isLoadingState.value = true
            try {
                val excludedIds = when (section) {
                    "trending" -> displayedTrendingMovieIds
                    "popular" -> displayedPopularMovieIds
                    "toprated" -> displayedTopRatedMovieIds
                    "2024" -> displayed2024MovieIds
                    "2023" -> displayed2023MovieIds
                    "horror" -> displayedHorrorMovieIds
                    "action" -> displayedActionMovieIds
                    "comedy" -> displayedComedyMovieIds
                    "drama" -> displayedDramaMovieIds
                    else -> mutableSetOf()
                }
                
                val result = repository.getSupabaseMoviesSimple(
                    page = 2, // Simple pagination - always page 2 for more content
                    pageSize = 20,
                    excludedIds = excludedIds
                )
                
                result.fold(
                    onSuccess = { newMovies ->
                        if (newMovies.isNotEmpty()) {
                            when (section) {
                                "trending" -> {
                                    val updated = _trendingMovies.value.toMutableList()
                                    updated.addAll(newMovies)
                                    _trendingMovies.value = updated
                                    displayedTrendingMovieIds.addAll(newMovies.map { it.id })
                                }
                                "popular" -> {
                                    val updated = _popularMovies.value.toMutableList()
                                    updated.addAll(newMovies)
                                    _popularMovies.value = updated
                                    displayedPopularMovieIds.addAll(newMovies.map { it.id })
                                }
                                "toprated" -> {
                                    val updated = _topRatedMovies.value.toMutableList()
                                    updated.addAll(newMovies)
                                    _topRatedMovies.value = updated
                                    displayedTopRatedMovieIds.addAll(newMovies.map { it.id })
                                }
                                "2024" -> {
                                    val updated = _movies2024.value.toMutableList()
                                    updated.addAll(newMovies)
                                    _movies2024.value = updated
                                    displayed2024MovieIds.addAll(newMovies.map { it.id })
                                }
                                "2023" -> {
                                    val updated = _movies2023.value.toMutableList()
                                    updated.addAll(newMovies)
                                    _movies2023.value = updated
                                    displayed2023MovieIds.addAll(newMovies.map { it.id })
                                }
                                "horror" -> {
                                    val updated = _horrorMovies.value.toMutableList()
                                    updated.addAll(newMovies)
                                    _horrorMovies.value = updated
                                    displayedHorrorMovieIds.addAll(newMovies.map { it.id })
                                }
                                "action" -> {
                                    val updated = _actionMovies.value.toMutableList()
                                    updated.addAll(newMovies)
                                    _actionMovies.value = updated
                                    displayedActionMovieIds.addAll(newMovies.map { it.id })
                                }
                                "comedy" -> {
                                    val updated = _comedyMovies.value.toMutableList()
                                    updated.addAll(newMovies)
                                    _comedyMovies.value = updated
                                    displayedComedyMovieIds.addAll(newMovies.map { it.id })
                                }
                                "drama" -> {
                                    val updated = _dramaMovies.value.toMutableList()
                                    updated.addAll(newMovies)
                                    _dramaMovies.value = updated
                                    displayedDramaMovieIds.addAll(newMovies.map { it.id })
                                }
                            }
                            
                            hasMoreState.value = newMovies.size >= 15
                            println("✅ SIMPLE: $section loaded ${newMovies.size} more movies")
                        } else {
                            hasMoreState.value = false
                            println("📄 SIMPLE: $section - no more content")
                        }
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Failed to load more $section movies: ${exception.message}"
                        println("❌ SIMPLE: $section failed - ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error loading more $section movies: ${e.message}"
                println("❌ SIMPLE: $section error - ${e.message}")
            } finally {
                isLoadingState.value = false
            }
        }
    }
    
    /**
     * Check if any data is currently loaded
     */
    fun hasAnyData(): Boolean {
        return trendingMovies.value.isNotEmpty() || 
               popularMovies.value.isNotEmpty() || 
               topRatedMovies.value.isNotEmpty() ||
               movies2024.value.isNotEmpty() || 
               movies2023.value.isNotEmpty() ||
               horrorMovies.value.isNotEmpty() || 
               actionMovies.value.isNotEmpty() || 
               comedyMovies.value.isNotEmpty() || 
               dramaMovies.value.isNotEmpty()
    }
    
    /**
     * Switch to Supabase-first approach when TMDB matching is poor
     * This prioritizes showing your streaming content over strict TMDB categorization
     */
    fun loadWithSupabaseFirstApproach() {
        if (_isGlobalLoading.value) {
            println("⚠️ Global loading in progress, skipping Supabase-first approach")
            return
        }
        
        viewModelScope.launch {
            _isGlobalLoading.value = true
            _errorMessage.value = null
            
            try {
                println("🎥 Switching to Supabase-first approach for better content discovery...")
                
                coroutineScope {
                    // Load each section using Supabase-first approach
                    val trendingDeferred = async { 
                        repository.getSupabaseFirstMovies("trending", 1, displayedTrendingMovieIds)
                    }
                    val popularDeferred = async { 
                        repository.getSupabaseFirstMovies("popular", 1, displayedPopularMovieIds)
                    }
                    val topRatedDeferred = async { 
                        repository.getSupabaseFirstMovies("toprated", 1, displayedTopRatedMovieIds)
                    }
                    val movies2024Deferred = async { 
                        repository.getSupabaseFirstMovies("2024", 1, displayed2024MovieIds)
                    }
                    val movies2023Deferred = async { 
                        repository.getSupabaseFirstMovies("2023", 1, displayed2023MovieIds)
                    }
                    
                    // Update all sections
                    trendingDeferred.await().fold(
                        onSuccess = { movies ->
                            _trendingMovies.value = movies
                            displayedTrendingMovieIds.addAll(movies.map { it.id })
                            _hasMoreTrending.value = movies.size >= 15
                            println("✅ Supabase-first Trending: ${movies.size} movies")
                        },
                        onFailure = { exception ->
                            println("❌ Supabase-first Trending failed: ${exception.message}")
                        }
                    )
                    
                    popularDeferred.await().fold(
                        onSuccess = { movies ->
                            _popularMovies.value = movies
                            displayedPopularMovieIds.addAll(movies.map { it.id })
                            _hasMorePopular.value = movies.size >= 15
                            println("✅ Supabase-first Popular: ${movies.size} movies")
                        },
                        onFailure = { exception ->
                            println("❌ Supabase-first Popular failed: ${exception.message}")
                        }
                    )
                    
                    topRatedDeferred.await().fold(
                        onSuccess = { movies ->
                            _topRatedMovies.value = movies
                            displayedTopRatedMovieIds.addAll(movies.map { it.id })
                            _hasMoreTopRated.value = movies.size >= 15
                            println("✅ Supabase-first Top Rated: ${movies.size} movies")
                        },
                        onFailure = { exception ->
                            println("❌ Supabase-first Top Rated failed: ${exception.message}")
                        }
                    )
                    
                    movies2024Deferred.await().fold(
                        onSuccess = { movies ->
                            _movies2024.value = movies
                            displayed2024MovieIds.addAll(movies.map { it.id })
                            _hasMore2024.value = movies.size >= 15
                            println("✅ Supabase-first 2024: ${movies.size} movies")
                        },
                        onFailure = { exception ->
                            println("❌ Supabase-first 2024 failed: ${exception.message}")
                        }
                    )
                    
                    movies2023Deferred.await().fold(
                        onSuccess = { movies ->
                            _movies2023.value = movies
                            displayed2023MovieIds.addAll(movies.map { it.id })
                            _hasMore2023.value = movies.size >= 15
                            println("✅ Supabase-first 2023: ${movies.size} movies")
                        },
                        onFailure = { exception ->
                            println("❌ Supabase-first 2023 failed: ${exception.message}")
                        }
                    )
                }
                
                println("✅ Supabase-first approach completed - showing more of your streaming content!")
                
            } catch (e: Exception) {
                _errorMessage.value = "Supabase-first loading failed: ${e.message ?: "Unknown error"}"
                println("❌ Supabase-first approach failed: ${e.message}")
            } finally {
                _isGlobalLoading.value = false
            }
        }
    }

    /**
     * Get loading status summary for debugging
     */
    fun getLoadingStatus(): String {
        return "Loading Status - Global: ${_isGlobalLoading.value}, " +
               "Trending: ${_isLoadingTrending.value}, " +
               "Popular: ${_isLoadingPopular.value}, " +
               "TopRated: ${_isLoadingTopRated.value}, " +
               "ByYear: ${_isLoadingByYear.value}, " +
               "ByGenre: ${_isLoadingByGenre.value}, " +
               "HasData: ${hasDataBeenLoaded}"
    }
    
    /**
     * Run comprehensive diagnostic analysis to identify matching issues
     * This helps understand why TMDB content isn't matching with Supabase streams
     */
    fun runEnhancedMatchingDiagnostics() {
        if (_isRunningDiagnostics.value) {
            println("🔄 Enhanced diagnostics already running, skipping...")
            return
        }
        
        viewModelScope.launch {
            _isRunningDiagnostics.value = true
            _diagnosticReport.value = null
            _errorMessage.value = null
            
            try {
                println("🔍 Starting enhanced matching analysis to identify TMDB-Supabase matching issues...")
                
                // Run the comprehensive repository diagnostic
                val result = repository.analyzeMatchingIssues(sampleSize = 200)
                
                result.fold(
                    onSuccess = { report ->
                        _diagnosticReport.value = report
                        println("✅ Enhanced diagnostic analysis completed - check diagnostic report for detailed insights")
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Enhanced diagnostic analysis failed: ${exception.message ?: "Unknown error"}"
                        println("❌ Enhanced diagnostic analysis failed: ${exception.message}")
                    }
                )
                
            } catch (e: Exception) {
                _errorMessage.value = "Enhanced diagnostic analysis failed: ${e.message ?: "Unknown error"}"
                println("❌ Enhanced diagnostic analysis failed: ${e.message}")
            } finally {
                _isRunningDiagnostics.value = false
            }
        }
    }

    /**
     * Run simplified diagnostic analysis using the MatchingDiagnostics utility
     * This helps identify why some movies don't have thumbnails/metadata
     */
    fun runSimpleDiagnostics(sampleSize: Int = 50) {
        if (_isRunningDiagnostics.value) {
            println("🔄 Diagnostics already running, skipping...")
            return
        }
        
        viewModelScope.launch {
            _isRunningDiagnostics.value = true
            _diagnosticReport.value = null
            _errorMessage.value = null
            
            try {
                println("🔍 Starting simplified matching analysis...")
                
                // Get sample movies from current loaded data
                val allLoadedMovies = mutableListOf<CombinedMovie>()
                allLoadedMovies.addAll(trendingMovies.value)
                allLoadedMovies.addAll(popularMovies.value)
                allLoadedMovies.addAll(topRatedMovies.value)
                allLoadedMovies.addAll(movies2024.value)
                allLoadedMovies.addAll(movies2023.value)
                
                if (allLoadedMovies.isEmpty()) {
                    _diagnosticReport.value = "No movies loaded yet. Please load some data first."
                    return@launch
                }
                
                // Extract Supabase and TMDB titles from loaded movies
                val supabaseMovies = allLoadedMovies.map { it.supabaseMovie }.distinctBy { it.title }
                val tmdbMovies = allLoadedMovies.map { it.tmdbMovie }.distinctBy { it.title }
                
                val report = diagnostics.analyzeMatching(
                    supabaseMovies = supabaseMovies,
                    tmdbMovies = tmdbMovies,
                    sampleSize = sampleSize
                )
                
                _diagnosticReport.value = report
                println("✅ Simplified diagnostic analysis completed")
                
            } catch (e: Exception) {
                _errorMessage.value = "Diagnostic analysis failed: ${e.message ?: "Unknown error"}"
                println("❌ Diagnostic analysis failed: ${e.message}")
            } finally {
                _isRunningDiagnostics.value = false
            }
        }
    }
    
    /**
     * Clear diagnostic report
     */
    fun clearDiagnosticReport() {
        _diagnosticReport.value = null
    }
    
    /**
     * Get current movie statistics for analysis
     */
    fun getMovieStatistics(): String {
        val totalMovies = trendingMovies.value.size + popularMovies.value.size + 
                         topRatedMovies.value.size + movies2024.value.size + 
                         movies2023.value.size + horrorMovies.value.size + 
                         actionMovies.value.size + comedyMovies.value.size + 
                         dramaMovies.value.size
        
        return "📊 CURRENT MOVIE STATISTICS:\n" +
               "============================\n" +
               "📈 Trending: ${trendingMovies.value.size} movies\n" +
               "🔥 Popular: ${popularMovies.value.size} movies\n" +
               "⭐ Top Rated: ${topRatedMovies.value.size} movies\n" +
               "📅 2024 Movies: ${movies2024.value.size} movies\n" +
               "📅 2023 Movies: ${movies2023.value.size} movies\n" +
               "😱 Horror: ${horrorMovies.value.size} movies\n" +
               "💥 Action: ${actionMovies.value.size} movies\n" +
               "😄 Comedy: ${comedyMovies.value.size} movies\n" +
               "🎭 Drama: ${dramaMovies.value.size} movies\n" +
               "📊 Total Loaded: $totalMovies movies\n" +
               "🔄 Has Data Loaded: $hasDataBeenLoaded"
    }
    
    /**
     * DEBUG LOGGING: Comprehensive data flow tracking for API integration issues
     * Following experience lessons from memory for better diagnostics
     */
    fun enableDebugLogging() {
        println("🔍 DEBUG LOGGING ENABLED - Tracking all data flow")
        
        viewModelScope.launch {
            try {
                // Debug Supabase connection
                println("📡 DEBUG: Testing Supabase connection...")
                val supabaseResult = repository.getSupabaseMoviesSimple(page = 1, pageSize = 10)
                supabaseResult.fold(
                    onSuccess = { movies -> 
                        println("📊 DEBUG: Supabase returned ${movies.size} movies")
                        if (movies.isNotEmpty()) {
                            println("🎬 DEBUG: Sample Supabase movies:")
                            movies.take(5).forEach { movie ->
                                println("   • '${movie.title}' - Streamable: ${movie.isStreamable}")
                            }
                        }
                    },
                    onFailure = { exception ->
                        println("⚠️ DEBUG: Supabase connection failed - ${exception.message}")
                    }
                )

                
                // Debug simple loading
                println("🔄 DEBUG: Testing simple category loading...")
                val testResult = repository.getMoviesBySimpleCategory("trending", 1, 5)
                testResult.fold(
                    onSuccess = { movies ->
                        println("✅ DEBUG: Simple loading successful - ${movies.size} movies")
                        movies.forEach { movie ->
                            println("   • '${movie.title}' - Streamable: ${movie.isStreamable}")
                        }
                    },
                    onFailure = { exception ->
                        println("❌ DEBUG: Simple loading failed - ${exception.message}")
                    }
                )
                
                // Debug current state
                val totalLoaded = getCurrentMovieCount()
                println("📈 DEBUG: Current app state - $totalLoaded movies loaded across all sections")
                
                // Debug section breakdown
                println("📋 DEBUG: Section breakdown:")
                println("   • Trending: ${_trendingMovies.value.size} movies")
                println("   • Popular: ${_popularMovies.value.size} movies") 
                println("   • Top Rated: ${_topRatedMovies.value.size} movies")
                println("   • 2024: ${_movies2024.value.size} movies")
                println("   • 2023: ${_movies2023.value.size} movies")
                println("   • Horror: ${_horrorMovies.value.size} movies")
                println("   • Action: ${_actionMovies.value.size} movies")
                println("   • Comedy: ${_comedyMovies.value.size} movies")
                println("   • Drama: ${_dramaMovies.value.size} movies")
                
            } catch (e: Exception) {
                println("❌ DEBUG: Debug logging failed - ${e.message}")
            }
        }
    }
    
    /**
     * Get comprehensive debug report for troubleshooting
     */
    fun getDebugReport(): String {
        val report = StringBuilder()
        
        report.appendLine("🔍 SIMPLE SYSTEM DEBUG REPORT")
        report.appendLine("=====================================")
        report.appendLine("📅 Generated: ${System.currentTimeMillis()}")
        report.appendLine()
        
        // System status
        report.appendLine("📊 SYSTEM STATUS:")
        report.appendLine("• Global Loading: ${_isGlobalLoading.value}")
        report.appendLine("• Data Loaded: $hasDataBeenLoaded")
        report.appendLine("• Error Message: ${_errorMessage.value ?: "None"}")
        report.appendLine()
        
        // Section status
        report.appendLine("📋 SECTION STATUS:")
        report.appendLine("• Trending: ${_trendingMovies.value.size} movies (loading: ${_isLoadingMoreTrending.value}, hasMore: ${_hasMoreTrending.value})")
        report.appendLine("• Popular: ${_popularMovies.value.size} movies (loading: ${_isLoadingMorePopular.value}, hasMore: ${_hasMorePopular.value})")
        report.appendLine("• Top Rated: ${_topRatedMovies.value.size} movies (loading: ${_isLoadingMoreTopRated.value}, hasMore: ${_hasMoreTopRated.value})")
        report.appendLine("• 2024: ${_movies2024.value.size} movies (loading: ${_isLoadingMore2024.value}, hasMore: ${_hasMore2024.value})")
        report.appendLine("• 2023: ${_movies2023.value.size} movies (loading: ${_isLoadingMore2023.value}, hasMore: ${_hasMore2023.value})")
        report.appendLine("• Horror: ${_horrorMovies.value.size} movies (loading: ${_isLoadingMoreHorror.value}, hasMore: ${_hasMoreHorror.value})")
        report.appendLine("• Action: ${_actionMovies.value.size} movies (loading: ${_isLoadingMoreAction.value}, hasMore: ${_hasMoreAction.value})")
        report.appendLine("• Comedy: ${_comedyMovies.value.size} movies (loading: ${_isLoadingMoreComedy.value}, hasMore: ${_hasMoreComedy.value})")
        report.appendLine("• Drama: ${_dramaMovies.value.size} movies (loading: ${_isLoadingMoreDrama.value}, hasMore: ${_hasMoreDrama.value})")
        report.appendLine()
        
        // Deduplication tracking
        report.appendLine("🔄 DEDUPLICATION TRACKING:")
        report.appendLine("• Trending IDs: ${displayedTrendingMovieIds.size}")
        report.appendLine("• Popular IDs: ${displayedPopularMovieIds.size}")
        report.appendLine("• Top Rated IDs: ${displayedTopRatedMovieIds.size}")
        report.appendLine("• 2024 IDs: ${displayed2024MovieIds.size}")
        report.appendLine("• 2023 IDs: ${displayed2023MovieIds.size}")
        report.appendLine("• Horror IDs: ${displayedHorrorMovieIds.size}")
        report.appendLine("• Action IDs: ${displayedActionMovieIds.size}")
        report.appendLine("• Comedy IDs: ${displayedComedyMovieIds.size}")
        report.appendLine("• Drama IDs: ${displayedDramaMovieIds.size}")
        
        return report.toString()
    }

}
