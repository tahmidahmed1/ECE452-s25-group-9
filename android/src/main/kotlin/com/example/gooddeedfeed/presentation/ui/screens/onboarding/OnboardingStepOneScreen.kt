package com.example.gooddeedfeed.presentation.ui.screens.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.base.ScreenContainer
import com.example.gooddeedfeed.presentation.ui.components.base.ScreenSubtitle
import com.example.gooddeedfeed.presentation.ui.components.base.SelectableOptionCard
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStepOneScreen(
    onUserTypeSelected: (DomainUserType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedUserType by remember { mutableStateOf<DomainUserType?>(null) }

    ScreenContainer(modifier = modifier) {
        // Header removed per design

        VerticalSpacer(SpacingSize.Small)

        ScreenSubtitle("What type of account would you like to create?")

        VerticalSpacer(SpacingSize.Large)
        Spacer(modifier = Modifier.height(24.dp)) // Extra space for visual balance

        // User type options
        SelectableOptionCard(
            icon = Icons.Default.Person,
            title = "Volunteer",
            description = "Find and participate in volunteer opportunities in your community",
            isSelected = selectedUserType == DomainUserType.VOLUNTEER,
            onClick = { selectedUserType = DomainUserType.VOLUNTEER },
        )

        VerticalSpacer(SpacingSize.Small)

        SelectableOptionCard(
            icon = Icons.Default.Star,
            title = "Organizer",
            description = "Create and manage volunteer events and opportunities for your organization",
            isSelected = selectedUserType == DomainUserType.ORGANIZER,
            onClick = { selectedUserType = DomainUserType.ORGANIZER },
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
            enabled = selectedUserType != null,
        )
    }
}
