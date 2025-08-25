package com.movieapp.data.repository

import com.movieapp.data.model.*
import com.movieapp.utils.RetrofitInstance
import com.movieapp.utils.SupabaseRetrofitInstance
import com.movieapp.utils.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.max

/**
 * Combined Movie Repository that integrates TMDB and Supabase data
 * New approach: Get TMDB movies, match with Supabase by title, show only streamable ones
 * TMDB provides all movie data, Supabase provides only video URLs
 * 
 * Features flexible title matching for cases where Supabase titles don't exactly match TMDB
 * Optimized with caching and pre-processing for faster performance
 */
class CombinedMovieRepository {
    
    // Supabase-only in Supabase-first mode
    private val supabaseApi = SupabaseRetrofitInstance.apiInterface
    
    // Cache for Supabase movies to avoid repeated API calls
    private var cachedSupabaseMovies: List<SupabaseMovie>? = null
    private var lastCacheTime = 0L
    private val cacheTimeout = 5 * 60 * 1000L // 5 minutes
    
    // Pre-processed title cache for faster matching
    private val titleCache = mutableMapOf<String, String>()
    
    // Enriched Combined cache (Supabase-first)
    private var cachedEnrichedCombined: List<CombinedMovie>? = null
    private var lastEnrichedCacheTime = 0L
    
    /**
     * Enhanced clean and normalize title for better matching
     * Removes common prefixes, suffixes, and normalizes text more aggressively
     * Public method for external access
     */
    fun cleanTitle(title: String): String {
        return title
            .lowercase()
            // Remove common articles at start
            .replace(Regex("^(the|a|an)\\s+"), "") 
            // Remove years at end in various formats
            .replace(Regex("\\s*[\\(\\[]\\d{4}[\\)\\]]*\\s*$"), "")
            // Remove common movie suffixes and editions (more aggressive)
            .replace(Regex("\\s*(director's cut|extended|unrated|theatrical|special edition|remastered|4k|hd|bluray|dvd|rip|cam|ts|dvdrip|brrip|webrip|hdtv|web-dl)\\s*$"), "")
            // Remove quality indicators
            .replace(Regex("\\s*(720p|1080p|480p|360p|2160p|4k)\\s*"), " ")
            // Remove release group tags
            .replace(Regex("\\s*\\[[^\\]]+\\]\\s*"), " ")
            .replace(Regex("\\s*\\([^\\)]*(?:rip|cam|ts|hdtv|web)[^\\)]*\\)\\s*"), " ")
            // Remove special characters and punctuation but keep spaces
            .replace(Regex("[^a-z0-9\\s]"), " ")
            // Normalize multiple spaces to single space
            .replace(Regex("\\s+"), " ")
            // Remove common movie number patterns
            .replace(Regex("\\s+(part|vol|volume|chapter)\\s*\\d+\\s*$"), "")
            // Remove single standalone numbers at the end
            .replace(Regex("\\s+\\d+\\s*$"), "")
            .trim()
    }
    
    /**
     * Clean and normalize title for better matching (private version)
     * Removes common prefixes, suffixes, and normalizes text
     */
    private fun cleanTitleInternal(title: String): String {
        return cleanTitle(title)
    }
    
