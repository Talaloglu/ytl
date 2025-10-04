package com.movieapp.data.preferences

/**
 * User Preferences Data Class
 * Holds all user preference settings
 */
data class UserPreferences(
    // Display Settings
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val textScale: TextScaleOption = TextScaleOption.NORMAL,
    val useHighContrast: Boolean = false,
    
    // Video Settings
    val videoQuality: VideoQuality = VideoQuality.AUTO,
    val autoPlayNext: Boolean = false,
    val skipIntro: Boolean = false,
    val dataUsageWarning: Boolean = true,
    
    // Subtitle Settings
    val subtitlesEnabled: Boolean = true,
    val subtitleLanguage: String = "en",
    val subtitleSize: Float = 1.0f,
    val subtitleBackgroundOpacity: Float = 0.75f,
    
    // Playback Settings
    val rememberPlaybackSpeed: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val continueWatchingEnabled: Boolean = true,
    
    // Accessibility Settings
    val screenReaderOptimized: Boolean = false,
    val reduceAnimations: Boolean = false,
    
    // Privacy Settings
    val trackWatchHistory: Boolean = true,
    val syncWatchProgress: Boolean = true,
    
    // App Settings
    val autoSyncEnabled: Boolean = true,
    val wifiOnlySync: Boolean = false,
    val clearCacheOnExit: Boolean = false,
    
    // Notification Settings (for future use)
    val notificationsEnabled: Boolean = true
) {
    companion object {
        /**
         * Default preferences
         */
        val DEFAULT = UserPreferences()
    }
}
