package com.movieapp.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Custom ExoPlayer composable for video streaming
 * Integrates Media3 ExoPlayer with Jetpack Compose
 */
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    onFullscreenToggle: ((Boolean) -> Unit)? = null,
    autoPlay: Boolean = true
) {
    val context = LocalContext.current
    
    // Create ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(videoUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = autoPlay
            }
    }
    
    // State for player controls visibility
    var showControls by remember { mutableStateOf(true) }
    
    // Dispose player when leaving composition
    DisposableEffect(
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = true
                    controllerAutoShow = true
                    controllerHideOnTouch = true
                }
            },
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // Custom overlay controls (optional)
    if (showControls && (onBackClick != null || onFullscreenToggle != null)) {
        VideoPlayerOverlay(
            onBackClick = onBackClick,
            isFullscreen = isFullscreen,
            onFullscreenToggle = onFullscreenToggle,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Custom overlay controls for the video player
 */
@Composable
private fun VideoPlayerOverlay(
    onBackClick: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    onFullscreenToggle: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Top controls
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            
            // Info/Settings button (replacement for fullscreen toggle)
            if (onFullscreenToggle != null) {
                IconButton(
                    onClick = { onFullscreenToggle(!isFullscreen) },
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.Info else Icons.Default.Star,
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Simple video player without overlay controls
 * Uses built-in ExoPlayer controls only
 */
@Composable
fun SimpleVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true
) {
    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(videoUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = autoPlay
            }
    }
    
    DisposableEffect(
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = true
                    controllerAutoShow = true
                    controllerHideOnTouch = true
                }
            },
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}

/**
 * Video player with loading state
 */
@Composable
fun VideoPlayerWithLoading(
    videoUrl: String?,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    onFullscreenToggle: ((Boolean) -> Unit)? = null,
    autoPlay: Boolean = true,
    errorMessage: String? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading video...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            errorMessage != null -> {
                // Error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Video playback error",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = errorMessage,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            !videoUrl.isNullOrBlank() -> {
                // Video player
                VideoPlayer(
                    videoUrl = videoUrl,
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = onBackClick,
                    isFullscreen = isFullscreen,
                    onFullscreenToggle = onFullscreenToggle,
                    autoPlay = autoPlay
                )
            }
            else -> {
                // No video URL
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No video available",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        // Back button overlay (always visible if provided)
        if (onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }
    }
}