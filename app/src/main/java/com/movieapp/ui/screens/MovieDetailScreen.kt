package com.movieapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movieapp.data.model.MovieDetails
import com.movieapp.viewmodel.MainViewModel

/**
 * MovieDetailScreen Composable demonstrating ViewModel integration as specified
 * Observes movieDetails state variable from ViewModel to display detailed movie information
 */
@Composable
fun MovieDetailScreen(
    movieId: Int,
    viewModel: MainViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    // Observe the state variables exposed by the ViewModel as specified
    val movieDetails by viewModel.movieDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Load movie details when the composable is created or movieId changes
    LaunchedEffect(movieId) {
        viewModel.getMovieDetails(movieId)
    }
    
    // Clean up when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearMovieDetails()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button
        Button(
            onClick = onBackClick,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("← Back")
        }
        
        // Handle different UI states based on ViewModel state
        when {
            isLoading -> {
                // Show loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            errorMessage != null -> {
                // Show error message
                val error = errorMessage // Create local variable to avoid smart cast issues
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            viewModel.clearError()
                            viewModel.getMovieDetails(movieId)
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
            
            movieDetails == null -> {
                // Show empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Movie details not found",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            else -> {
                // Show movie details
                movieDetails?.let { details ->
                    MovieDetailsContent(details = details)
                }
            }
        }
    }
}

/**
 * MovieDetailsContent composable for displaying detailed movie information
 */
@Composable
fun MovieDetailsContent(details: MovieDetails) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Movie title
        Text(
            text = details.title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Original title (if different)
        if (details.originalTitle != details.title) {
            Text(
                text = "Original: ${details.originalTitle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Release date and runtime
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Released: ${details.releaseDate}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            details.getFormattedRuntime()?.let { runtime ->
                Text(
                    text = "Runtime: $runtime",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Rating and vote count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Rating: ${details.voteAverage}/10",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Votes: ${details.voteCount}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        // Genres
        if (details.genres.isNotEmpty()) {
            Text(
                text = "Genres: ${details.getGenreNames()}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Tagline
        details.tagline?.let { tagline ->
            if (tagline.isNotBlank()) {
                Text(
                    text = "\"$tagline\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
        
        // Overview
        details.overview?.let { overview ->
            if (overview.isNotBlank()) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
        
        // Additional details like budget, revenue, etc.
        details.budget?.let { budget ->
            if (budget > 0) {
                Text(
                    text = "Budget: $${String.format("%,d", budget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        
        details.revenue?.let { revenue ->
            if (revenue > 0) {
                Text(
                    text = "Revenue: $${String.format("%,d", revenue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        
        // Production companies
        if (details.productionCompanies.isNotEmpty()) {
            Text(
                text = "Production Companies",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            details.productionCompanies.forEach { company ->
                Text(
                    text = "• ${company.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}