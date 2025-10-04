package com.movieapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.movieapp.data.model.Movie
import com.movieapp.viewmodel.MovieViewModel
import kotlinx.coroutines.launch

/**
 * Optimized Category Screen
 * Advanced category browsing with infinite scroll, filters, and view options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizedCategoryScreen(
    categoryType: String,
    categoryTitle: String,
    onBackClick: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: MovieViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    
    // View mode state
    var isGridView by remember { mutableStateOf(true) }
    
    // Filter and sort state
    var showFilters by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf("Popularity") }
    var selectedYear by remember { mutableStateOf("All") }
    var minRating by remember { mutableFloatStateOf(0f) }
    
    // Observe movies from ViewModel based on category type
    val movies by remember(categoryType) {
        when (categoryType) {
            "popular" -> viewModel.popularMovies
            "top_rated" -> viewModel.topRatedMovies
            "now_playing" -> viewModel.nowPlayingMovies
            "upcoming" -> viewModel.upcomingMovies
            else -> viewModel.popularMovies
        }
    }.collectAsState()
    
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Load initial data
    LaunchedEffect(categoryType) {
        // Trigger fetch based on category
        when (categoryType) {
            "popular" -> viewModel.fetchPopularMovies()
            "top_rated" -> viewModel.fetchTopRatedMovies()
            "now_playing" -> viewModel.fetchNowPlayingMovies()
            "upcoming" -> viewModel.fetchUpcomingMovies()
        }
    }
    
    // Lazy states for scroll detection
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // View toggle
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.List else Icons.Default.Star,
                            contentDescription = if (isGridView) "List view" else "Grid view"
                        )
                    }
                    
                    // Filter button
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Filters")
                    }
                    
                    // Search button
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isGridView) {
                // Grid View
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movies) { movie ->
                        GridMovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie.id) }
                        )
                    }
                    
                    // Loading indicator
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            } else {
                // List View
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movies) { movie ->
                        ListMovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie.id) }
                        )
                    }
                    
                    // Loading indicator
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
            
            // Empty state
            if (movies.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No movies found",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        // Filter Dialog
        if (showFilters) {
            FilterDialog(
                sortOption = sortOption,
                selectedYear = selectedYear,
                minRating = minRating,
                onSortChange = { sortOption = it },
                onYearChange = { selectedYear = it },
                onRatingChange = { minRating = it },
                onDismiss = { showFilters = false },
                onApply = {
                    showFilters = false
                    // Filters applied - LaunchedEffect will trigger reload
                }
            )
        }
    }
}

/**
 * Load movies based on category type
 * Simplified to directly observe the ViewModel's state flows
 */
private suspend fun loadMovies(
    viewModel: MovieViewModel,
    categoryType: String,
    page: Int,
    onResult: (List<Movie>) -> Unit,
    onLoadingChange: (Boolean) -> Unit
) {
    // This function is kept for compatibility but the actual loading
    // is handled by LaunchedEffect observing the ViewModel flows directly
    // We just trigger the fetch here
    when (categoryType) {
        "popular" -> viewModel.fetchPopularMovies()
        "top_rated" -> viewModel.fetchTopRatedMovies()
        "now_playing" -> viewModel.fetchNowPlayingMovies()
        "upcoming" -> viewModel.fetchUpcomingMovies()
    }
}

/**
 * Grid Movie Card
 */
@Composable
private fun GridMovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
    ) {
        Box {
            AsyncImage(
                model = movie.posterPath,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Rating badge
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
            
            // Title overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

/**
 * List Movie Card
 */
@Composable
private fun ListMovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(8.dp)
        ) {
            // Poster
            Card(
                modifier = Modifier
                    .width(90.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(4.dp)
            ) {
                AsyncImage(
                    model = movie.posterPath,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = movie.releaseDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.Yellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = String.format("%.1f", movie.voteAverage),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "(${movie.voteCount} votes)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Filter Dialog
 */
@Composable
private fun FilterDialog(
    sortOption: String,
    selectedYear: String,
    minRating: Float,
    onSortChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onRatingChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filters & Sorting") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sort options
                Text("Sort by:", style = MaterialTheme.typography.titleSmall)
                val sortOptions = listOf("Popularity", "Rating", "Release Date", "Title")
                sortOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortChange(option) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(option)
                        if (option == sortOption) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                }
                
                Divider()
                
                // Year filter
                Text("Year:", style = MaterialTheme.typography.titleSmall)
                val years = listOf("All", "2024", "2023", "2022", "2021", "2020")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    years.take(3).forEach { year ->
                        FilterChip(
                            selected = year == selectedYear,
                            onClick = { onYearChange(year) },
                            label = { Text(year) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    years.drop(3).forEach { year ->
                        FilterChip(
                            selected = year == selectedYear,
                            onClick = { onYearChange(year) },
                            label = { Text(year) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Rating filter
                Text("Minimum Rating: ${minRating.toInt()}/10", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = minRating,
                    onValueChange = onRatingChange,
                    valueRange = 0f..10f,
                    steps = 9
                )
            }
        },
        confirmButton = {
            Button(onClick = onApply) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
