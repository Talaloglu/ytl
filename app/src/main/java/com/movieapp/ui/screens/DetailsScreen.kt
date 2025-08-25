package com.movieapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
 
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.movieapp.data.model.CombinedMovie
import com.movieapp.viewmodel.StreamingViewModel

/**
 * DetailsScreen Composable as specified
 * Displays detailed information about a selected movie
 * Including images, title, rating, actors, and summary
 * Connected to ViewModel for state observation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    movieId: Int,
    onBackClick: () -> Unit = {},
    onWatchClick: ((Int) -> Unit)? = null,
    streamingViewModel: StreamingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // Observe streaming state only (Supabase-first)
    val streamingLoading by streamingViewModel.isLoading.collectAsState()
    val currentMovieState by streamingViewModel.currentMovie.collectAsState()
    
    // Track specific movie streaming availability
    var isStreamingAvailable by remember { mutableStateOf(false) }
    var checkingStreamingAvailability by remember { mutableStateOf(true) }
    
    // Fetch from Supabase when screen is displayed or movieId changes (no TMDB)
    LaunchedEffect(movieId) {
        // Reset streaming state
        streamingViewModel.clearCurrentMovie()
        checkingStreamingAvailability = true
        isStreamingAvailable = false
        streamingViewModel.getMovieWithStreamDetails(movieId)
    }
    
    // Monitor the current movie to update streaming availability
    LaunchedEffect(currentMovieState) {
        val currentMovie = currentMovieState
        checkingStreamingAvailability = false
        isStreamingAvailable = currentMovie != null && currentMovie.id == movieId
        println("🎬 DetailsScreen: Movie $movieId availability check - Available: $isStreamingAvailable, CurrentMovie: ${currentMovie?.title}")
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        val currentMovie = currentMovieState
        when {
            streamingLoading || checkingStreamingAvailability -> {
                LoadingContent()
            }
            currentMovie != null && currentMovie.id == movieId -> {
                CombinedMovieDetailsContent(
                    movie = currentMovie,
                    onBackClick = onBackClick,
                    isStreamingAvailable = isStreamingAvailable,
                    isStreamingLoading = checkingStreamingAvailability,
                    onWatchClick = onWatchClick
                )
            }
            else -> {
                ErrorContent(
                    errorMessage = "This title isn't available from Supabase right now.",
                    onRetry = { streamingViewModel.getMovieWithStreamDetails(movieId) },
                    onBackClick = onBackClick
                )
            }
        }
    }
}

/**
 * Combined movie details content (Supabase-first)
 */
@Composable
private fun CombinedMovieDetailsContent(
    movie: CombinedMovie,
    onBackClick: () -> Unit,
    isStreamingAvailable: Boolean = false,
    isStreamingLoading: Boolean = false,
    onWatchClick: ((Int) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Backdrop header section
        BackdropHeaderCombined(
            movie = movie,
            onBackClick = onBackClick,
            isStreamingAvailable = isStreamingAvailable,
            isStreamingLoading = isStreamingLoading,
            onWatchClick = onWatchClick
        )
        
        // Overview section (basic info)
        MovieInfoSectionCombined(movie = movie)
        OverviewSectionCombined(movie = movie)
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Backdrop header with movie poster and basic info (Supabase-first)
 */
@Composable
private fun BackdropHeaderCombined(
    movie: CombinedMovie,
    onBackClick: () -> Unit,
    isStreamingAvailable: Boolean = false,
    isStreamingLoading: Boolean = false,
    onWatchClick: ((Int) -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Backdrop image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(movie.getFullBackdropUrl())
                .crossfade(true)
                .build(),
            contentDescription = "Backdrop for ${movie.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Dark overlay for better readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(50)
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        // Movie poster and basic info
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Movie poster
            Card(
                modifier = Modifier
                    .width(120.dp)
                    .height(180.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(movie.getFullPosterUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = "Poster for ${movie.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Basic movie info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 32.sp
                )
                
                // Tagline not available in CombinedMovie; omit
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Rating and year
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Text(
                                text = String.format("%.1f", movie.voteAverage),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = movie.releaseDate.take(4),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Watch Now button (if streaming is available or loading)
                if (onWatchClick != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onWatchClick(movie.id) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isStreamingAvailable && !isStreamingLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isStreamingLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Watch",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isStreamingLoading) "Checking..." 
                                   else if (isStreamingAvailable) "Watch Now" 
                                   else "Not Available",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Movie information section (Supabase-first)
 */
@Composable
private fun MovieInfoSectionCombined(movie: CombinedMovie) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Language
            InfoRow(
                icon = Icons.Default.DateRange,
                label = "Language",
                value = movie.tmdbMovie.originalLanguage.uppercase()
            )
            // Duration
            InfoRow(
                icon = Icons.Default.DateRange,
                label = "Duration",
                value = movie.getFormattedRuntime()
            )
            // Genres
            InfoRow(
                icon = Icons.Default.DateRange,
                label = "Genres",
                value = movie.getGenresString()
            )
            // Companies
            InfoRow(
                icon = Icons.Default.DateRange,
                label = "Companies",
                value = movie.getCompaniesString()
            )
            // Rating
            InfoRow(
                icon = Icons.Default.Star,
                label = "Rating",
                value = String.format("%.1f", movie.voteAverage)
            )
            // Votes
            InfoRow(
                icon = Icons.Default.Star,
                label = "Votes",
                value = "${String.format("%,d", movie.voteCount)} votes"
            )
            // Release year
            InfoRow(
                icon = Icons.Default.DateRange,
                label = "Year",
                value = movie.releaseDate.take(4)
            )
        }
    }
}

/**
 * Overview section with movie summary (Supabase-first)
 */
@Composable
private fun OverviewSectionCombined(movie: CombinedMovie) {
    if (movie.overview.isNotBlank()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 24.sp
                )
            }
        }
    }
}

// Removed TMDB-specific composables (MovieDetailsContent, BackdropHeader with MovieDetails,
// AdditionalDetailsSection, ProductionSection) to prevent TMDB coupling.

 

/**
 * Reusable info row component
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Loading content for details screen
 */
@Composable
private fun LoadingContent() {
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
                text = "Loading movie details...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Error content for details screen
 */
@Composable
private fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(50)
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Error content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(32.dp),
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
                    text = "Failed to load movie details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Go Back")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}