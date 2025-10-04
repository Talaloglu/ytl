package com.movieapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.preferences.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Subtitle Configuration ViewModel
 * Manages subtitle preferences and preview state
 */
class SubtitleConfigViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferencesRepository = SubtitlePreferencesRepository.getInstance(application)
    
    companion object {
        private const val TAG = "SubtitleConfigVM"
    }
    
    /**
     * Subtitle preferences flow
     */
    val subtitlePreferences: StateFlow<SubtitlePreferences> = preferencesRepository.subtitlePreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubtitlePreferences.DEFAULT
        )
    
    // Action result flow for UI feedback
    private val _actionResult = MutableSharedFlow<ActionResult>()
    val actionResult: SharedFlow<ActionResult> = _actionResult.asSharedFlow()
    
    sealed class ActionResult {
        data class Success(val message: String) : ActionResult()
        data class Error(val message: String) : ActionResult()
    }
    
    /**
     * Update preferred language
     */
    fun updatePreferredLanguage(language: String) {
        viewModelScope.launch {
            try {
                preferencesRepository.updatePreferredLanguage(language)
                _actionResult.emit(ActionResult.Success("Language updated"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update language", e)
                _actionResult.emit(ActionResult.Error("Failed to update language"))
            }
        }
    }
    
    /**
     * Update font size
     */
    fun updateFontSize(size: SubtitleSize) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateFontSize(size)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update font size", e)
            }
        }
    }
    
    /**
     * Update font color
     */
    fun updateFontColor(color: SubtitleColor) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateFontColor(color)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update font color", e)
            }
        }
    }
    
    /**
     * Update background color
     */
    fun updateBackgroundColor(color: SubtitleColor) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateBackgroundColor(color)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update background color", e)
            }
        }
    }
    
    /**
     * Update background opacity
     */
    fun updateBackgroundOpacity(opacity: Float) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateBackgroundOpacity(opacity)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update background opacity", e)
            }
        }
    }
    
    /**
     * Update position
     */
    fun updatePosition(position: SubtitlePosition) {
        viewModelScope.launch {
            try {
                preferencesRepository.updatePosition(position)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update position", e)
            }
        }
    }
    
    /**
     * Update vertical offset
     */
    fun updateVerticalOffset(offset: Int) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateVerticalOffset(offset)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update vertical offset", e)
            }
        }
    }
    
    /**
     * Update timing offset
     */
    fun updateTimingOffset(offset: Long) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateTimingOffset(offset)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update timing offset", e)
            }
        }
    }
    
    /**
     * Update bold
     */
    fun updateBold(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateBold(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update bold", e)
            }
        }
    }
    
    /**
     * Update italic
     */
    fun updateItalic(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateItalic(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update italic", e)
            }
        }
    }
    
    /**
     * Update outline
     */
    fun updateOutline(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateOutline(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update outline", e)
            }
        }
    }
    
    /**
     * Reset to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                preferencesRepository.resetToDefaults()
                _actionResult.emit(ActionResult.Success("Subtitle settings reset to defaults"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset settings", e)
                _actionResult.emit(ActionResult.Error("Failed to reset settings"))
            }
        }
    }
}
