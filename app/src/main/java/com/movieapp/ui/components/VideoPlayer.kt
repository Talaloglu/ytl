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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import android.util.Log

/**
 * Creates an ExoPlayer instance with proper HTTP headers for streaming
 */
private fun createExoPlayerWithHeaders(
    context: android.content.Context,
    customHeaders: Map<String, String> = emptyMap()
): ExoPlayer {
    val TAG = "VideoPlayerHeaders"
    Log.d(TAG, "🎬 Creating ExoPlayer with enhanced headers")
    Log.d(TAG, "📋 Custom headers provided: ${customHeaders.size} headers")
    customHeaders.forEach { (key, value) ->
        Log.d(TAG, "   $key: $value")
    }
    
    // Create HTTP data source factory with custom headers
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        .setConnectTimeoutMs(30000)
        .setReadTimeoutMs(30000)
        .setAllowCrossProtocolRedirects(true)
        .setDefaultRequestProperties(
            buildMap {
                // Enhanced default headers for most streaming sources
                put("Accept", "*/*")
                put("Accept-Language", "en-US,en;q=0.9")
                put("Accept-Encoding", "gzip, deflate, br")
                put("Connection", "keep-alive")
                put("Upgrade-Insecure-Requests", "1")
                put("Sec-Fetch-Dest", "video")
                put("Sec-Fetch-Mode", "no-cors")
                put("Sec-Fetch-Site", "cross-site")
                put("Cache-Control", "no-cache")
                put("Pragma", "no-cache")
                
                // Additional headers for stubborn streaming sources
                put("DNT", "1")
                put("Sec-CH-UA", "\"Google Chrome\";v=\"119\", \"Chromium\";v=\"119\", \"Not?A_Brand\";v=\"24\"")
                put("Sec-CH-UA-Mobile", "?0")
                put("Sec-CH-UA-Platform", "\"Windows\"")
                put("X-Requested-With", "XMLHttpRequest")
                
                // Add custom headers (will override defaults if same key)
                putAll(customHeaders)
            }.also { finalHeaders ->
                Log.d(TAG, "🔧 Final headers applied to ExoPlayer:")
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
private fun getHeadersForUrl(videoUrl: String): Map<String, String> {
    val TAG = "VideoPlayerHeaders"
    val url = videoUrl.lowercase()
    
    Log.d(TAG, "🔍 Analyzing URL for headers: $videoUrl")
    Log.d(TAG, "🔍 Lowercase URL: $url")
    
    return when {
        // Common streaming sites that require specific headers
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
        url.contains("fembed") || url.contains("embedsito") -> mapOf(
            "Referer" to "https://fembed.com/",
            "Origin" to "https://fembed.com"
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
        // For unknown sources, try aggressive headers
        else -> mapOf(
            "Referer" to extractDomainReferer(videoUrl),
            "Origin" to extractDomainOrigin(videoUrl)
        ).filterValues { it.isNotEmpty() }
    }.also { headers ->
        if (headers.isNotEmpty()) {
            Log.d(TAG, "✅ Detected streaming source, applying ${headers.size} specific headers:")
            headers.forEach { (key, value) ->
                Log.d(TAG, "   $key: $value")
            }
        } else {
            Log.d(TAG, "ℹ️ No specific headers detected for this URL, using default headers only")
        }
    }
}

/**
 * Extracts domain for Referer header from video URL
 */
private fun extractDomainReferer(url: String): String {
    return try {
        val uri = java.net.URI(url)
        "${uri.scheme}://${uri.host}/"
    } catch (e: Exception) {
        ""
    }
}

/**
 * Extracts domain for Origin header from video URL
 */
private fun extractDomainOrigin(url: String): String {
    return try {
        val uri = java.net.URI(url)
        "${uri.scheme}://${uri.host}"
    } catch (e: Exception) {
        ""
    }
}

/**
 * Custom ExoPlayer composable for video streaming with intelligent header detection
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
    
    // Create ExoPlayer instance with intelligent headers based on URL
    val exoPlayer = remember(videoUrl) {
        val headers = getHeadersForUrl(videoUrl)
        createExoPlayerWithHeaders(context, headers).apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = autoPlay
        }
    }
    
    // State for player controls visibility
    var showControls by remember { mutableStateOf(true) }
    
    // AndroidView that hosts PlayerView; keep the same instance across recompositions
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
                keepScreenOn = true
                // Optional: keep aspect ratio behavior predictable
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = { view ->
            // Ensure the current player is always attached after recompositions
            if (view.player !== exoPlayer) {
                view.player = exoPlayer
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )

    // Dispose player only when this composable leaves the composition
    DisposableEffect(exoPlayer) {
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
 * Attach an existing ExoPlayer to a PlayerView in Compose without owning lifecycle.
 * Use this when you need to keep the same ExoPlayer instance across UI mode changes
 * (e.g., toggling fullscreen), to avoid pausing or rebuffering.
 */
@Composable
fun PlayerViewHost(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    onFullscreenToggle: ((Boolean) -> Unit)? = null
) {
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
                keepScreenOn = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = { view ->
            if (view.player !== exoPlayer) {
                view.player = exoPlayer
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )

    if (onBackClick != null || onFullscreenToggle != null) {
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
    
    val exoPlayer = remember(videoUrl) {
        val headers = getHeadersForUrl(videoUrl)
        createExoPlayerWithHeaders(context, headers).apply {
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