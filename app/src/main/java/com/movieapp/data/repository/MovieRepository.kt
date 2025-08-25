package com.movieapp.data.repository

import com.movieapp.data.api.ApiInterface
import com.movieapp.data.api.GenresResponse
import com.movieapp.data.model.MoviesList
import com.movieapp.data.model.MovieDetails
import com.movieapp.utils.RetrofitInstance
import retrofit2.Response

/**
 * Repository class for handling movie data operations in the data layer
 * Provides clean interface between ViewModels and API service
 * Acts as single source of truth for data operations
 * Note: This is the data layer repository - separate from viewmodel.Repository
 */
class MovieRepository {
    
    private val apiInterface: ApiInterface = RetrofitInstance.apiInterface
    
    // TMDB disabled in Supabase-first mode
    private val apiKey = "TMDB_DISABLED_IN_SUPABASE_FIRST_MODE"
    
    /**
     * Get movies with pagination support as specified
     * @param page The page number for pagination
     * @return Response containing MoviesList with pagination metadata
     */
    suspend fun getMovies(page: Int = 1): Response<MoviesList> {
        println("⛔ TMDB getMovies disabled (Supabase-first). Use Supabase repositories.")
        throw UnsupportedOperationException("TMDB getMovies disabled in Supabase-first mode")
    }
    
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
     * Get movie details by ID as specified
     * @param id The movie ID
     * @return Response containing MovieDetails with comprehensive information
     */
    suspend fun getDetailsById(id: Int): Response<MovieDetails> {
        // Supabase-first mode: disable TMDB details network call
        println("⛔ TMDB details disabled (Supabase-first): id='${id}'")
        throw UnsupportedOperationException("TMDB details disabled in Supabase-first mode. Use Supabase-enriched data.")
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