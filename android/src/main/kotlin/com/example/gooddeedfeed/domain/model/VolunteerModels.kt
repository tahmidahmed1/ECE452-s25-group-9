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
    val isActive: Boolean = true
)

enum class OpportunityCategory {
    COMMUNITY_SERVICE,
    EDUCATION,
    ENVIRONMENTAL,
    HEALTHCARE,
    SOCIAL_SERVICES,
    DISASTER_RELIEF,
    OTHER
} 