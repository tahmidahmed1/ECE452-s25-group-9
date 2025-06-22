package com.example.gooddeedfeed.domain.util

import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType

/**
 * Utility functions for user type management and validation
 */
object UserTypeUtils {
    
    /**
     * Check if user has completed onboarding for their user type
     */
    fun isOnboardingComplete(user: DomainUser): Boolean {
        return user.onboardingCompleted && 
               user.userType != null &&
               user.fullName?.isNotBlank() == true &&
               user.phone?.isNotBlank() == true &&
               isUserTypeSpecificDataComplete(user)
    }
    
    /**
     * Check if user type specific data is complete
     */
    private fun isUserTypeSpecificDataComplete(user: DomainUser): Boolean {
        return when (user.userType) {
            DomainUserType.VOLUNTEER -> true // No additional fields required
            DomainUserType.ORGANIZER -> user.organizationName?.isNotBlank() == true
            DomainUserType.INSTITUTION -> user.institutionName != null
            null -> false
        }
    }
    
    /**
     * Get display name for user type
     */
    fun getUserTypeDisplayName(userType: DomainUserType?): String {
        return when (userType) {
            DomainUserType.VOLUNTEER -> "Volunteer"
            DomainUserType.ORGANIZER -> "Organizer"
            DomainUserType.INSTITUTION -> "Institution"
            null -> "User"
        }
    }
    
    /**
     * Get user's full display name including organization/institution
     */
    fun getFullDisplayName(user: DomainUser): String {
        val baseName = user.fullName ?: user.username
        return when (user.userType) {
            DomainUserType.ORGANIZER -> {
                if (!user.organizationName.isNullOrBlank()) {
                    "$baseName (${user.organizationName})"
                } else {
                    "$baseName (Organizer)"
                }
            }
            DomainUserType.INSTITUTION -> {
                user.institutionName?.let { institutionName ->
                    "$baseName (${institutionName.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }})"
                } ?: "$baseName (Institution)"
            }
            DomainUserType.VOLUNTEER -> baseName
            null -> baseName
        }
    }
    
    /**
     * Get available user types for registration
     */
    fun getAvailableUserTypes(): List<DomainUserType> {
        return listOf(
            DomainUserType.VOLUNTEER,
            DomainUserType.ORGANIZER,
            DomainUserType.INSTITUTION
        )
    }
    
    /**
     * Get description for user type
     */
    fun getUserTypeDescription(userType: DomainUserType): String {
        return when (userType) {
            DomainUserType.VOLUNTEER -> "Join community service activities and make a difference"
            DomainUserType.ORGANIZER -> "Create and manage volunteer opportunities for your organization"
            DomainUserType.INSTITUTION -> "Review and approve volunteer activities for institutional credit"
        }
    }
} 