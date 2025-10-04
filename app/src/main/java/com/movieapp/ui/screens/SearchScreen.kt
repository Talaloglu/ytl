package com.movieapp.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movieapp.data.model.Movie
import com.movieapp.viewmodel.MovieViewModel

/**
 * Search Screen
 * Advanced search with suggestions, trending, and history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    viewModel: MovieViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val searchResults = remember { mutableStateListOf<Movie>() }
    
    // Search history (in real app, this would be persisted)
    var searchHistory by remember { mutableStateOf(listOf<String>()) }
    
    // Trending searches (mock data - in real app from backend)
    val trendingSearches = remember {
        listOf("Action", "Comedy", "Thriller", "Drama", "Sci-Fi", "Horror")
    }
    
    // Genre quick filters
    val genreFilters = remember {
        listOf(
            "Action", "Adventure", "Animation", "Comedy", "Crime",
            "Documentary", "Drama", "Family", "Fantasy", "History",
            "Horror", "Music", "Mystery", "Romance", "Sci-Fi",
            "Thriller", "War", "Western"
        )
    }
    
    Scaffold(
        topBar = {
            SearchTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    searchQuery = query
                    if (query.isNotBlank()) {
                        isSearching = true
                        // TODO: Trigger search when search functionality is implemented
                        // viewModel.searchMovies(query)
                    } else {
                        isSearching = false
                        searchResults.clear()
                    }
                },
                onBack = onBack,
                onClearSearch = {
                    searchQuery = ""
                    isSearching = false
                }
            )
        }
    ) { paddingValues ->
        if (isSearching && searchQuery.isNotBlank()) {
            // Show search results
            SearchResults(
                paddingValues = paddingValues,
                searchResults = searchResults,
                searchQuery = searchQuery,
                onMovieClick = { movie ->
                    // Add to search history
                    if (!searchHistory.contains(searchQuery)) {
                        searchHistory = listOf(searchQuery) + searchHistory.take(9)
                    }
                    onMovieClick(movie)
                }
            )
        } else {
            // Show search suggestions and history
            SearchSuggestions(
                paddingValues = paddingValues,
                searchHistory = searchHistory,
                trendingSearches = trendingSearches,
                genreFilters = genreFilters,
                onSearchHistoryClick = { query ->
                    searchQuery = query
                    isSearching = true
                    // TODO: Trigger search
                },
                onTrendingClick = { query ->
                    searchQuery = query
                    isSearching = true
                    // TODO: Trigger search
                },
                onGenreClick = { genre ->
                    searchQuery = genre
                    isSearching = true
                    // TODO: Trigger search
                },
                onClearHistory = {
                    searchHistory = emptyList()
                }
            )
        }
    }
}

/**
 * Search Top Bar with search field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onClearSearch: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search movies...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

/**
 * Search Results List
 */
@Composable
private fun SearchResults(
    paddingValues: PaddingValues,
    searchResults: List<Movie>,
    searchQuery: String,
    onMovieClick: (Movie) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Results for \"$searchQuery\"",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        if (searchResults.isEmpty()) {
            item {
                EmptySearchResults(searchQuery)
            }
        } else {
            items(searchResults) { movie ->
                SearchResultItem(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

/**
 * Search Result Item
 */
@Composable
private fun SearchResultItem(
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Movie poster placeholder
            Surface(
                modifier = Modifier.size(60.dp, 90.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = movie.releaseDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Rating: ${movie.voteAverage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}

/**
 * Empty Search Results
 */
@Composable
private fun EmptySearchResults(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleMedium
        )
        
        Text(
            text = "Try different keywords",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Search Suggestions (History, Trending, Genres)
 */
@Composable
private fun SearchSuggestions(
    paddingValues: PaddingValues,
    searchHistory: List<String>,
    trendingSearches: List<String>,
    genreFilters: List<String>,
    onSearchHistoryClick: (String) -> Unit,
    onTrendingClick: (String) -> Unit,
    onGenreClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Search History
        if (searchHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextButton(onClick = onClearHistory) {
                        Text("Clear")
                    }
                }
            }
            
            items(searchHistory) { query ->
                SearchHistoryItem(
                    query = query,
                    onClick = { onSearchHistoryClick(query) }
                )
            }
            
            item {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }
        }
        
        // Trending Searches
        item {
            Text(
                text = "Trending",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(trendingSearches) { query ->
                    SuggestionChip(
                        onClick = { onTrendingClick(query) },
                        label = { Text(query) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
        
        item {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }
        
        // Genre Quick Filters
        item {
            Text(
                text = "Browse by Genre",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(genreFilters.chunked(3)) { genreRow ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genreRow.forEach { genre ->
                    AssistChip(
                        onClick = { onGenreClick(genre) },
                        label = { Text(genre) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if row is not complete
                repeat(3 - genreRow.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Search History Item
 */
@Composable
private fun SearchHistoryItem(
    query: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
