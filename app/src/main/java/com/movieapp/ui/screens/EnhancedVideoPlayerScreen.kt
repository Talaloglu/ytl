package com.movieapp.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.movieapp.data.preferences.UserPreferencesRepository
import com.movieapp.viewmodel.MovieViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Enhanced Video Player Screen
 * Advanced video player with custom controls, subtitles, and gestures
 */
@Composable
fun EnhancedVideoPlayerScreen(
    movieId: Int,
    onBackClick: () -> Unit,
    viewModel: MovieViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Player state
    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingVideo by remember { mutableStateOf(true) }
    
    // Control state
    var selectedQuality by remember { mutableStateOf("Auto") }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showQualitySelector by remember { mutableStateOf(false) }
    var showSpeedSelector by remember { mutableStateOf(false) }
    
    // Load video URL from Supabase
    LaunchedEffect(movieId) {
        isLoadingVideo = true
        // Get movie details which includes video URL
        val repository = com.movieapp.data.repository.SupabaseMovieRepository()
        val result = repository.getAllMovies()
        if (result.isSuccess) {
            val movie = result.getOrNull()?.find { it.id == movieId }
            if (movie != null) {
                // We need to get the video URL from Supabase
                // The Movie model doesn't have videoUrl, so we need to query Supabase directly
                val supabaseApi = com.movieapp.utils.SupabaseRetrofitInstance.apiInterface
                val enrichedResult = supabaseApi.getEnrichedById(
                    apiKey = com.movieapp.utils.SupabaseRetrofitInstance.getApiKeyHeader(),
                    authorization = com.movieapp.utils.SupabaseRetrofitInstance.getAuthorizationHeader(),
                    tmdbIdEq = "eq.$movieId"
                )
                if (enrichedResult.isSuccessful) {
                    val enrichedMovie = enrichedResult.body()?.firstOrNull()
                    videoUrl = enrichedMovie?.videoUrl
                }
            }
        }
        isLoadingVideo = false
    }
    
    // ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Player listener
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }
    
    // Load video when URL is available
    LaunchedEffect(videoUrl) {
        if (videoUrl != null && videoUrl!!.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(videoUrl!!)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }
    
    // Load user preferences
    LaunchedEffect(Unit) {
        val prefsRepo = UserPreferencesRepository.getInstance(context)
        val prefs = prefsRepo.userPreferencesFlow.first()
        
        // Apply saved playback speed
        if (prefs.rememberPlaybackSpeed) {
            playbackSpeed = prefs.defaultPlaybackSpeed
            exoPlayer.setPlaybackSpeed(playbackSpeed)
        }
    }
    
    // Update playback position
    LaunchedEffect(Unit) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
                bufferedPosition = exoPlayer.bufferedPosition
            }
            delay(100)
        }
    }
    
    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
    ) {
        // ExoPlayer View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Custom Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // Top Bar
                TopControlBar(
                    onBackClick = {
                        exoPlayer.release()
                        onBackClick()
                    }
                )
                
                // Center Play/Pause
                CenterControls(
                    isPlaying = isPlaying,
                    onPlayPauseClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                        isPlaying = !isPlaying
                    },
                    onSeekForward = {
                        exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration))
                    },
                    onSeekBackward = {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                    }
                )
                
                // Bottom Bar
                BottomControlBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    bufferedPosition = bufferedPosition,
                    isPlaying = isPlaying,
                    playbackSpeed = playbackSpeed,
                    onSeek = { position ->
                        exoPlayer.seekTo(position)
                        currentPosition = position
                    },
                    onPlayPauseClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                        isPlaying = !isPlaying
                    },
                    onQualityClick = {
                        showQualitySelector = true
                    },
                    onSpeedClick = {
                        showSpeedSelector = true
                    },
                    onSubtitleClick = {
                        // TODO: Show subtitle selector
                    }
                )
            }
        }
        
        // Quality Selector Dialog
        if (showQualitySelector) {
            QualitySelector(
                currentQuality = selectedQuality,
                onQualitySelected = { quality ->
                    selectedQuality = quality
                    showQualitySelector = false
                    // TODO: Switch quality
                },
                onDismiss = { showQualitySelector = false }
            )
        }
        
        // Speed Selector Dialog
        if (showSpeedSelector) {
            SpeedSelector(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { speed ->
                    playbackSpeed = speed
                    exoPlayer.setPlaybackSpeed(speed)
                    showSpeedSelector = false
                },
                onDismiss = { showSpeedSelector = false }
            )
        }
    }
}

/**
 * Top Control Bar
 */
@Composable
private fun BoxScope.TopControlBar(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

/**
 * Center Play/Pause Controls
 */
@Composable
private fun BoxScope.CenterControls(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit
) {
    Row(
        modifier = Modifier
            .align(Alignment.Center),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Seek Backward
        IconButton(
            onClick = onSeekBackward,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Seek backward 10s",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Play/Pause
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(64.dp)
                .background(Color.White.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Seek Forward
        IconButton(
            onClick = onSeekForward,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Seek forward 10s",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Bottom Control Bar
 */
@Composable
private fun BoxScope.BottomControlBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    onSeek: (Long) -> Unit,
    onPlayPauseClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSubtitleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Progress Bar
        VideoProgressBar(
            currentPosition = currentPosition,
            duration = duration,
            bufferedPosition = bufferedPosition,
            onSeek = onSeek
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause + Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Quality, Speed, Subtitle buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onSpeedClick) {
                    Text(
                        text = "${playbackSpeed}x",
                        color = Color.White
                    )
                }
                
                IconButton(onClick = onSubtitleClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Subtitles",
                        tint = Color.White
                    )
                }
                
                IconButton(onClick = onQualityClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Quality",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Video Progress Bar
 */
@Composable
private fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isUserSeeking by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentPosition, isUserSeeking) {
        if (!isUserSeeking && duration > 0) {
            sliderPosition = (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
        }
    }
    
    Slider(
        value = sliderPosition,
        onValueChange = { value ->
            isUserSeeking = true
            sliderPosition = value
        },
        onValueChangeFinished = {
            isUserSeeking = false
            val seekPosition = (sliderPosition * duration).toLong()
            onSeek(seekPosition)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = Color.Red,
            activeTrackColor = Color.Red,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
        )
    )
}

/**
 * Quality Selector Dialog
 */
@Composable
private fun QualitySelector(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf("Auto", "1080p", "720p", "480p", "360p")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Video Quality") },
        text = {
            Column {
                qualities.forEach { quality ->
                    TextButton(
                        onClick = { onQualitySelected(quality) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(quality)
                            if (quality == currentQuality) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Speed Selector Dialog
 */
@Composable
private fun SpeedSelector(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed") },
        text = {
            Column {
                speeds.forEach { speed ->
                    TextButton(
                        onClick = { onSpeedSelected(speed) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${speed}x")
                            if (speed == currentSpeed) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Format time in mm:ss
 */
private fun formatTime(millis: Long): String {
    if (millis < 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
