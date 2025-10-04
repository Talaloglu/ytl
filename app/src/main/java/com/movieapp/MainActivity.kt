package com.movieapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.movieapp.ui.theme.MovieAppTheme
import com.movieapp.ui.navigation.MovieAppNavigation

/**
 * Main Activity for the Movie App
 * Sets up Jetpack Compose UI and navigation with Scaffold structure
 * Uses consistent layout structure with theming as specified
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieAppTheme {
                // Scaffold structure with theming and consistent layout as specified
                MovieApp()
            }
        }
    }
}

/**
 * Main app composable with Scaffold structure as specified
 * Provides consistent layout with navigation
 */
@Composable
fun MovieApp() {
    // Remember navigation controller for consistent navigation
    val navController = rememberNavController()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // Navigation with proper padding for system bars
        MovieAppNavigation(
            navController = navController
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MovieAppPreview() {
    MovieAppTheme {
        MovieApp()
    }
}