package com.movieapp.data.repository

import com.movieapp.utils.SupabaseRetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Subtitle Repository
 * Fetches subtitles from Supabase for movie playback
 */
class SubtitleRepository {
    
    private val supabaseApi = SupabaseRetrofitInstance.apiInterface
    
    /**
     * Get available subtitle languages for a movie
     * @param movieTitle The movie title to search for
     * @return List of available language codes
     */
    suspend fun getAvailableSubtitleLanguages(movieTitle: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                // Query subtitles table for this movie
                val response = supabaseApi.getSubtitlesByMovie(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    movieTitle = "eq.$movieTitle",
                    select = "language"
                )
                
                if (response.isSuccessful) {
                    val languages = response.body()
                        ?.mapNotNull { it.language }
                        ?.distinct()
                        ?: emptyList()
                    Result.success(languages)
                } else {
                    Result.failure(Exception("Failed to fetch subtitle languages: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get subtitle content for a specific language
     * @param movieTitle The movie title
     * @param language The subtitle language code (e.g., "en", "es", "fr")
     * @return Subtitle content in SRT format
     */
    suspend fun getSubtitleContent(movieTitle: String, language: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseApi.getSubtitlesByMovieAndLanguage(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    movieTitle = "eq.$movieTitle",
                    language = "eq.$language",
                    select = "content"
                )
                
                if (response.isSuccessful) {
                    val content = response.body()?.firstOrNull()?.content
                    if (content != null) {
                        Result.success(content)
                    } else {
                        Result.failure(Exception("No subtitle content found"))
                    }
                } else {
                    Result.failure(Exception("Failed to fetch subtitle: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get all subtitle info for a movie (title, languages, status)
     * @param movieTitle The movie title
     * @return List of subtitle information
     */
    suspend fun getMovieSubtitles(movieTitle: String): Result<List<SubtitleInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseApi.getSubtitlesByMovie(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    movieTitle = "eq.$movieTitle",
                    select = "language,subtitle_status,content"
                )
                
                if (response.isSuccessful) {
                    val subtitles = response.body()?.mapNotNull { subtitle ->
                        if (subtitle.language != null) {
                            SubtitleInfo(
                                language = subtitle.language,
                                languageName = getLanguageName(subtitle.language),
                                hasContent = !subtitle.content.isNullOrEmpty(),
                                status = subtitle.subtitleStatus ?: "unknown"
                            )
                        } else null
                    } ?: emptyList()
                    Result.success(subtitles)
                } else {
                    Result.failure(Exception("Failed to fetch subtitles: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Convert language code to readable name
     */
    private fun getLanguageName(code: String): String {
        return when (code.lowercase()) {
            "en" -> "English"
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            "ru" -> "Russian"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "zh" -> "Chinese"
            "ar" -> "Arabic"
            "hi" -> "Hindi"
            else -> code.uppercase()
        }
    }
}

/**
 * Subtitle information model
 */
data class SubtitleInfo(
    val language: String,
    val languageName: String,
    val hasContent: Boolean,
    val status: String
)
