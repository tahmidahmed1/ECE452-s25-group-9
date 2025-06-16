package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.presentation.ui.components.ScreenContainer
import com.example.gooddeedfeed.presentation.ui.components.ScreenTitle
import com.example.gooddeedfeed.presentation.ui.components.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.SpacingSize

@Composable
fun ProfileScreen(
    user: User,
    onLogout: () -> Unit,
) {
    ScreenContainer {
        ScreenTitle("Profile")
        
        VerticalSpacer()
        
        Text(
            "Username: ${user.username}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        
        VerticalSpacer()
        
        Text(
            "Email: ${user.email ?: "Not provided"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        
        user.full_name?.let { fullName ->
            VerticalSpacer()
            Text(
                "Full Name: $fullName",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        
        user.phone?.let { phone ->
            VerticalSpacer()
            Text(
                "Phone: $phone",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        
        VerticalSpacer(SpacingSize.Large)
        
        PrimaryButton(
            text = "Log Out",
            onClick = onLogout
        )
    }
} 
