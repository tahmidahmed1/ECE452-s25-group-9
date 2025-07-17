package com.example.gooddeedfeed.domain.util

import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType

/**
 * Utility functions for user type operations and validation
 */
object UserTypeUtils {

    /**
     * Check if a user has completed their profile setup based on their user type
     */
    fun hasCompletedProfile(user: DomainUser): Boolean {
        return when (user.userType) {
            DomainUserType.VOLUNTEER -> {
                user.fullName != null && user.phone != null && user.sex != null &&
                user.description != null && user.skills != null && user.age != null &&
                user.emergencyContactName != null && user.emergencyContactPhone != null &&
                user.locationArea != null && user.hasDriversLicense != null
            }
            DomainUserType.ORGANIZER -> user.organizationName != null
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
            null -> "Unknown"
        }
    }

    /**
     * Get user's full display name including organization
     */
    fun getUserFullDisplayName(user: DomainUser): String {
        val baseName = user.fullName ?: user.username
        
        return when (user.userType) {
            DomainUserType.VOLUNTEER -> baseName
            DomainUserType.ORGANIZER -> {
                user.organizationName?.let { orgName ->
                    "$baseName ($orgName)"
                } ?: baseName
            }
            null -> baseName
        }
    }

    /**
     * Get all available user types for registration
     */
    fun getAvailableUserTypes(): List<DomainUserType> {
        return listOf(
            DomainUserType.VOLUNTEER,
            DomainUserType.ORGANIZER,
        )
    }

    /**
     * Get description for user type
     */
    fun getUserTypeDescription(userType: DomainUserType?): String {
        return when (userType) {
            DomainUserType.VOLUNTEER -> "Find and participate in volunteer opportunities in your community"
            DomainUserType.ORGANIZER -> "Create and manage volunteer events and opportunities"
            null -> "Please select a user type to continue"
        }
    }
} 
