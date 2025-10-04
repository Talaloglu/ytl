package com.movieapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.movieapp.data.local.UserProfileEntity
import com.movieapp.viewmodel.ProfileActionResult
import com.movieapp.viewmodel.ProfileUiState
import com.movieapp.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

/**
 * Profile Screen
 * Displays and manages user profile information
 * 
 * Features:
 * - Display profile information (avatar, name, email)
 * - Edit display name and avatar
 * - Refresh profile from server
 * - Sign out
 * - Navigation to settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToWatchlist: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val profileState by viewModel.profileState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Handle action results
    LaunchedEffect(Unit) {
        viewModel.actionResult.collect { result ->
            when (result) {
                is ProfileActionResult.Success -> {
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is ProfileActionResult.Error -> {
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
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshProfile() },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = profileState) {
            is ProfileUiState.Loading -> {
                LoadingContent(paddingValues)
            }
            is ProfileUiState.NotAuthenticated -> {
                NotAuthenticatedContent(
                    paddingValues = paddingValues,
                    onNavigateToAuth = onNavigateToAuth
                )
            }
            is ProfileUiState.Success -> {
                ProfileContent(
                    paddingValues = paddingValues,
                    profile = state.profile,
                    isLoading = isLoading,
                    onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                    onUpdateAvatar = { viewModel.updateAvatarUrl(it) },
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToWatchlist = onNavigateToWatchlist,
                    onSignOut = {
                        scope.launch {
                            viewModel.signOut()
                            onNavigateToAuth()
                        }
                    }
                )
            }
            is ProfileUiState.Error -> {
                ErrorContent(
                    paddingValues = paddingValues,
                    message = state.message,
                    onRetry = { viewModel.loadProfile() }
                )
            }
        }
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
        CircularProgressIndicator()
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
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Not Signed In",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Sign in to access your profile and sync your data",
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
private fun ProfileContent(
    paddingValues: PaddingValues,
    profile: UserProfileEntity,
    isLoading: Boolean,
    onUpdateDisplayName: (String) -> Unit,
    onUpdateAvatar: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWatchlist: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditAvatarDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Header
        ProfileHeader(
            profile = profile,
            onEditAvatar = { showEditAvatarDialog = true }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Profile Info Section
        ProfileInfoSection(
            profile = profile,
            onEditDisplayName = { showEditNameDialog = true }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Actions
        QuickActionsSection(
            onNavigateToWatchlist = onNavigateToWatchlist,
            onNavigateToSettings = onNavigateToSettings
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Account Actions
        AccountActionsSection(
            onSignOut = { showSignOutDialog = true }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Edit Name Dialog
    if (showEditNameDialog) {
        EditTextDialog(
            title = "Edit Display Name",
            label = "Display Name",
            initialValue = profile.displayName ?: "",
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                onUpdateDisplayName(newName)
                showEditNameDialog = false
            }
        )
    }
    
    // Edit Avatar Dialog
    if (showEditAvatarDialog) {
        EditTextDialog(
            title = "Edit Avatar URL",
            label = "Avatar URL",
            initialValue = profile.avatarUrl ?: "",
            onDismiss = { showEditAvatarDialog = false },
            onConfirm = { newUrl ->
                onUpdateAvatar(newUrl)
                showEditAvatarDialog = false
            }
        )
    }
    
    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSignOut()
                        showSignOutDialog = false
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfileEntity,
    onEditAvatar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            if (profile.avatarUrl != null) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Profile Avatar",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Default Avatar",
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(
                onClick = onEditAvatar,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Avatar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = profile.displayName ?: "No Name",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = profile.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProfileInfoSection(
    profile: UserProfileEntity,
    onEditDisplayName: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Profile Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ProfileInfoRow(
                icon = Icons.Default.Person,
                label = "Display Name",
                value = profile.displayName ?: "Not set",
                onEdit = onEditDisplayName
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            ProfileInfoRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = profile.email,
                onEdit = null
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            ProfileInfoRow(
                icon = Icons.Default.DateRange,
                label = "Member Since",
                value = formatDate(profile.createdAt),
                onEdit = null
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    onEdit: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onNavigateToWatchlist: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ActionButton(
                icon = Icons.Default.Favorite,
                title = "My Watchlist",
                subtitle = "View your saved movies",
                onClick = onNavigateToWatchlist
            )
            
            ActionButton(
                icon = Icons.Default.Settings,
                title = "Settings",
                subtitle = "App preferences and configuration",
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun AccountActionsSection(
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ActionButton(
                icon = Icons.Default.ExitToApp,
                title = "Sign Out",
                subtitle = "Sign out of your account",
                onClick = onSignOut,
                iconTint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTextDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
            text = "Error Loading Profile",
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
