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
        android.util.Log.d("AuthRepositoryImpl", "Starting signUp for username: $username")

        val result = try {
            val resp = api.signUp(username, email, password)
            android.util.Log.d("AuthRepositoryImpl", "API response: success=${resp.success}, token=${resp.token?.take(10)}..., message=${resp.message}")

            // Check if the API response indicates failure
            if (!resp.success) {
                android.util.Log.e("AuthRepositoryImpl", "API returned failure: ${resp.message}")
                Result.failure(Exception(resp.message ?: "Sign up failed"))
            } else if (resp.token == null) {
                android.util.Log.e("AuthRepositoryImpl", "No token received from successful response")
                Result.failure(Exception("No authentication token received"))
            } else {
                // Save token before returning success
                android.util.Log.d("AuthRepositoryImpl", "Saving token...")
                saveToken(resp.token)
                // Add a small delay to ensure DataStore write completes
                kotlinx.coroutines.delay(50)
                android.util.Log.d("AuthRepositoryImpl", "Token saved successfully")
                Result.success(resp.toDomain())
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "SignUp failed: ${e.message}")
            android.util.Log.e("AuthRepositoryImpl", "Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
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
        android.util.Log.d("AuthRepositoryImpl", "Getting current user...")

        val result = try {
            val token = getTokenString()
            android.util.Log.d("AuthRepositoryImpl", "Token retrieved: ${token?.take(10)}...")
            if (token.isNullOrBlank()) {
                android.util.Log.e("AuthRepositoryImpl", "No authentication token found")
                Result.failure(Exception("No authentication token found"))
            } else {
                android.util.Log.d("AuthRepositoryImpl", "Making API call to get user...")
                val user = api.getCurrentUser(token)
                android.util.Log.d("AuthRepositoryImpl", "User retrieved: ${user.username}, onboarding: ${user.onboarding_completed}")
                Result.success(user.toDomain())
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "getCurrentUser failed: ${e.message}")
            android.util.Log.e("AuthRepositoryImpl", "Exception type: ${e.javaClass.simpleName}")
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

    // Helper method to get token as String for API calls
    private suspend fun getTokenString(): String? = getToken().firstOrNull()

    private suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }
}
