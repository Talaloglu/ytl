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
 * Subtitle Preferences Repository
 * Manages subtitle preferences using DataStore
 */
class SubtitlePreferencesRepository(private val context: Context) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "subtitle_preferences")
    
    companion object {
        private const val TAG = "SubtitlePrefsRepo"
        
        // Language Keys
        private val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        private val FALLBACK_LANGUAGE = stringPreferencesKey("fallback_language")
        private val AUTO_SELECT_LANGUAGE = booleanPreferencesKey("auto_select_language")
        
        // Display Keys
        private val FONT_SIZE = stringPreferencesKey("font_size")
        private val FONT_COLOR = stringPreferencesKey("font_color")
        private val BACKGROUND_COLOR = stringPreferencesKey("background_color")
        private val BACKGROUND_OPACITY = floatPreferencesKey("background_opacity")
        
        // Position Keys
        private val POSITION = stringPreferencesKey("position")
        private val VERTICAL_OFFSET = intPreferencesKey("vertical_offset")
        
        // Timing Keys
        private val TIMING_OFFSET = longPreferencesKey("timing_offset")
        private val AUTO_SYNC_TIMING = booleanPreferencesKey("auto_sync_timing")
        
        // Style Keys
        private val BOLD = booleanPreferencesKey("bold")
        private val ITALIC = booleanPreferencesKey("italic")
        private val OUTLINE = booleanPreferencesKey("outline")
        private val OUTLINE_COLOR = stringPreferencesKey("outline_color")
        
        // Advanced Keys
        private val WORD_WRAP = booleanPreferencesKey("word_wrap")
        private val MAX_LINES = intPreferencesKey("max_lines")
        private val FADE_IN_DURATION = longPreferencesKey("fade_in_duration")
        private val FADE_OUT_DURATION = longPreferencesKey("fade_out_duration")
        
        @Volatile
        private var INSTANCE: SubtitlePreferencesRepository? = null
        
        fun getInstance(context: Context): SubtitlePreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SubtitlePreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Get subtitle preferences as Flow
     */
    val subtitlePreferencesFlow: Flow<SubtitlePreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading subtitle preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferences(preferences)
        }
    
    /**
     * Map DataStore preferences to SubtitlePreferences data class
     */
    private fun mapPreferences(preferences: Preferences): SubtitlePreferences {
        return SubtitlePreferences(
            preferredLanguage = preferences[PREFERRED_LANGUAGE] ?: "en",
            fallbackLanguage = preferences[FALLBACK_LANGUAGE] ?: "en",
            autoSelectLanguage = preferences[AUTO_SELECT_LANGUAGE] ?: true,
            
            fontSize = SubtitleSize.fromString(preferences[FONT_SIZE] ?: SubtitleSize.MEDIUM.name),
            fontColor = SubtitleColor.fromString(preferences[FONT_COLOR] ?: SubtitleColor.WHITE.name),
            backgroundColor = SubtitleColor.fromString(preferences[BACKGROUND_COLOR] ?: SubtitleColor.BLACK.name),
            backgroundOpacity = preferences[BACKGROUND_OPACITY] ?: 0.75f,
            
            position = SubtitlePosition.fromString(preferences[POSITION] ?: SubtitlePosition.BOTTOM.name),
            verticalOffset = preferences[VERTICAL_OFFSET] ?: 0,
            
            timingOffset = preferences[TIMING_OFFSET] ?: 0L,
            autoSyncTiming = preferences[AUTO_SYNC_TIMING] ?: true,
            
            bold = preferences[BOLD] ?: false,
            italic = preferences[ITALIC] ?: false,
            outline = preferences[OUTLINE] ?: true,
            outlineColor = SubtitleColor.fromString(preferences[OUTLINE_COLOR] ?: SubtitleColor.BLACK.name),
            
            wordWrap = preferences[WORD_WRAP] ?: true,
            maxLinesPerSubtitle = preferences[MAX_LINES] ?: 2,
            fadeInDuration = preferences[FADE_IN_DURATION] ?: 200L,
            fadeOutDuration = preferences[FADE_OUT_DURATION] ?: 200L
        )
    }
    
    // Update methods
    suspend fun updatePreferredLanguage(language: String) {
        context.dataStore.edit { it[PREFERRED_LANGUAGE] = language }
        Log.d(TAG, "Preferred language updated: $language")
    }
    
    suspend fun updateFontSize(size: SubtitleSize) {
        context.dataStore.edit { it[FONT_SIZE] = size.name }
        Log.d(TAG, "Font size updated: $size")
    }
    
    suspend fun updateFontColor(color: SubtitleColor) {
        context.dataStore.edit { it[FONT_COLOR] = color.name }
        Log.d(TAG, "Font color updated: $color")
    }
    
    suspend fun updateBackgroundColor(color: SubtitleColor) {
        context.dataStore.edit { it[BACKGROUND_COLOR] = color.name }
        Log.d(TAG, "Background color updated: $color")
    }
    
    suspend fun updateBackgroundOpacity(opacity: Float) {
        context.dataStore.edit { it[BACKGROUND_OPACITY] = opacity }
        Log.d(TAG, "Background opacity updated: $opacity")
    }
    
    suspend fun updatePosition(position: SubtitlePosition) {
        context.dataStore.edit { it[POSITION] = position.name }
        Log.d(TAG, "Position updated: $position")
    }
    
    suspend fun updateVerticalOffset(offset: Int) {
        context.dataStore.edit { it[VERTICAL_OFFSET] = offset }
        Log.d(TAG, "Vertical offset updated: $offset")
    }
    
    suspend fun updateTimingOffset(offset: Long) {
        context.dataStore.edit { it[TIMING_OFFSET] = offset }
        Log.d(TAG, "Timing offset updated: $offset")
    }
    
    suspend fun updateBold(enabled: Boolean) {
        context.dataStore.edit { it[BOLD] = enabled }
        Log.d(TAG, "Bold updated: $enabled")
    }
    
    suspend fun updateItalic(enabled: Boolean) {
        context.dataStore.edit { it[ITALIC] = enabled }
        Log.d(TAG, "Italic updated: $enabled")
    }
    
    suspend fun updateOutline(enabled: Boolean) {
        context.dataStore.edit { it[OUTLINE] = enabled }
        Log.d(TAG, "Outline updated: $enabled")
    }
    
    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
        Log.d(TAG, "Subtitle preferences reset to defaults")
    }
}
