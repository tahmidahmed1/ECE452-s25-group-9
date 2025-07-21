package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.base.ScreenContainer
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer

@Composable
fun ProfileScreen(
    user: DomainUser,
    onLogout: () -> Unit,
) {
    ScreenContainer {

        VerticalSpacer()

        Text(
            "Username: ${user.username}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        VerticalSpacer()

        Text(
            "Email: ${user.email}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        user.fullName?.let { fullName ->
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
            onClick = onLogout,
        )
    }
} 
