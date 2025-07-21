package com.example.gooddeedfeed.data.remote

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.mapper.toDto
import com.example.gooddeedfeed.data.remote.dto.AuthResponseDto
import com.example.gooddeedfeed.data.remote.dto.BannerUploadResponse
import com.example.gooddeedfeed.data.remote.dto.OnboardingStepOneDto
import com.example.gooddeedfeed.data.remote.dto.OnboardingStepThreeVolunteerDto
import com.example.gooddeedfeed.data.remote.dto.OnboardingStepTwoOrganizerDto
import com.example.gooddeedfeed.data.remote.dto.OrganizationImagesResponseDto
import com.example.gooddeedfeed.data.remote.dto.ProfilePictureUploadResponse
import com.example.gooddeedfeed.data.remote.dto.SignUpRequestDto
import com.example.gooddeedfeed.data.remote.dto.UserDto
import com.example.gooddeedfeed.data.remote.dto.UserType
import com.example.gooddeedfeed.data.remote.dto.UserUpdateDto
import com.example.gooddeedfeed.domain.model.DomainOrganizerProfile
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthApiService @Inject constructor(
    client: HttpClient,
    private val dataStore: DataStore<Preferences>,
) : BaseApiService(client) {

    companion object {
        private const val TAG = "AuthApiService"
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
    }

    override val possibleUrls: List<String> = listOf(
        "http://10.0.2.2:9000/api", // Android emulator → Windows host
        "http://172.28.7.154:9000/api", // Current WSL IP
        "http://172.28.0.1:9000/api", // Windows host from WSL
        "http://localhost:9000/api", // Localhost
        "http://127.0.0.1:9000/api", // Loopback
    )

    private suspend fun getSessionIdFromDataStore(): String? {
        return try {
            val sessionId = dataStore.data.first()[SESSION_ID_KEY]
            Log.d(TAG, "🔍 Session ID from DataStore: ${if (sessionId != null) "Found ($sessionId)" else "Not found"}")
            sessionId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get session ID from DataStore", e)
            null
        }
    }

    suspend fun signUp(username: String, email: String, password: String): AuthResponseDto {
        Log.d(TAG, "🚀 Starting signUp request")
        Log.d(TAG, "📝 SignUp details - Username: $username, Email: $email")

        val request = SignUpRequestDto(username, email, password)

        return try {
            Log.d(TAG, "📤 Sending signUp request with URL fallback...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying signUp URL: $baseUrl/register")
                client.post("$baseUrl/register") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            }

            Log.d(TAG, "📥 SignUp response status: ${response.status}")
            Log.d(TAG, "📥 SignUp response headers: ${response.headers}")

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                Log.e(TAG, "❌ SignUp failed with status ${response.status}")
                Log.e(TAG, "❌ Error response body: $errorBody")

                when (response.status.value) {
                    409 -> throw Exception("Username or email already exists")
                    400 -> throw Exception("Invalid signup data provided")
                    else -> throw Exception("Signup failed: ${response.status.description}")
                }
            }

            val authResponse: AuthResponseDto = response.body()
            Log.d(TAG, "✅ SignUp successful - Session type: ${authResponse.session_type}")
            Log.d(TAG, "✅ SignUp successful - User ID: ${authResponse.user.id}")
            Log.d(TAG, "✅ SignUp successful - Username: ${authResponse.user.username}")
            Log.d(TAG, "✅ SignUp successful - Onboarding completed: ${authResponse.user.onboarding_completed}")

            authResponse
        } catch (e: Exception) {
            Log.e(TAG, "❌ SignUp failed with exception", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            throw e
        }
    }

    suspend fun signIn(username: String, password: String): AuthResponseDto {
        Log.d(TAG, "🚀 Starting signIn request")
        Log.d(TAG, "📝 SignIn details - Username: $username")

        return try {
            Log.d(TAG, "📤 Sending signIn request with URL fallback...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying signIn URL: $baseUrl/login")
                client.post("$baseUrl/login") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("username=$username&password=$password")
                }
            }

            Log.d(TAG, "📥 SignIn response status: ${response.status}")
            Log.d(TAG, "📥 SignIn response headers: ${response.headers}")

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                Log.e(TAG, "❌ SignIn failed with status ${response.status}")
                Log.e(TAG, "❌ Error response body: $errorBody")

                when (response.status.value) {
                    401 -> throw Exception("Invalid username or password")
                    400 -> throw Exception("Invalid signin data provided")
                    else -> throw Exception("Signin failed: ${response.status.description}")
                }
            }

            val authResponse: AuthResponseDto = response.body()
            Log.d(TAG, "✅ SignIn successful - Session type: ${authResponse.session_type}")
            Log.d(TAG, "✅ SignIn successful - User ID: ${authResponse.user.id}")
            Log.d(TAG, "✅ SignIn successful - Username: ${authResponse.user.username}")
            Log.d(TAG, "✅ SignIn successful - Onboarding completed: ${authResponse.user.onboarding_completed}")

            authResponse
        } catch (e: Exception) {
            Log.e(TAG, "❌ SignIn failed with exception", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            throw e
        }
    }

    suspend fun getCurrentUser(): UserDto {
        Log.d(TAG, "🚀 Starting getCurrentUser request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore")
            throw Exception("No authentication session found")
        }

        return try {
            Log.d(TAG, "📤 Sending getCurrentUser request with session authorization...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying getCurrentUser URL: $baseUrl/users/me")
                client.get("$baseUrl/users/me") {
                    header("Authorization", "Bearer $sessionId")
                }
            }

            Log.d(TAG, "📥 getCurrentUser response status: ${response.status}")
            Log.d(TAG, "📥 getCurrentUser response headers: ${response.headers}")

            if (response.status.value == 401) {
                Log.e(TAG, "❌ getCurrentUser received 401 Unauthorized - session not sent or invalid")
                throw Exception("Authentication failed: ${response.status}")
            }

            val userDto: UserDto = response.body()
            Log.d(TAG, "✅ getCurrentUser successful - User ID: ${userDto.id}")
            Log.d(TAG, "✅ getCurrentUser successful - Username: ${userDto.username}")

            userDto
        } catch (e: Exception) {
            Log.e(TAG, "❌ getCurrentUser failed with exception", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            throw e
        }
    }

    suspend fun setUserType(userType: UserType): Boolean {
        Log.d(TAG, "🚀 Starting setUserType request")
        Log.d(TAG, "📝 UserType: $userType")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for setUserType")
            return false
        }

        val request = OnboardingStepOneDto(userType)
        return try {
            Log.d(TAG, "📤 Sending setUserType request with manual Authorization header...")
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying setUserType URL: $baseUrl/onboarding/step-one")
                client.post("$baseUrl/onboarding/step-one") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $sessionId")
                    setBody(request)
                }
            }
            Log.d(TAG, "✅ setUserType successful")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ setUserType failed with exception", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            false
        }
    }

    suspend fun uploadProfilePicture(file: File): ProfilePictureUploadResponse {
        Log.d(TAG, "🚀 Starting uploadProfilePicture request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for uploadProfilePicture")
            throw Exception("No authentication session found")
        }

        return withFallbackUrls { baseUrl ->
            Log.d(TAG, "🌐 Trying uploadProfilePicture URL: $baseUrl/upload-profile-picture")
            client.post("$baseUrl/upload-profile-picture") {
                header("Authorization", "Bearer $sessionId")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "file",
                                file.readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/*")
                                    append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                                },
                            )
                        },
                    ),
                )
            }
        }.body()
    }

    suspend fun removeProfilePicture(): Boolean {
        Log.d(TAG, "🚀 Starting removeProfilePicture request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for removeProfilePicture")
            throw Exception("No authentication session found")
        }

        return try {
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying removeProfilePicture URL: $baseUrl/remove-profile-picture")
                client.post("$baseUrl/remove-profile-picture") {
                    header("Authorization", "Bearer $sessionId")
                    contentType(ContentType.Application.Json)
                }
            }
            Log.d(TAG, "✅ removeProfilePicture successful")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ removeProfilePicture failed with exception", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            throw e
        }
    }

    suspend fun uploadBannerImage(file: File): BannerUploadResponse {
        Log.d(TAG, "🚀 Starting uploadBannerImage request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for uploadBannerImage")
            throw Exception("No authentication session found")
        }

        return withFallbackUrls { baseUrl ->
            Log.d(TAG, "🌐 Trying uploadBannerImage URL: $baseUrl/upload-profile-banner")
            client.post("$baseUrl/upload-profile-banner") {
                header("Authorization", "Bearer $sessionId")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "file",
                                file.readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/*")
                                    append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                                },
                            )
                        },
                    ),
                )
            }
        }.body()
    }

    suspend fun uploadOrganizationImages(files: List<File>): OrganizationImagesResponseDto {
        Log.d(TAG, "🚀 Starting uploadOrganizationImages request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for uploadOrganizationImages")
            throw Exception("No authentication session found")
        }

        return withFallbackUrls { baseUrl ->
            Log.d(TAG, "🌐 Trying uploadOrganizationImages URL: $baseUrl/upload-organization-images")
            client.post("$baseUrl/upload-organization-images") {
                header("Authorization", "Bearer $sessionId")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            files.forEach { file ->
                                append(
                                    "files",
                                    file.readBytes(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "image/*")
                                        append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                                    },
                                )
                            }
                        },
                    ),
                )
            }
        }.body<OrganizationImagesResponseDto>()
    }

    suspend fun completeOrganizerOnboarding(
        profile: DomainOrganizerProfile,
        profilePictureFile: File?,
    ): Boolean {
        Log.d(TAG, "🚀 Starting completeOrganizerOnboarding")
        Log.d(TAG, "📝 Profile: $profile")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for completeOrganizerOnboarding")
            return false
        }

        return try {
            var profilePictureUrl: String? = null
            profilePictureFile?.let {
                Log.d(TAG, "📤 Uploading profile picture...")
                profilePictureUrl = uploadProfilePicture(it).profile_picture_url
                Log.d(TAG, "✅ Profile picture uploaded: $profilePictureUrl")
            }

            Log.d(TAG, "📤 Sending organizer onboarding request...")
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying URL: $baseUrl/complete-organizer-onboarding")
                client.post("$baseUrl/complete-organizer-onboarding") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $sessionId")
                    setBody(
                        OnboardingStepTwoOrganizerDto(
                            full_name = profile.fullName,
                            phone = profile.phone,
                            organization_name = profile.organizationName,
                            organization_description = profile.organizationDescription,
                            organization_website = profile.organizationWebsite,
                            organization_social_media = profile.organizationSocialMedia?.map { it.toDto() },
                            organization_images = profile.organizationImages,
                        ),
                    )
                }
            }
            Log.d(TAG, "✅ Organizer onboarding completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Organizer onboarding failed", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            false
        }
    }

    suspend fun completeVolunteerOnboarding(
        profile: DomainVolunteerProfile,
        profilePictureFile: File?,
    ): Boolean {
        Log.d(TAG, "🚀 Starting completeVolunteerOnboarding")
        Log.d(TAG, "📝 Profile: $profile")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for completeVolunteerOnboarding")
            return false
        }

        return try {
            var profilePictureUrl: String? = null
            profilePictureFile?.let {
                Log.d(TAG, "📤 Uploading profile picture...")
                profilePictureUrl = uploadProfilePicture(it).profile_picture_url
                Log.d(TAG, "✅ Profile picture uploaded: $profilePictureUrl")
            }

            Log.d(TAG, "📤 Sending volunteer onboarding request...")
            withFallbackUrls { baseUrl ->
                val endpoint = "$baseUrl/onboarding/volunteer-complete"
                Log.d(TAG, "🌐 Trying URL: $endpoint")
                client.post(endpoint) {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $sessionId")
                    setBody(
                        OnboardingStepThreeVolunteerDto(
                            full_name = profile.fullName,
                            phone = profile.phone,
                            sex = profile.sex.toDto(),
                            description = profile.description,
                            skills = profile.skills,
                            age = profile.age,
                            emergency_contact_name = profile.emergencyContactName,
                            emergency_contact_phone = profile.emergencyContactPhone,
                            location_area = profile.locationArea,
                            has_drivers_license = profile.hasDriversLicense,
                            disabilities = profile.disabilities,
                        ),
                    )
                }
            }
            Log.d(TAG, "✅ Volunteer onboarding completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Volunteer onboarding failed", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            false
        }
    }

    suspend fun updateProfile(updates: UserUpdateDto): UserDto {
        Log.d(TAG, "🚀 Starting updateProfile request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for updateProfile")
            throw Exception("No authentication session found")
        }

        val response = withFallbackUrls { baseUrl ->
            val endpoint = "$baseUrl/users/me"
            Log.d(TAG, "🌐 Trying updateProfile URL: $endpoint")
            client.put(endpoint) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $sessionId")
                setBody(updates)
            }
        }

        Log.d(TAG, "📥 updateProfile response status: ${response.status}")

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            Log.e(TAG, "❌ updateProfile failed with status ${response.status}")
            Log.e(TAG, "❌ Error response body: $errorBody")
            throw Exception("Profile update failed: ${response.status.description}")
        }

        val updatedUser: UserDto = response.body()

        Log.d(TAG, "✅ updateProfile successful. Returned userId=${updatedUser.id}, fullName=${updatedUser.full_name}")
        return updatedUser
    }

    suspend fun signOut(): Boolean {
        Log.d(TAG, "🚀 Starting signOut request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.w(TAG, "⚠️ No session ID found for logout - proceeding with client-side logout only")
            return true
        }

        return try {
            Log.d(TAG, "📤 Calling server logout endpoint...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying logout URL: $baseUrl/logout")
                client.post("$baseUrl/logout") {
                    header("Authorization", "Bearer $sessionId")
                    contentType(ContentType.Application.Json)
                }
            }

            Log.d(TAG, "📥 Logout response status: ${response.status}")

            if (response.status.isSuccess()) {
                Log.d(TAG, "✅ Server logout successful")
                true
            } else {
                Log.w(TAG, "⚠️ Server logout failed with status ${response.status} - continuing with client logout")
                true // Still return true to allow client-side cleanup
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Server logout failed with exception - continuing with client logout", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            true // Still return true to allow client-side cleanup
        }
    }

    suspend fun increaseKarmaPointsDevOnly(): UserDto {
        Log.d(TAG, "🚀 Starting increaseKarmaPointsDevOnly request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for increaseKarmaPointsDevOnly")
            throw Exception("No authentication session found")
        }

        return try {
            Log.d(TAG, "📤 Calling dev karma increase endpoint...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying karma increase URL: $baseUrl/dev/increase-karma")
                client.post("$baseUrl/dev/increase-karma") {
                    header("Authorization", "Bearer $sessionId")
                    contentType(ContentType.Application.Json)
                }
            }

            Log.d(TAG, "📥 Karma increase response status: ${response.status}")

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                Log.e(TAG, "❌ Karma increase failed with status ${response.status}")
                Log.e(TAG, "❌ Error response body: $errorBody")
                throw Exception("Failed to increase karma points: ${response.status.description}")
            }

            val userDto: UserDto = response.body()
            Log.d(TAG, "✅ Karma increased successfully. New karma: ${userDto.karma_points}")
            userDto
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to increase karma points", e)
            throw e
        }
    }
}
