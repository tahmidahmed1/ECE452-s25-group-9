package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.data.remote.AuthResponse
import com.example.gooddeedfeed.data.remote.InstitutionName
import com.example.gooddeedfeed.data.remote.ProfilePictureUploadResponse
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.data.remote.UserType
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AuthRepository {
    suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): AuthResponse

    suspend fun signIn(
        username: String,
        password: String,
    ): AuthResponse

    suspend fun signOut()

    fun getToken(): Flow<String?>

    suspend fun getCurrentUser(): User?

    // Onboarding methods
    suspend fun completeOnboardingStepOne(token: String, userType: UserType): Boolean

    suspend fun completeOnboarding(
        token: String,
        userType: UserType,
        fullName: String,
        phone: String,
        organizationName: String?,
        institutionName: InstitutionName?,
    ): Boolean

    // Profile picture upload
    suspend fun uploadProfilePicture(token: String, imageFile: File): ProfilePictureUploadResponse?
}
