package com.example.gooddeedfeed.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Domain models specific to volunteer functionality
 */

data class VolunteerOpportunity(
    val id: Int,
    val title: String,
    val organizationName: String,
    val location: String,
    val date: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val description: String,
    val requiredVolunteers: Int,
    val currentVolunteers: Int,
    val category: OpportunityCategory,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isActive: Boolean = true,
    val karmaPoints: Int = 10,
    val imageUrl: String? = null,
    val isJoined: Boolean = false,
    val images: List<com.example.gooddeedfeed.data.remote.dto.EventImageDto> = emptyList(),
)

data class JoinedVolunteer(
    val id: Int,
    val username: String,
    val fullName: String,
    val profilePictureUrl: String? = null,
)

enum class OpportunityCategory {
    COMMUNITY_SERVICE,
    EDUCATION,
    ENVIRONMENTAL,
    HEALTHCARE,
    SOCIAL_SERVICES,
    DISASTER_RELIEF,
    FOOD_SECURITY,
    ANIMAL_WELFARE,
    ARTS_CULTURE,
    YOUTH_MENTORING,
    ELDERLY_CARE,
    TECHNOLOGY,
    OTHER,
}

fun OpportunityCategory.toApiValue(): String = when (this) {
    OpportunityCategory.COMMUNITY_SERVICE -> "community_service"
    OpportunityCategory.EDUCATION -> "education"
    OpportunityCategory.ENVIRONMENTAL -> "environmental"
    OpportunityCategory.HEALTHCARE -> "healthcare"
    OpportunityCategory.SOCIAL_SERVICES -> "social_services"
    OpportunityCategory.DISASTER_RELIEF -> "disaster_relief"
    OpportunityCategory.FOOD_SECURITY -> "food_security"
    OpportunityCategory.ANIMAL_WELFARE -> "animal_welfare"
    OpportunityCategory.ARTS_CULTURE -> "arts_culture"
    OpportunityCategory.YOUTH_MENTORING -> "youth_mentoring"
    OpportunityCategory.ELDERLY_CARE -> "elderly_care"
    OpportunityCategory.TECHNOLOGY -> "technology"
    OpportunityCategory.OTHER -> "other"
}

fun OpportunityCategory.toDisplayString(): String = when (this) {
    OpportunityCategory.COMMUNITY_SERVICE -> "Community Service"
    OpportunityCategory.EDUCATION -> "Education"
    OpportunityCategory.ENVIRONMENTAL -> "Environmental"
    OpportunityCategory.HEALTHCARE -> "Healthcare"
    OpportunityCategory.SOCIAL_SERVICES -> "Social Services"
    OpportunityCategory.DISASTER_RELIEF -> "Disaster Relief"
    OpportunityCategory.FOOD_SECURITY -> "Food Security"
    OpportunityCategory.ANIMAL_WELFARE -> "Animal Welfare"
    OpportunityCategory.ARTS_CULTURE -> "Arts & Culture"
    OpportunityCategory.YOUTH_MENTORING -> "Youth Mentoring"
    OpportunityCategory.ELDERLY_CARE -> "Elderly Care"
    OpportunityCategory.TECHNOLOGY -> "Technology"
    OpportunityCategory.OTHER -> "Other"
}

/**
 * Extension function to check if a volunteer opportunity has passed
 */
fun VolunteerOpportunity.hasPassed(): Boolean {
    fun parseDate(dateStr: String): LocalDate? {
        val patterns = listOf("yyyy-MM-dd", "MMM d, yyyy", "MMMM d, yyyy")
        for (p in patterns) {
            runCatching {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(p, Locale.ENGLISH))
            }
        }
        return null
    }

    fun parseTime(timeStr: String): LocalTime? {
        val patterns = listOf("H:mm", "H:mm:ss", "h:mm a", "hh:mm a")
        for (p in patterns) {
            runCatching {
                return LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern(p, Locale.ENGLISH))
            }
        }
        return null
    }

    val eventDate = parseDate(this.date) ?: return false // If we cannot parse, keep visible to avoid hiding future events incorrectly
    val currentDate = LocalDate.now()
    val currentDateTime = LocalDateTime.now()

    return when {
        eventDate.isBefore(currentDate) -> true
        eventDate.isEqual(currentDate) -> {
            val start = this.startTime?.let { parseTime(it) }
            if (start != null) {
                val eventStartDateTime = LocalDateTime.of(eventDate, start)
                !eventStartDateTime.isAfter(currentDateTime)
            } else {
                false
            }
        }
        else -> false
    }
} 
