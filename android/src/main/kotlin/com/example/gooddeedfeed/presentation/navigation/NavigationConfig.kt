package com.example.gooddeedfeed.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.screens.HomeScreen
import com.example.gooddeedfeed.presentation.ui.screens.SimpleChatScreen
import com.example.gooddeedfeed.presentation.ui.screens.volunteer.ListScreen
import com.example.gooddeedfeed.presentation.ui.screens.volunteer.MapScreen
import com.example.gooddeedfeed.presentation.ui.screens.StatsScreen
import com.example.gooddeedfeed.presentation.ui.screens.organizer.EventManagementScreen

data class TabItem(
    val title: String,
    val icon: ImageVector,
    val screen: @Composable (DomainUser, () -> Unit) -> Unit
    )

    /**
     * Tab configuration for volunteer users
     */
    fun getVolunteerTabs(): List<TabItem> {
        return listOf(
            TabItem("Home", Icons.Default.Home) { user, onLogout -> HomeScreen(user = user, onLogout = onLogout) },
            TabItem("Events", Icons.AutoMirrored.Filled.List) { user, onLogout -> ListScreen(user = user, onLogout = onLogout) },
            TabItem("Map", Icons.Default.Map) { user, onLogout -> MapScreen(user = user, onLogout = onLogout) },
            TabItem("Chat", Icons.Default.Chat) { user, onLogout -> SimpleChatScreen(user = user) },
            TabItem("Leaderboard", Icons.Default.Leaderboard) { user, onLogout -> StatsScreen() },
        )
    }

    /**
 * Tab configuration for organizer users
     */
    fun getOrganizerTabs(): List<TabItem> {
        return listOf(
        TabItem("Home", Icons.Default.Home) { user, onLogout -> HomeScreen(user = user, onLogout = onLogout) },
        TabItem("Manage Events", Icons.Default.Event) { user, onLogout -> EventManagementScreen(user = user) },
        TabItem("Chat", Icons.Default.Chat) { user, onLogout -> SimpleChatScreen(user = user) },
        )
    }

    /**
 * Get tabs based on user type
 */
fun getTabsForUserType(userType: DomainUserType): List<TabItem> {
        return when (userType) {
            DomainUserType.VOLUNTEER -> getVolunteerTabs()
            DomainUserType.ORGANIZER -> getOrganizerTabs()
    }
}

/**
 * Get the number of tabs for a user type (used for bottom navigation height calculation)
 */
fun getTabCountForUserType(userType: DomainUserType?): Int {
    return when (userType) {
        DomainUserType.VOLUNTEER -> 5 // Home, Events, Map, Chat, Leaderboard
        DomainUserType.ORGANIZER -> 3 // Home, Manage Events, Chat
        null -> 5 // Default fallback
    }
} 
