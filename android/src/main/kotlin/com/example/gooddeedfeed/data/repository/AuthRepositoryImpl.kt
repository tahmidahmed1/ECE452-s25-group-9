package com.example.gooddeedfeed.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.mapper.toDomain
import com.example.gooddeedfeed.data.mapper.toDto
import com.example.gooddeedfeed.data.remote.AuthApiService
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.domain.model.DomainOrganizerProfile
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import com.example.gooddeedfeed.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val dataStore: DataStore<Preferences>
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
        private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    private suspend fun saveAuthData(token: String, userId: String, username: String) {
        Log.d(TAG, "💾 Saving auth data to DataStore")
        Log.d(TAG, "💾 Token: ${token.take(20)}...")
        Log.d(TAG, "💾 User ID: $userId")
        Log.d(TAG, "💾 Username: $username")
        
        dataStore.edit { preferences ->
            preferences[JWT_TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[USERNAME_KEY] = username
        }
        
        Log.d(TAG, "✅ Auth data saved successfully")
    }

    private suspend fun getToken(): String? {
        val token = dataStore.data.first()[JWT_TOKEN_KEY]
        Log.d(TAG, "🔍 Retrieved token from DataStore: ${if (token != null) "Found" else "Not found"}")
        return token
    }

    private suspend fun clearAuthData() {
        Log.d(TAG, "🧹 Clearing auth data from DataStore")
        dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
        }
        Log.d(TAG, "✅ Auth data cleared successfully")
    }

    override suspend fun signUp(username: String, email: String, password: String): Result<DomainUser> {
        Log.d(TAG, "🔄 Repository signUp called")
        Log.d(TAG, "📝 SignUp params - Username: $username, Email: $email")
        
        return try {
            Log.d(TAG, "📞 Calling AuthApiService.signUp...")
            val response = api.signUp(username, email, password)
            
            Log.d(TAG, "🔄 Converting response to domain model...")
            val domainUser = response.user.toDomain()
            
            Log.d(TAG, "💾 Saving authentication data...")
            saveAuthData(
                token = response.access_token,
                userId = response.user.id.toString(),
                username = response.user.username
            )
            
            Log.d(TAG, "✅ Repository signUp successful")
            Log.d(TAG, "✅ Domain user - ID: ${domainUser.id}, Username: ${domainUser.username}")
            
            Result.success(domainUser)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository signUp failed", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signIn(username: String, password: String): Result<DomainUser> {
        Log.d(TAG, "🔄 Repository signIn called")
        Log.d(TAG, "📝 SignIn params - Username: $username")
        
        return try {
            Log.d(TAG, "📞 Calling AuthApiService.signIn...")
            val response = api.signIn(username, password)
            
            Log.d(TAG, "🔄 Converting response to domain model...")
            val domainUser = response.user.toDomain()
            
            Log.d(TAG, "💾 Saving authentication data...")
            saveAuthData(
                token = response.access_token,
                userId = response.user.id.toString(),
                username = response.user.username
            )
            
            Log.d(TAG, "✅ Repository signIn successful")
            Log.d(TAG, "✅ Domain user - ID: ${domainUser.id}, Username: ${domainUser.username}")
            
            Result.success(domainUser)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository signIn failed", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        Log.d(TAG, "🔄 Repository signOut called")
        
        return try {
            Log.d(TAG, "📞 Calling AuthApiService.signOut...")
            api.signOut()
            
            Log.d(TAG, "🧹 Clearing stored authentication data...")
            clearAuthData()
            
            Log.d(TAG, "✅ Repository signOut successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository signOut failed", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            
            // Even if API call fails, clear local data
            Log.d(TAG, "🧹 Clearing local auth data despite API failure...")
            clearAuthData()
            
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<DomainUser> {
        Log.d(TAG, "🔄 Repository getCurrentUser called")
        
        return try {
            val token = getToken()
            if (token == null) {
                Log.w(TAG, "⚠️ No token found - user not authenticated")
                return Result.failure(Exception("No authentication token found"))
            }
            
            Log.d(TAG, "📞 Calling AuthApiService.getCurrentUser...")
            val user = api.getCurrentUser()
            
            Log.d(TAG, "🔄 Converting user to domain model...")
            val domainUser = user.toDomain()
            
            Log.d(TAG, "✅ Repository getCurrentUser successful")
            Log.d(TAG, "✅ Domain user - ID: ${domainUser.id}, Username: ${domainUser.username}")
            
            Result.success(domainUser)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository getCurrentUser failed", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            
            // If getCurrentUser fails, it might be due to invalid token
            if (e.message?.contains("401") == true || e.message?.contains("unauthorized") == true) {
                Log.d(TAG, "🧹 Clearing invalid authentication data...")
                clearAuthData()
            }
            
            Result.failure(e)
        }
    }

    override suspend fun setUserType(userType: DomainUserType): Result<Unit> {
        return try {
            api.setUserType(userType.toDto())
                    Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfilePicture(file: File): Result<String> {
        return try {
            val response = api.uploadProfilePicture(file)
            Result.success(response.profile_picture_url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadBannerImage(file: File): Result<String> {
        return try {
            val response = api.uploadBannerImage(file)
            Result.success(response.banner_url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadOrganizationImages(files: List<File>): Result<List<String>> {
        return try {
            val response = api.uploadOrganizationImages(files)
            Result.success(response.organization_images)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeOrganizerOnboarding(
        profile: DomainOrganizerProfile,
        profilePictureFile: File?
    ): Result<Unit> {
        return try {
            val success = api.completeOrganizerOnboarding(
                profile = profile,
                profilePictureFile = profilePictureFile
                )
                if (success) {
                    Result.success(Unit)
                } else {
                Result.failure(Exception("Failed to complete organizer onboarding"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeVolunteerOnboarding(
        profile: DomainVolunteerProfile,
        profilePictureFile: File?
    ): Result<Unit> {
        return try {
            val success = api.completeVolunteerOnboarding(
                profile = profile,
                profilePictureFile = profilePictureFile
            )
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to complete volunteer onboarding"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(updates: DomainUserUpdate): Result<DomainUser> {
        return try {
            val user = api.updateProfile(updates.toDto())
            Result.success(user.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

