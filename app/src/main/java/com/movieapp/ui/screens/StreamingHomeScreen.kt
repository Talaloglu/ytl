package com.movieapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * StreamingHomeScreen - Shows only movies available for streaming
 * Integrates TMDB movie data with Supabase streaming URLs
 * Follows Material Design 3 and project specifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingHomeScreen(
    onMovieClick: (Int) -> Unit = {},
    viewModel: StreamingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // Observe state variables from StreamingViewModel
    val streamingMovies by viewModel.streamingMovies.collectAsState()
    val hasMorePages by viewModel.hasMorePages.collectAsState()
    val uiState by viewModel.streamingState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val searchUiState by viewModel.searchState.collectAsState()
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    // Load streaming movies when screen is first displayed
    // Using viewModel as dependency key ensures this only runs once per ViewModel instance
    LaunchedEffect(viewModel) {
        if (streamingMovies.isEmpty()) {
            println("🎬 StreamingHomeScreen: Loading streaming movies for first time")
            viewModel.loadStreamingMovies()
        } else {
            println("📋 StreamingHomeScreen: Movies already loaded, skipping")
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        // Header with search
        StreamingHeaderSection(
            searchQuery = searchQuery,
            onSearchQueryChange = { 
                searchQuery = it
                if (it.isNotBlank()) {
                    isSearchActive = true
                    viewModel.searchStreamingMovies(it)
                } else {
                    isSearchActive = false
                    viewModel.clearSearch()
                }
            },
            onRefresh = { viewModel.refreshMovies() },
            isLoading = isLoading
        )
        
        // Content based on state
        when {
            isSearchActive -> {
                // Show search results
                SearchResultsSection(
                    searchResults = searchResults,
                    isSearching = isSearching,
                    searchError = searchError,
                    onMovieClick = onMovieClick,
                    onClearSearch = {
                        searchQuery = ""
                        isSearchActive = false
                        viewModel.clearSearch()
                    }
                )
            }
            isLoading && streamingMovies.isEmpty() -> {
                LoadingContent()
            }
            errorMessage != null -> {
                val error = errorMessage ?: "Unknown error"
                ErrorContent(
                    errorMessage = error,
                    onRetry = {
                        viewModel.clearError()
                        viewModel.refreshMovies()
                    }
                )
            }
            streamingMovies.isEmpty() -> {
                EmptyStreamingContent()
            }
            else -> {
                StreamingMoviesGrid(
                    movies = streamingMovies,
                    onMovieClick = onMovieClick,
                    onLoadMore = { viewModel.loadNextPage() },
                    hasMorePages = hasMorePages,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Header section with streaming info and search
 */
@Composable
private fun StreamingHeaderSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎬 Streaming Movies",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Watch movies with streaming URLs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search streaming movies...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

/**
 * Streaming movies grid with watch buttons
 */
@Composable
private fun StreamingMoviesGrid(
    movies: List<CombinedMovie>,
    onMovieClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
    hasMorePages: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyGridState()
    
    // Infinite scroll detection
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()
                if (lastVisibleItem != null && hasMorePages) {
                    val totalItems = listState.layoutInfo.totalItemsCount
                    if (lastVisibleItem.index >= totalItems - 5) {
                        onLoadMore()
                    }
                }
            }
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = listState,
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(movies) { movie ->
            StreamingMovieCard(
                movie = movie,
                onMovieClick = { onMovieClick(movie.id) }
            )
        }
        
        // Loading indicator for pagination
        if (hasMorePages) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Streaming movie card with watch button
 */
@Composable
private fun StreamingMovieCard(
    movie: CombinedMovie,
    onMovieClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onMovieClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Movie poster with streaming indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.67f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(movie.getFullPosterUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = "Poster for ${movie.title}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Rating badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = String.format("%.1f", movie.voteAverage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
            
            // Movie info and watch button
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = movie.releaseDate.take(4),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Search results section
 */
@Composable
private fun SearchResultsSection(
    searchResults: List<CombinedMovie>,
    isSearching: Boolean,
    searchError: String?,
    onMovieClick: (Int) -> Unit,
    onClearSearch: () -> Unit
) {
    Column {
        // Search header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search Results",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(onClick = onClearSearch) {
                Text("Clear")
            }
        }
        
        when {
            isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            searchError != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search error: $searchError",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            searchResults.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No streaming movies found",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(searchResults) { movie ->
                        StreamingMovieCard(
                            movie = movie,
                            onMovieClick = { onMovieClick(movie.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state for no streaming content
 */
@Composable
private fun EmptyStreamingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🎬",
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "No streaming movies available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Add movies to your Supabase database to see them here",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Loading content
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
                text = "Loading streaming movies...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Error content
 */
@Composable
private fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit
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
                    text = "Failed to load streaming movies",
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
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry")
                }
            }
        }
    }
}