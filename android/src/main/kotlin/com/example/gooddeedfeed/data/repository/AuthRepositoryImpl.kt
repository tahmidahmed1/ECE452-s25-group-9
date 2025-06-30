package com.example.gooddeedfeed.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gooddeedfeed.data.mapper.toData
import com.example.gooddeedfeed.data.mapper.toDomain
import com.example.gooddeedfeed.data.remote.AuthApiService
import com.example.gooddeedfeed.domain.model.DomainAuthResponse
import com.example.gooddeedfeed.domain.model.DomainInstitutionName
import com.example.gooddeedfeed.domain.model.DomainProfilePictureUploadResponse
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import com.example.gooddeedfeed.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "auth")
private val TOKEN_KEY = stringPreferencesKey("jwt_token")

class AuthRepositoryImpl(
    private val context: Context,
    private val api: AuthApiService,
) : AuthRepository {

    override suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): Flow<Result<DomainAuthResponse>> = flow {
        val result = try {
            val resp = api.signUp(username, email, password)
            if (resp?.success == true && resp.token != null) {
                saveToken(resp.token)
                Result.success(resp.toDomain())
            } else {
                Result.failure(Exception(resp?.message ?: "Sign up failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
        emit(result)
    }

    override suspend fun signIn(
        username: String,
        password: String,
    ): Flow<Result<DomainAuthResponse>> = flow {
        val result = try {
            val resp = api.signIn(username, password)
            resp.token?.let { token -> saveToken(token) }
            Result.success(resp.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
        emit(result)
    }

    override suspend fun signOut(): Flow<Result<Unit>> = flow {
        val result = try {
            saveToken("")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
        emit(result)
    }

    override suspend fun getCurrentUser(): Flow<Result<DomainUser>> = flow {
        val result = try {
            val token = getTokenString()
            if (token.isNullOrBlank()) {
                Result.failure(Exception("No authentication token found"))
            } else {
                val user = api.getCurrentUser(token)
                Result.success(user.toDomain())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
        emit(result)
    }

    override suspend fun updateUserType(userType: DomainUserType): Result<Unit> {
        return try {
            val token = getTokenString()
            if (token.isNullOrBlank()) {
                Result.failure(Exception("No authentication token found"))
            } else {
                val success = api.completeOnboardingStepOne(token, userType.toData())
                if (success) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to update user type"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeOnboarding(
        userType: DomainUserType,
        fullName: String,
        phone: String,
        organizationName: String?,
        institutionName: DomainInstitutionName?,
        profilePictureUrl: String?,
    ): Result<Unit> {
        return try {
            val token = getTokenString()
            if (token.isNullOrBlank()) {
                Result.failure(Exception("No authentication token found"))
            } else {
                val success = api.completeOnboarding(
                    token = token,
                    userType = userType.toData(),
                    fullName = fullName,
                    phone = phone,
                    organizationName = organizationName,
                    institutionName = institutionName?.toData(),
                )
                if (success) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to complete onboarding"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeVolunteerOnboarding(
        volunteerProfile: DomainVolunteerProfile,
        profilePictureUrl: String?,
    ): Result<Unit> {
        return try {
            val token = getTokenString()
            if (token.isNullOrBlank()) {
                Result.failure(Exception("No authentication token found"))
            } else {
                val success = api.completeVolunteerOnboarding(
                    token = token,
                    volunteerProfile = volunteerProfile,
                    profilePictureUrl = profilePictureUrl,
                )
                if (success) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to complete volunteer onboarding"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfilePicture(file: File): Flow<Result<DomainProfilePictureUploadResponse>> = flow {
        val result = try {
            val token = getTokenString()
            if (token.isNullOrBlank()) {
                Result.failure(Exception("No authentication token found"))
            } else {
                val response = api.uploadProfilePicture(token, file)
                if (response != null) {
                    Result.success(response.toDomain())
                } else {
                    Result.failure(Exception("Failed to upload profile picture"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
        emit(result)
    }

    override fun getToken(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    private suspend fun getTokenString(): String? = getToken().firstOrNull()

    private suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    override suspend fun updateUserProfile(update: DomainUserUpdate): Result<Unit> {
        return try {
            val token = getTokenString()
            if (token.isNullOrBlank()) {
                Result.failure(Exception("No authentication token found"))
            } else {
                val success = api.updateUserProfile(token, update)
                if (success) Result.success(Unit) else Result.failure(Exception("Failed to update profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
