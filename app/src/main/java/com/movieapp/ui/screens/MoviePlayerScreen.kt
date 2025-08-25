package com.movieapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.movieapp.data.model.CombinedMovie
import com.movieapp.viewmodel.StreamingViewModel
import com.movieapp.ui.components.VideoPlayerWithLoading

/**
 * MoviePlayerScreen - Video player for streaming movies
 * Shows movie details and streaming player interface
 * Integrates with StreamingViewModel for movie data
 */
@Composable
fun MoviePlayerScreen(
    movieId: Int,
    onBackClick: () -> Unit,
    viewModel: StreamingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // Observe state from StreamingViewModel
    val currentMovie by viewModel.currentMovie.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Load movie details when screen is first displayed
    LaunchedEffect(movieId) {
        viewModel.getMovieWithStreamDetails(movieId)
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            isLoading -> {
                LoadingPlayerContent()
            }
            errorMessage != null -> {
                val error = errorMessage ?: "Unknown error"
                ErrorPlayerContent(
                    errorMessage = error,
                    onRetry = {
                        viewModel.clearError()
                        viewModel.getMovieWithStreamDetails(movieId)
                    },
                    onBackClick = onBackClick
                )
            }
            currentMovie != null -> {
                MoviePlayerContent(
                    movie = currentMovie!!,
                    onBackClick = onBackClick
                )
            }
            else -> {
                NoStreamContent(onBackClick = onBackClick)
            }
        }
    }
}

/**
 * Main movie player content with video and details
 */
@Composable
private fun MoviePlayerContent(
    movie: CombinedMovie,
    onBackClick: () -> Unit
) {
    // State for fullscreen mode
    var isFullscreen by remember { mutableStateOf(false) }
    
    if (isFullscreen) {
        // Fullscreen video player
        VideoPlayerWithLoading(
            videoUrl = movie.videoUrl,
            isLoading = false,
            onBackClick = { isFullscreen = false }, // Exit fullscreen instead of going back
            isFullscreen = true,
            onFullscreenToggle = { fullscreen ->
                isFullscreen = fullscreen
            },
            autoPlay = true,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Normal layout with video and details
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Video player section
            VideoPlayerSection(
                movie = movie,
                onBackClick = onBackClick,
                isFullscreen = isFullscreen,
                onFullscreenToggle = { isFullscreen = it }
            )
            
            // Movie information section
            MoviePlayerInfoSection(movie = movie)
        }
    }
}

/**
 * Video player section with ExoPlayer integration
 */
@Composable
private fun VideoPlayerSection(
    movie: CombinedMovie,
    onBackClick: () -> Unit,
    isFullscreen: Boolean = false,
    onFullscreenToggle: ((Boolean) -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f/9f) // Standard video aspect ratio
    ) {
        // ExoPlayer integration
        VideoPlayerWithLoading(
            videoUrl = movie.videoUrl,
            isLoading = false,
            onBackClick = onBackClick,
            isFullscreen = isFullscreen,
            onFullscreenToggle = onFullscreenToggle,
            autoPlay = true,
            modifier = Modifier.fillMaxSize()
        )
        
        // Movie info overlay (only show when not fullscreen)
        if (!isFullscreen) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Streaming",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Green
                    )
                    Text(
                        text = movie.duration,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Movie information section below the player
 */
@Composable
private fun MoviePlayerInfoSection(movie: CombinedMovie) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Title and rating
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = String.format("%.1f", movie.voteAverage),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Release date and streaming quality
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoChip(
                label = "Release Year",
                value = movie.releaseDate.take(4)
            )
            InfoChip(
                label = "Duration",
                value = movie.duration
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Overview
        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = movie.overview.ifEmpty { "No overview available." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Info chip for movie details
 */
@Composable
private fun InfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Loading state for player
 */
@Composable
private fun LoadingPlayerContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading movie...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Error state for player
 */
@Composable
private fun ErrorPlayerContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Failed to load movie",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

/**
 * No stream available state
 */
@Composable
private fun NoStreamContent(
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎬",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Movie not available for streaming",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This movie is not available in the streaming database",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go Back")
                }
            }
        }
    }
}