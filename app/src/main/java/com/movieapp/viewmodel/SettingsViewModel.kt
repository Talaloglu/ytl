package com.movieapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.preferences.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Settings ViewModel
 * Manages user preferences and settings state
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferencesRepository = UserPreferencesRepository.getInstance(application)
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    /**
     * User preferences flow
     */
    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences.DEFAULT
        )
    
    // Action result flow for UI feedback
    private val _actionResult = MutableSharedFlow<ActionResult>()
    val actionResult: SharedFlow<ActionResult> = _actionResult.asSharedFlow()
    
    sealed class ActionResult {
        data class Success(val message: String) : ActionResult()
        data class Error(val message: String) : ActionResult()
    }
    
    // =================== Display Settings ===================
    
    /**
     * Update theme mode
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateThemeMode(themeMode)
                _actionResult.emit(ActionResult.Success("Theme updated to ${themeMode.getDisplayName()}"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update theme mode", e)
                _actionResult.emit(ActionResult.Error("Failed to update theme"))
            }
        }
    }
    
    /**
     * Update text scale
     */
    fun updateTextScale(textScale: TextScaleOption) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateTextScale(textScale)
                _actionResult.emit(ActionResult.Success("Text size updated"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update text scale", e)
                _actionResult.emit(ActionResult.Error("Failed to update text size"))
            }
        }
    }
    
    /**
     * Update high contrast mode
     */
    fun updateHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateHighContrast(enabled)
                _actionResult.emit(ActionResult.Success("High contrast ${if (enabled) "enabled" else "disabled"}"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update high contrast", e)
                _actionResult.emit(ActionResult.Error("Failed to update high contrast"))
            }
        }
    }
    
    // =================== Video Settings ===================
    
    /**
     * Update video quality
     */
    fun updateVideoQuality(quality: VideoQuality) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateVideoQuality(quality)
                _actionResult.emit(ActionResult.Success("Video quality set to ${quality.getDisplayName()}"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update video quality", e)
                _actionResult.emit(ActionResult.Error("Failed to update video quality"))
            }
        }
    }
    
    /**
     * Update auto play next
     */
    fun updateAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateAutoPlayNext(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update auto play next", e)
            }
        }
    }
    
    /**
     * Update skip intro
     */
    fun updateSkipIntro(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateSkipIntro(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update skip intro", e)
            }
        }
    }
    
    /**
     * Update data usage warning
     */
    fun updateDataUsageWarning(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateDataUsageWarning(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update data usage warning", e)
            }
        }
    }
    
    // =================== Subtitle Settings ===================
    
    /**
     * Update subtitles enabled
     */
    fun updateSubtitlesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateSubtitlesEnabled(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update subtitles enabled", e)
            }
        }
    }
    
    /**
     * Update subtitle language
     */
    fun updateSubtitleLanguage(language: String) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateSubtitleLanguage(language)
                _actionResult.emit(ActionResult.Success("Subtitle language updated"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update subtitle language", e)
                _actionResult.emit(ActionResult.Error("Failed to update subtitle language"))
            }
        }
    }
    
    /**
     * Update subtitle size
     */
    fun updateSubtitleSize(size: Float) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateSubtitleSize(size)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update subtitle size", e)
            }
        }
    }
    
    /**
     * Update subtitle background opacity
     */
    fun updateSubtitleBackgroundOpacity(opacity: Float) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateSubtitleBackgroundOpacity(opacity)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update subtitle background opacity", e)
            }
        }
    }
    
    // =================== Playback Settings ===================
    
    /**
     * Update remember playback speed
     */
    fun updateRememberPlaybackSpeed(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateRememberPlaybackSpeed(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update remember playback speed", e)
            }
        }
    }
    
    /**
     * Update default playback speed
     */
    fun updateDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateDefaultPlaybackSpeed(speed)
                _actionResult.emit(ActionResult.Success("Playback speed set to ${speed}x"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update default playback speed", e)
                _actionResult.emit(ActionResult.Error("Failed to update playback speed"))
            }
        }
    }
    
    /**
     * Update continue watching enabled
     */
    fun updateContinueWatchingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateContinueWatchingEnabled(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update continue watching enabled", e)
            }
        }
    }
    
    // =================== Accessibility Settings ===================
    
    /**
     * Update screen reader optimized
     */
    fun updateScreenReaderOptimized(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateScreenReaderOptimized(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update screen reader optimized", e)
            }
        }
    }
    
    /**
     * Update reduce animations
     */
    fun updateReduceAnimations(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateReduceAnimations(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update reduce animations", e)
            }
        }
    }
    
    // =================== Privacy Settings ===================
    
    /**
     * Update track watch history
     */
    fun updateTrackWatchHistory(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateTrackWatchHistory(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update track watch history", e)
            }
        }
    }
    
    /**
     * Update sync watch progress
     */
    fun updateSyncWatchProgress(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateSyncWatchProgress(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update sync watch progress", e)
            }
        }
    }
    
    // =================== App Settings ===================
    
    /**
     * Update auto sync enabled
     */
    fun updateAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateAutoSyncEnabled(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update auto sync enabled", e)
            }
        }
    }
    
    /**
     * Update WiFi only sync
     */
    fun updateWifiOnlySync(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateWifiOnlySync(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update WiFi only sync", e)
            }
        }
    }
    
    /**
     * Update clear cache on exit
     */
    fun updateClearCacheOnExit(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateClearCacheOnExit(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update clear cache on exit", e)
            }
        }
    }
    
    /**
     * Update notifications enabled
     */
    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateNotificationsEnabled(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notifications enabled", e)
            }
        }
    }
    
    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                preferencesRepository.resetToDefaults()
                _actionResult.emit(ActionResult.Success("Settings reset to defaults"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset settings", e)
                _actionResult.emit(ActionResult.Error("Failed to reset settings"))
            }
        }
    }
}
