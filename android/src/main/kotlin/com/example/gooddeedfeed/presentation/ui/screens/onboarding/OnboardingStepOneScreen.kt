package com.example.gooddeedfeed.presentation.ui.screens.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.data.remote.UserType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStepOneScreen(
    onUserTypeSelected: (UserType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedUserType by remember { mutableStateOf<UserType?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome to GoodDeedFeed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "What type of account would you like to create?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // User type options
        UserTypeOption(
            icon = Icons.Default.Person,
            title = "Volunteer",
            description = "I want to participate in community service activities",
            isSelected = selectedUserType == UserType.VOLUNTEER,
            onClick = { selectedUserType = UserType.VOLUNTEER },
        )

        Spacer(modifier = Modifier.height(16.dp))

        UserTypeOption(
            icon = Icons.Default.Star,
            title = "Organizer",
            description = "I want to organize and manage community events",
            isSelected = selectedUserType == UserType.ORGANIZER,
            onClick = { selectedUserType = UserType.ORGANIZER },
        )

        Spacer(modifier = Modifier.height(16.dp))

        UserTypeOption(
            icon = Icons.Default.Home,
            title = "Institution",
            description = "I represent an organization or institution",
            isSelected = selectedUserType == UserType.INSTITUTION,
            onClick = { selectedUserType = UserType.INSTITUTION },
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                selectedUserType?.let { userType ->
                    onUserTypeSelected(userType)
                }
            },
            enabled = selectedUserType != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "Continue",
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun UserTypeOption(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
} 
