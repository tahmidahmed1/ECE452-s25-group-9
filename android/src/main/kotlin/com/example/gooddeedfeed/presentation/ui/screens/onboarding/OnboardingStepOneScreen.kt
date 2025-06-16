package com.example.gooddeedfeed.presentation.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.data.remote.UserType
import com.example.gooddeedfeed.presentation.ui.components.ScreenContainer
import com.example.gooddeedfeed.presentation.ui.components.ScreenTitle
import com.example.gooddeedfeed.presentation.ui.components.ScreenSubtitle
import com.example.gooddeedfeed.presentation.ui.components.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.SelectableOptionCard
import com.example.gooddeedfeed.presentation.ui.components.SpacingSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStepOneScreen(
    onUserTypeSelected: (UserType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedUserType by remember { mutableStateOf<UserType?>(null) }

    ScreenContainer(modifier = modifier) {
        ScreenTitle("Welcome to GoodDeedFeed!")
        
        VerticalSpacer(SpacingSize.Small)
        
        ScreenSubtitle("What type of account would you like to create?")
        
        VerticalSpacer(SpacingSize.Large)
        Spacer(modifier = Modifier.height(24.dp)) // Extra space for visual balance

        // User type options
        SelectableOptionCard(
            icon = Icons.Default.Person,
            title = "Volunteer",
            description = "I want to participate in community service activities",
            isSelected = selectedUserType == UserType.VOLUNTEER,
            onClick = { selectedUserType = UserType.VOLUNTEER },
        )

        VerticalSpacer(SpacingSize.Small)

        SelectableOptionCard(
            icon = Icons.Default.Star,
            title = "Organizer",
            description = "I want to organize and manage community events",
            isSelected = selectedUserType == UserType.ORGANIZER,
            onClick = { selectedUserType = UserType.ORGANIZER },
        )

        VerticalSpacer(SpacingSize.Small)

        SelectableOptionCard(
            icon = Icons.Default.Home,
            title = "Institution",
            description = "I represent an organization or institution",
            isSelected = selectedUserType == UserType.INSTITUTION,
            onClick = { selectedUserType = UserType.INSTITUTION },
        )

        VerticalSpacer(SpacingSize.Large)
        Spacer(modifier = Modifier.height(24.dp)) // Extra space for visual balance

        PrimaryButton(
            text = "Continue",
            onClick = {
                selectedUserType?.let { userType ->
                    onUserTypeSelected(userType)
                }
            },
            enabled = selectedUserType != null
        )
    }
}


