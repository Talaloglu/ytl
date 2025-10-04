package com.movieapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movieapp.data.local.WatchlistCacheEntity
import com.movieapp.viewmodel.WatchlistActionResult
import com.movieapp.viewmodel.WatchlistUiState
import com.movieapp.viewmodel.WatchlistViewModel
import kotlinx.coroutines.launch

/**
 * Enhanced Watchlist Screen
 * Displays user's watchlist with advanced features
 * 
 * Features:
 * - Grid layout with movie cards
 * - Remove from watchlist
 * - Sync with remote
 * - Empty state handling
 * - Sort options (date added, alphabetical)
 * - Statistics (count, last updated)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedWatchlistScreen(
    viewModel: WatchlistViewModel = viewModel(),
    onMovieClick: (Int) -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val watchlistState by viewModel.watchlistState.collectAsState()
    val watchlistCount by viewModel.watchlistCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showClearDialog by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Handle action results
    LaunchedEffect(Unit) {
        viewModel.actionResult.collect { result ->
            when (result) {
                is WatchlistActionResult.Success -> {
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is WatchlistActionResult.Error -> {
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("My Watchlist")
                        if (watchlistCount > 0) {
                            Text(
                                text = "$watchlistCount ${if (watchlistCount == 1) "movie" else "movies"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Sort button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.List, contentDescription = "Sort")
                        }
                        
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Date Added (Newest)") },
                                onClick = {
                                    sortOrder = SortOrder.DATE_DESC
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == SortOrder.DATE_DESC) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Added (Oldest)") },
                                onClick = {
                                    sortOrder = SortOrder.DATE_ASC
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == SortOrder.DATE_ASC) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Sync button
                    IconButton(
                        onClick = { viewModel.syncWatchlist() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync")
                        }
                    }
                    
                    // More options
                    var showMoreMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Watchlist") },
                                onClick = {
                                    showMoreMenu = false
                                    showClearDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = watchlistState) {
            is WatchlistUiState.Loading -> {
                LoadingContent(paddingValues)
            }
            is WatchlistUiState.NotAuthenticated -> {
                NotAuthenticatedContent(
                    paddingValues = paddingValues,
                    onNavigateToAuth = onNavigateToAuth
                )
            }
            is WatchlistUiState.Success -> {
                val sortedEntries = when (sortOrder) {
                    SortOrder.DATE_DESC -> state.entries.sortedByDescending { it.addedAt }
                    SortOrder.DATE_ASC -> state.entries.sortedBy { it.addedAt }
                }
                
                if (sortedEntries.isEmpty()) {
                    EmptyWatchlistContent(paddingValues)
                } else {
                    WatchlistContent(
                        paddingValues = paddingValues,
                        entries = sortedEntries,
                        onMovieClick = onMovieClick,
                        onRemove = { movieId ->
                            scope.launch {
                                viewModel.removeFromWatchlist(movieId)
                            }
                        }
                    )
                }
            }
            is WatchlistUiState.Error -> {
                ErrorContent(
                    paddingValues = paddingValues,
                    message = state.message,
                    onRetry = { viewModel.loadWatchlist() }
                )
            }
        }
    }
    
    // Clear watchlist confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Clear Watchlist") },
            text = { Text("Are you sure you want to remove all movies from your watchlist? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearWatchlist()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading watchlist...")
        }
    }
}

@Composable
private fun NotAuthenticatedContent(
    paddingValues: PaddingValues,
    onNavigateToAuth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Sign In Required",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Sign in to create and sync your watchlist across devices",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNavigateToAuth,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign In")
        }
    }
}

@Composable
private fun EmptyWatchlistContent(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Your Watchlist is Empty",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Start adding movies to keep track of what you want to watch",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WatchlistContent(
    paddingValues: PaddingValues,
    entries: List<WatchlistCacheEntity>,
    onMovieClick: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries, key = { it.movieId }) { entry ->
            WatchlistMovieCard(
                entry = entry,
                onClick = { onMovieClick(entry.movieId) },
                onRemove = { onRemove(entry.movieId) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistMovieCard(
    entry: WatchlistCacheEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Movie content would go here (poster image, etc.)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Movie ID: ${entry.movieId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Column {
                    Text(
                        text = "Added: ${formatDate(entry.addedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (!entry.needsSync) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Synced",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Remove button
            IconButton(
                onClick = { showRemoveDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from watchlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    // Remove confirmation dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Remove from Watchlist") },
            text = { Text("Remove this movie from your watchlist?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ErrorContent(
    paddingValues: PaddingValues,
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Error Loading Watchlist",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date(timestamp))
}

private enum class SortOrder {
    DATE_DESC,
    DATE_ASC
}
