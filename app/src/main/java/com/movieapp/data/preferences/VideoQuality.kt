package com.movieapp.data.preferences

/**
 * Video Quality Options
 * Defines available video quality settings
 */
enum class VideoQuality {
    /**
     * Automatic quality based on network
     */
    AUTO,
    
    /**
     * 360p resolution
     */
    LOW,
    
    /**
     * 480p resolution
     */
    MEDIUM,
    
    /**
     * 720p resolution
     */
    HD,
    
    /**
     * 1080p resolution
     */
    FULL_HD;
    
    companion object {
        /**
         * Get VideoQuality from string value
         * 
         * @param value String value
         * @return VideoQuality or AUTO as default
         */
        fun fromString(value: String): VideoQuality {
            return entries.find { it.name == value } ?: AUTO
        }
    }
    
    /**
     * Get display name for video quality
     * 
     * @return User-friendly name
     */
    fun getDisplayName(): String {
        return when (this) {
            AUTO -> "Auto"
            LOW -> "360p"
            MEDIUM -> "480p"
            HD -> "720p (HD)"
            FULL_HD -> "1080p (Full HD)"
        }
    }
    
    /**
     * Get resolution height
     * 
     * @return Resolution height in pixels, null for AUTO
     */
    fun getResolutionHeight(): Int? {
        return when (this) {
            AUTO -> null
            LOW -> 360
            MEDIUM -> 480
            HD -> 720
            FULL_HD -> 1080
        }
    }
}
