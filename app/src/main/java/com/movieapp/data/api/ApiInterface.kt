package com.movieapp.data.api

import com.movieapp.data.model.MoviesList
import com.movieapp.data.model.MovieDetails
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Main API Interface for movie-related endpoints
 * Defines all the suspend functions for network calls as specified
 */
interface ApiInterface {
    
    /**
     * Get movies with pagination support
     * @param page The page number for pagination
     * @param apiKey The API key for authentication
     * @return Response containing MoviesList with pagination metadata
     */
    @GET("movie/popular")
    suspend fun getMovies(
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String
    ): Response<MoviesList>
    
    /**
     * Get popular movies with pagination
     * @param page The page number for pagination
     * @param apiKey The API key for authentication
     * @return Response containing MoviesList with popular movies
     */
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String
    ): Response<MoviesList>
    
    /**
     * Get top rated movies with pagination
     * @param page The page number for pagination
     * @param apiKey The API key for authentication
     * @return Response containing MoviesList with top rated movies
     */
    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String
    ): Response<MoviesList>
    
    /**
     * Get now playing movies with pagination
     * @param page The page number for pagination
     * @param apiKey The API key for authentication
     * @return Response containing MoviesList with now playing movies
     */
    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String
    ): Response<MoviesList>
    
    /**
     * Get upcoming movies with pagination
     * @param page The page number for pagination
     * @param apiKey The API key for authentication
     * @return Response containing MoviesList with upcoming movies
     */
    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String
    ): Response<MoviesList>
    
    /**
     * Get movie details by ID as specified
     * @param id The movie ID
     * @param apiKey The API key for authentication
     * @return Response containing MovieDetails with comprehensive movie information
     */
    @GET("movie/{id}")
    suspend fun getDetailsById(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String
    ): Response<MovieDetails>
    
    /**
     * Search movies by query with pagination
     * @param query The search query
     * @param page The page number for pagination
     * @param apiKey The API key for authentication
     * @return Response containing MoviesList with search results
     */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String
    ): Response<MoviesList>
    
    /**
     * Get trending movies
     * @param timeWindow The time window for trending (day or week)
     * @param apiKey The API key for authentication
     * @return Response containing MoviesList with trending movies
     */
    @GET("trending/movie/{time_window}")
    suspend fun getTrendingMovies(
        @Path("time_window") timeWindow: String = "week",
        @Query("api_key") apiKey: String
    ): Response<MoviesList>
    
    /**
     * Discover movies with advanced filtering
     * @param apiKey The API key for authentication
     * @param page The page number for pagination
     * @param sortBy Sort the results (e.g., "popularity.desc", "release_date.desc")
     * @param withGenres Comma-separated list of genre IDs
     * @param primaryReleaseYear Filter by primary release year
     * @param primaryReleaseDateGte Filter by primary release date greater than or equal
     * @param primaryReleaseDateLte Filter by primary release date less than or equal
     * @return Response containing MoviesList with filtered movies
     */
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String? = null,
        @Query("with_genres") withGenres: String? = null,
        @Query("primary_release_year") primaryReleaseYear: Int? = null,
        @Query("primary_release_date.gte") primaryReleaseDateGte: String? = null,
        @Query("primary_release_date.lte") primaryReleaseDateLte: String? = null
    ): Response<MoviesList>
    
    /**
     * Get movie genres list
     * @param apiKey The API key for authentication
     * @return Response containing list of genres
     */
    @GET("genre/movie/list")
    suspend fun getGenres(
        @Query("api_key") apiKey: String
    ): Response<GenresResponse>
    
    /**
     * Get movies by genre with pagination
     * @param genreId The genre ID to filter by
     * @param page The page number for pagination
     * @param apiKey The API key for authentication
     * @return Response containing MoviesList filtered by genre
     */
    @GET("discover/movie")
    suspend fun getMoviesByGenre(
        @Query("with_genres") genreId: Int,
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String
    ): Response<MoviesList>
    
    /**
     * Get similar movies for a given movie ID
     * @param movieId The movie ID to find similar movies for
     * @param page The page number for pagination
     * @param apiKey The API key for authentication
     * @return Response containing MoviesList with similar movies
     */
    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String
    ): Response<MoviesList>
    
    /**
     * Get movie recommendations for a given movie ID
     * @param movieId The movie ID to get recommendations for
     * @param page The page number for pagination
     * @param apiKey The API key for authentication
     * @return Response containing MoviesList with recommended movies
     */
    @GET("movie/{movie_id}/recommendations")
    suspend fun getRecommendedMovies(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String
    ): Response<MoviesList>
}