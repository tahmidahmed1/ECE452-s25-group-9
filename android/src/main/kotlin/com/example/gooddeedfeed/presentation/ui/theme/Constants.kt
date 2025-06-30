package com.example.gooddeedfeed.presentation.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity

/**
 * UI Constants for consistent design
 */
object Spacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val xxLarge = 48.dp
}

object CornerRadius {
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
}

object Elevation {
    val small = 2.dp
    val medium = 4.dp
    val large = 8.dp
    val extraLarge = 16.dp
}

object IconSize {
    val small = 16.dp
    val medium = 24.dp
    val large = 32.dp
    val extraLarge = 48.dp
    val xxLarge = 64.dp
}

object ContentPadding {
    val screen = 16.dp
    val card = 16.dp
    val dialog = 24.dp
    val list = 16.dp
}

/**
 * Application-wide constants to reduce duplication and improve maintainability
 */
object AppConstants {

    /**
     * Predefined skills for volunteer registration
     */
    val PREDEFINED_SKILLS = listOf(
        "First Aid", "CPR", "Teaching", "Cooking", "Construction",
        "Gardening", "Event Planning", "Photography", "Translation",
        "Computer Skills", "Social Media", "Leadership", "Customer Service",
        "Animal Care", "Child Care", "Senior Care", "Art & Crafts",
        "Music", "Sports", "Driving",
    )

    /**
     * Mock leaderboard data
     */
    val MOCK_LEADERS = listOf(
        "Alice" to 1200,
        "Bob" to 950,
        "Charlie" to 700,
    )

    /**
     * Badge definitions
     */
    data class Badge(
        val name: String,
        val requiredPoints: Int,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
    )

    val BADGES = listOf(
        Badge("Rising Star", 100, Icons.Default.Star),
        Badge("Flame Keeper", 500, Icons.Default.LocalFireDepartment),
        Badge("Legend", 1000, Icons.Default.EmojiEvents),
    )

    /**
     * History items for volunteer profiles
     */
    data class HistoryItem(val title: String, val date: String, val verified: Boolean)

    val VOLUNTEER_HISTORY_ITEMS = listOf(
        HistoryItem("Park Cleanup", "May 5 2024", true),
        HistoryItem("Food Drive", "Apr 20 2024", false),
        HistoryItem("Coding Workshop", "Mar 12 2024", true),
    )

    /**
     * Mock organizer profiles
     */
    data class OrganizerProfile(
        val name: String,
        val description: String,
        val followersCount: Int,
        val events: List<VolunteerOpportunity> = emptyList(),
    )

    // Simple event data class for mock events
    data class SimpleEvent(val title: String, val date: String)

    val MOCK_ORGANIZERS = listOf(
        OrganizerProfile(
            "Green Earth Org",
            "Environmental clean-ups and awareness.",
            120,
            events = listOf(
                VolunteerOpportunity(1, "River Cleanup", "Green Earth Org", "Riverside", "May 22", "Help clean the riverbank", 3, 10, OpportunityCategory.ENVIRONMENTAL),
                VolunteerOpportunity(2, "Tree Planting", "Green Earth Org", "City Park", "Jun 01", "Plant new saplings", 5, 20, OpportunityCategory.ENVIRONMENTAL),
            ),
        ),
        OrganizerProfile("Food For All", "Meals for homeless community.", 340),
        OrganizerProfile("TeachTech", "Free coding workshops for kids.", 220),
        OrganizerProfile("BuildBetter", "Habitat construction projects.", 150),
        OrganizerProfile("HealthFirst", "Community health drives.", 90),
    )
} 
