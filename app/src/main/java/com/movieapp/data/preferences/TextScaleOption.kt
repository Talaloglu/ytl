package com.movieapp.data.preferences

/**
 * Text Scale Options
 * Defines available text scaling settings for accessibility
 */
enum class TextScaleOption(val scale: Float) {
    /**
     * Very small text (0.85x)
     */
    VERY_SMALL(0.85f),
    
    /**
     * Small text (0.95x)
     */
    SMALL(0.95f),
    
    /**
     * Normal text (1.0x) - Default
     */
    NORMAL(1.0f),
    
    /**
     * Large text (1.15x)
     */
    LARGE(1.15f),
    
    /**
     * Very large text (1.3x)
     */
    VERY_LARGE(1.3f),
    
    /**
     * Extra large text (1.5x)
     */
    EXTRA_LARGE(1.5f);
    
    companion object {
        /**
         * Get TextScaleOption from string value
         * 
         * @param value String value
         * @return TextScaleOption or NORMAL as default
         */
        fun fromString(value: String): TextScaleOption {
            return entries.find { it.name == value } ?: NORMAL
        }
        
        /**
         * Get TextScaleOption from scale value
         * 
         * @param scale Scale value
         * @return TextScaleOption closest to the scale
         */
        fun fromScale(scale: Float): TextScaleOption {
            return entries.minByOrNull { kotlin.math.abs(it.scale - scale) } ?: NORMAL
        }
    }
    
    /**
     * Get display name for text scale
     * 
     * @return User-friendly name
     */
    fun getDisplayName(): String {
        return when (this) {
            VERY_SMALL -> "Very Small"
            SMALL -> "Small"
            NORMAL -> "Normal"
            LARGE -> "Large"
            VERY_LARGE -> "Very Large"
            EXTRA_LARGE -> "Extra Large"
        }
    }
}
