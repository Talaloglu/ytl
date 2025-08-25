package com.movieapp.data.api

import com.movieapp.data.model.MovieResponse
import com.movieapp.data.model.Movie
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API interface for movie-related endpoints
 * Contains methods for fetching movies from The Movie Database API
 */
interface MovieApiService {
    
    /**
     * Get popular movies
     * @param apiKey The API key for TMDB
     * @param page The page number for pagination
     * @return Response containing list of popular movies
     */
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<MovieResponse>
    
    /**
     * Get top rated movies
     * @param apiKey The API key for TMDB
     * @param page The page number for pagination
     * @return Response containing list of top rated movies
     */
    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<MovieResponse>
    
    /**
     * Get now playing movies
     * @param apiKey The API key for TMDB
     * @param page The page number for pagination
     * @return Response containing list of now playing movies
     */
    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<MovieResponse>
    
    /**
     * Get upcoming movies
     * @param apiKey The API key for TMDB
     * @param page The page number for pagination
     * @return Response containing list of upcoming movies
     */
    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<MovieResponse>
    
    /**
     * Get movie details by ID
     * @param movieId The ID of the movie
     * @param apiKey The API key for TMDB
     * @return Response containing movie details
     */
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): Response<Movie>
    
    /**
     * Search movies by query
     * @param apiKey The API key for TMDB
     * @param query The search query
     * @param page The page number for pagination
     * @return Response containing search results
     */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): Response<MovieResponse>
}