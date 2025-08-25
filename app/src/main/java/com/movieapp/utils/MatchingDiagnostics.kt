package com.movieapp.utils

import com.movieapp.data.model.SupabaseMovie
import com.movieapp.data.model.Movie
import kotlin.math.max

/**
 * Simple diagnostic utility for analyzing TMDB-Supabase matching issues
 * This class helps identify why some movies don't get thumbnails/metadata
 */
class MatchingDiagnostics {
    
    /**
     * Enhanced clean and normalize title for better matching
     */
    private fun cleanTitle(title: String): String {
        return title
            .lowercase()
            // Remove common articles at start
            .replace(Regex("^(the|a|an)\\s+"), "") 
            // Remove years at end in various formats
            .replace(Regex("\\s*[\\(\\[]\\d{4}[\\)\\]]*\\s*$"), "")
            // Remove common movie suffixes and editions
            .replace(Regex("\\s*(director's cut|extended|unrated|theatrical|special edition|remastered|4k|hd|bluray|dvd)\\s*$"), "")
            // Remove special characters and punctuation but keep spaces
            .replace(Regex("[^a-z0-9\\s]"), " ")
            // Normalize multiple spaces to single space
            .replace(Regex("\\s+"), " ")
            // Remove common movie number patterns
            .replace(Regex("\\s+(part|vol|volume|chapter)\\s*\\d+\\s*$"), "")
            .trim()
    }
    
