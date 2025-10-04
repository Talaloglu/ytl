package com.movieapp.data.preferences

/**
 * Subtitle Preferences Data Class
 * Holds subtitle display and configuration settings
 */
data class SubtitlePreferences(
    // Language Settings
    val preferredLanguage: String = "en",
    val fallbackLanguage: String = "en",
    val autoSelectLanguage: Boolean = true,
    
    // Display Settings
    val fontSize: SubtitleSize = SubtitleSize.MEDIUM,
    val fontColor: SubtitleColor = SubtitleColor.WHITE,
    val backgroundColor: SubtitleColor = SubtitleColor.BLACK,
    val backgroundOpacity: Float = 0.75f,
    
    // Position Settings
    val position: SubtitlePosition = SubtitlePosition.BOTTOM,
    val verticalOffset: Int = 0, // Pixels from default position
    
    // Timing Settings
    val timingOffset: Long = 0L, // Milliseconds offset
    val autoSyncTiming: Boolean = true,
    
    // Style Settings
    val bold: Boolean = false,
    val italic: Boolean = false,
    val outline: Boolean = true,
    val outlineColor: SubtitleColor = SubtitleColor.BLACK,
    
    // Advanced Settings
    val wordWrap: Boolean = true,
    val maxLinesPerSubtitle: Int = 2,
    val fadeInDuration: Long = 200L, // Milliseconds
    val fadeOutDuration: Long = 200L // Milliseconds
) {
    companion object {
        /**
         * Default subtitle preferences
         */
        val DEFAULT = SubtitlePreferences()
    }
}

/**
 * Subtitle Size Options
 */
enum class SubtitleSize(val scale: Float) {
    VERY_SMALL(0.7f),
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.2f),
    VERY_LARGE(1.5f),
    EXTRA_LARGE(2.0f);
    
    companion object {
        fun fromString(value: String): SubtitleSize {
            return entries.find { it.name == value } ?: MEDIUM
        }
        
        fun fromScale(scale: Float): SubtitleSize {
            return entries.minByOrNull { kotlin.math.abs(it.scale - scale) } ?: MEDIUM
        }
    }
    
    fun getDisplayName(): String {
        return when (this) {
            VERY_SMALL -> "Very Small"
            SMALL -> "Small"
            MEDIUM -> "Medium"
            LARGE -> "Large"
            VERY_LARGE -> "Very Large"
            EXTRA_LARGE -> "Extra Large"
        }
    }
}

/**
 * Subtitle Color Options
 */
enum class SubtitleColor(val hexValue: String) {
    WHITE("#FFFFFF"),
    BLACK("#000000"),
    YELLOW("#FFFF00"),
    CYAN("#00FFFF"),
    GREEN("#00FF00"),
    MAGENTA("#FF00FF"),
    RED("#FF0000"),
    BLUE("#0000FF");
    
    companion object {
        fun fromString(value: String): SubtitleColor {
            return entries.find { it.name == value } ?: WHITE
        }
    }
    
    fun getDisplayName(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }
}

/**
 * Subtitle Position Options
 */
enum class SubtitlePosition {
    TOP,
    MIDDLE,
    BOTTOM;
    
    companion object {
        fun fromString(value: String): SubtitlePosition {
            return entries.find { it.name == value } ?: BOTTOM
        }
    }
    
    fun getDisplayName(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
