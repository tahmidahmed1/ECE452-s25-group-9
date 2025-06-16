package com.example.gooddeedfeed.domain.util

import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.data.remote.UserType
import com.example.gooddeedfeed.data.remote.InstitutionName

/**
 * Utility functions for user type management and validation
 */
object UserTypeUtils {
    
    /**
     * Check if user has completed onboarding for their user type
     */
    fun isOnboardingComplete(user: User): Boolean {
        return user.onboarding_completed && 
               user.user_type != null &&
               user.full_name?.isNotBlank() == true &&
               user.phone?.isNotBlank() == true &&
               isUserTypeSpecificDataComplete(user)
    }
    
    /**
     * Check if user type specific data is complete
     */
    private fun isUserTypeSpecificDataComplete(user: User): Boolean {
        return when (user.user_type) {
            UserType.VOLUNTEER -> true // No additional fields required
            UserType.ORGANIZER -> user.organization_name?.isNotBlank() == true
            UserType.INSTITUTION -> user.institution_name != null
            null -> false
        }
    }
    
    /**
     * Get display name for user type
     */
    fun getUserTypeDisplayName(userType: UserType?): String {
        return when (userType) {
            UserType.VOLUNTEER -> "Volunteer"
            UserType.ORGANIZER -> "Organizer"
            UserType.INSTITUTION -> "Institution"
            null -> "User"
        }
    }
    
    /**
     * Get user's full display name including organization/institution
     */
    fun getUserFullDisplayName(user: User): String {
        val baseType = getUserTypeDisplayName(user.user_type)
        return when (user.user_type) {
            UserType.ORGANIZER -> {
                user.organization_name?.let { "$baseType • $it" } ?: baseType
            }
            UserType.INSTITUTION -> {
                user.institution_name?.let { 
                    "$baseType • ${getInstitutionDisplayName(it)}" 
                } ?: baseType
            }
            else -> baseType
        }
    }
    
    /**
     * Get display name for institution
     */
    fun getInstitutionDisplayName(institutionName: InstitutionName): String {
        return institutionName.name.replace("_", " ")
    }
    
    /**
     * Check if user can access a specific feature based on their type
     */
    fun canAccessFeature(user: User, feature: UserFeature): Boolean {
        if (!isOnboardingComplete(user)) return false
        
        return when (feature) {
            UserFeature.BROWSE_OPPORTUNITIES -> user.user_type == UserType.VOLUNTEER
            UserFeature.VIEW_MAP -> user.user_type == UserType.VOLUNTEER
            UserFeature.MANAGE_EVENTS -> user.user_type == UserType.ORGANIZER
            UserFeature.CREATE_EVENTS -> user.user_type == UserType.ORGANIZER
            UserFeature.REVIEW_ACTIVITIES -> user.user_type == UserType.INSTITUTION
            UserFeature.VIEW_ANALYTICS -> user.user_type == UserType.INSTITUTION
            UserFeature.SETTINGS -> true // All users can access settings
            UserFeature.HOME -> true // All users can access home
        }
    }
    
    /**
     * Get required fields for user type during onboarding
     */
    fun getRequiredFieldsForUserType(userType: UserType): List<RequiredField> {
        val baseFields = listOf(
            RequiredField.FULL_NAME,
            RequiredField.PHONE
        )
        
        return when (userType) {
            UserType.VOLUNTEER -> baseFields
            UserType.ORGANIZER -> baseFields + RequiredField.ORGANIZATION_NAME
            UserType.INSTITUTION -> baseFields + RequiredField.INSTITUTION_NAME
        }
    }
    
    /**
     * Validate user data completeness for their type
     */
    fun validateUserData(user: User): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (user.user_type == null) {
            errors.add("User type is required")
        }
        
        if (user.full_name.isNullOrBlank()) {
            errors.add("Full name is required")
        }
        
        if (user.phone.isNullOrBlank()) {
            errors.add("Phone number is required")
        }
        
        when (user.user_type) {
            UserType.ORGANIZER -> {
                if (user.organization_name.isNullOrBlank()) {
                    errors.add("Organization name is required for organizers")
                }
            }
            UserType.INSTITUTION -> {
                if (user.institution_name == null) {
                    errors.add("Institution selection is required for institutions")
                }
            }
            else -> { /* No additional validation needed */ }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
}

/**
 * Features that users can access based on their type
 */
enum class UserFeature {
    BROWSE_OPPORTUNITIES,
    VIEW_MAP,
    MANAGE_EVENTS,
    CREATE_EVENTS,
    REVIEW_ACTIVITIES,
    VIEW_ANALYTICS,
    SETTINGS,
    HOME
}

/**
 * Required fields for user onboarding
 */
enum class RequiredField {
    FULL_NAME,
    PHONE,
    ORGANIZATION_NAME,
    INSTITUTION_NAME
}

/**
 * Validation result for user data
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
} 