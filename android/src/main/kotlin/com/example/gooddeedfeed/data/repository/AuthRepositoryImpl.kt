package com.example.gooddeedfeed.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gooddeedfeed.data.remote.AuthApiService
import com.example.gooddeedfeed.data.remote.AuthResponse
import com.example.gooddeedfeed.data.remote.InstitutionName
import com.example.gooddeedfeed.data.remote.ProfilePictureUploadResponse
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.data.remote.UserType
import com.example.gooddeedfeed.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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
    ): AuthResponse {
        val resp = api.signUp(username, email, password)
        resp.token?.let { token -> saveToken(token) }
        return resp
    }

    override suspend fun signIn(
        username: String,
        password: String,
    ): AuthResponse {
        val resp = api.signIn(username, password)
        resp.token?.let { token -> saveToken(token) }
        return resp
    }

    override suspend fun signOut() {
        saveToken("")
    }

    override fun getToken(): Flow<String?> = context.dataStore.data.map { preferences -> preferences[TOKEN_KEY] }

    // Helper method to get token as String for API calls
    suspend fun getTokenString(): String? = getToken().firstOrNull()

    override suspend fun getCurrentUser(): User? {
        val token = getTokenString()
        return if (token.isNullOrEmpty()) {
            null
        } else {
            try {
                api.getCurrentUser(token)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun completeOnboardingStepOne(token: String, userType: UserType): Boolean {
        return try {
            api.completeOnboardingStepOne(token, userType)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun completeOnboarding(
        token: String,
        userType: UserType,
        fullName: String,
        phone: String,
        organizationName: String?,
        institutionName: InstitutionName?,
    ): Boolean {
        return try {
            api.completeOnboarding(token, userType, fullName, phone, organizationName, institutionName)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun uploadProfilePicture(token: String, imageFile: File): ProfilePictureUploadResponse? {
        return try {
            api.uploadProfilePicture(token, imageFile)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }
}
