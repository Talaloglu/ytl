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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.movieapp.data.model.CombinedMovie
import com.movieapp.viewmodel.StreamingViewModel
import com.movieapp.ui.components.PlayerViewHost
import com.movieapp.ui.components.VideoPlayerWithLoading
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import android.util.Log
import androidx.activity.compose.BackHandler
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Creates an ExoPlayer instance with proper HTTP headers for streaming
 */
private fun createEnhancedExoPlayer(context: Context, videoUrl: String): ExoPlayer {
    val TAG = "MoviePlayerHeaders"
    Log.e(TAG, "🎬 CREATING ENHANCED EXOPLAYER FOR MOVIEPLAYERSCREEN")
    Log.e(TAG, "🔍 VIDEO URL: $videoUrl")
    println("🎬 CREATING ENHANCED EXOPLAYER FOR MOVIEPLAYERSCREEN")
    println("🔍 VIDEO URL: $videoUrl")
    
    // Get headers based on URL
    val headers = getHeadersForVideoUrl(videoUrl)
    
    // Create HTTP data source factory with headers
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
        .setConnectTimeoutMs(30000)
        .setReadTimeoutMs(30000)
        .setAllowCrossProtocolRedirects(true)
        .setDefaultRequestProperties(
            buildMap {
                // Enhanced default headers
                put("Accept", "*/*")
                put("Accept-Language", "en-US,en;q=0.9")
                put("Accept-Encoding", "gzip, deflate, br")
                put("Connection", "keep-alive")
                put("Upgrade-Insecure-Requests", "1")
                put("Sec-Fetch-Dest", "video")
                put("Sec-Fetch-Mode", "cors")
                put("Sec-Fetch-Site", "cross-site")
                put("Cache-Control", "no-cache")
                put("Pragma", "no-cache")
                put("DNT", "1")
                put("Sec-CH-UA", "\"Google Chrome\";v=\"140\", \"Chromium\";v=\"140\", \"Not?A_Brand\";v=\"24\"")
                put("Sec-CH-UA-Mobile", "?0")
                put("Sec-CH-UA-Platform", "\"Windows\"")
                put("X-Requested-With", "XMLHttpRequest")
                
                // Add URL-specific headers
                putAll(headers)
            }.also { finalHeaders ->
                Log.d(TAG, "🔧 Applied ${finalHeaders.size} headers to ExoPlayer:")
                finalHeaders.forEach { (key, value) ->
                    Log.d(TAG, "   $key: $value")
                }
            }
        )

    // Create media source factory with the HTTP data source
    val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

    // Build ExoPlayer with custom media source factory
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
}

/**
 * Gets appropriate headers based on the video URL domain
 */
