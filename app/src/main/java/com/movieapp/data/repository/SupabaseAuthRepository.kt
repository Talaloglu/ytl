package com.movieapp.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email

/**
 * Repository for handling authentication with Supabase.
 * This class abstracts the Supabase auth calls from the ViewModel.
 *
 * @property supabaseClient The Supabase client instance.
 */
class SupabaseAuthRepository(private val supabaseClient: SupabaseClient) {

    /**
     * Signs in a user with the given email and password.
     *
     * @param email The user's email.
     * @param password The user's password.
     * @throws Exception if the sign-in fails.
     */
    suspend fun signIn(email: String, password: String) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Signs up a new user with the given email and password.
     *
     * @param email The new user's email.
     * @param password The new user's password.
     * @throws Exception if the sign-up fails.
     */
    suspend fun signUp(email: String, password: String) {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Signs out the currently logged-in user.
     */
    suspend fun signOut() {
        supabaseClient.auth.signOut()
    }

    /**
     * Gets the current user's ID.
     *
     * @return The current user's ID, or null if no user is logged in.
     */
    fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }
}