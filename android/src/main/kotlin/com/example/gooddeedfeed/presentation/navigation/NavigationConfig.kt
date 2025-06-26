package com.example.gooddeedfeed.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.screens.ChatScreen
import com.example.gooddeedfeed.presentation.ui.screens.HomeScreen
import com.example.gooddeedfeed.presentation.ui.screens.SettingsScreen
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
    private fun createSettingsTab(): TabItem = TabItem(
        title = "Settings",
        icon = Icons.Default.Settings,
        screen = { user, onLogout -> SettingsScreen(user, onLogout) },
    )

    @Composable
    private fun createChatTab(): TabItem = TabItem(
        title = "Chat",
        icon = Icons.Default.Chat,
        screen = { user, _ -> ChatScreen(user) },
    )

    /**
     * Tab configuration for volunteer users
     * Features: Home, List of opportunities, Map view, Chat, Settings
     */
    @Composable
    fun getVolunteerTabs(): List<TabItem> {
        return listOf(
            createHomeTab(),
            TabItem(
                title = "Opportunities",
                icon = Icons.AutoMirrored.Filled.List,
                screen = { user, _ -> ListScreen(user) },
            ),
            TabItem(
                title = "Map",
                icon = Icons.Default.LocationOn,
                screen = { _, _ -> MapScreen() },
            ),
            createChatTab(),
            createSettingsTab(),
        )
    }

    /**
     * Tab configuration for organization users
     * Features: Home, Event Management (CRUD), Chat, Settings
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
            createSettingsTab(),
        )
    }

    /**
     * Tab configuration for institution users
     * Features: Home, Review and approval system, Settings
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
            createSettingsTab(),
        )
    }

    /**
     * Default tab configuration for users without a defined type
     * Features: Basic Home and Settings only
     */
    @Composable
    fun getDefaultTabs(): List<TabItem> {
        return listOf(
            createHomeTab(),
            createSettingsTab(),
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
            null -> getDefaultTabs()
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
        DomainUserType.VOLUNTEER -> 5 // Home, List, Map, Chat, Settings
        DomainUserType.ORGANIZER -> 4 // Home, Events, Chat, Settings
        DomainUserType.INSTITUTION -> 3 // Home, Reviews, Settings
        null -> 2 // Home, Settings
    }
} 
