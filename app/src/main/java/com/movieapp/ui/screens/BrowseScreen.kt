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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.movieapp.data.model.Movie
import com.movieapp.viewmodel.MovieViewModel

/**
 * Browse Screen
 * Category browsing interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onCategoryClick: (String, String) -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: MovieViewModel = viewModel()
) {
    val popularMovies by viewModel.popularMovies.collectAsState()
    val topRatedMovies by viewModel.topRatedMovies.collectAsState()
    val nowPlayingMovies by viewModel.nowPlayingMovies.collectAsState()
    val upcomingMovies by viewModel.upcomingMovies.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.fetchPopularMovies()
        viewModel.fetchTopRatedMovies()
        viewModel.fetchNowPlayingMovies()
        viewModel.fetchUpcomingMovies()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Categories
            item {
                CategorySection(
                    title = "Popular",
                    movies = popularMovies,
                    onSeeAllClick = { onCategoryClick("popular", "Popular Movies") },
                    onMovieClick = onMovieClick
                )
            }
            
            item {
                CategorySection(
                    title = "Top Rated",
                    movies = topRatedMovies,
                    onSeeAllClick = { onCategoryClick("top_rated", "Top Rated") },
                    onMovieClick = onMovieClick
                )
            }
            
            item {
                CategorySection(
                    title = "Now Playing",
                    movies = nowPlayingMovies,
                    onSeeAllClick = { onCategoryClick("now_playing", "Now Playing") },
                    onMovieClick = onMovieClick
                )
            }
            
            item {
                CategorySection(
                    title = "Upcoming",
                    movies = upcomingMovies,
                    onSeeAllClick = { onCategoryClick("upcoming", "Upcoming Movies") },
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@Composable
private fun CategorySection(
    title: String,
    movies: List<Movie>,
    onSeeAllClick: () -> Unit,
    onMovieClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = onSeeAllClick) {
                Text("See All")
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies.take(10)) { movie ->
                Card(
                    onClick = { onMovieClick(movie.id) },
                    modifier = Modifier
                        .width(140.dp)
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = movie.posterPath,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
