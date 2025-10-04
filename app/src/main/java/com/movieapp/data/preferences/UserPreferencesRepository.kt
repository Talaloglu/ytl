package com.movieapp.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * User Preferences Repository
 * Manages app preferences using DataStore
 */
class UserPreferencesRepository(private val context: Context) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
    
    companion object {
        private const val TAG = "UserPreferencesRepo"
        
        // Display Settings Keys
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val TEXT_SCALE = stringPreferencesKey("text_scale")
        private val USE_HIGH_CONTRAST = booleanPreferencesKey("use_high_contrast")
        
        // Video Settings Keys
        private val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        private val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        private val SKIP_INTRO = booleanPreferencesKey("skip_intro")
        private val DATA_USAGE_WARNING = booleanPreferencesKey("data_usage_warning")
        
        // Subtitle Settings Keys
        private val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        private val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        private val SUBTITLE_SIZE = floatPreferencesKey("subtitle_size")
        private val SUBTITLE_BACKGROUND_OPACITY = floatPreferencesKey("subtitle_background_opacity")
        
        // Playback Settings Keys
        private val REMEMBER_PLAYBACK_SPEED = booleanPreferencesKey("remember_playback_speed")
        private val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        private val CONTINUE_WATCHING_ENABLED = booleanPreferencesKey("continue_watching_enabled")
        
        // Accessibility Settings Keys
        private val SCREEN_READER_OPTIMIZED = booleanPreferencesKey("screen_reader_optimized")
        private val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        
        // Privacy Settings Keys
        private val TRACK_WATCH_HISTORY = booleanPreferencesKey("track_watch_history")
        private val SYNC_WATCH_PROGRESS = booleanPreferencesKey("sync_watch_progress")
        
        // App Settings Keys
        private val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        private val WIFI_ONLY_SYNC = booleanPreferencesKey("wifi_only_sync")
        private val CLEAR_CACHE_ON_EXIT = booleanPreferencesKey("clear_cache_on_exit")
        
        // Notification Settings Keys
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null
        
        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Get user preferences as Flow
     */
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferences(preferences)
        }
    
    /**
     * Map DataStore preferences to UserPreferences data class
     */
    private fun mapPreferences(preferences: Preferences): UserPreferences {
        return UserPreferences(
            // Display Settings
            themeMode = ThemeMode.fromString(
                preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
            ),
            textScale = TextScaleOption.fromString(
                preferences[TEXT_SCALE] ?: TextScaleOption.NORMAL.name
            ),
            useHighContrast = preferences[USE_HIGH_CONTRAST] ?: false,
            
            // Video Settings
            videoQuality = VideoQuality.fromString(
                preferences[VIDEO_QUALITY] ?: VideoQuality.AUTO.name
            ),
            autoPlayNext = preferences[AUTO_PLAY_NEXT] ?: false,
            skipIntro = preferences[SKIP_INTRO] ?: false,
            dataUsageWarning = preferences[DATA_USAGE_WARNING] ?: true,
            
            // Subtitle Settings
            subtitlesEnabled = preferences[SUBTITLES_ENABLED] ?: true,
            subtitleLanguage = preferences[SUBTITLE_LANGUAGE] ?: "en",
            subtitleSize = preferences[SUBTITLE_SIZE] ?: 1.0f,
            subtitleBackgroundOpacity = preferences[SUBTITLE_BACKGROUND_OPACITY] ?: 0.75f,
            
            // Playback Settings
            rememberPlaybackSpeed = preferences[REMEMBER_PLAYBACK_SPEED] ?: true,
            defaultPlaybackSpeed = preferences[DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
            continueWatchingEnabled = preferences[CONTINUE_WATCHING_ENABLED] ?: true,
            
            // Accessibility Settings
            screenReaderOptimized = preferences[SCREEN_READER_OPTIMIZED] ?: false,
            reduceAnimations = preferences[REDUCE_ANIMATIONS] ?: false,
            
            // Privacy Settings
            trackWatchHistory = preferences[TRACK_WATCH_HISTORY] ?: true,
            syncWatchProgress = preferences[SYNC_WATCH_PROGRESS] ?: true,
            
            // App Settings
            autoSyncEnabled = preferences[AUTO_SYNC_ENABLED] ?: true,
            wifiOnlySync = preferences[WIFI_ONLY_SYNC] ?: false,
            clearCacheOnExit = preferences[CLEAR_CACHE_ON_EXIT] ?: false,
            
            // Notification Settings
            notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true
        )
    }
    
    /**
     * Update theme mode
     */
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.name
        }
        Log.d(TAG, "Theme mode updated: $themeMode")
    }
    
    /**
     * Update text scale
     */
    suspend fun updateTextScale(textScale: TextScaleOption) {
        context.dataStore.edit { preferences ->
            preferences[TEXT_SCALE] = textScale.name
        }
        Log.d(TAG, "Text scale updated: $textScale")
    }
    
    /**
     * Update high contrast mode
     */
    suspend fun updateHighContrast(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_HIGH_CONTRAST] = enabled
        }
        Log.d(TAG, "High contrast updated: $enabled")
    }
    
    /**
     * Update video quality
     */
    suspend fun updateVideoQuality(quality: VideoQuality) {
        context.dataStore.edit { preferences ->
            preferences[VIDEO_QUALITY] = quality.name
        }
        Log.d(TAG, "Video quality updated: $quality")
    }
    
    /**
     * Update auto play next
     */
    suspend fun updateAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_NEXT] = enabled
        }
        Log.d(TAG, "Auto play next updated: $enabled")
    }
    
    /**
     * Update skip intro
     */
    suspend fun updateSkipIntro(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SKIP_INTRO] = enabled
        }
        Log.d(TAG, "Skip intro updated: $enabled")
    }
    
    /**
     * Update data usage warning
     */
    suspend fun updateDataUsageWarning(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DATA_USAGE_WARNING] = enabled
        }
        Log.d(TAG, "Data usage warning updated: $enabled")
    }
    
    /**
     * Update subtitles enabled
     */
    suspend fun updateSubtitlesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLES_ENABLED] = enabled
        }
        Log.d(TAG, "Subtitles enabled updated: $enabled")
    }
    
    /**
     * Update subtitle language
     */
    suspend fun updateSubtitleLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_LANGUAGE] = language
        }
        Log.d(TAG, "Subtitle language updated: $language")
    }
    
    /**
     * Update subtitle size
     */
    suspend fun updateSubtitleSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_SIZE] = size
        }
        Log.d(TAG, "Subtitle size updated: $size")
    }
    
    /**
     * Update subtitle background opacity
     */
    suspend fun updateSubtitleBackgroundOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_BACKGROUND_OPACITY] = opacity
        }
        Log.d(TAG, "Subtitle background opacity updated: $opacity")
    }
    
    /**
     * Update remember playback speed
     */
    suspend fun updateRememberPlaybackSpeed(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMEMBER_PLAYBACK_SPEED] = enabled
        }
        Log.d(TAG, "Remember playback speed updated: $enabled")
    }
    
    /**
     * Update default playback speed
     */
    suspend fun updateDefaultPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PLAYBACK_SPEED] = speed
        }
        Log.d(TAG, "Default playback speed updated: $speed")
    }
    
    /**
     * Update continue watching enabled
     */
    suspend fun updateContinueWatchingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CONTINUE_WATCHING_ENABLED] = enabled
        }
        Log.d(TAG, "Continue watching enabled updated: $enabled")
    }
    
    /**
     * Update screen reader optimized
     */
    suspend fun updateScreenReaderOptimized(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SCREEN_READER_OPTIMIZED] = enabled
        }
        Log.d(TAG, "Screen reader optimized updated: $enabled")
    }
    
    /**
     * Update reduce animations
     */
    suspend fun updateReduceAnimations(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REDUCE_ANIMATIONS] = enabled
        }
        Log.d(TAG, "Reduce animations updated: $enabled")
    }
    
    /**
     * Update track watch history
     */
    suspend fun updateTrackWatchHistory(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TRACK_WATCH_HISTORY] = enabled
        }
        Log.d(TAG, "Track watch history updated: $enabled")
    }
    
    /**
     * Update sync watch progress
     */
    suspend fun updateSyncWatchProgress(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_WATCH_PROGRESS] = enabled
        }
        Log.d(TAG, "Sync watch progress updated: $enabled")
    }
    
    /**
     * Update auto sync enabled
     */
    suspend fun updateAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SYNC_ENABLED] = enabled
        }
        Log.d(TAG, "Auto sync enabled updated: $enabled")
    }
    
    /**
     * Update WiFi only sync
     */
    suspend fun updateWifiOnlySync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WIFI_ONLY_SYNC] = enabled
        }
        Log.d(TAG, "WiFi only sync updated: $enabled")
    }
    
    /**
     * Update clear cache on exit
     */
    suspend fun updateClearCacheOnExit(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLEAR_CACHE_ON_EXIT] = enabled
        }
        Log.d(TAG, "Clear cache on exit updated: $enabled")
    }
    
    /**
     * Update notifications enabled
     */
    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
        Log.d(TAG, "Notifications enabled updated: $enabled")
    }
    
    /**
     * Reset all preferences to defaults
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        Log.d(TAG, "All preferences reset to defaults")
    }
}
