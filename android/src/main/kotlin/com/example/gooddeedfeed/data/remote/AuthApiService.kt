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
import com.example.gooddeedfeed.data.remote.dto.TokenResponseDto
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
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
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
        private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
    }

    // Override the base URLs to use the correct server port (9000)
    override val possibleUrls: List<String> = listOf(
        "http://10.0.2.2:9000/api", // Android emulator → Windows host
        "http://172.28.7.154:9000/api", // Current WSL IP
        "http://172.28.0.1:9000/api", // Windows host from WSL
        "http://localhost:9000/api", // Localhost
        "http://127.0.0.1:9000/api", // Loopback
    )

    private suspend fun getTokenFromDataStore(): String? {
        return try {
            val token = dataStore.data.first()[JWT_TOKEN_KEY]
            Log.d(TAG, "🔍 Token from DataStore: ${if (token != null) "Found (${token.take(20)}...)" else "Not found"}")
            token
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get token from DataStore", e)
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

            val tokenResponse: TokenResponseDto = response.body()
            Log.d(TAG, "✅ SignUp successful - Token type: ${tokenResponse.token_type}")

            // Now get user info
            Log.d(TAG, "📤 Getting user info after sign up...")
            val userResponse = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying getCurrentUser URL: $baseUrl/users/me")
                client.get("$baseUrl/users/me") {
                    header("Authorization", "Bearer ${tokenResponse.access_token}")
                }
            }

            val userDto: UserDto = userResponse.body()
            Log.d(TAG, "✅ User info retrieved - User ID: ${userDto.id}")
            Log.d(TAG, "✅ User info retrieved - Username: ${userDto.username}")

            // Return combined response
            AuthResponseDto(
                access_token = tokenResponse.access_token,
                token_type = tokenResponse.token_type,
                user = userDto,
            )
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
                Log.d(TAG, "🌐 Trying signIn URL: $baseUrl/token")
                client.post("$baseUrl/token") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("username=$username&password=$password")
                }
            }

            Log.d(TAG, "📥 SignIn response status: ${response.status}")
            Log.d(TAG, "📥 SignIn response headers: ${response.headers}")

            val tokenResponse: TokenResponseDto = response.body()
            Log.d(TAG, "✅ SignIn successful - Token type: ${tokenResponse.token_type}")

            // Now get user info
            Log.d(TAG, "📤 Getting user info after sign in...")
            val userResponse = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying getCurrentUser URL: $baseUrl/users/me")
                client.get("$baseUrl/users/me") {
                    header("Authorization", "Bearer ${tokenResponse.access_token}")
                }
            }

            val userDto: UserDto = userResponse.body()
            Log.d(TAG, "✅ User info retrieved - User ID: ${userDto.id}")
            Log.d(TAG, "✅ User info retrieved - Username: ${userDto.username}")

            // Return combined response
            AuthResponseDto(
                access_token = tokenResponse.access_token,
                token_type = tokenResponse.token_type,
                user = userDto,
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ SignIn failed with exception", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            throw e
        }
    }

    suspend fun getCurrentUser(): UserDto {
        Log.d(TAG, "🚀 Starting getCurrentUser request")

        // Manually get token from DataStore since Auth interceptor is not working
        val token = getTokenFromDataStore()
        if (token == null) {
            Log.e(TAG, "❌ No JWT token found in DataStore")
            throw Exception("No authentication token found")
        }

        return try {
            Log.d(TAG, "📤 Sending getCurrentUser request with manual Authorization header...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying getCurrentUser URL: $baseUrl/users/me")
                client.get("$baseUrl/users/me") {
                    header("Authorization", "Bearer $token")
                }
            }

            Log.d(TAG, "📥 getCurrentUser response status: ${response.status}")
            Log.d(TAG, "📥 getCurrentUser response headers: ${response.headers}")

            if (response.status.value == 401) {
                Log.e(TAG, "❌ getCurrentUser received 401 Unauthorized - token not sent or invalid")
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

        // Manually get token from DataStore since Auth interceptor is not working
        val token = getTokenFromDataStore()
        if (token == null) {
            Log.e(TAG, "❌ No JWT token found in DataStore for setUserType")
            return false
        }

        val request = OnboardingStepOneDto(userType)
        return try {
            Log.d(TAG, "📤 Sending setUserType request with manual Authorization header...")
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying setUserType URL: $baseUrl/onboarding/step-one")
                client.post("$baseUrl/onboarding/step-one") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $token")
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

        // Manually get token from DataStore since Auth interceptor is not working
        val token = getTokenFromDataStore()
        if (token == null) {
            Log.e(TAG, "❌ No JWT token found in DataStore for uploadProfilePicture")
            throw Exception("No authentication token found")
        }

        return withFallbackUrls { baseUrl ->
            Log.d(TAG, "🌐 Trying uploadProfilePicture URL: $baseUrl/upload-profile-picture")
            client.post("$baseUrl/upload-profile-picture") {
                header("Authorization", "Bearer $token")
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

    suspend fun uploadBannerImage(file: File): BannerUploadResponse {
        Log.d(TAG, "🚀 Starting uploadBannerImage request")

        // Manually get token from DataStore since Auth interceptor is not working
        val token = getTokenFromDataStore()
        if (token == null) {
            Log.e(TAG, "❌ No JWT token found in DataStore for uploadBannerImage")
            throw Exception("No authentication token found")
        }

        return withFallbackUrls { baseUrl ->
            Log.d(TAG, "🌐 Trying uploadBannerImage URL: $baseUrl/upload-profile-banner")
            client.post("$baseUrl/upload-profile-banner") {
                header("Authorization", "Bearer $token")
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

        // Manually get token from DataStore since Auth interceptor is not working
        val token = getTokenFromDataStore()
        if (token == null) {
            Log.e(TAG, "❌ No JWT token found in DataStore for uploadOrganizationImages")
            throw Exception("No authentication token found")
        }

        return withFallbackUrls { baseUrl ->
            Log.d(TAG, "🌐 Trying uploadOrganizationImages URL: $baseUrl/upload-organization-images")
            client.post("$baseUrl/upload-organization-images") {
                header("Authorization", "Bearer $token")
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

        // Manually get token from DataStore since Auth interceptor is not working
        val token = getTokenFromDataStore()
        if (token == null) {
            Log.e(TAG, "❌ No JWT token found in DataStore for completeOrganizerOnboarding")
            return false
        }

        return try {
            // First upload profile picture if provided
            var profilePictureUrl: String? = null
            profilePictureFile?.let {
                Log.d(TAG, "📤 Uploading profile picture...")
                profilePictureUrl = uploadProfilePicture(it).profile_picture_url
                Log.d(TAG, "✅ Profile picture uploaded: $profilePictureUrl")
            }

            // Then complete onboarding
            Log.d(TAG, "📤 Sending organizer onboarding request...")
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying URL: $baseUrl/complete-organizer-onboarding")
                client.post("$baseUrl/complete-organizer-onboarding") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $token")
                    setBody(
                        OnboardingStepTwoOrganizerDto(
                            full_name = profile.fullName,
                            phone = profile.phone,
                            organization_name = profile.organizationName,
                            organization_type = profile.organizationType.toDto(),
                            organization_description = profile.organizationDescription,
                            organization_website = profile.organizationWebsite,
                            organization_social_media = profile.organizationSocialMedia?.map { it.toDto() },
                            organization_images = profile.organizationImages,
                            organization_custom_type = profile.organizationCustomType,
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

        // Manually get token from DataStore since Auth interceptor is not working
        val token = getTokenFromDataStore()
        if (token == null) {
            Log.e(TAG, "❌ No JWT token found in DataStore for completeVolunteerOnboarding")
            return false
        }

        return try {
            // First upload profile picture if provided
            var profilePictureUrl: String? = null
            profilePictureFile?.let {
                Log.d(TAG, "📤 Uploading profile picture...")
                profilePictureUrl = uploadProfilePicture(it).profile_picture_url
                Log.d(TAG, "✅ Profile picture uploaded: $profilePictureUrl")
            }

            // Then complete onboarding
            Log.d(TAG, "📤 Sending volunteer onboarding request...")
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying URL: $baseUrl/complete-volunteer-onboarding")
                client.post("$baseUrl/complete-volunteer-onboarding") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $token")
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

        // Manually get token from DataStore since Auth interceptor is not working
        val token = getTokenFromDataStore()
        if (token == null) {
            Log.e(TAG, "❌ No JWT token found in DataStore for updateProfile")
            throw Exception("No authentication token found")
        }

        return withFallbackUrls { baseUrl ->
            Log.d(TAG, "🌐 Trying updateProfile URL: $baseUrl/profile")
            client.put("$baseUrl/profile") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(updates)
            }
        }.body()
    }

    suspend fun signOut(): Boolean {
        Log.d(TAG, "🚀 Starting signOut request")
        Log.d(TAG, "ℹ️ No server logout endpoint - performing client-side logout")

        return try {
            Log.d(TAG, "✅ SignOut successful (client-side)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ SignOut failed with exception", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "❌ Exception message: ${e.message}")
            false
        }
    }
}
