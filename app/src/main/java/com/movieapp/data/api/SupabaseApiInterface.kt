package com.movieapp.data.api

import com.movieapp.data.model.SupabaseMovie
import com.movieapp.data.model.SupabaseMovieResponse
import com.movieapp.data.model.SupabaseEnrichedMovie
import retrofit2.Response
import retrofit2.http.*

/**
 * Supabase API interface for movie video data
 * Works with your actual database schema that has videourl
 */
interface SupabaseApiInterface {
    
    /**
     * Get all movies with video URLs from Supabase
     * Optimized to fetch only essential fields: title and videourl
     * @param apiKey Supabase anon key
     * @param select Only fetch title and videourl fields for better performance
     * @param videoUrlFilter Filter to only include movies with video URLs
     */
    @GET("movies")
    suspend fun getAllMoviesWithVideos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "title,videourl",
        @Query("videourl") videoUrlFilter: String = "not.is.null"
    ): Response<List<SupabaseMovie>>
    
    /**
     * Get movie by title from Supabase
     * Used to match with TMDB movie titles
     * Optimized to fetch only essential fields
     * @param apiKey Supabase anon key
     * @param title Movie title to search for
     * @param select Only fetch title and videourl fields
     */
    @GET("movies")
    suspend fun getMovieByTitle(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("title") title: String,
        @Query("select") select: String = "title,videourl"
    ): Response<List<SupabaseMovie>>
    
    /**
     * Search movies by title (case insensitive)
     * Optimized to fetch only essential fields
     * @param apiKey Supabase anon key
     * @param title Title to search for
     * @param select Only fetch title and videourl fields
     */
    @GET("movies")
    suspend fun searchMoviesByTitle(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("title") title: String,
        @Query("select") select: String = "title,videourl"
    ): Response<List<SupabaseMovie>>
    
    /**
     * Get movies with pagination
     * Optimized to fetch only essential fields
     * @param apiKey Supabase anon key
     * @param range Range for pagination (e.g., "0-19")
     * @param order Order by field
     * @param select Only fetch title and videourl fields
     * @param videoUrlFilter Filter to only include movies with video URLs
     */
    @GET("movies")
    suspend fun getMoviesWithPagination(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Range") range: String,
        @Query("select") select: String = "title,videourl",
        @Query("order") order: String = "publishedat.desc",
        @Query("videourl") videoUrlFilter: String = "not.is.null"
    ): Response<List<SupabaseMovie>>
    
    /**
     * Update movie video URL (optional - for future use)
     * @param apiKey Supabase anon key
     * @param movieId ID of the movie to update
     * @param movie Updated movie data
     */
    @PATCH("movies")
    suspend fun updateMovie(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") movieId: String,
        @Body movie: SupabaseMovie
    ): Response<List<SupabaseMovie>>
    
    /**
     * Add new movie to Supabase (optional - for future use)
     * @param apiKey Supabase anon key
     * @param movie Movie data to insert
     */
    @POST("movies")
    suspend fun addMovie(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body movie: SupabaseMovie
    ): Response<List<SupabaseMovie>>

    // =================== Enriched movie endpoints (Supabase-only runtime) ===================

    /**
     * Fetch enriched movies (with TMDB metadata) ordered by popularity.
     * Requires columns to exist in Supabase: overview, poster_path, backdrop_path, release_date,
     * vote_average, vote_count, popularity, original_language, original_title, genre_ids, publishedat.
     */
    @GET("movies")
    suspend fun getEnrichedPopular(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Range") range: String,
        @Query("select") select: String = "id,tmdb_id,title,videourl,overview,poster_path,backdrop_path,release_date,vote_average,vote_count,popularity,original_language,original_title,genre_ids,genres_json,runtime,publishedat",
        @Query("order") order: String = "popularity.desc",
        @Query("videourl") videoUrlFilter: String = "not.is.null",
        @Query("poster_path") posterNotNull: String = "not.is.null",
        @Query("tmdb_id") tmdbNotNull: String = "not.is.null"
    ): Response<List<SupabaseEnrichedMovie>>

    /**
     * Fetch enriched top-rated movies using vote_average.
     */
    @GET("movies")
    suspend fun getEnrichedTopRated(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Range") range: String,
        @Query("select") select: String = "id,tmdb_id,title,videourl,overview,poster_path,backdrop_path,release_date,vote_average,vote_count,popularity,original_language,original_title,genre_ids,genres_json,runtime,publishedat",
        @Query("order") order: String = "vote_average.desc",
        @Query("videourl") videoUrlFilter: String = "not.is.null",
        @Query("poster_path") posterNotNull: String = "not.is.null",
        @Query("tmdb_id") tmdbNotNull: String = "not.is.null"
    ): Response<List<SupabaseEnrichedMovie>>

    /**
     * Fetch latest enriched movies using release_date.
     */
    @GET("movies")
    suspend fun getEnrichedLatest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Range") range: String,
        @Query("select") select: String = "id,tmdb_id,title,videourl,overview,poster_path,backdrop_path,release_date,vote_average,vote_count,popularity,original_language,original_title,genre_ids,genres_json,runtime,publishedat",
        @Query("order") order: String = "release_date.desc",
        @Query("videourl") videoUrlFilter: String = "not.is.null",
        @Query("poster_path") posterNotNull: String = "not.is.null",
        @Query("tmdb_id") tmdbNotNull: String = "not.is.null"
    ): Response<List<SupabaseEnrichedMovie>>

    /**
     * Search enriched movies by title using ilike (case-insensitive, substring).
     * Example filter value for titleIlike: "*.matrix.*". The Retrofit will send as title=ilike.*query*.
     */
    @GET("movies")
    suspend fun searchEnrichedByTitle(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Range") range: String,
        @Query("title") titleIlike: String, // pass as "ilike.*<query>*"
        @Query("select") select: String = "id,tmdb_id,title,videourl,overview,poster_path,backdrop_path,release_date,vote_average,vote_count,popularity,original_language,original_title,genre_ids,genres_json,runtime,publishedat",
        @Query("order") order: String = "popularity.desc",
        @Query("videourl") videoUrlFilter: String = "not.is.null",
        @Query("poster_path") posterNotNull: String = "not.is.null",
        @Query("tmdb_id") tmdbNotNull: String = "not.is.null"
    ): Response<List<SupabaseEnrichedMovie>>

    /**
     * Generic enriched range fetcher for flexible ordering.
     */
    @GET("movies")
    suspend fun getEnrichedRange(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Range") range: String,
        @Query("select") select: String = "id,tmdb_id,title,videourl,overview,poster_path,backdrop_path,release_date,vote_average,vote_count,popularity,original_language,original_title,genre_ids,genres_json,runtime,publishedat",
        @Query("order") order: String,
        @Query("videourl") videoUrlFilter: String = "not.is.null",
        @Query("poster_path") posterNotNull: String = "not.is.null",
        @Query("tmdb_id") tmdbNotNull: String = "not.is.null"
    ): Response<List<SupabaseEnrichedMovie>>

    /**
     * Fetch a single enriched movie by its TMDB id using PostgREST filter tmdb_id=eq.<value>.
     * The movies.id column is a UUID; tmdb_id is numeric and should be used for lookups with TMDB ids.
     */
    @GET("movies")
    suspend fun getEnrichedById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("tmdb_id") tmdbIdEq: String, // pass as "eq.<tmdbId>"
        @Query("select") select: String = "id,tmdb_id,title,videourl,overview,poster_path,backdrop_path,release_date,vote_average,vote_count,popularity,original_language,original_title,genre_ids,genres_json,runtime,publishedat"
    ): Response<List<SupabaseEnrichedMovie>>
}