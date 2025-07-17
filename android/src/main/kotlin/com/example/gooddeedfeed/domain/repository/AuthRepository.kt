package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainOrganizerProfile
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import java.io.File

interface AuthRepository {
    suspend fun signUp(username: String, email: String, password: String): Result<DomainUser>
    suspend fun signIn(username: String, password: String): Result<DomainUser>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): Result<DomainUser>
    suspend fun setUserType(userType: DomainUserType): Result<Unit>
    suspend fun uploadProfilePicture(file: File): Result<String>
    suspend fun uploadBannerImage(file: File): Result<String>
    suspend fun uploadOrganizationImages(files: List<File>): Result<List<String>>
    suspend fun completeOrganizerOnboarding(
        profile: DomainOrganizerProfile,
        profilePictureFile: File?
    ): Result<Unit>
    suspend fun completeVolunteerOnboarding(
        profile: DomainVolunteerProfile,
        profilePictureFile: File?
    ): Result<Unit>
    suspend fun updateProfile(updates: DomainUserUpdate): Result<DomainUser>
}
