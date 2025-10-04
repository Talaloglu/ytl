package com.movieapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.movieapp.data.model.Movie
import com.movieapp.viewmodel.MovieViewModel

/**
 * Enhanced Home Screen
 * Modern home screen with hero section, continue watching, and recommendations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedHomeScreen(
    onMovieClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCategoryClick: (String, String) -> Unit = { _, _ -> }, // categoryType, categoryTitle
    viewModel: MovieViewModel = viewModel()
) {
    val popularMovies by viewModel.popularMovies.collectAsState()
    val nowPlayingMovies by viewModel.nowPlayingMovies.collectAsState()
    val topRatedMovies by viewModel.topRatedMovies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Load data on first composition
    LaunchedEffect(Unit) {
        viewModel.fetchPopularMovies()
        viewModel.fetchNowPlayingMovies()
        viewModel.fetchTopRatedMovies()
    }
    
    // Organize data for UI sections
    val featuredMovie = popularMovies.firstOrNull()
    val continueWatching = nowPlayingMovies.take(5)
    val trending = popularMovies.take(10)
    val recommended = topRatedMovies.take(10)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movies") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Hero Section
            item {
                if (featuredMovie != null) {
                    HeroSection(
                        movie = featuredMovie,
                        onClick = { onMovieClick(featuredMovie.id) }
                    )
                }
            }
            
            // Continue Watching Section
            if (continueWatching.isNotEmpty()) {
                item {
                    MovieSection(
                        title = "Continue Watching",
                        movies = continueWatching,
                        onMovieClick = onMovieClick,
                        showProgress = true
                    )
                }
            }
            
            // Trending Now Section
            if (trending.isNotEmpty()) {
                item {
                    MovieSection(
                        title = "Trending Now",
                        movies = trending,
                        onMovieClick = onMovieClick,
                        onSeeAllClick = { onCategoryClick("popular", "Popular Movies") }
                    )
                }
            }
            
            // Recommended Section
            if (recommended.isNotEmpty()) {
                item {
                    MovieSection(
                        title = "Recommended for You",
                        movies = recommended,
                        onMovieClick = onMovieClick,
                        onSeeAllClick = { onCategoryClick("top_rated", "Top Rated Movies") }
                    )
                }
            }
            
            // Loading State
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Hero Section - Featured Movie
 */
@Composable
private fun HeroSection(
    movie: Movie,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clickable(onClick = onClick)
    ) {
        // Backdrop Image
        AsyncImage(
            model = movie.backdropPath,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rating
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = String.format("%.1f", movie.voteAverage),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                
                // Release Date
                Text(
                    text = movie.releaseDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
                
                OutlinedButton(
                    onClick = { /* Add to list */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("My List")
                }
            }
        }
    }
}

/**
 * Movie Section - Horizontal scrolling list
 */
@Composable
private fun MovieSection(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Int) -> Unit,
    onSeeAllClick: () -> Unit = {},
    showProgress: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            
            TextButton(onClick = onSeeAllClick) {
                Text("See All")
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Movie Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.id) },
                    showProgress = showProgress
                )
            }
        }
    }
}

/**
 * Movie Card
 */
@Composable
private fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    showProgress: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        // Poster
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box {
                AsyncImage(
                    model = movie.posterPath,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Rating Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.Yellow,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format("%.1f", movie.voteAverage),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                
                // Progress Bar (for continue watching)
                if (showProgress) {
                    LinearProgressIndicator(
                        progress = 0.5f, // Mock progress
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        Text(
            text = movie.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
