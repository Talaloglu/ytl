package com.movieapp.data.repository

import com.movieapp.data.model.Movie
import com.movieapp.data.model.SupabaseEnrichedMovie
import com.movieapp.utils.SupabaseRetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simple Supabase Movie Repository
 * Fetches movies directly from Supabase without TMDB API calls
 * All movies already contain TMDB metadata in Supabase
 */
class SupabaseMovieRepository {
    
    private val supabaseApi = SupabaseRetrofitInstance.apiInterface
    private val tmdbImageBaseUrl = "https://image.tmdb.org/t/p/w500"
    
    /**
     * Convert Supabase enriched movie to Movie model with proper image URLs
     */
    private fun convertToMovie(supabaseMovie: SupabaseEnrichedMovie): Movie {
        return Movie(
            id = supabaseMovie.tmdbId ?: 0,
            title = supabaseMovie.title,
            overview = supabaseMovie.overview ?: "",
            posterPath = if (supabaseMovie.posterPath != null) {
                "$tmdbImageBaseUrl${supabaseMovie.posterPath}"
            } else "",
            backdropPath = if (supabaseMovie.backdropPath != null) {
                "$tmdbImageBaseUrl${supabaseMovie.backdropPath}"
            } else "",
            releaseDate = supabaseMovie.releaseDate ?: "",
            voteAverage = supabaseMovie.voteAverage ?: 0.0,
            voteCount = supabaseMovie.voteCount ?: 0,
            popularity = supabaseMovie.popularity ?: 0.0,
            originalLanguage = supabaseMovie.originalLanguage ?: "",
            originalTitle = supabaseMovie.originalTitle ?: supabaseMovie.title,
            genreIds = supabaseMovie.genreIds ?: emptyList(),
            adult = false,
            video = false
        )
    }
    
    /**
     * Get popular movies from Supabase (sorted by popularity)
     */
    suspend fun getPopularMovies(page: Int = 1): Result<List<Movie>> {
        return withContext(Dispatchers.IO) {
            try {
                val start = (page - 1) * 20
                val end = start + 19
                
                val response = supabaseApi.getEnrichedPopular(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    range = "$start-$end"
                )
                
                if (response.isSuccessful) {
                    val movies = response.body()?.map { convertToMovie(it) } ?: emptyList()
                    Result.success(movies)
                } else {
                    Result.failure(Exception("Failed to fetch popular movies: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get top rated movies from Supabase (sorted by rating)
     */
    suspend fun getTopRatedMovies(page: Int = 1): Result<List<Movie>> {
        return withContext(Dispatchers.IO) {
            try {
                val start = (page - 1) * 20
                val end = start + 19
                
                val response = supabaseApi.getEnrichedTopRated(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    range = "$start-$end"
                )
                
                if (response.isSuccessful) {
                    val movies = response.body()?.map { convertToMovie(it) } ?: emptyList()
                    Result.success(movies)
                } else {
                    Result.failure(Exception("Failed to fetch top rated movies: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get all movies from Supabase (for filtering/searching)
     */
    suspend fun getAllMovies(): Result<List<Movie>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseApi.getEnrichedRange(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    range = "0-999",
                    order = "popularity.desc"
                )
                
                if (response.isSuccessful) {
                    val movies = response.body()?.map { convertToMovie(it) } ?: emptyList()
                    println("✅ Fetched ${movies.size} movies from Supabase")
                    Result.success(movies)
                } else {
                    Result.failure(Exception("Failed to fetch all movies: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get movies by genre
     */
    suspend fun getMoviesByGenre(genreId: Int, page: Int = 1): Result<List<Movie>> {
        return withContext(Dispatchers.IO) {
            try {
                // Get all movies and filter by genre client-side
                val allMoviesResult = getAllMovies()
                
                if (allMoviesResult.isSuccess) {
                    val start = (page - 1) * 20
                    val end = start + 20
                    
                    val filtered = allMoviesResult.getOrNull()
                        ?.filter { it.genreIds.contains(genreId) }
                        ?.drop(start)
                        ?.take(20)
                        ?: emptyList()
                    
                    Result.success(filtered)
                } else {
                    allMoviesResult
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
