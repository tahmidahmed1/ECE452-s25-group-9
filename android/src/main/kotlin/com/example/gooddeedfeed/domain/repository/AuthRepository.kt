package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.DomainAuthResponse
import com.example.gooddeedfeed.domain.model.DomainInstitutionName
import com.example.gooddeedfeed.domain.model.DomainProfilePictureUploadResponse
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AuthRepository {
    suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): Flow<Result<DomainAuthResponse>>

    suspend fun signIn(
        username: String,
        password: String,
    ): Flow<Result<DomainAuthResponse>>

    suspend fun signOut(): Flow<Result<Unit>>

    fun getToken(): Flow<String?>

    suspend fun getCurrentUser(): Flow<Result<DomainUser>>

    // Onboarding methods
    suspend fun updateUserType(userType: DomainUserType): Result<Unit>

    suspend fun completeOnboarding(
        userType: DomainUserType,
        fullName: String,
        phone: String,
        organizationName: String?,
        institutionName: DomainInstitutionName?,
        profilePictureUrl: String? = null,
    ): Result<Unit>

    suspend fun completeVolunteerOnboarding(
        volunteerProfile: DomainVolunteerProfile,
        profilePictureUrl: String? = null,
    ): Result<Unit>

    // Profile picture upload
    suspend fun uploadProfilePicture(file: File): Flow<Result<DomainProfilePictureUploadResponse>>
}
