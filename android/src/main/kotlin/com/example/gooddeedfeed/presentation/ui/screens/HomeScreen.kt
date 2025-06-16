package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.data.remote.UserType
import com.example.gooddeedfeed.presentation.ui.components.ScreenContainer
import com.example.gooddeedfeed.presentation.ui.components.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.ActionCard
import com.example.gooddeedfeed.presentation.ui.components.SpacingSize
import com.example.gooddeedfeed.presentation.viewmodel.HomeViewModel
import com.example.gooddeedfeed.presentation.viewmodel.HomeUiState
import com.example.gooddeedfeed.presentation.viewmodel.HomeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: User,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(user) {
        viewModel.loadUserHome(user)
    }
    
    when (uiState) {
        is HomeUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is HomeUiState.Success -> {
            val successState = uiState as HomeUiState.Success // Explicit cast
            HomeContent(
                user = successState.user,
                userTypeDisplay = successState.userTypeDisplay,
                onActionClick = { action -> viewModel.handleAction(action) },
                onLogout = onLogout
            )
        }
        is HomeUiState.Error -> {
            val errorState = uiState as HomeUiState.Error // Explicit cast
            
            ScreenContainer {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = errorState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Retry",
                        onClick = { viewModel.loadUserHome(user) }
                    )
                    PrimaryButton(
                        text = "Log Out",
                        onClick = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    user: User,
    userTypeDisplay: com.example.gooddeedfeed.presentation.viewmodel.UserTypeDisplay,
    onActionClick: (HomeAction) -> Unit,
    onLogout: () -> Unit
) {
    ScreenContainer {
        // Welcome header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = when (user.user_type) {
                    UserType.VOLUNTEER -> Icons.Default.Person
                    UserType.ORGANIZER -> Icons.Default.Star
                    UserType.INSTITUTION -> Icons.Default.Home
                    null -> Icons.Default.Person
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Welcome, ${user.full_name ?: user.username}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = userTypeDisplay.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // User type specific content
        Text(
            text = userTypeDisplay.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        VerticalSpacer(SpacingSize.Small)

        // Action items
        userTypeDisplay.actionItems.forEach { actionItem ->
            ActionCard(
                icon = getIconForAction(actionItem.iconName),
                title = actionItem.title,
                description = actionItem.description,
                onClick = { onActionClick(actionItem.action) }
            )
            VerticalSpacer(SpacingSize.Small)
        }

        VerticalSpacer()

        PrimaryButton(
            text = "Log Out",
            onClick = onLogout
        )
    }
}

@Composable
private fun getIconForAction(iconName: String) = when (iconName) {
    "favorite" -> Icons.Default.Favorite
    "list" -> Icons.Default.List
    "star" -> Icons.Default.Star
    "info" -> Icons.Default.Info
    else -> Icons.Default.List
}




