package com.example.gooddeedfeed.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.mapper.toDomain
import com.example.gooddeedfeed.data.mapper.toDto
import com.example.gooddeedfeed.data.remote.AuthApiService
import com.example.gooddeedfeed.domain.model.DomainOrganizerProfile
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.domain.model.DomainVolunteerHistoryEntry
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import com.example.gooddeedfeed.domain.repository.AuthRepository
import com.example.gooddeedfeed.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val dataStore: DataStore<Preferences>,
    private val notificationRepository: NotificationRepository,
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    private suspend fun saveAuthData(sessionId: String, userId: String, username: String) {
        Log.d(TAG, "💾 Saving auth data to DataStore")
        Log.d(TAG, "💾 Session ID: $sessionId")
        Log.d(TAG, "💾 User ID: $userId")
        Log.d(TAG, "💾 Username: $username")

        dataStore.edit { preferences ->
            preferences[SESSION_ID_KEY] = sessionId
            preferences[USER_ID_KEY] = userId
            preferences[USERNAME_KEY] = username
        }

        Log.d(TAG, "✅ Auth data saved successfully")

        val savedSessionId = dataStore.data.first()[SESSION_ID_KEY]
        val savedUserId = dataStore.data.first()[USER_ID_KEY]
        val savedUsername = dataStore.data.first()[USERNAME_KEY]
        Log.d(TAG, "🔍 Verification - Saved session ID: $savedSessionId")
        Log.d(TAG, "🔍 Verification - Saved user ID: $savedUserId")
        Log.d(TAG, "🔍 Verification - Saved username: $savedUsername")
    }

    private suspend fun getSessionId(): String? {
        val sessionId = dataStore.data.first()[SESSION_ID_KEY]
        Log.d(TAG, "🔍 Retrieved session ID from DataStore: ${if (sessionId != null) "Found" else "Not found"}")
        return sessionId
    }

    private suspend fun clearAuthData() {
        Log.d(TAG, "🧹 Clearing auth data from DataStore")
        dataStore.edit { preferences ->
            preferences.remove(SESSION_ID_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
        }
        Log.d(TAG, "✅ Auth data cleared successfully")
    }

    override suspend fun signUp(username: String, email: String, password: String): Result<DomainUser> {
        Log.d(TAG, "🔄 Repository signUp called")
        Log.d(TAG, "📝 SignUp params - Username: $username, Email: $email")

        return try {
            Log.d(TAG, "🧹 Clearing any existing auth data before signUp...")
            clearAuthData()

            Log.d(TAG, "📞 Calling AuthApiService.signUp...")
            val response = api.signUp(username, email, password)

            Log.d(TAG, "🔄 Converting response to domain model...")
            val domainUser = response.user.toDomain()

            Log.d(TAG, "💾 Saving new authentication data...")
            saveAuthData(
                sessionId = response.session_id,
                userId = response.user.id.toString(),
                username = response.user.username,
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
            Log.d(TAG, "🧹 Clearing any existing auth data before signIn...")
            clearAuthData()

            Log.d(TAG, "📞 Calling AuthApiService.signIn...")
            val response = api.signIn(username, password)

            Log.d(TAG, "🔄 Converting response to domain model...")
            val domainUser = response.user.toDomain()

            Log.d(TAG, "💾 Saving new authentication data...")
            saveAuthData(
                sessionId = response.session_id,
                userId = response.user.id.toString(),
                username = response.user.username,
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

        try {
            Log.d(TAG, "📞 Calling AuthApiService.signOut...")
            api.signOut()
            Log.d(TAG, "✅ Server signOut successful")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository signOut API failed", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            Log.w(TAG, "⚠️ Server signOut failed, but continuing with local cleanup...")
        } finally {
            Log.d(TAG, "🧹 Clearing stored authentication data...")
            clearAuthData()
            Log.d(TAG, "✅ Local authentication data cleared")
        }

        return Result.success(Unit)
    }

    override suspend fun getCurrentUser(): Result<DomainUser> {
        Log.d(TAG, "🔄 Repository getCurrentUser called")

        return try {
            val sessionId = getSessionId()
            if (sessionId == null) {
                Log.w(TAG, "⚠️ No session ID found - user not authenticated")
                return Result.failure(Exception("No authentication session found"))
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

            if (e.message?.contains("401") == true ||
                e.message?.contains("unauthorized") == true ||
                e.message?.contains("Authentication failed") == true ||
                e.message?.contains("No authentication token found") == true
            ) {
                Log.d(TAG, "🧹 Clearing invalid authentication data due to auth failure...")
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

    override suspend fun removeProfilePicture(): Result<Unit> {
        return try {
            api.removeProfilePicture()
            Result.success(Unit)
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
        profilePictureFile: File?,
    ): Result<Unit> {
        return try {
            val success = api.completeOrganizerOnboarding(
                profile = profile,
                profilePictureFile = profilePictureFile,
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
        profilePictureFile: File?,
    ): Result<Unit> {
        return try {
            val success = api.completeVolunteerOnboarding(
                profile = profile,
                profilePictureFile = profilePictureFile,
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

    override suspend fun increaseKarmaPointsDevOnly(): Result<DomainUser> {
        return try {
            val user = api.increaseKarmaPointsDevOnly()
            Result.success(user.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getVolunteerHistory(): Result<List<DomainVolunteerHistoryEntry>> {
        return try {
            val historyEntries = api.getVolunteerHistory()
            Result.success(historyEntries.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadVolunteerHistoryPdf(): Result<ByteArray> {
        return try {
            val bytes = api.downloadVolunteerHistoryPdf()
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
