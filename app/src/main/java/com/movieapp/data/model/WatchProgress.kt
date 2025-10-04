package com.movieapp.data.model

/**
 * Watch Progress data model
 * Represents user's viewing progress for a movie
 * 
 * @property userId User ID
 * @property movieId Movie ID
 * @property currentPositionMs Current playback position in milliseconds
 * @property durationMs Total video duration in milliseconds
 * @property watchPercentage Percentage watched (0.0 to 1.0)
 * @property lastUpdatedAt Timestamp of last update
 * @property isCompleted Whether the movie has been fully watched
 */
data class WatchProgress(
    val userId: String,
    val movieId: Int,
    val currentPositionMs: Long,
    val durationMs: Long,
    val watchPercentage: Float,
    val lastUpdatedAt: Long,
    val isCompleted: Boolean
) {
    companion object {
        /**
         * Calculate watch percentage from position and duration
         * 
         * @param currentPositionMs Current position in milliseconds
         * @param durationMs Total duration in milliseconds
         * @return Watch percentage (0.0 to 1.0)
         */
        fun calculateWatchPercentage(currentPositionMs: Long, durationMs: Long): Float {
            if (durationMs <= 0) return 0f
            val percentage = currentPositionMs.toFloat() / durationMs.toFloat()
            return percentage.coerceIn(0f, 1f)
        }
        
        /**
         * Create new watch progress
         * 
         * @param userId User ID
         * @param movieId Movie ID
         * @param currentPositionMs Current position
         * @param durationMs Total duration
         * @return New WatchProgress instance
         */
        fun create(
            userId: String,
            movieId: Int,
            currentPositionMs: Long,
            durationMs: Long
        ): WatchProgress {
            val percentage = calculateWatchPercentage(currentPositionMs, durationMs)
            return WatchProgress(
                userId = userId,
                movieId = movieId,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                watchPercentage = percentage,
                lastUpdatedAt = System.currentTimeMillis(),
                isCompleted = percentage >= 0.95f
            )
        }
    }
}
