package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.data.remote.UserType
import com.example.gooddeedfeed.presentation.theme.CORNER_RADIUS
import com.example.gooddeedfeed.presentation.theme.GLASS_BACKGROUND
import com.example.gooddeedfeed.presentation.theme.GLASS_OVERLAY
import com.example.gooddeedfeed.presentation.theme.PADDING_LARGE
import com.example.gooddeedfeed.presentation.theme.PADDING_MEDIUM
import com.example.gooddeedfeed.presentation.theme.TEXT_ON_GLASS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun homeScreen(
    user: User,
    onLogout: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GLASS_OVERLAY),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(PADDING_MEDIUM)
                .background(
                    color = GLASS_BACKGROUND,
                    shape = RoundedCornerShape(CORNER_RADIUS),
                )
                .padding(PADDING_LARGE),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Welcome message with user type
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
                        color = TEXT_ON_GLASS,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when (user.user_type) {
                            UserType.VOLUNTEER -> "Volunteer"
                            UserType.ORGANIZER -> "Organizer${user.organization_name?.let { " • $it" } ?: ""}"
                            UserType.INSTITUTION -> "Institution${user.institution_name?.name?.replace("_", " ")?.let { " • $it" } ?: ""}"
                            null -> "User"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User type specific content
            when (user.user_type) {
                UserType.VOLUNTEER -> VolunteerHomeContent()
                UserType.ORGANIZER -> OrganizerHomeContent()
                UserType.INSTITUTION -> InstitutionHomeContent()
                null -> DefaultHomeContent()
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Log Out")
            }
        }
    }
}

@Composable
private fun VolunteerHomeContent() {
    Column {
        Text(
            text = "Find opportunities to help your community!",
            style = MaterialTheme.typography.bodyLarge,
            color = TEXT_ON_GLASS,
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeActionCard(
            icon = Icons.Default.Favorite,
            title = "Browse Opportunities",
            description = "Find volunteer opportunities near you",
        ) {
            // TODO: Navigate to opportunities
        }

        Spacer(modifier = Modifier.height(12.dp))

        HomeActionCard(
            icon = Icons.Default.List,
            title = "My Activities",
            description = "View your volunteer history",
        ) {
            // TODO: Navigate to activities
        }
    }
}

@Composable
private fun OrganizerHomeContent() {
    Column {
        Text(
            text = "Manage your events and volunteers",
            style = MaterialTheme.typography.bodyLarge,
            color = TEXT_ON_GLASS,
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeActionCard(
            icon = Icons.Default.Star,
            title = "Create Event",
            description = "Organize a new volunteer opportunity",
        ) {
            // TODO: Navigate to create event
        }

        Spacer(modifier = Modifier.height(12.dp))

        HomeActionCard(
            icon = Icons.Default.List,
            title = "Manage Events",
            description = "View and edit your events",
        ) {
            // TODO: Navigate to manage events
        }
    }
}

@Composable
private fun InstitutionHomeContent() {
    Column {
        Text(
            text = "Coordinate institutional volunteer programs",
            style = MaterialTheme.typography.bodyLarge,
            color = TEXT_ON_GLASS,
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeActionCard(
            icon = Icons.Default.Info,
            title = "Dashboard",
            description = "View volunteer program analytics",
        ) {
            // TODO: Navigate to dashboard
        }

        Spacer(modifier = Modifier.height(12.dp))

        HomeActionCard(
            icon = Icons.Default.List,
            title = "Manage Programs",
            description = "Oversee institutional volunteer programs",
        ) {
            // TODO: Navigate to programs
        }
    }
}

@Composable
private fun DefaultHomeContent() {
    Text(
        text = "Please complete your profile setup",
        style = MaterialTheme.typography.bodyLarge,
        color = TEXT_ON_GLASS,
    )
}

@Composable
private fun HomeActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TEXT_ON_GLASS,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
