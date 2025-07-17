package com.example.gooddeedfeed.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.domain.model.DomainBadgeAchievement
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.viewmodel.BadgeViewModel
import kotlinx.coroutines.delay

@Composable
fun BadgeManager(
    badgeViewModel: BadgeViewModel,
    userKarmaPoints: Int?,
) {
    var showAchievementDialog by remember { mutableStateOf(false) }
    var achievedBadges by remember { mutableStateOf<List<DomainBadgeAchievement>>(emptyList()) }

    val badgeCheckState by badgeViewModel.badgeCheckState.collectAsStateWithLifecycle()

    // Check for new badges when karma points change
    LaunchedEffect(userKarmaPoints) {
        if (userKarmaPoints != null && userKarmaPoints > 0) {
            // Add a small delay to avoid checking too frequently
            delay(1000)
            badgeViewModel.checkBadgeAchievements()
        }
    }

    // Handle badge check results
    LaunchedEffect(badgeCheckState) {
        when (val currentState = badgeCheckState) {
            is UiState.Success -> {
                val response = currentState.data
                if (response.newlyEarnedBadges.isNotEmpty()) {
                    achievedBadges = response.newlyEarnedBadges
                    showAchievementDialog = true
                }
            }
            else -> {
                // Handle other states if needed
            }
        }
    }

    // Show achievement dialog
    if (showAchievementDialog && achievedBadges.isNotEmpty()) {
        BadgeAchievementDialog(
            badges = achievedBadges,
            onDismiss = {
                showAchievementDialog = false
                achievedBadges = emptyList()
                badgeViewModel.clearBadgeCheckState()
            },
        )
    }
} 
