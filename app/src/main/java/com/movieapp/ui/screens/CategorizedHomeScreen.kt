package com.movieapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.movieapp.viewmodel.CategorizedHomeViewModel

/**
 * Categorized Home Screen with multiple sections
 * Each section has horizontal scrollable content based on TMDB data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorizedHomeScreen(
    onMovieClick: (Int) -> Unit = {},
    onSectionClick: (String, String) -> Unit = { _, _ -> }, // (sectionType, sectionTitle) for navigation
    viewModel: CategorizedHomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // Observe all movie sections
    val trendingMovies by viewModel.trendingMovies.collectAsState()
    val popularMovies by viewModel.popularMovies.collectAsState()
    val topRatedMovies by viewModel.topRatedMovies.collectAsState()
    val movies2024 by viewModel.movies2024.collectAsState()
    val movies2023 by viewModel.movies2023.collectAsState()
    val horrorMovies by viewModel.horrorMovies.collectAsState()
    val actionMovies by viewModel.actionMovies.collectAsState()
    val comedyMovies by viewModel.comedyMovies.collectAsState()
    val dramaMovies by viewModel.dramaMovies.collectAsState()
    
    // Observe loading states
    val isGlobalLoading by viewModel.isGlobalLoading.collectAsState()
    val isLoadingTrending by viewModel.isLoadingTrending.collectAsState()
    val isLoadingPopular by viewModel.isLoadingPopular.collectAsState()
    val isLoadingTopRated by viewModel.isLoadingTopRated.collectAsState()
    val isLoadingByYear by viewModel.isLoadingByYear.collectAsState()
    val isLoadingByGenre by viewModel.isLoadingByGenre.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Observe horizontal pagination loading states
    val isLoadingMoreTrending by viewModel.isLoadingMoreTrending.collectAsState()
    val isLoadingMorePopular by viewModel.isLoadingMorePopular.collectAsState()
    val isLoadingMoreTopRated by viewModel.isLoadingMoreTopRated.collectAsState()
    val isLoadingMore2024 by viewModel.isLoadingMore2024.collectAsState()
    val isLoadingMore2023 by viewModel.isLoadingMore2023.collectAsState()
    val isLoadingMoreHorror by viewModel.isLoadingMoreHorror.collectAsState()
    val isLoadingMoreAction by viewModel.isLoadingMoreAction.collectAsState()
    val isLoadingMoreComedy by viewModel.isLoadingMoreComedy.collectAsState()
    val isLoadingMoreDrama by viewModel.isLoadingMoreDrama.collectAsState()
    
    // Observe has more data states
    val hasMoreTrending by viewModel.hasMoreTrending.collectAsState()
    val hasMorePopular by viewModel.hasMorePopular.collectAsState()
    val hasMoreTopRated by viewModel.hasMoreTopRated.collectAsState()
    val hasMore2024 by viewModel.hasMore2024.collectAsState()
    val hasMore2023 by viewModel.hasMore2023.collectAsState()
    val hasMoreHorror by viewModel.hasMoreHorror.collectAsState()
    val hasMoreAction by viewModel.hasMoreAction.collectAsState()
    val hasMoreComedy by viewModel.hasMoreComedy.collectAsState()
    val hasMoreDrama by viewModel.hasMoreDrama.collectAsState()
    
    // Initialize data only once when screen is first displayed
    // Using viewModel as dependency key ensures this only runs once per ViewModel instance
    LaunchedEffect(viewModel) {
        println("🎬 CategorizedHomeScreen: LaunchedEffect triggered")
        viewModel.initializeData()
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
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        HomeHeader(
            onRefresh = { 
                println("🔄 HomeHeader: Refresh button clicked")
                viewModel.refreshAllSections() 
            },
            isLoading = isGlobalLoading
        )
        
        // Error handling
        errorMessage?.let { error ->
            ErrorBanner(
                message = error,
                onDismiss = { viewModel.clearError() }
            )
        }
        
        // Trending Movies Section
        MovieSection(
            title = "🔥 Trending This Week",
            movies = trendingMovies,
            isLoading = isLoadingTrending,
            isLoadingMore = isLoadingMoreTrending,
            hasMoreData = hasMoreTrending,
            onMovieClick = onMovieClick,
            onSeeAllClick = { onSectionClick("trending", "Trending Movies") },
            onLoadMore = { viewModel.loadMoreTrending() }
        )
        
        // Popular Movies Section
        MovieSection(
            title = "⭐ Popular Movies",
            movies = popularMovies,
            isLoading = isLoadingPopular,
            isLoadingMore = isLoadingMorePopular,
            hasMoreData = hasMorePopular,
            onMovieClick = onMovieClick,
            onSeeAllClick = { onSectionClick("popular", "Popular Movies") },
            onLoadMore = { viewModel.loadMorePopular() }
        )
        
        // Top Rated Movies Section
        MovieSection(
            title = "🏆 Top Rated",
            movies = topRatedMovies,
            isLoading = isLoadingTopRated,
            isLoadingMore = isLoadingMoreTopRated,
            hasMoreData = hasMoreTopRated,
            onMovieClick = onMovieClick,
            onSeeAllClick = { onSectionClick("top_rated", "Top Rated Movies") },
            onLoadMore = { viewModel.loadMoreTopRated() }
        )
        
        // 2024 Movies Section
        MovieSection(
            title = "🎬 2024 Movies",
            movies = movies2024,
            isLoading = isLoadingByYear,
            isLoadingMore = isLoadingMore2024,
            hasMoreData = hasMore2024,
            onMovieClick = onMovieClick,
            onSeeAllClick = { onSectionClick("year_2024", "2024 Movies") },
            onLoadMore = { viewModel.loadMore2024() }
        )
        
        // 2023 Movies Section
        MovieSection(
            title = "📅 2023 Movies",
            movies = movies2023,
            isLoading = isLoadingByYear,
            isLoadingMore = isLoadingMore2023,
            hasMoreData = hasMore2023,
            onMovieClick = onMovieClick,
            onSeeAllClick = { onSectionClick("year_2023", "2023 Movies") },
            onLoadMore = { viewModel.loadMore2023() }
        )
        
        // Horror Movies Section
        MovieSection(
            title = "🎃 Horror Movies",
            movies = horrorMovies,
            isLoading = isLoadingByGenre,
            isLoadingMore = isLoadingMoreHorror,
            hasMoreData = hasMoreHorror,
            onMovieClick = onMovieClick,
            onSeeAllClick = { onSectionClick("genre_horror", "Horror Movies") },
            onLoadMore = { viewModel.loadMoreHorror() }
        )
        
        // Action Movies Section
        MovieSection(
            title = "💥 Action Movies",
            movies = actionMovies,
            isLoading = isLoadingByGenre,
            isLoadingMore = isLoadingMoreAction,
            hasMoreData = hasMoreAction,
            onMovieClick = onMovieClick,
            onSeeAllClick = { onSectionClick("genre_action", "Action Movies") },
            onLoadMore = { viewModel.loadMoreAction() }
        )
        
        // Comedy Movies Section
        MovieSection(
            title = "😄 Comedy Movies",
            movies = comedyMovies,
            isLoading = isLoadingByGenre,
            isLoadingMore = isLoadingMoreComedy,
            hasMoreData = hasMoreComedy,
            onMovieClick = onMovieClick,
            onSeeAllClick = { onSectionClick("genre_comedy", "Comedy Movies") },
            onLoadMore = { viewModel.loadMoreComedy() }
        )
        
        // Drama Movies Section
        MovieSection(
            title = "🎭 Drama Movies",
            movies = dramaMovies,
            isLoading = isLoadingByGenre,
            isLoadingMore = isLoadingMoreDrama,
            hasMoreData = hasMoreDrama,
            onMovieClick = onMovieClick,
            onSeeAllClick = { onSectionClick("genre_drama", "Drama Movies") },
            onLoadMore = { viewModel.loadMoreDrama() }
        )
        
        // Load More Movies Section
        LoadMoreSection(
            viewModel = viewModel,
            isGlobalLoading = isGlobalLoading
        )
        
        // Bottom spacing
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Header section with app title and refresh button
 * Enhanced with loading state to prevent multiple refresh calls
 */
