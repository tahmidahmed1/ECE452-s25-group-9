package com.example.gooddeedfeed.domain.model

/**
 * Domain models specific to volunteer functionality
 */

data class VolunteerOpportunity(
    val id: Int,
    val title: String,
    val organizationName: String,
    val location: String,
    val date: String,
    val description: String,
    val requiredVolunteers: Int,
    val currentVolunteers: Int,
    val category: OpportunityCategory,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isActive: Boolean = true,
    val karmaPoints: Int = 10,
    val imageUrl: String? = null,
)

enum class OpportunityCategory {
    COMMUNITY_SERVICE,
    EDUCATION,
    ENVIRONMENTAL,
    HEALTHCARE,
    SOCIAL_SERVICES,
    DISASTER_RELIEF,
    FOOD_SECURITY,
    OTHER,
}

// Helper to convert enum to backend-accepted string value
fun OpportunityCategory.toApiValue(): String = when (this) {
    OpportunityCategory.COMMUNITY_SERVICE -> "community_service"
    OpportunityCategory.EDUCATION -> "education"
    OpportunityCategory.ENVIRONMENTAL -> "environmental"
    OpportunityCategory.HEALTHCARE -> "healthcare"
    OpportunityCategory.SOCIAL_SERVICES -> "social_services"
    OpportunityCategory.DISASTER_RELIEF -> "disaster_relief"
    OpportunityCategory.FOOD_SECURITY -> "other" // Backend doesn't support food_security; map to other
    OpportunityCategory.OTHER -> "other"
} 
