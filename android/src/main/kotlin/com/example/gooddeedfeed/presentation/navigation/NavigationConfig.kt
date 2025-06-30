package com.example.gooddeedfeed.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.screens.ChatScreen
import com.example.gooddeedfeed.presentation.ui.screens.HomeScreen
import com.example.gooddeedfeed.presentation.ui.screens.LeaderboardScreen
import com.example.gooddeedfeed.presentation.ui.screens.institution.ReviewScreen
import com.example.gooddeedfeed.presentation.ui.screens.organizer.EventManagementScreen
import com.example.gooddeedfeed.presentation.ui.screens.volunteer.ListScreen
import com.example.gooddeedfeed.presentation.ui.screens.volunteer.MapScreen

/**
 * Configuration object that defines navigation tabs for different user types.
 * This centralizes the navigation logic and makes it easy to modify tab configurations.
 */
object NavigationConfig {

    /**
     * Helper functions to create common tab items
     */
    @Composable
    private fun createHomeTab(): TabItem = TabItem(
        title = "Home",
        icon = Icons.Default.Home,
        screen = { user, onLogout -> HomeScreen(user, onLogout) },
    )

    @Composable
    private fun createLeaderboardTab(): TabItem = TabItem(
        title = "Leaderboard",
        icon = Icons.Default.Star,
        screen = { _, _ -> LeaderboardScreen() },
    )

    @Composable
    private fun createChatTab(): TabItem = TabItem(
        title = "Chat",
        icon = Icons.Default.Chat,
        screen = { user, _ -> ChatScreen(user) },
    )

    /**
     * Tab configuration for volunteer users
     * Features: Home, List of opportunities, Map view, Chat, Leaderboard
     */
    @Composable
    fun getVolunteerTabs(): List<TabItem> {
        return listOf(
            createHomeTab(),
            TabItem(
                title = "Opportunities",
                icon = Icons.AutoMirrored.Filled.List,
                screen = { user, onLogout -> ListScreen(user, onLogout) },
            ),
            TabItem(
                title = "Map",
                icon = Icons.Default.LocationOn,
                screen = { user, onLogout -> MapScreen(user, onLogout) },
            ),
            createChatTab(),
            createLeaderboardTab(),
        )
    }

    /**
     * Tab configuration for organization users
     * Features: Home, Event Management (CRUD), Chat
     */
    @Composable
    fun getOrganizerTabs(): List<TabItem> {
        return listOf(
            createHomeTab(),
            TabItem(
                title = "Events",
                icon = Icons.Default.Edit,
                screen = { user, _ -> EventManagementScreen(user) },
            ),
            createChatTab(),
        )
    }

    /**
     * Tab configuration for institution users
     * Features: Home, Review and approval system
     */
    @Composable
    fun getInstitutionTabs(): List<TabItem> {
        return listOf(
            createHomeTab(),
            TabItem(
                title = "Reviews",
                icon = Icons.Default.CheckCircle,
                screen = { _, _ -> ReviewScreen() },
            ),
        )
    }

    /**
     * Main function to get tabs based on user type
     * This is the entry point for determining which tabs to show
     */
    @Composable
    fun getTabsForUserType(userType: DomainUserType?): List<TabItem> {
        return when (userType) {
            DomainUserType.VOLUNTEER -> getVolunteerTabs()
            DomainUserType.ORGANIZER -> getOrganizerTabs()
            DomainUserType.INSTITUTION -> getInstitutionTabs()
            null -> getVolunteerTabs()
        }
    }
}

/**
 * Extension functions for better readability and type safety
 */
fun DomainUserType?.getDisplayName(): String {
    return com.example.gooddeedfeed.domain.util.UserTypeUtils.getUserTypeDisplayName(this)
}

fun DomainUserType?.getTabCount(): Int {
    return when (this) {
        DomainUserType.VOLUNTEER -> 5 // Home, List, Map, Chat, Leaderboard
        DomainUserType.ORGANIZER -> 4 // Home, Events, Chat, Leaderboard
        DomainUserType.INSTITUTION -> 3 // Home, Reviews, Leaderboard
        null -> 2 // Home, Leaderboard
    }
} 