    /**
     * Calculate similarity between two titles using Levenshtein distance
     */
    private fun calculateSimilarity(title1: String, title2: String): Double {
        val clean1 = cleanTitle(title1)
        val clean2 = cleanTitle(title2)
        
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
     * Enhanced method to check if titles match using multiple strategies
     */
    private fun titlesMatch(tmdbTitle: String, supabaseTitle: String): Boolean {
        // Strategy 1: Exact match (case insensitive)
        if (tmdbTitle.equals(supabaseTitle, ignoreCase = true)) {
            return true
        }
        
        // Strategy 2: Clean title match
        val cleanTmdb = cleanTitle(tmdbTitle)
        val cleanSupabase = cleanTitle(supabaseTitle)
        if (cleanTmdb == cleanSupabase && cleanTmdb.isNotEmpty()) {
            return true
        }
        
        // Strategy 3: Similarity match (65% threshold)
        val similarity = calculateSimilarity(tmdbTitle, supabaseTitle)
        if (similarity >= 0.65) {
            return true
        }
        
        // Strategy 4: Keyword matching
        val lowerTmdb = tmdbTitle.lowercase()
        val lowerSupabase = supabaseTitle.lowercase()
        
        if (lowerTmdb.length >= 3 && lowerSupabase.length >= 3) {
            if ((lowerTmdb.contains(lowerSupabase) && lowerSupabase.length >= lowerTmdb.length * 0.4) ||
                (lowerSupabase.contains(lowerTmdb) && lowerTmdb.length >= lowerSupabase.length * 0.4)) {
                return true
            }
        }
        
        // Strategy 5: Word overlap
        val tmdbWords = cleanTmdb.split(" ").filter { it.length > 1 }
        val supabaseWords = cleanSupabase.split(" ").filter { it.length > 1 }
        
        if (tmdbWords.isNotEmpty() && supabaseWords.isNotEmpty()) {
            val matchingWords = tmdbWords.intersect(supabaseWords.toSet()).size
            val totalWords = maxOf(tmdbWords.size, supabaseWords.size)
            if (matchingWords.toDouble() / totalWords >= 0.5) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Analyze a sample of Supabase movies against a list of TMDB movies
     * Returns a detailed diagnostic report
     */
    fun analyzeMatching(
        supabaseMovies: List<SupabaseMovie>,
        tmdbMovies: List<Movie>,
        sampleSize: Int = 100
    ): String {
        val report = StringBuilder()
        
        report.appendLine("🔍 MATCHING ANALYSIS REPORT")
        report.appendLine("============================")
        report.appendLine("📊 Total Supabase Movies: ${supabaseMovies.size}")
        report.appendLine("📊 TMDB Movies Available: ${tmdbMovies.size}")
        report.appendLine("🔬 Sample Size: ${minOf(sampleSize, supabaseMovies.size)}")
        report.appendLine("")
        
        var matchedCount = 0
        var unmatchedCount = 0
        val unmatchedMovies = mutableListOf<SupabaseMovie>()
        val lowQualityMatches = mutableListOf<Pair<SupabaseMovie, Movie>>()
        
        val moviesToAnalyze = supabaseMovies.take(sampleSize)
        
        moviesToAnalyze.forEach { supabaseMovie ->
            var bestMatch: Movie? = null
            var bestScore = 0.0
            
            // Find best matching TMDB movie
            for (tmdbMovie in tmdbMovies) {
                if (titlesMatch(tmdbMovie.title, supabaseMovie.title)) {
                    val similarity = calculateSimilarity(supabaseMovie.title, tmdbMovie.title)
                    if (similarity > bestScore) {
                        bestScore = similarity
                        bestMatch = tmdbMovie
                    }
                }
            }
            
            if (bestMatch != null) {
                matchedCount++
                if (bestScore < 0.8) {
                    lowQualityMatches.add(Pair(supabaseMovie, bestMatch))
                }
            } else {
                unmatchedCount++
                unmatchedMovies.add(supabaseMovie)
            }
        }
        
        val matchPercentage = (matchedCount.toDouble() / moviesToAnalyze.size * 100)
        
        report.appendLine("📊 MATCHING STATISTICS:")
        report.appendLine("----------------------")
        report.appendLine("✅ Matched: $matchedCount movies (${String.format("%.1f", matchPercentage)}%)")
        report.appendLine("❌ Unmatched: $unmatchedCount movies (${String.format("%.1f", 100 - matchPercentage)}%)")
        report.appendLine("⚠️ Low quality matches: ${lowQualityMatches.size} movies")
        report.appendLine("")
        
        // Pattern analysis
        val shortTitles = unmatchedMovies.filter { it.title.length <= 10 }
        val longTitles = unmatchedMovies.filter { it.title.length > 30 }
        val withNumbers = unmatchedMovies.filter { it.title.contains(Regex("\\d+")) }
        val withSpecialChars = unmatchedMovies.filter { it.title.contains(Regex("[^a-zA-Z0-9\\s]")) }
        val foreignLanguage = unmatchedMovies.filter { 
            !it.title.matches(Regex("[a-zA-Z0-9\\s\\p{Punct}]+"))
        }
        
        if (unmatchedMovies.isNotEmpty()) {
            report.appendLine("🔍 UNMATCHED MOVIES ANALYSIS:")
            report.appendLine("-----------------------------")
            
            report.appendLine("📌 Pattern Analysis:")
            report.appendLine("   • Short titles (≤10 chars): ${shortTitles.size}")
            report.appendLine("   • Long titles (>30 chars): ${longTitles.size}")
            report.appendLine("   • With numbers: ${withNumbers.size}")
            report.appendLine("   • With special characters: ${withSpecialChars.size}")
            report.appendLine("   • Possible foreign language: ${foreignLanguage.size}")
            report.appendLine("")
            
            report.appendLine("🎦 SAMPLE UNMATCHED MOVIES (first 10):")
            report.appendLine("--------------------------------------")
            unmatchedMovies.take(10).forEach { movie ->
                val cleanedTitle = cleanTitle(movie.title)
                report.appendLine("❌ '${movie.title}' (cleaned: '$cleanedTitle')")
            }
            report.appendLine("")
        }
        
        if (lowQualityMatches.isNotEmpty()) {
            report.appendLine("⚠️ LOW QUALITY MATCHES (first 5):")
            report.appendLine("----------------------------------")
            lowQualityMatches.take(5).forEach { (supabase, tmdb) ->
                val similarity = calculateSimilarity(supabase.title, tmdb.title)
                report.appendLine("⚠️ '${supabase.title}' → '${tmdb.title}' (${String.format("%.2f", similarity)})")
            }
            report.appendLine("")
        }
        
        // Recommendations
        report.appendLine("💡 RECOMMENDATIONS:")
        report.appendLine("-------------------")
        
        if (matchPercentage < 70) {
            report.appendLine("🚨 CRITICAL: Low match rate (${String.format("%.1f", matchPercentage)}%)")
        }
        
        if (shortTitles.size > moviesToAnalyze.size * 0.1) {
            report.appendLine("• Many short titles - consider expanding search criteria")
        }
        
        if (longTitles.size > moviesToAnalyze.size * 0.1) {
            report.appendLine("• Many long titles - improve title normalization")
        }
        
        if (withSpecialChars.size > moviesToAnalyze.size * 0.15) {
            report.appendLine("• Many special characters - enhance normalization")
        }
        
        if (foreignLanguage.size > moviesToAnalyze.size * 0.1) {
            report.appendLine("• Add foreign language title support")
        }
        
        if (lowQualityMatches.size > matchedCount * 0.2) {
            report.appendLine("• Many low-quality matches - consider manual curation")
        }
        
        report.appendLine("• Threshold adjustments may help improve match rate")
        
        return report.toString()
    }
    
    /**
     * Quick diagnostic for a single movie title
     */
    fun analyzeSingleTitle(
        supabaseTitle: String,
        candidateTmdbTitles: List<String>
    ): String {
        val report = StringBuilder()
        
        report.appendLine("🔍 SINGLE TITLE ANALYSIS")
        report.appendLine("========================")
        report.appendLine("🎬 Supabase Title: '$supabaseTitle'")
        report.appendLine("🧹 Cleaned Title: '${cleanTitle(supabaseTitle)}'")
        report.appendLine("")
        
        val matches = mutableListOf<Pair<String, Double>>()
        
        candidateTmdbTitles.forEach { tmdbTitle ->
            if (titlesMatch(tmdbTitle, supabaseTitle)) {
                val similarity = calculateSimilarity(supabaseTitle, tmdbTitle)
                matches.add(Pair(tmdbTitle, similarity))
            }
        }
        
        if (matches.isNotEmpty()) {
            report.appendLine("✅ POTENTIAL MATCHES:")
            report.appendLine("--------------------")
            matches.sortedByDescending { it.second }.take(5).forEach { (title, score) ->
                report.appendLine("• '$title' (${String.format("%.2f", score)})")
            }
        } else {
            report.appendLine("❌ NO MATCHES FOUND")
            report.appendLine("Consider adjusting matching thresholds or title normalization")
        }
        
        return report.toString()
    }
}