@Composable
private fun HomeHeader(
    onRefresh: () -> Unit,
    isLoading: Boolean = false
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🎬 Movie Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (isLoading) "Loading categories..." else "Discover movies by categories",
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
    }
}

/**
 * Section for loading more movies from the database
 */
@Composable
private fun LoadMoreSection(
    viewModel: CategorizedHomeViewModel,
    isGlobalLoading: Boolean
) {
    // Get movie count information
    val currentCount = viewModel.getCurrentMovieCount()
    val remainingCount = viewModel.getEstimatedRemainingMovies()
    val shouldSuggestMore = viewModel.shouldSuggestLoadingMore()
    
    if (shouldSuggestMore && remainingCount > 0) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "More Movies",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "More Movies Available!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Currently showing $currentCount movies. $remainingCount more available in your database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        println("🔄 Load More button clicked - loading additional movies")
                        viewModel.loadMoreFromDatabase(500)
                    },
                    enabled = !isGlobalLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isGlobalLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isGlobalLoading) "Loading..." else "Load More Movies",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Error banner component
 */
@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Movie section with horizontal scrollable content and pagination support
 */
@Composable
private fun MovieSection(
    title: String,
    movies: List<CombinedMovie>,
    isLoading: Boolean,
    isLoadingMore: Boolean = false,
    hasMoreData: Boolean = true,
    onMovieClick: (Int) -> Unit,
    onSeeAllClick: () -> Unit,
    onLoadMore: () -> Unit = {}
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            TextButton(
                onClick = onSeeAllClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("See All")
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "See All",
                    modifier = Modifier
                        .size(16.dp)
                        .padding(start = 4.dp)
                )
            }
        }
        
        // Horizontal scrollable content with pagination
        if (isLoading) {
            LoadingSection()
        } else if (movies.isNotEmpty()) {
            val lazyListState = rememberLazyListState()
            
            // Auto-load more when near the end
            LaunchedEffect(lazyListState.layoutInfo) {
                val totalItems = lazyListState.layoutInfo.totalItemsCount
                val lastVisibleIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                
                // Trigger load more when user is 3 items away from the end
                if (hasMoreData && !isLoadingMore && totalItems > 0 && lastVisibleIndex >= totalItems - 3) {
                    onLoadMore()
                }
            }
            
            LazyRow(
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(movies) { movie ->
                    HorizontalMovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie.id) }
                    )
                }
                
                // Load more indicator and trigger
                if (hasMoreData) {
                    item {
                        LoadMoreCard(
                            isLoadingMore = isLoadingMore,
                            onLoadMore = onLoadMore
                        )
                    }
                }
            }
        } else {
            EmptySection()
        }
    }
}

/**
 * Horizontal movie card for sections
 */
@Composable
private fun HorizontalMovieCard(
    movie: CombinedMovie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(140.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Movie poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
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
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            modifier = Modifier.size(10.dp),
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
            
            // Movie info
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = movie.releaseDate.take(4),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Loading section with shimmer cards
 */
@Composable
private fun LoadingSection() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) {
            LoadingMovieCard()
        }
    }
}

/**
 * Loading movie card placeholder
 */
@Composable
private fun LoadingMovieCard() {
    Card(
        modifier = Modifier.width(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Placeholder poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // Placeholder text
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

/**
 * Empty section placeholder
 */
@Composable
private fun EmptySection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No movies available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Load more card for horizontal pagination
 */
@Composable
private fun LoadMoreCard(
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(enabled = !isLoadingMore) { onLoadMore() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingMore) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Load More",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Load More",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}