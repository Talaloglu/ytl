package com.movieapp.data.preferences

/**
 * Theme Mode Options
 * Defines available theme modes for the app
 */
enum class ThemeMode {
    /**
     * Light theme
     */
    LIGHT,
    
    /**
     * Dark theme
     */
    DARK,
    
    /**
     * Follow system theme
     */
    SYSTEM;
    
    companion object {
        /**
         * Get ThemeMode from string value
         * 
         * @param value String value
         * @return ThemeMode or SYSTEM as default
         */
        fun fromString(value: String): ThemeMode {
            return entries.find { it.name == value } ?: SYSTEM
        }
    }
    
    /**
     * Get display name for theme mode
     * 
     * @return User-friendly name
     */
    fun getDisplayName(): String {
        return when (this) {
            LIGHT -> "Light"
            DARK -> "Dark"
            SYSTEM -> "System Default"
        }
    }
}
