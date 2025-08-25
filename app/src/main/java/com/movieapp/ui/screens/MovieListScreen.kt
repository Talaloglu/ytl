package com.movieapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movieapp.data.model.Movie
import com.movieapp.viewmodel.MainViewModel

/**
 * MovieListScreen Composable demonstrating ViewModel integration as specified
 * Observes state variables exposed by the ViewModel to display data
 */
@Composable
fun MovieListScreen(
    viewModel: MainViewModel = viewModel(),
    onMovieClick: (Int) -> Unit = {}
) {
    // Observe the state variables exposed by the ViewModel as specified
    val moviesList by viewModel.moviesList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Load movies when the composable is first created
    LaunchedEffect(Unit) {
        viewModel.getMovies()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Popular Movies",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
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
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            viewModel.clearError()
                            viewModel.getMovies()
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
            
            moviesList.isEmpty() -> {
                // Show empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No movies found",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            else -> {
                // Show movies list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(moviesList) { movie ->
                        MovieListItem(
                            movie = movie,
                            onClick = { onMovieClick(movie.id) }
                        )
                    }
                    
                    // Load more button
                    item {
                        Button(
                            onClick = { viewModel.loadNextPage() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Text("Load More")
                        }
                    }
                }
            }
        }
    }
}

/**
 * MovieListItem composable for displaying individual movie items
 */
@Composable
fun MovieListItem(
    movie: Movie,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = movie.overview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rating: ${movie.voteAverage}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = movie.releaseDate,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}