    /**
     * Calculate similarity between two titles (0.0 to 1.0)
     * Uses Levenshtein distance algorithm
     */
    private fun calculateSimilarity(title1: String, title2: String): Double {
        val clean1 = cleanTitleInternal(title1)
        val clean2 = cleanTitleInternal(title2)
        
        if (clean1 == clean2) return 1.0
        if (clean1.isEmpty() || clean2.isEmpty()) return 0.0
        
        val maxLength = max(clean1.length, clean2.length)
        val distance = levenshteinDistance(clean1, clean2)
        
        return (maxLength - distance).toDouble() / maxLength
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    /**
     * Enhanced method to check if titles match using multiple advanced strategies
     * Following project specifications with aggressive matching for better match rates
     * Returns true if titles are considered a match
     */
    private fun titlesMatch(tmdbTitle: String, supabaseTitle: String): Boolean {
        // Strategy 1: Exact match (case insensitive)
        if (tmdbTitle.equals(supabaseTitle, ignoreCase = true)) {
            return true
        }
        
        // Strategy 2: Clean title match (removes articles, years, etc.)
        val cleanTmdb = cleanTitle(tmdbTitle)
        val cleanSupabase = cleanTitle(supabaseTitle)
        if (cleanTmdb == cleanSupabase && cleanTmdb.isNotEmpty()) {
            return true
        }
        
        // Strategy 3: Similarity match - lowered to 60% for better matching
        val similarity = calculateSimilarity(tmdbTitle, supabaseTitle)
        if (similarity >= 0.60) { // Further lowered from 0.65 to 0.60 for more matches
            return true
        }
        
        // Strategy 4: Enhanced keyword matching with lower threshold
        val lowerTmdb = tmdbTitle.lowercase()
        val lowerSupabase = supabaseTitle.lowercase()
        
        // If one title is significantly shorter and contained in the other
        if (lowerTmdb.length >= 3 && lowerSupabase.length >= 3) {
            if ((lowerTmdb.contains(lowerSupabase) && lowerSupabase.length >= lowerTmdb.length * 0.3) || // Lowered from 0.4 to 0.3
                (lowerSupabase.contains(lowerTmdb) && lowerTmdb.length >= lowerSupabase.length * 0.3)) {
                return true
            }
        }
        
        // Strategy 5: Enhanced word overlap - lowered threshold for more matches
        val tmdbWords = cleanTmdb.split(" ").filter { it.length > 1 }
        val supabaseWords = cleanSupabase.split(" ").filter { it.length > 1 }
        
        if (tmdbWords.isNotEmpty() && supabaseWords.isNotEmpty()) {
            val matchingWords = tmdbWords.intersect(supabaseWords.toSet()).size
            val totalWords = maxOf(tmdbWords.size, supabaseWords.size)
            if (matchingWords.toDouble() / totalWords >= 0.4) { // Lowered from 0.5 to 0.4
                return true
            }
        }
        
        // Strategy 6: Fuzzy acronym matching
        if (isAcronymMatch(tmdbTitle, supabaseTitle)) {
            return true
        }
        
        // Strategy 7: Common title variations
        if (isCommonVariation(tmdbTitle, supabaseTitle)) {
            return true
        }
        
        // Strategy 8: Significant word overlap with lower threshold
        if (hasSignificantWordOverlap(cleanTmdb, cleanSupabase)) {
            return true
        }
        
        // Strategy 9: Loose phonetic matching
        if (isPhoneticMatch(cleanTmdb, cleanSupabase)) {
            return true
        }
        
        // Strategy 10: Core word matching (NEW - very aggressive)
        if (hasCoreWordMatch(cleanTmdb, cleanSupabase)) {
            return true
        }
        
        // Strategy 11: Partial similarity on cleaned titles (NEW)
        val cleanedSimilarity = calculateSimilarity(cleanTmdb, cleanSupabase)
        if (cleanedSimilarity >= 0.55) { // Lower threshold for cleaned titles
            return true
        }
        
        return false
    }
    
    /**
     * Check if one title could be an acronym of another
     */
    private fun isAcronymMatch(title1: String, title2: String): Boolean {
        val longer = if (title1.length > title2.length) title1 else title2
        val shorter = if (title1.length > title2.length) title2 else title1
        
        if (shorter.length >= 3 && longer.length >= shorter.length * 2) {
            val words = longer.split(" ").filter { it.isNotEmpty() }
            if (words.size >= shorter.length) {
                val acronym = words.map { it.first().lowercase() }.joinToString("")
                if (acronym == shorter.lowercase().replace("[^a-z]".toRegex(), "")) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Check for common title variations and alternate names with more aggressive matching
     */
    private fun isCommonVariation(title1: String, title2: String): Boolean {
        val clean1 = cleanTitle(title1)
        val clean2 = cleanTitle(title2)
        
        // Check if one is a significant subset of the other
        val shorter = if (clean1.length < clean2.length) clean1 else clean2
        val longer = if (clean1.length < clean2.length) clean2 else clean1
        
        if (shorter.length >= 3 && longer.contains(shorter)) { // Lowered from 4 to 3
            val ratio = shorter.length.toDouble() / longer.length
            if (ratio >= 0.4) { // Lowered from 0.5 to 0.4 for more flexibility
                return true
            }
        }
        
        // Check for number variations (e.g., "Movie 2" vs "Movie II")
        val romanNumerals = mapOf(
            "ii" to "2", "iii" to "3", "iv" to "4", "v" to "5",
            "vi" to "6", "vii" to "7", "viii" to "8", "ix" to "9", "x" to "10",
            "1" to "i", "2" to "ii", "3" to "iii", "4" to "iv", "5" to "v"
        )
        
        for ((roman, arabic) in romanNumerals) {
            if ((clean1.contains(roman) && clean2.contains(arabic)) ||
                (clean1.contains(arabic) && clean2.contains(roman))) {
                return true
            }
        }
        
        // Check for common word substitutions
        val substitutions = mapOf(
            "and" to "&", "&" to "and",
            "vs" to "versus", "versus" to "vs",
            "pt" to "part", "part" to "pt",
            "vol" to "volume", "volume" to "vol"
        )
        
        for ((from, to) in substitutions) {
            val replaced1 = clean1.replace(from, to)
            val replaced2 = clean2.replace(from, to)
            if (replaced1 == clean2 || replaced2 == clean1) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check for significant word overlap between titles with lower threshold
     * Implements 40% threshold as specified in project requirements
     */
    private fun hasSignificantWordOverlap(title1: String, title2: String): Boolean {
        val words1 = title1.split(" ").filter { it.length >= 2 }.toSet()
        val words2 = title2.split(" ").filter { it.length >= 2 }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return false
        
        val intersection = words1.intersect(words2)
        val union = words1.union(words2)
        
        // Jaccard similarity: intersection / union - lowered to 35% for better matching
        val jaccardSimilarity = intersection.size.toDouble() / union.size.toDouble()
        
        return jaccardSimilarity >= 0.35 // Lowered from 0.4 to 0.35
    }
    
    /**
     * Core word matching strategy - very aggressive matching
     * Matches if any significant word appears in both titles
     */
    private fun hasCoreWordMatch(title1: String, title2: String): Boolean {
        val words1 = title1.split(" ").filter { it.length >= 3 }.toSet() // Only words 3+ chars
        val words2 = title2.split(" ").filter { it.length >= 3 }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return false
        
        val commonWords = words1.intersect(words2)
        
        // If we have at least one significant common word and titles aren't too different in length
        if (commonWords.isNotEmpty()) {
            val lengthRatio = minOf(title1.length, title2.length).toDouble() / maxOf(title1.length, title2.length).toDouble()
            return lengthRatio >= 0.5 // Titles shouldn't be drastically different in length
        }
        
        return false
    }
    
    /**
     * Simple phonetic matching for common misspellings and variations (new strategy)
     */
    private fun isPhoneticMatch(title1: String, title2: String): Boolean {
        // Common letter substitutions in movie titles
        val substitutions = mapOf(
            "ph" to "f", "ck" to "k", "gh" to "g", "qu" to "kw",
            "x" to "ks", "z" to "s", "c" to "k", "ie" to "y"
        )
        
        fun normalizePhonetically(title: String): String {
            var normalized = title.lowercase()
            for ((from, to) in substitutions) {
                normalized = normalized.replace(from, to)
            }
            return normalized
        }
        
        val phonetic1 = normalizePhonetically(title1)
        val phonetic2 = normalizePhonetically(title2)
        
        // Check if phonetically normalized titles are similar
        val similarity = calculateSimilarity(phonetic1, phonetic2)
        return similarity >= 0.7
    }
    
    /**
     * Get cached title or compute and cache it
     */
    private fun getCachedCleanTitle(title: String): String {
        return titleCache.getOrPut(title) { cleanTitle(title) }
    }

    /**
     * Invalidate in-memory caches to force fresh fetches on next calls.
     */
    fun invalidateCache() {
        // Base caches
        cachedSupabaseMovies = null
        lastCacheTime = 0L
        extendedSupabaseMovies = null
        lastExtendedCacheTime = 0L
        currentLoadedCount = 0
        titleCache.clear()

        // Enriched caches
        cachedEnrichedCombined = null
        lastEnrichedCacheTime = 0L

        println("🧹 Caches invalidated")
    }

    /**
     * Convert a TMDB MovieDetails object to the basic Movie model
     * so that matching utilities expecting Movie can be reused.
     */
    private fun toBasicMovie(details: MovieDetails): Movie {
        return Movie(
            id = details.id,
            title = details.title,
            overview = details.overview,
            posterPath = details.posterPath,
            backdropPath = details.backdropPath,
            releaseDate = details.releaseDate,
            voteAverage = details.voteAverage,
            voteCount = details.voteCount,
            popularity = details.popularity,
            genreIds = details.genres.map { it.id },
            adult = details.adult,
            video = details.video,
            originalLanguage = details.originalLanguage,
            originalTitle = details.originalTitle
        )
    }
    
    // Additional cache for storing extended movie collections
    private var extendedSupabaseMovies: List<SupabaseMovie>? = null
    private var lastExtendedCacheTime = 0L
    private var currentLoadedCount = 0
    
    /**
     * Get cached Supabase movies or fetch them initially with 1000 movies for performance
     * Load more movies on demand when needed
     */
    private suspend fun getCachedSupabaseMovies(): List<SupabaseMovie> {
        val currentTime = System.currentTimeMillis()
        
        // Return cached movies if still valid
        cachedSupabaseMovies?.let { cached ->
            if (currentTime - lastCacheTime < cacheTimeout) {
                println("📋 Using cached Supabase movies: ${cached.size} movies")
                return cached
            }
        }
        
        println("🔄 Fetching initial 1000 Supabase movies for performance...")
        
        // Initially fetch 1000 movies for better content variety
        val initialLimit = 1000
        val allMovies = mutableListOf<SupabaseMovie>()
        var currentPage = 0
        val pageSize = 500 // Larger pages for initial load
        var totalFetched = 0
        
        try {
            while (totalFetched < initialLimit) {
                val startRange = currentPage * pageSize
                val endRange = startRange + pageSize - 1
                val rangeHeader = "$startRange-$endRange"
                
                println("📡 Fetching initial Supabase page $currentPage (range: $rangeHeader)")
                
                val response = supabaseApi.getMoviesWithPagination(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    range = rangeHeader,
                    select = "title,videourl,publishedat", // Get essential fields
                    order = "publishedat.desc",
                    videoUrlFilter = "not.is.null"
                )
                
                if (response.isSuccessful) {
                    val movies = response.body() ?: emptyList()
                    println("✅ Retrieved ${movies.size} movies from Supabase (page $currentPage)")
                    
                    if (movies.isNotEmpty()) {
                        allMovies.addAll(movies)
                        totalFetched += movies.size
                        currentPage++
                        
                        // Stop if we've reached our initial limit or got fewer movies than requested
                        if (totalFetched >= initialLimit || movies.size < pageSize) {
                            println("📄 Initial load complete: ${totalFetched} movies loaded")
                            break
                        }
                    } else {
                        println("📄 No more data available from Supabase")
                        break
                    }
                } else {
                    println("❌ Failed to fetch Supabase movies page $currentPage: ${response.code()} - ${response.message()}")
                    
                    // If first page fails, try the basic method as fallback
                    if (currentPage == 0) {
                        val fallbackResponse = supabaseApi.getAllMoviesWithVideos(
                            apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                            authorization = SupabaseRetrofitInstance.getAuthorizationHeader()
                        )
                        
                        if (fallbackResponse.isSuccessful) {
                            val fallbackMovies = fallbackResponse.body() ?: emptyList()
                            println("🔄 Fallback: Retrieved ${fallbackMovies.size} movies from Supabase")
                            // Take first 1000 from fallback
                            allMovies.addAll(fallbackMovies.take(initialLimit))
                        }
                    }
                    break
                }
            }
        } catch (e: Exception) {
            println("❌ Exception fetching initial Supabase movies: ${e.message}")
            
            // Try fallback method
            try {
                val fallbackResponse = supabaseApi.getAllMoviesWithVideos(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader()
                )
                
                if (fallbackResponse.isSuccessful) {
                    val fallbackMovies = fallbackResponse.body() ?: emptyList()
                    println("🔄 Exception fallback: Retrieved ${fallbackMovies.size} movies from Supabase")
                    // Take first 1000 from fallback
                    allMovies.addAll(fallbackMovies.take(initialLimit))
                }
            } catch (fallbackException: Exception) {
                println("❌ Fallback also failed: ${fallbackException.message}")
                return cachedSupabaseMovies ?: emptyList()
            }
        }
        
        println("🎬 Initial Supabase movies fetched: ${allMovies.size} (out of potential 1440+)")
        
        // Filter out movies without valid video URLs
        val validMovies = allMovies.filter { it.hasValidVideoUrl() }
        println("✅ Valid streamable movies: ${validMovies.size}/${allMovies.size}")
        
        // Cache the results
        cachedSupabaseMovies = validMovies
        lastCacheTime = currentTime
        currentLoadedCount = validMovies.size
        
        return validMovies
    }
    
    /**
     * Load additional movies beyond the initial 1000 when needed
     * This method extends the cache to include more movies from the database
     */
    private suspend fun loadMoreSupabaseMovies(additionalCount: Int = 500): List<SupabaseMovie> {
        val currentTime = System.currentTimeMillis()
        
        // Check if we have extended cache that's still valid
        extendedSupabaseMovies?.let { extended ->
            if (currentTime - lastExtendedCacheTime < cacheTimeout) {
                println("📋 Using extended cached Supabase movies: ${extended.size} movies")
                return extended
            }
        }
        
        println("🔄 Loading additional $additionalCount movies from database...")
        
        val allMovies = mutableListOf<SupabaseMovie>()
        
        // Add currently cached movies first
        cachedSupabaseMovies?.let { cached ->
            allMovies.addAll(cached)
        }
        
        val startFromCount = currentLoadedCount
        val targetTotal = startFromCount + additionalCount
        var currentPage = (startFromCount / 250) // Resume from where we left off
        val pageSize = 250
        var totalFetched = startFromCount
        
        try {
            while (totalFetched < targetTotal) {
                val startRange = currentPage * pageSize
                val endRange = startRange + pageSize - 1
                val rangeHeader = "$startRange-$endRange"
                
                println("📡 Fetching additional Supabase page $currentPage (range: $rangeHeader)")
                
                val response = supabaseApi.getMoviesWithPagination(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    range = rangeHeader,
                    select = "title,videourl,publishedat",
                    order = "publishedat.desc",
                    videoUrlFilter = "not.is.null"
                )
                
                if (response.isSuccessful) {
                    val movies = response.body() ?: emptyList()
                    println("✅ Retrieved ${movies.size} additional movies from Supabase (page $currentPage)")
                    
                    if (movies.isNotEmpty()) {
                        // Only add movies we don't already have
                        val existingTitles = allMovies.map { it.title }.toSet()
                        val newMovies = movies.filter { !existingTitles.contains(it.title) }
                        
                        allMovies.addAll(newMovies)
                        totalFetched += newMovies.size
                        currentPage++
                        
                        // Stop if we've reached our target or got fewer movies than requested
                        if (totalFetched >= targetTotal || movies.size < pageSize) {
                            println("📄 Additional load complete: ${totalFetched} total movies loaded")
                            break
                        }
                    } else {
                        println("📄 No more additional data available from Supabase")
                        break
                    }
                } else {
                    println("❌ Failed to fetch additional Supabase movies page $currentPage: ${response.code()} - ${response.message()}")
                    break
                }
            }
        } catch (e: Exception) {
            println("❌ Exception loading additional Supabase movies: ${e.message}")
        }
        
        println("🎬 Extended Supabase movies total: ${allMovies.size}")
        
        // Filter out movies without valid video URLs
        val validMovies = allMovies.filter { it.hasValidVideoUrl() }
        println("✅ Total valid streamable movies: ${validMovies.size}/${allMovies.size}")
        
        // Update both caches
        extendedSupabaseMovies = validMovies
        lastExtendedCacheTime = currentTime
        currentLoadedCount = validMovies.size
        
        // Also update the main cache
        cachedSupabaseMovies = validMovies
        lastCacheTime = currentTime
        
        return validMovies
    }
    
    /**
     * Find the best matching Supabase movie for a TMDB movie
     * Enhanced with multiple matching strategies and robust validation
     */
    private fun findBestMatch(tmdbMovie: Movie, supabaseMovies: List<SupabaseMovie>): SupabaseMovie? {
        // Minimal, safe matcher: prefer exact title match with valid URL
        if (supabaseMovies.isEmpty()) return null
        val tmdbTitle = tmdbMovie.title.trim()
        return supabaseMovies.firstOrNull { it.hasValidVideoUrl() && it.title.equals(tmdbTitle, ignoreCase = true) }
    }

    /**
     * Enhanced diagnostic method to analyze matching issues with detailed insights
     * Provides a comprehensive report on movies without TMDB matches
     */
    suspend fun analyzeMatchingIssues(sampleSize: Int = 100): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val supabaseMovies = getCachedSupabaseMovies()
                val report = StringBuilder()
                
                report.appendLine("🔍 ENHANCED MATCHING ANALYSIS REPORT")
                report.appendLine("============================================")
                report.appendLine("📊 Total Supabase Movies: ${supabaseMovies.size}")
                report.appendLine("🔬 Sample Size for Analysis: ${minOf(sampleSize, supabaseMovies.size)}")
                report.appendLine("")
                
                var matchedCount = 0
                var unmatchedCount = 0
                val unmatchedMovies = mutableListOf<SupabaseMovie>()
                val lowQualityMatches = mutableListOf<Pair<SupabaseMovie, Movie>>()
                
                // Analyze a sample of movies for performance
                val moviesToAnalyze = supabaseMovies.take(sampleSize)
                
                moviesToAnalyze.forEachIndexed { index, supabaseMovie ->
                    if (index % 25 == 0) {
                        println("🔄 Analyzing movie ${index + 1}/${moviesToAnalyze.size}: '${supabaseMovie.title}'")
                    }
                    
                    val tmdbMatch = findTmdbMovieByTitle(supabaseMovie.title)
                    if (tmdbMatch != null) {
                        matchedCount++
                        
                        // Check match quality
                        val similarity = calculateSimilarity(supabaseMovie.title, tmdbMatch.title)
                        if (similarity < 0.8) {
                            lowQualityMatches.add(Pair(supabaseMovie, tmdbMatch))
                        }
                        
                        if (index < 5) { // Show first 5 matches as examples
                            report.appendLine("✅ MATCHED: '${supabaseMovie.title}' → '${tmdbMatch.title}' (${String.format("%.2f", similarity)})")
                        }
                    } else {
                        unmatchedCount++
                        unmatchedMovies.add(supabaseMovie)
                    }
                }
                
                val matchPercentage = (matchedCount.toDouble() / moviesToAnalyze.size * 100)
                
                report.appendLine("")
                report.appendLine("📊 MATCHING STATISTICS:")
                report.appendLine("----------------------")
                report.appendLine("✅ Matched: $matchedCount movies (${String.format("%.1f", matchPercentage)}%)")
                report.appendLine("❌ Unmatched: $unmatchedCount movies (${String.format("%.1f", 100 - matchPercentage)}%)")
                report.appendLine("⚠️ Low quality matches: ${lowQualityMatches.size} movies")
                report.appendLine("")
                
                // Pattern analysis variables (declare outside conditional blocks)
                val shortTitles = unmatchedMovies.filter { it.title.length <= 10 }
                val longTitles = unmatchedMovies.filter { it.title.length > 30 }
                val withNumbers = unmatchedMovies.filter { it.title.contains(Regex("\\d+")) }
                val withSpecialChars = unmatchedMovies.filter { it.title.contains(Regex("[^a-zA-Z0-9\\s]")) }
                val foreignLanguage = unmatchedMovies.filter { 
                    !it.title.matches(Regex("[a-zA-Z0-9\\s\\p{Punct}]+"))
                }
                
                // Detailed unmatched analysis
                if (unmatchedMovies.isNotEmpty()) {
                    report.appendLine("🔍 UNMATCHED MOVIES DETAILED ANALYSIS:")
                    report.appendLine("------------------------------------")
                    
                    report.appendLine("📌 Pattern Analysis:")
                    report.appendLine("   • Short titles (≤10 chars): ${shortTitles.size}")
                    report.appendLine("   • Long titles (>30 chars): ${longTitles.size}")
                    report.appendLine("   • With numbers: ${withNumbers.size}")
                    report.appendLine("   • With special characters: ${withSpecialChars.size}")
                    report.appendLine("   • Possible foreign language: ${foreignLanguage.size}")
                    report.appendLine("")
                    
                    // Show detailed breakdown of problematic titles
                    report.appendLine("🎦 SAMPLE UNMATCHED MOVIES (detailed analysis):")
                    report.appendLine("----------------------------------------------")
                    unmatchedMovies.take(15).forEach { movie ->
                        val cleanedTitle = cleanTitle(movie.title)
                        val titleLength = movie.title.length
                        val hasNumbers = movie.title.contains(Regex("\\d+"))
                        val hasSpecialChars = movie.title.contains(Regex("[^a-zA-Z0-9\\s]"))
                        
                        report.appendLine("❌ '${movie.title}'")
                        report.appendLine("   → Cleaned: '$cleanedTitle'")
                        report.appendLine("   → Length: $titleLength | Numbers: $hasNumbers | Special chars: $hasSpecialChars")
                        report.appendLine("")
                    }
                }
                
                // Low quality matches analysis
                if (lowQualityMatches.isNotEmpty()) {
                    report.appendLine("⚠️ LOW QUALITY MATCHES ANALYSIS:")
                    report.appendLine("--------------------------------")
                    report.appendLine("These movies were matched but with low similarity scores:")
                    report.appendLine("")
                    
                    lowQualityMatches.take(10).forEach { (supabase, tmdb) ->
                        val similarity = calculateSimilarity(supabase.title, tmdb.title)
                        report.appendLine("⚠️ '${supabase.title}' → '${tmdb.title}' (${String.format("%.2f", similarity)})")
                    }
                    report.appendLine("")
                }
                
                // Provide actionable recommendations
                report.appendLine("💡 ACTIONABLE RECOMMENDATIONS:")
                report.appendLine("------------------------------")
                
                if (matchPercentage < 70) {
                    report.appendLine("🚨 CRITICAL: Low match rate (${String.format("%.1f", matchPercentage)}%) - requires immediate attention")
                }
                
                if (shortTitles.size > moviesToAnalyze.size * 0.1) {
                    report.appendLine("• Consider expanding search to include TMDB keywords for short titles")
                }
                
                if (longTitles.size > moviesToAnalyze.size * 0.1) {
                    report.appendLine("• Implement better title truncation for long titles")
                }
                
                if (withSpecialChars.size > moviesToAnalyze.size * 0.15) {
                    report.appendLine("• Enhance special character normalization in cleanTitle method")
                }
                
                if (foreignLanguage.size > moviesToAnalyze.size * 0.1) {
                    report.appendLine("• Add support for foreign language title matching")
                }
                
                if (lowQualityMatches.size > matchedCount * 0.2) {
                    report.appendLine("• Many low-quality matches - consider manual curation for top movies")
                }
                
                report.appendLine("• Test the new matching strategies (word overlap & phonetic matching)")
                report.appendLine("• Consider implementing manual title mapping for frequently accessed unmatchable movies")
                
                val finalReport = report.toString()
                println(finalReport)
                Result.success(finalReport)
            } catch (e: Exception) {
                val errorMsg = "❌ analyzeMatchingIssues failed: ${e.message ?: "Unknown error"}"
                println(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        }
    }
    
    /**
     * Enhanced method to get enriched movies starting with 1000 movies for performance
     * Can load more movies on demand using loadMoreSupabaseMovies()
     * Supabase-first approach: Fetch Supabase movies, then enrich with TMDB data
     */
    suspend fun getAllEnrichedMovies(loadExtended: Boolean = false): Result<List<CombinedMovie>> {
        return withContext(Dispatchers.IO) {
            try {
                println("🎬 Starting getAllEnrichedMovies - Supabase-first approach (loadExtended: $loadExtended)")
                
                // Step 1: Fetch movies from Supabase (initial 1000 or extended collection)
                val supabaseMovies = if (loadExtended) {
                    loadMoreSupabaseMovies(500) // Load additional 500 movies
                } else {
                    getCachedSupabaseMovies() // Get initial 1000 movies
                }
                
                if (supabaseMovies.isEmpty()) {
                    println("⚠️ No Supabase movies found!")
                    return@withContext Result.success(emptyList<CombinedMovie>())
                }
                
                println("📁 Found ${supabaseMovies.size} movies in Supabase database")
                
                // Step 2: For each Supabase movie, try to find TMDB metadata
                val enrichedMovies = mutableListOf<CombinedMovie>()
                val tmdbCache = mutableMapOf<String, Movie?>()
                
                supabaseMovies.forEachIndexed { index, supabaseMovie ->
                    if (index % 50 == 0) {
                        println("🔄 Processing movie ${index + 1}/${supabaseMovies.size}: '${supabaseMovie.title}'")
                    }
                    
                    try {
                        // Check cache first to avoid duplicate TMDB searches
                        val cleanTitle = cleanTitle(supabaseMovie.title)
                        val cachedTmdbMovie = tmdbCache[cleanTitle]
                        
                        val tmdbMovie = if (cachedTmdbMovie != null) {
                            cachedTmdbMovie
                        } else {
                            // Search TMDB for this movie
                            val tmdbResult = findTmdbMovieByTitle(supabaseMovie.title)
                            tmdbCache[cleanTitle] = tmdbResult
                            tmdbResult
                        }
                        
                        if (tmdbMovie != null) {
                            // Create enriched CombinedMovie with TMDB metadata
                            val combinedMovie = CombinedMovie(tmdbMovie, supabaseMovie)
                            // Only include if we have at least one thumbnail
                            if (!combinedMovie.tmdbMovie.posterPath.isNullOrBlank() || !combinedMovie.tmdbMovie.backdropPath.isNullOrBlank()) {
                                enrichedMovies.add(combinedMovie)
                            }
                            
                            if (index % 50 == 0) {
                                println("✅ Enriched '${supabaseMovie.title}' with TMDB data")
                            }
                        } else {
                            // Create CombinedMovie with Supabase data only (fallback)
                            val fallbackTmdbMovie = createFallbackTmdbMovie(supabaseMovie)
                            val combinedMovie = CombinedMovie(fallbackTmdbMovie, supabaseMovie)
                            // Skip fallback entries without thumbnails
                            if (!combinedMovie.tmdbMovie.posterPath.isNullOrBlank() || !combinedMovie.tmdbMovie.backdropPath.isNullOrBlank()) {
                                enrichedMovies.add(combinedMovie)
                            }
                            
                            if (index % 50 == 0) {
                                println("🔄 No TMDB match for '${supabaseMovie.title}', using fallback data")
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ Error processing '${supabaseMovie.title}': ${e.message ?: "Unknown error"}")
                        
                        // Create fallback CombinedMovie to ensure movie is still included
                        try {
                            val fallbackTmdbMovie = createFallbackTmdbMovie(supabaseMovie)
                            val combinedMovie = CombinedMovie(fallbackTmdbMovie, supabaseMovie)
                            if (!combinedMovie.tmdbMovie.posterPath.isNullOrBlank() || !combinedMovie.tmdbMovie.backdropPath.isNullOrBlank()) {
                                enrichedMovies.add(combinedMovie)
                            }
                        } catch (fallbackException: Exception) {
                            println("❌ Failed to create fallback for '${supabaseMovie.title}': ${fallbackException.message ?: "Unknown error"}")
                        }
                    }
                }
                
                println("🎆 Final result: ${enrichedMovies.size}/${supabaseMovies.size} movies enriched and ready (thumbnails required)")

                // Extra safety: ensure only movies with thumbnails make it through
                val filtered = enrichedMovies.filter { !it.tmdbMovie.posterPath.isNullOrBlank() || !it.tmdbMovie.backdropPath.isNullOrBlank() }
                    .distinctBy { it.id }

                Result.success(filtered)
            } catch (e: Exception) {
                println("❌ getAllEnrichedMovies failed: ${e.message ?: "Unknown error"}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create a fallback TMDB movie object for Supabase movies without TMDB matches
     * This ensures all Supabase movies are included even without TMDB metadata
     */
    private fun createFallbackTmdbMovie(supabaseMovie: SupabaseMovie): Movie {
        return Movie(
            id = supabaseMovie.title.hashCode(), // Generate stable ID from title
            title = supabaseMovie.title,
            overview = "Movie available for streaming", // Generic description
            posterPath = null, // No poster available
            backdropPath = null, // No backdrop available
            releaseDate = "2024-01-01", // Default date
            voteAverage = 7.0, // Default rating
            voteCount = 100, // Default vote count
            popularity = 50.0, // Default popularity
            genreIds = emptyList(), // No genre information
            adult = false,
            video = false,
            originalLanguage = "en",
            originalTitle = supabaseMovie.title
        )
    }

    // ===================== Disabled TMDB helpers (Supabase-first) =====================

    /**
     * Stubbed TMDB title search. Returns null and logs that TMDB is disabled.
     */
    private suspend fun findTmdbMovieByTitle(title: String): Movie? {
        println("⛔ TMDB search disabled (Supabase-first): title='$title'")
        return null
    }

    /**
     * Stubbed TMDB query search. Returns null and logs that TMDB is disabled.
     */
    private suspend fun searchTmdbWithQuery(query: String): Movie? {
        println("⛔ TMDB query disabled (Supabase-first): query='$query'")
        return null
    }

    // ===================== Supabase-only enriched helpers =====================

    /**
     * Build a TMDB-like Movie model from a Supabase enriched row so UI can reuse
     * existing rendering logic without runtime TMDB calls.
     */
    private fun toMovieFromEnriched(enriched: SupabaseEnrichedMovie): Movie {
        return Movie(
            id = enriched.tmdbId ?: enriched.id.hashCode(), // Prefer real TMDB id; fallback to stable local hash
            title = enriched.title,
            overview = enriched.overview ?: "",
            posterPath = enriched.posterPath,
            backdropPath = enriched.backdropPath,
            releaseDate = enriched.releaseDate ?: "",
            voteAverage = enriched.voteAverage ?: 0.0,
            voteCount = enriched.voteCount ?: 0,
            popularity = enriched.popularity ?: 0.0,
            genreIds = enriched.genreIds ?: emptyList(),
            adult = false,
            video = false,
            originalLanguage = enriched.originalLanguage ?: "",
            originalTitle = enriched.originalTitle ?: enriched.title
        )
    }

    /**
     * Convert enriched Supabase row to minimal SupabaseMovie for streaming URL validation.
     */
    private fun toSupabaseMovieFromEnriched(enriched: SupabaseEnrichedMovie): SupabaseMovie {
        return SupabaseMovie(
            title = enriched.title,
            videoUrl = enriched.videoUrl ?: ""
        )
    }

    /**
     * Map enriched rows to CombinedMovie, keeping existing UI contracts.
     */
    private fun toCombinedFromEnriched(enriched: SupabaseEnrichedMovie): CombinedMovie {
        val tmdbLike = toMovieFromEnriched(enriched)
        val sb = toSupabaseMovieFromEnriched(enriched)
        return CombinedMovie(
            tmdbMovie = tmdbLike,
            supabaseMovie = sb,
            tmdbMovieDetails = null,
            runtimeMinutes = enriched.runtime,
            genreNames = enriched.genresJson?.map { it.name },
            companyNames = enriched.companiesJson?.map { it.name }
        )
    }

    /**
     * Compute Supabase Range header for pagination.
     */
    private fun pageRange(page: Int, pageSize: Int = 20): String {
        val p = if (page < 1) 1 else page
        val start = (p - 1) * pageSize
        val end = start + pageSize - 1
        return "$start-$end"
    }

    

    /**
     * Fetch all enriched movies from Supabase with pagination and cache.
     * Applies strict filters: must have valid videourl and at least one thumbnail.
     */
    private suspend fun getAllEnrichedMovies(): Result<List<CombinedMovie>> {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                cachedEnrichedCombined?.let { cached ->
                    if (now - lastEnrichedCacheTime < cacheTimeout) {
                        return@withContext Result.success(cached)
                    }
                }

                val pageSize = 200
                var page = 1
                val collected = mutableListOf<SupabaseEnrichedMovie>()

                while (true) {
                    val range = pageRange(page, pageSize)
                    val response = supabaseApi.getEnrichedPopular(
                        apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                        authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                        range = range
                    )
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Supabase enriched fetch failed: ${response.code()} ${response.message()}"))
                    }
                    val batch = response.body().orEmpty()
                    if (batch.isEmpty()) break
                    collected.addAll(batch)
                    if (batch.size < pageSize) break
                    page += 1
                    // Safety cap to avoid excessive paging
                    if (page > 25) break
                }

                val combined = collected.asSequence()
                    .filter { it.hasValidVideoUrl() && it.hasPosterOrBackdrop() && it.tmdbId != null }
                    .map { toCombinedFromEnriched(it) }
                    .distinctBy { it.id }
                    .toList()

                cachedEnrichedCombined = combined
                lastEnrichedCacheTime = now
                Result.success(combined)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ===================== Supabase-only enriched fetchers =====================

    /**
     * Supabase-only: Popular movies by popularity.desc
     */
    suspend fun getPopularMovies(page: Int = 1): Result<List<CombinedMovie>> {
        return withContext(Dispatchers.IO) {
            try {
                val range = pageRange(page)
                val response = supabaseApi.getEnrichedPopular(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    range = range
                )
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Supabase popular failed: ${response.code()} ${response.message()}"))
                }
                val body = response.body() ?: emptyList()
                val items = body
                    .filter { it.hasValidVideoUrl() && it.hasPosterOrBackdrop() && it.tmdbId != null }
                    .map { toCombinedFromEnriched(it) }
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Supabase-only: Trending (use popularity window; same as popular for backend simplicity)
     */
    suspend fun getTrendingMovies(timeWindow: String = "week"): Result<List<CombinedMovie>> {
        // Ignoring timeWindow on backend; popularity is dynamic enough
        return getPopularMovies(page = 1)
    }

    /**
     * Supabase-only: Top rated by vote_average.desc
     */
    suspend fun getTopRatedMovies(page: Int = 1): Result<List<CombinedMovie>> {
        return withContext(Dispatchers.IO) {
            try {
                val range = pageRange(page)
                val response = supabaseApi.getEnrichedTopRated(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    range = range
                )
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Supabase top-rated failed: ${response.code()} ${response.message()}"))
                }
                val body = response.body() ?: emptyList()
                val items = body
                    .filter { it.hasValidVideoUrl() && it.hasPosterOrBackdrop() }
                    .map { toCombinedFromEnriched(it) }
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Supabase-only: Search by title using ilike
     */
    suspend fun searchAvailableMovies(query: String, page: Int = 1, pageSize: Int = 20): Result<List<CombinedMovie>> {
        return withContext(Dispatchers.IO) {
            try {
                val range = pageRange(page, pageSize)
                val ilike = "ilike.*${query}*"
                val response = supabaseApi.searchEnrichedByTitle(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    range = range,
                    titleIlike = ilike
                )
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Supabase search failed: ${response.code()} ${response.message()}"))
                }
                val body = response.body() ?: emptyList()
                val items = body
                    .filter { it.hasValidVideoUrl() && it.hasPosterOrBackdrop() }
                    .map { toCombinedFromEnriched(it) }
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get paginated enriched movies from your complete Supabase database
     * This replaces the TMDB-first approach with Supabase-first approach
     */
    suspend fun getEnrichedMoviesPaginated(page: Int = 1, pageSize: Int = 20): Result<List<CombinedMovie>> {
        return withContext(Dispatchers.IO) {
            try {
                // Get all enriched movies
                val allMoviesResult = getAllEnrichedMovies()
                
                if (allMoviesResult.isFailure) {
                    return@withContext allMoviesResult
                }
                
                val allMovies = allMoviesResult.getOrNull() ?: emptyList()
                
                // Calculate pagination
                val startIndex = (page - 1) * pageSize
                val endIndex = kotlin.math.min(startIndex + pageSize, allMovies.size)
                
                val paginatedMovies = if (startIndex < allMovies.size) {
                    allMovies.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }
                
                println("📄 Page $page: Returning ${paginatedMovies.size} movies (total available: ${allMovies.size})")
                
                Result.success(paginatedMovies)
            } catch (e: Exception) {
                println("❌ getEnrichedMoviesPaginated failed: ${e.message ?: "Unknown error"}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get streamable movies by combining TMDB data with Supabase video URLs
     * Enhanced with robust matching and validation for streamable content
     */
    suspend fun getAvailableMovies(page: Int = 1): Result<List<CombinedMovie>> {
        // Supabase-first: return enriched, streamable movies without any TMDB calls
        return getEnrichedMoviesPaginated(page = page, pageSize = 20)
    }

    /**
     * Fetch a single movie with streaming details by TMDB ID.
     * Supabase-first: query enriched movie by tmdb_id (tmdb_id=eq.<tmdbId>) from Supabase only.
     */
    suspend fun getMovieWithStreamDetails(tmdbId: Int): Result<CombinedMovie?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseApi.getEnrichedById(
                    apiKey = SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = SupabaseRetrofitInstance.getAuthorizationHeader(),
                    tmdbIdEq = "eq.$tmdbId"
                )

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Supabase getEnrichedById failed: ${response.code()} - ${response.message()}"))
                }

                val enriched = response.body()?.firstOrNull()
                if (enriched == null) {
                    return@withContext Result.success(null)
                }

                // Ensure we have a playable video URL and at least one thumbnail
                if (!enriched.hasValidVideoUrl() || !enriched.hasPosterOrBackdrop()) {
                    return@withContext Result.success(null)
                }

                // Map Supabase enriched fields to our basic Movie model
                val mappedMovie = Movie(
                    id = tmdbId,
                    title = enriched.title,
                    overview = enriched.overview ?: "",
                    posterPath = enriched.posterPath,
                    backdropPath = enriched.backdropPath,
                    releaseDate = enriched.releaseDate ?: "",
                    voteAverage = enriched.voteAverage ?: 0.0,
                    voteCount = enriched.voteCount ?: 0,
                    popularity = enriched.popularity ?: 0.0,
                    genreIds = enriched.genreIds ?: emptyList(),
                    adult = false,
                    video = false,
                    originalLanguage = enriched.originalLanguage ?: "en",
                    originalTitle = enriched.originalTitle ?: enriched.title
                )

                val supabaseMovie = SupabaseMovie(
                    title = enriched.title,
                    videoUrl = enriched.videoUrl ?: ""
                )

                Result.success(CombinedMovie(
                    tmdbMovie = mappedMovie,
                    supabaseMovie = supabaseMovie,
                    tmdbMovieDetails = null,
                    runtimeMinutes = enriched.runtime,
                    genreNames = enriched.genresJson?.map { it.name },
                    companyNames = enriched.companiesJson?.map { it.name }
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Search streamable movies by query using Supabase-first approach
     */
    suspend fun searchAvailableMovies(query: String): Result<List<CombinedMovie>> {
        // Delegate to Supabase enriched search with a sensible page size.
        // This avoids any TMDB search calls and returns already-enriched records.
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())
        return searchAvailableMovies(trimmed, page = 1, pageSize = 40)
    }

    /**
     * Get popular movies using consistent approach (same pattern as getTopRatedMoviesConsistent)
     */
    suspend fun getPopularMoviesConsistent(page: Int = 1): Result<List<CombinedMovie>> {
        // Supabase-first: derive "popular" from enriched list, sorted by popularity
        val all = getAllEnrichedMovies()
        if (all.isFailure) return all
        val items = all.getOrNull().orEmpty().sortedByDescending { it.tmdbMovie.popularity }
        val start = (page - 1) * 20
        val end = kotlin.math.min(start + 20, items.size)
        return Result.success(if (start in 0 until items.size) items.subList(start, end) else emptyList())
    }

    /**
     * Get trending movies using consistent approach (same pattern as getTopRatedMoviesConsistent)
     */
    suspend fun getTrendingMoviesConsistent(timeWindow: String = "week"): Result<List<CombinedMovie>> {
        // Supabase-first: treat trending as popularity-sorted list
        val all = getAllEnrichedMovies()
        if (all.isFailure) return all
        val items = all.getOrNull().orEmpty().sortedByDescending { it.tmdbMovie.popularity }.take(20)
        return Result.success(items)
    }

    /**
     * Simple category router used by the ViewModel's efficient loading path
     */
    suspend fun getMoviesBySimpleCategory(
        category: String,
        page: Int = 1,
        pageSize: Int = 20,
        excludedIds: Set<Int> = emptySet()
    ): Result<List<CombinedMovie>> {
        val baseResult: Result<List<CombinedMovie>> = when (category.lowercase()) {
            // Use Supabase-first enriched endpoints/filters
            "trending" -> getPopularMovies(page) // backend uses popularity; good proxy for trending
            "popular" -> getPopularMovies(page)
            "toprated", "top_rated", "top-rated" -> getTopRatedMovies(page)
            // For years and genres, use enriched list with light filters
            "2024" -> getSupabaseFirstMovies("2024", page = page, excludedIds = excludedIds, pageSize = pageSize)
            "2023" -> getSupabaseFirstMovies("2023", page = page, excludedIds = excludedIds, pageSize = pageSize)
            "horror" -> getSupabaseFirstMovies("horror", page = page, excludedIds = excludedIds, pageSize = pageSize)
            "action" -> getSupabaseFirstMovies("action", page = page, excludedIds = excludedIds, pageSize = pageSize)
            "comedy" -> getSupabaseFirstMovies("comedy", page = page, excludedIds = excludedIds, pageSize = pageSize)
            "drama" -> getSupabaseFirstMovies("drama", page = page, excludedIds = excludedIds, pageSize = pageSize)
            else -> Result.success(emptyList())
        }

        return baseResult.map { list ->
            list.asSequence()
                .filter { it.id !in excludedIds }
                .distinctBy { it.id }
                .take(pageSize)
                .toList()
        }
    }

    /**
     * Very simple Supabase-first pagination over enriched movies
     * Used by ViewModel infinite scroll for any section without strict filtering
     */
    suspend fun getSupabaseMoviesSimple(
        page: Int = 1,
        pageSize: Int = 20,
        excludedIds: Set<Int> = emptySet()
    ): Result<List<CombinedMovie>> {
        // Reuse enriched movies list (Supabase-first enrichment)
        val allResult = getAllEnrichedMovies()
        if (allResult.isFailure) return allResult

        val all = allResult.getOrNull().orEmpty()
        val filtered = all.asSequence()
            .filter { it.id !in excludedIds }
            .distinctBy { it.id }
            .toList()

        val startIndex = (page - 1) * pageSize
        val endIndex = kotlin.math.min(startIndex + pageSize, filtered.size)
        val pageItems = if (startIndex in 0 until filtered.size) filtered.subList(startIndex, endIndex) else emptyList()
        return Result.success(pageItems)
    }

    /**
     * Supabase-first category loader. Pulls enriched list then applies light filters.
     * Falls back to simple pagination for categories without clear filters.
     */
    suspend fun getSupabaseFirstMovies(
        category: String,
        page: Int = 1,
        excludedIds: Set<Int> = emptySet(),
        pageSize: Int = 20
    ): Result<List<CombinedMovie>> {
        val allResult = getAllEnrichedMovies()
        if (allResult.isFailure) return allResult

        val all = allResult.getOrNull().orEmpty()

        // Apply category-specific filtering using TMDB metadata if available
        val filteredByCategory: List<CombinedMovie> = when (category.lowercase()) {
            "toprated", "top_rated", "top-rated" ->
                all.sortedByDescending { it.tmdbMovie.voteAverage }
            "2024" -> all.filter { it.tmdbMovie.releaseDate.startsWith("2024") }
            "2023" -> all.filter { it.tmdbMovie.releaseDate.startsWith("2023") }
            "horror" -> all.filter { (it.tmdbMovie.genreIds).contains(27) }
            "action" -> all.filter { (it.tmdbMovie.genreIds).contains(28) }
            "comedy" -> all.filter { (it.tmdbMovie.genreIds).contains(35) }
            "drama" -> all.filter { (it.tmdbMovie.genreIds).contains(18) }
            // For trending/popular or unknown: just use simple pagination of enriched list
            else -> all
        }

        val filtered = filteredByCategory.asSequence()
            .filter { it.id !in excludedIds }
            .distinctBy { it.id }
            .toList()

        val startIndex = (page - 1) * pageSize
        val endIndex = kotlin.math.min(startIndex + pageSize, filtered.size)
        val pageItems = if (startIndex in 0 until filtered.size) filtered.subList(startIndex, endIndex) else emptyList()
        return Result.success(pageItems)
    }

 

    /**
     * Get top rated movies using consistent approach (same pattern as getMoviesByYear)
     */
    suspend fun getTopRatedMoviesConsistent(page: Int = 1): Result<List<CombinedMovie>> {
        // Supabase-first: sort enriched by voteAverage
        val all = getAllEnrichedMovies()
        if (all.isFailure) return all
        val items = all.getOrNull().orEmpty().sortedByDescending { it.tmdbMovie.voteAverage }
        val start = (page - 1) * 20
        val end = kotlin.math.min(start + 20, items.size)
        return Result.success(if (start in 0 until items.size) items.subList(start, end) else emptyList())
    }

    /**
     * Get trending movies with deduplication support for pagination
     */
    suspend fun getTrendingMoviesWithDeduplication(
        timeWindow: String = "week",
        excludedIds: Set<Int> = emptySet(),
        targetCount: Int = 20
    ): Result<List<CombinedMovie>> {
        // Supabase-first: trending = popularity-sorted, dedup by id, exclude provided ids
        val all = getAllEnrichedMovies()
        if (all.isFailure) return all
        val items = all.getOrNull().orEmpty()
            .asSequence()
            .filter { it.id !in excludedIds }
            .sortedByDescending { it.tmdbMovie.popularity }
            .distinctBy { it.id }
            .take(targetCount)
            .toList()
        return Result.success(items)
    }

    /**
     * Get popular movies with deduplication support for pagination
     */
    suspend fun getPopularMoviesWithDeduplication(
        page: Int = 1,
        excludedIds: Set<Int> = emptySet(),
        targetCount: Int = 20
    ): Result<List<CombinedMovie>> {
        val all = getAllEnrichedMovies()
        if (all.isFailure) return all
        val items = all.getOrNull().orEmpty()
            .asSequence()
            .filter { it.id !in excludedIds }
            .sortedByDescending { it.tmdbMovie.popularity }
            .distinctBy { it.id }
            .take(targetCount)
            .toList()
        return Result.success(items)
    }

    /**
     * Get movies by genre with deduplication support for pagination
     * @param genreId The genre ID to filter by
     * @param page The starting page number
     * @param excludedIds Set of movie IDs to exclude (already shown movies)
     * @param targetCount Number of new movies to fetch (default 20)
     * @return Result containing list of new unique movies
     */
    suspend fun getMoviesByGenreWithDeduplication(
        genreId: Int,
        page: Int = 1,
        excludedIds: Set<Int> = emptySet(),
        targetCount: Int = 20
    ): Result<List<CombinedMovie>> {
        val all = getAllEnrichedMovies()
        if (all.isFailure) return all
        val items = all.getOrNull().orEmpty()
            .asSequence()
            .filter { it.tmdbMovie.genreIds.contains(genreId) }
            .filter { it.id !in excludedIds }
            .sortedByDescending { it.tmdbMovie.popularity }
            .distinctBy { it.id }
            .take(targetCount)
            .toList()
        return Result.success(items)
    }

    suspend fun getMoviesByGenre(genreId: Int, page: Int = 1): Result<List<CombinedMovie>> {
        val all = getAllEnrichedMovies()
        if (all.isFailure) return all
        val filtered = all.getOrNull().orEmpty().filter { it.tmdbMovie.genreIds.contains(genreId) }
        val start = (page - 1) * 20
        val end = kotlin.math.min(start + 20, filtered.size)
        return Result.success(if (start in 0 until filtered.size) filtered.subList(start, end) else emptyList())
    }

    /**
     * Supabase-first: Movies by year with simple pagination
     */
    suspend fun getMoviesByYear(year: Int, page: Int = 1): Result<List<CombinedMovie>> {
        val all = getAllEnrichedMovies()
        if (all.isFailure) return all
        val yearPrefix = year.toString()
        val filtered = all.getOrNull().orEmpty()
            .filter { it.tmdbMovie.releaseDate.startsWith(yearPrefix) }
            .sortedByDescending { it.tmdbMovie.popularity }
        val start = (page - 1) * 20
        val end = kotlin.math.min(start + 20, filtered.size)
        return Result.success(if (start in 0 until filtered.size) filtered.subList(start, end) else emptyList())
    }

    /**
     * Supabase-first: Movies by year with deduplication and exclusions
     */
    suspend fun getMoviesByYearWithDeduplication(
        year: Int,
        page: Int = 1,
        excludedIds: Set<Int> = emptySet(),
        targetCount: Int = 20
    ): Result<List<CombinedMovie>> {
        val all = getAllEnrichedMovies()
        if (all.isFailure) return all
        val yearPrefix = year.toString()
        val items = all.getOrNull().orEmpty()
            .asSequence()
            .filter { it.tmdbMovie.releaseDate.startsWith(yearPrefix) }
            .filter { it.id !in excludedIds }
            .sortedByDescending { it.tmdbMovie.popularity }
            .distinctBy { it.id }
            .drop((page - 1) * targetCount)
            .take(targetCount)
            .toList()
        return Result.success(items)
    }

    /**
     * Supabase-first: Top rated with deduplication and exclusions
     */
    suspend fun getTopRatedMoviesWithDeduplication(
        page: Int = 1,
        excludedIds: Set<Int> = emptySet(),
        targetCount: Int = 20
    ): Result<List<CombinedMovie>> {
        val all = getAllEnrichedMovies()
        if (all.isFailure) return all
        val items = all.getOrNull().orEmpty()
            .asSequence()
            .filter { it.id !in excludedIds }
            .sortedByDescending { it.tmdbMovie.voteAverage }
            .distinctBy { it.id }
            .drop((page - 1) * targetCount)
            .take(targetCount)
            .toList()
        return Result.success(items)
    }
    
    /**
     * Get more movies from your Supabase database for horizontal pagination
     * This method provides additional content beyond TMDB results
     */
    suspend fun getMoreMoviesFromDatabase(excludedTitles: Set<String>, limit: Int = 20): Result<List<CombinedMovie>> {
        return withContext(Dispatchers.IO) {
            try {
                val allSupabaseMovies = getCachedSupabaseMovies()
                
                if (allSupabaseMovies.isEmpty()) {
                    return@withContext Result.success(emptyList<CombinedMovie>())
                }
                
                val additionalMovies = mutableListOf<CombinedMovie>()
                var processedCount = 0
                
                for (supabaseMovie in allSupabaseMovies) {
                    if (additionalMovies.size >= limit) break
                    
                    val cleanSupabaseTitle = cleanTitle(supabaseMovie.title)
                    
                    // Skip movies that are already displayed
                    if (!excludedTitles.contains(cleanSupabaseTitle)) {
                        // Supabase-first: avoid TMDB enrichment; rely on fallback mapping only
                        val fallbackMovie = createFallbackTmdbMovie(supabaseMovie)
                        additionalMovies.add(CombinedMovie(fallbackMovie, supabaseMovie))
                    }
                    
                    processedCount++
                    
                    // Avoid processing too many at once for performance
                    if (processedCount >= 100) break
                }
                
                println("📄 Pagination: Added ${additionalMovies.size} more movies from database")
                
                Result.success(additionalMovies)
            } catch (e: Exception) {
                println("❌ getMoreMoviesFromDatabase failed: ${e.message ?: "Unknown error"}")
                Result.failure(e)
            }
        }
    }
    
    
}