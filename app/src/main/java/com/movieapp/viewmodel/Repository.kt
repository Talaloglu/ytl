package com.movieapp.viewmodel

import com.movieapp.data.model.MoviesList
import com.movieapp.data.model.MovieDetails
import com.movieapp.data.api.ApiInterface
import com.movieapp.data.api.GenresResponse
import com.movieapp.utils.RetrofitInstance
import com.movieapp.utils.ApiConfig
import retrofit2.Response

/**
 * Repository class as specified - single source of truth for data
 * Abstracts the data source from the ViewModel
 * Manages network calls and data operations
 */
class Repository {
    
    private val apiInterface: ApiInterface = RetrofitInstance.apiInterface
    
    // TMDB disabled in Supabase-first mode
    private val apiKey = "TMDB_DISABLED_IN_SUPABASE_FIRST_MODE"
    
    /**
     * Suspend function to get the list of movies as specified
     * Calls the Retrofit API interface's getMovies function, passing the page number as a parameter
     * @param page The page number for pagination
     * @return Response containing MoviesList with movies data
     */
    suspend fun getMovies(page: Int = 1): Response<MoviesList> {
        println("⛔ TMDB getMovies disabled (Supabase-first). Use Supabase repositories.")
        throw UnsupportedOperationException("TMDB getMovies disabled in Supabase-first mode")
    }
    
    /**
     * Suspend function to get movie details by ID as specified
     * Calls the Retrofit API interface's getDetailsById function
     * @param id The movie ID to fetch details for
     * @return Response containing MovieDetails with comprehensive movie information
     */
    suspend fun getMovieDetails(id: Int): Response<MovieDetails> {
        // Supabase-first mode: TMDB details fetching is disabled to avoid external network calls
        // DetailsScreen should rely on Supabase-enriched data instead.
        println("⛔ TMDB details disabled (Supabase-first): id='${id}'")
        throw UnsupportedOperationException("TMDB details disabled in Supabase-first mode. Use Supabase-enriched data.")
    }
    
    // Additional suspend functions for enhanced functionality
    
    /**
     * Get popular movies
     * @param page The page number for pagination
     * @return Response containing MoviesList with popular movies
     */
    suspend fun getPopularMovies(page: Int = 1): Response<MoviesList> {
        println("⛔ TMDB getPopularMovies disabled (Supabase-first). Use Supabase repositories.")
        throw UnsupportedOperationException("TMDB getPopularMovies disabled in Supabase-first mode")
    }
    
    /**
     * Get top rated movies
     * @param page The page number for pagination
     * @return Response containing MoviesList with top rated movies
     */
    suspend fun getTopRatedMovies(page: Int = 1): Response<MoviesList> {
        println("⛔ TMDB getTopRatedMovies disabled (Supabase-first). Use Supabase repositories.")
        throw UnsupportedOperationException("TMDB getTopRatedMovies disabled in Supabase-first mode")
    }
    
    /**
     * Get now playing movies
     * @param page The page number for pagination
     * @return Response containing MoviesList with now playing movies
     */
    suspend fun getNowPlayingMovies(page: Int = 1): Response<MoviesList> {
        println("⛔ TMDB getNowPlayingMovies disabled (Supabase-first). Use Supabase repositories.")
        throw UnsupportedOperationException("TMDB getNowPlayingMovies disabled in Supabase-first mode")
    }
    
    /**
     * Get upcoming movies
     * @param page The page number for pagination
     * @return Response containing MoviesList with upcoming movies
     */
    suspend fun getUpcomingMovies(page: Int = 1): Response<MoviesList> {
        println("⛔ TMDB getUpcomingMovies disabled (Supabase-first). Use Supabase repositories.")
        throw UnsupportedOperationException("TMDB getUpcomingMovies disabled in Supabase-first mode")
    }
    
    /**
     * Search movies by query
     * @param query The search query
     * @param page The page number for pagination
     * @return Response containing MoviesList with search results
     */
    suspend fun searchMovies(query: String, page: Int = 1): Response<MoviesList> {
        println("⛔ TMDB searchMovies disabled (Supabase-first). Use Supabase search.")
        throw UnsupportedOperationException("TMDB searchMovies disabled in Supabase-first mode")
    }
    
    /**
     * Get movie genres list
     * @return Response containing list of genres
     */
    suspend fun getGenres(): Response<GenresResponse> {
        println("⛔ TMDB getGenres disabled (Supabase-first). Use Supabase-enriched metadata.")
        throw UnsupportedOperationException("TMDB getGenres disabled in Supabase-first mode")
    }
    
    /**
     * Get movies by genre
     * @param genreId The genre ID to filter by
     * @param page The page number for pagination
     * @return Response containing MoviesList filtered by genre
     */
    suspend fun getMoviesByGenre(genreId: Int, page: Int = 1): Response<MoviesList> {
        println("⛔ TMDB getMoviesByGenre disabled (Supabase-first). Use Supabase filters.")
        throw UnsupportedOperationException("TMDB getMoviesByGenre disabled in Supabase-first mode")
    }
    
    /**
     * Get similar movies for a given movie ID
     * @param movieId The movie ID to find similar movies for
     * @param page The page number for pagination
     * @return Response containing MoviesList with similar movies
     */
    suspend fun getSimilarMovies(movieId: Int, page: Int = 1): Response<MoviesList> {
        println("⛔ TMDB getSimilarMovies disabled (Supabase-first). Use Supabase recommendations if available.")
        throw UnsupportedOperationException("TMDB getSimilarMovies disabled in Supabase-first mode")
    }
    
    /**
     * Get movie recommendations for a given movie ID
     * @param movieId The movie ID to get recommendations for
     * @param page The page number for pagination
     * @return Response containing MoviesList with recommended movies
     */
    suspend fun getRecommendedMovies(movieId: Int, page: Int = 1): Response<MoviesList> {
        println("⛔ TMDB getRecommendedMovies disabled (Supabase-first). Use Supabase recommendations if available.")
        throw UnsupportedOperationException("TMDB getRecommendedMovies disabled in Supabase-first mode")
    }
}