private fun getHeadersForVideoUrl(videoUrl: String): Map<String, String> {
    val TAG = "MoviePlayerHeaders"
    val url = videoUrl.lowercase()
    
    Log.d(TAG, "🔍 Analyzing URL for headers: $videoUrl")
    
    return when {
        url.contains("hakunaymatata") || url.contains("bcdnw") -> mapOf(
            "Accept-Encoding" to "identity;q=1, *;q=0",
            "Range" to "bytes=0-",
            "Referer" to "https://fmoviesunblocked.net/spa/videoPlayPage/movies/",
            "Sec-CH-UA" to "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Google Chrome\";v=\"140\"",
            "Sec-CH-UA-Mobile" to "?0",
            "Sec-CH-UA-Platform" to "\"Windows\"",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
        )
        url.contains("streamtape") || url.contains("doodstream") -> mapOf(
            "Referer" to "https://streamtape.com/",
            "Origin" to "https://streamtape.com"
        )
        url.contains("mixdrop") -> mapOf(
            "Referer" to "https://mixdrop.co/",
            "Origin" to "https://mixdrop.co"
        )
        url.contains("upstream") -> mapOf(
            "Referer" to "https://upstream.to/",
            "Origin" to "https://upstream.to"
        )
        url.contains("vidoza") -> mapOf(
            "Referer" to "https://vidoza.net/",
            "Origin" to "https://vidoza.net"
        )
        url.contains("voe") || url.contains("voe-unblock") -> mapOf(
            "Referer" to "https://voe.sx/",
            "Origin" to "https://voe.sx"
        )
        url.contains("streamwish") -> mapOf(
            "Referer" to "https://streamwish.to/",
            "Origin" to "https://streamwish.to"
        )
        url.contains("filemoon") -> mapOf(
            "Referer" to "https://filemoon.sx/",
            "Origin" to "https://filemoon.sx"
        )
        // For unknown sources, try to extract domain
        else -> {
            try {
                val uri = java.net.URI(videoUrl)
                mapOf(
                    "Referer" to "${uri.scheme}://${uri.host}/",
                    "Origin" to "${uri.scheme}://${uri.host}"
                )
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }.also { headers ->
        if (headers.isNotEmpty()) {
            Log.d(TAG, "✅ Applied ${headers.size} URL-specific headers:")
            headers.forEach { (key, value) ->
                Log.d(TAG, "   $key: $value")
            }
        } else {
            Log.d(TAG, "ℹ️ No specific headers for this URL, using defaults only")
        }
    }
}

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
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current

    // Single ExoPlayer instance with enhanced headers
    val exoPlayer = remember(movie.videoUrl) {
        val url = movie.videoUrl
        if (url.isNotBlank()) {
            Log.d("ExoPlayerScreen", "▶️ Starting from beginning")
            Log.d("ExoPlayerScreen", "✅ Loading stream for ${movie.title}: $url")
            Log.e("ENHANCED_PLAYER", "🚀 USING ENHANCED EXOPLAYER WITH HEADERS!")
            createEnhancedExoPlayer(context, url).apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
        } else {
            Log.e("ENHANCED_PLAYER", "❌ No URL provided, using basic ExoPlayer")
            ExoPlayer.Builder(context).build()
        }
    }

    // Update media if URL changes (recreate player with new headers)
    LaunchedEffect(movie.videoUrl) {
        val url = movie.videoUrl
        if (url.isNotBlank() && exoPlayer.mediaItemCount == 0) {
            // Only set media if player doesn't have any media yet
            // (avoid duplicate setup since remember() already handles this)
            Log.d("ExoPlayerScreen", "🔄 URL changed, media will be set by remember() block")
        }
    }

    // Handle system back press to exit fullscreen first
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    // Toggle system UI and orientation when fullscreen changes
    LaunchedEffect(isFullscreen) {
        activity?.let { act ->
            setFullscreen(act, isFullscreen)
            act.requestedOrientation = if (isFullscreen) {
                // Force landscape even if the user has system rotation lock enabled
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Auto toggle fullscreen when device orientation changes
    LaunchedEffect(configuration.orientation) {
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> if (!isFullscreen) isFullscreen = true
            Configuration.ORIENTATION_PORTRAIT -> if (isFullscreen) isFullscreen = false
        }
    }

    // Ensure cleanup if this composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                setFullscreen(act, false)
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
    
    // Render player once with dynamic sizing; avoids recreation when toggling fullscreen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val playerModifier = if (isFullscreen) {
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        }

        // Player host attaches existing ExoPlayer
        PlayerViewHost(
            exoPlayer = exoPlayer,
            modifier = playerModifier,
            onBackClick = if (isFullscreen) ({ isFullscreen = false }) else onBackClick,
            isFullscreen = isFullscreen,
            onFullscreenToggle = { isFullscreen = it }
        )

        // Movie info only when not fullscreen
        if (!isFullscreen) {
            MoviePlayerInfoSection(movie = movie)
        }
    }

    // Release player when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            try { exoPlayer.release() } catch (_: Throwable) {}
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

/**
 * Helper function to find Activity from Context
 */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Helper function to set fullscreen mode
 */
private fun setFullscreen(activity: Activity, fullscreen: Boolean) {
    val window = activity.window
    val insetsController = WindowInsetsControllerCompat(window, window.decorView)
    
    if (fullscreen) {
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}