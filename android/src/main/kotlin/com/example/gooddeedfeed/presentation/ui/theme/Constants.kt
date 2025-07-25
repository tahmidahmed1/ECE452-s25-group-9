package com.example.gooddeedfeed.presentation.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Constants {
    object Colors {
        val primary = Color(0xFF6366F1) // Indigo
        val secondary = Color(0xFF8B5CF6) // Purple
        val lightGray = Color(0xFFF3F4F6)
        val darkGray = Color(0xFF6B7280)
        val lightBlue = Color(0xFF3B82F6)
        val starYellow = Color(0xFFFBBF24)
        val error = Color(0xFFEF4444)
        val success = Color(0xFF10B981)
    }
}

object CornerRadius {
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
}

object AppConstants {

    val PREDEFINED_SKILLS = listOf(
        "First Aid", "CPR", "Teaching", "Cooking", "Construction",
        "Gardening", "Event Planning", "Photography", "Translation",
        "Computer Skills", "Social Media", "Leadership", "Customer Service",
        "Animal Care", "Child Care", "Senior Care", "Art & Crafts",
        "Music", "Sports", "Driving",
    )

    data class HistoryItem(val title: String, val date: String, val verified: Boolean)

    val VOLUNTEER_HISTORY_ITEMS = listOf(
        HistoryItem("Park Cleanup", "May 5 2024", true),
        HistoryItem("Food Drive", "Apr 20 2024", false),
        HistoryItem("Coding Workshop", "Mar 12 2024", true),
    )
} 
