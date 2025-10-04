package com.movieapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movieapp.data.preferences.*
import com.movieapp.viewmodel.SubtitleConfigViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Subtitle Configuration Screen
 * Allows users to customize subtitle appearance with live preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleConfigurationScreen(
    onBack: () -> Unit,
    viewModel: SubtitleConfigViewModel = viewModel()
) {
    val preferences by viewModel.subtitlePreferences.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle action results
    LaunchedEffect(Unit) {
        viewModel.actionResult.collectLatest { result ->
            when (result) {
                is SubtitleConfigViewModel.ActionResult.Success -> {
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is SubtitleConfigViewModel.ActionResult.Error -> {
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
                title = { Text("Subtitle Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
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
            // Preview Section
            item {
                SubtitlePreview(preferences)
                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }
            
            // Appearance Section
            item {
                SectionHeader(title = "Appearance")
            }
            
            item {
                FontSizeSetting(
                    currentSize = preferences.fontSize,
                    onSizeSelected = viewModel::updateFontSize
                )
            }
            
            item {
                FontColorSetting(
                    currentColor = preferences.fontColor,
                    onColorSelected = viewModel::updateFontColor
                )
            }
            
            item {
                BackgroundColorSetting(
                    currentColor = preferences.backgroundColor,
                    onColorSelected = viewModel::updateBackgroundColor
                )
            }
            
            item {
                OpacitySlider(
                    label = "Background Opacity",
                    value = preferences.backgroundOpacity,
                    onValueChange = viewModel::updateBackgroundOpacity
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // Style Section
            item {
                SectionHeader(title = "Style")
            }
            
            item {
                StyleSwitch(
                    title = "Bold",
                    icon = Icons.Default.Settings,
                    checked = preferences.bold,
                    onCheckedChange = viewModel::updateBold
                )
            }
            
            item {
                StyleSwitch(
                    title = "Italic",
                    icon = Icons.Default.Settings,
                    checked = preferences.italic,
                    onCheckedChange = viewModel::updateItalic
                )
            }
            
            item {
                StyleSwitch(
                    title = "Outline",
                    icon = Icons.Default.Settings,
                    checked = preferences.outline,
                    onCheckedChange = viewModel::updateOutline
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // Position Section
            item {
                SectionHeader(title = "Position")
            }
            
            item {
                PositionSetting(
                    currentPosition = preferences.position,
                    onPositionSelected = viewModel::updatePosition
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            // Timing Section
            item {
                SectionHeader(title = "Timing")
            }
            
            item {
                TimingOffsetSlider(
                    value = preferences.timingOffset,
                    onValueChange = viewModel::updateTimingOffset
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
            title = { Text("Reset Subtitle Settings?") },
            text = { Text("This will reset all subtitle settings to their default values.") },
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
 * Subtitle Preview Component
 */
@Composable
private fun SubtitlePreview(preferences: SubtitlePreferences) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = Color.DarkGray,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = when (preferences.position) {
                SubtitlePosition.TOP -> Alignment.TopCenter
                SubtitlePosition.MIDDLE -> Alignment.Center
                SubtitlePosition.BOTTOM -> Alignment.BottomCenter
            }
        ) {
            Text(
                text = "This is a subtitle preview",
                fontSize = (16 * preferences.fontSize.scale).sp,
                color = Color(android.graphics.Color.parseColor(preferences.fontColor.hexValue)),
                fontWeight = if (preferences.bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (preferences.italic) FontStyle.Italic else FontStyle.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .background(
                        color = Color(android.graphics.Color.parseColor(preferences.backgroundColor.hexValue))
                            .copy(alpha = preferences.backgroundOpacity),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Section Header
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * Font Size Setting
 */
@Composable
private fun FontSizeSetting(
    currentSize: SubtitleSize,
    onSizeSelected: (SubtitleSize) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Font Size", style = MaterialTheme.typography.bodyLarge)
                Text(
                    currentSize.getDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        SubtitleSize.entries.forEach { size ->
            DropdownMenuItem(
                text = { Text(size.getDisplayName()) },
                onClick = {
                    onSizeSelected(size)
                    expanded = false
                },
                leadingIcon = if (size == currentSize) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

/**
 * Font Color Setting
 */
@Composable
private fun FontColorSetting(
    currentColor: SubtitleColor,
    onColorSelected: (SubtitleColor) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Font Color", style = MaterialTheme.typography.bodyLarge)
                Text(
                    currentColor.getDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = Color(android.graphics.Color.parseColor(currentColor.hexValue)),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        SubtitleColor.entries.forEach { color ->
            DropdownMenuItem(
                text = { Text(color.getDisplayName()) },
                onClick = {
                    onColorSelected(color)
                    expanded = false
                },
                leadingIcon = if (color == currentColor) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

/**
 * Background Color Setting
 */
@Composable
private fun BackgroundColorSetting(
    currentColor: SubtitleColor,
    onColorSelected: (SubtitleColor) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Background Color", style = MaterialTheme.typography.bodyLarge)
                Text(
                    currentColor.getDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = Color(android.graphics.Color.parseColor(currentColor.hexValue)),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        SubtitleColor.entries.forEach { color ->
            DropdownMenuItem(
                text = { Text(color.getDisplayName()) },
                onClick = {
                    onColorSelected(color)
                    expanded = false
                },
                leadingIcon = if (color == currentColor) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

/**
 * Opacity Slider
 */
@Composable
private fun OpacitySlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Style Switch
 */
@Composable
private fun StyleSwitch(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * Position Setting
 */
@Composable
private fun PositionSetting(
    currentPosition: SubtitlePosition,
    onPositionSelected: (SubtitlePosition) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Position", style = MaterialTheme.typography.bodyLarge)
                Text(
                    currentPosition.getDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        SubtitlePosition.entries.forEach { position ->
            DropdownMenuItem(
                text = { Text(position.getDisplayName()) },
                onClick = {
                    onPositionSelected(position)
                    expanded = false
                },
                leadingIcon = if (position == currentPosition) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

/**
 * Timing Offset Slider
 */
@Composable
private fun TimingOffsetSlider(
    value: Long,
    onValueChange: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Timing Offset", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${value}ms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toLong()) },
            valueRange = -5000f..5000f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Adjust subtitle timing (negative = earlier, positive = later)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
