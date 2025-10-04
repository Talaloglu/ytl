package com.movieapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movieapp.data.preferences.*
import com.movieapp.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Settings Screen
 * Comprehensive settings interface with multiple sections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle action results
    LaunchedEffect(Unit) {
        viewModel.actionResult.collectLatest { result ->
            when (result) {
                is SettingsViewModel.ActionResult.Success -> {
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is SettingsViewModel.ActionResult.Error -> {
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }
    
    var showResetDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Reset to defaults button
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset to defaults")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Display Settings
            item {
                SettingsSection(title = "Display")
            }
            
            item {
                ThemeModeSetting(
                    currentMode = preferences.themeMode,
                    onModeSelected = viewModel::updateThemeMode
                )
            }
            
            item {
                TextScaleSetting(
                    currentScale = preferences.textScale,
                    onScaleSelected = viewModel::updateTextScale
                )
            }
            
            item {
                SwitchPreference(
                    title = "High Contrast",
                    description = "Increase contrast for better visibility",
                    icon = Icons.Default.Settings,
                    checked = preferences.useHighContrast,
                    onCheckedChange = viewModel::updateHighContrast
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // Video Settings
            item {
                SettingsSection(title = "Video")
            }
            
            item {
                VideoQualitySetting(
                    currentQuality = preferences.videoQuality,
                    onQualitySelected = viewModel::updateVideoQuality
                )
            }
            
            item {
                SwitchPreference(
                    title = "Auto-play Next",
                    description = "Automatically play next episode",
                    icon = Icons.Default.PlayArrow,
                    checked = preferences.autoPlayNext,
                    onCheckedChange = viewModel::updateAutoPlayNext
                )
            }
            
            item {
                SwitchPreference(
                    title = "Skip Intro",
                    description = "Automatically skip intro sequences",
                    icon = Icons.Default.PlayArrow,
                    checked = preferences.skipIntro,
                    onCheckedChange = viewModel::updateSkipIntro
                )
            }
            
            item {
                SwitchPreference(
                    title = "Data Usage Warning",
                    description = "Warn when streaming on mobile data",
                    icon = Icons.Default.Warning,
                    checked = preferences.dataUsageWarning,
                    onCheckedChange = viewModel::updateDataUsageWarning
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // Subtitle Settings
            item {
                SettingsSection(title = "Subtitles")
            }
            
            item {
                SwitchPreference(
                    title = "Enable Subtitles",
                    description = "Show subtitles when available",
                    icon = Icons.Default.Settings,
                    checked = preferences.subtitlesEnabled,
                    onCheckedChange = viewModel::updateSubtitlesEnabled
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // Playback Settings
            item {
                SettingsSection(title = "Playback")
            }
            
            item {
                SwitchPreference(
                    title = "Remember Playback Speed",
                    description = "Remember your preferred playback speed",
                    icon = Icons.Default.Settings,
                    checked = preferences.rememberPlaybackSpeed,
                    onCheckedChange = viewModel::updateRememberPlaybackSpeed
                )
            }
            
            item {
                SwitchPreference(
                    title = "Continue Watching",
                    description = "Show continue watching section",
                    icon = Icons.Default.PlayArrow,
                    checked = preferences.continueWatchingEnabled,
                    onCheckedChange = viewModel::updateContinueWatchingEnabled
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // Accessibility Settings
            item {
                SettingsSection(title = "Accessibility")
            }
            
            item {
                SwitchPreference(
                    title = "Screen Reader Optimization",
                    description = "Optimize UI for screen readers",
                    icon = Icons.Default.Person,
                    checked = preferences.screenReaderOptimized,
                    onCheckedChange = viewModel::updateScreenReaderOptimized
                )
            }
            
            item {
                SwitchPreference(
                    title = "Reduce Animations",
                    description = "Minimize motion and animations",
                    icon = Icons.Default.Settings,
                    checked = preferences.reduceAnimations,
                    onCheckedChange = viewModel::updateReduceAnimations
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // Privacy Settings
            item {
                SettingsSection(title = "Privacy")
            }
            
            item {
                SwitchPreference(
                    title = "Track Watch History",
                    description = "Keep history of watched content",
                    icon = Icons.Default.Settings,
                    checked = preferences.trackWatchHistory,
                    onCheckedChange = viewModel::updateTrackWatchHistory
                )
            }
            
            item {
                SwitchPreference(
                    title = "Sync Watch Progress",
                    description = "Sync progress across devices",
                    icon = Icons.Default.Refresh,
                    checked = preferences.syncWatchProgress,
                    onCheckedChange = viewModel::updateSyncWatchProgress
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // App Settings
            item {
                SettingsSection(title = "App")
            }
            
            item {
                SwitchPreference(
                    title = "Auto Sync",
                    description = "Automatically sync data in background",
                    icon = Icons.Default.Refresh,
                    checked = preferences.autoSyncEnabled,
                    onCheckedChange = viewModel::updateAutoSyncEnabled
                )
            }
            
            item {
                SwitchPreference(
                    title = "WiFi Only Sync",
                    description = "Only sync when connected to WiFi",
                    icon = Icons.Default.Settings,
                    checked = preferences.wifiOnlySync,
                    onCheckedChange = viewModel::updateWifiOnlySync,
                    enabled = preferences.autoSyncEnabled
                )
            }
            
            item {
                SwitchPreference(
                    title = "Clear Cache on Exit",
                    description = "Automatically clear cache when closing app",
                    icon = Icons.Default.Delete,
                    checked = preferences.clearCacheOnExit,
                    onCheckedChange = viewModel::updateClearCacheOnExit
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // About Section
            item {
                SettingsSection(title = "About")
            }
            
            item {
                PreferenceItem(
                    title = "Version",
                    description = "1.0.0",
                    icon = Icons.Default.Info,
                    onClick = {}
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings?") },
            text = { Text("This will reset all settings to their default values. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Generic Preference Item
 */
@Composable
private fun PreferenceItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

/**
 * Switch Preference
 */
@Composable
private fun SwitchPreference(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

/**
 * Settings Section Header
 */
@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * Theme Mode Setting
 */
@Composable
private fun ThemeModeSetting(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    PreferenceItem(
        title = "Theme",
        description = currentMode.getDisplayName(),
        icon = Icons.Default.Settings,
        onClick = { expanded = true }
    )
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        ThemeMode.entries.forEach { mode ->
            DropdownMenuItem(
                text = { Text(mode.getDisplayName()) },
                onClick = {
                    onModeSelected(mode)
                    expanded = false
                },
                leadingIcon = if (mode == currentMode) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

/**
 * Text Scale Setting
 */
@Composable
private fun TextScaleSetting(
    currentScale: TextScaleOption,
    onScaleSelected: (TextScaleOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    PreferenceItem(
        title = "Text Size",
        description = currentScale.getDisplayName(),
        icon = Icons.Default.Settings,
        onClick = { expanded = true }
    )
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        TextScaleOption.entries.forEach { scale ->
            DropdownMenuItem(
                text = { Text(scale.getDisplayName()) },
                onClick = {
                    onScaleSelected(scale)
                    expanded = false
                },
                leadingIcon = if (scale == currentScale) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

/**
 * Video Quality Setting
 */
@Composable
private fun VideoQualitySetting(
    currentQuality: VideoQuality,
    onQualitySelected: (VideoQuality) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    PreferenceItem(
        title = "Video Quality",
        description = currentQuality.getDisplayName(),
        icon = Icons.Default.Settings,
        onClick = { expanded = true }
    )
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        VideoQuality.entries.forEach { quality ->
            DropdownMenuItem(
                text = { Text(quality.getDisplayName()) },
                onClick = {
                    onQualitySelected(quality)
                    expanded = false
                },
                leadingIcon = if (quality == currentQuality) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}
