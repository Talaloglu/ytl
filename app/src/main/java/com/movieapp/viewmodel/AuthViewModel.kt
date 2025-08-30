package com.movieapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieapp.data.repository.SupabaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.update

class AuthViewModel(
    private val authRepository: SupabaseAuthRepository
) : ViewModel() {

    // Internal mutable state flow
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    // Publicly exposed read-only state flow for the UI to observe
    val authState = _authState.asStateFlow()

    // Sealed interface to represent the different states of the authentication UI
    sealed interface AuthState {
        object Idle : AuthState
        object Loading : AuthState
        object Success : AuthState
        data class Error(val message: String) : AuthState
    }

    /**
     * Signs in a user with the provided email and password.
     * Updates the authState flow to reflect loading, success, or error states.
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.signIn(email, password)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    /**
     * Signs up a new user with the provided email and password.
     * Updates the authState flow to reflect loading, success, or error states.
     */
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.signUp(email, password)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
    
    /**
     * Resets the authentication state to Idle.
     * Useful for clearing a previous error or success message when the user
     * navigates away or dismisses a message.
     */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}