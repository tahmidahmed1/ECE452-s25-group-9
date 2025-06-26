package com.example.gooddeedfeed.data.remote

import android.util.Log
import com.example.gooddeedfeed.data.mapper.toData
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import com.example.gooddeedfeed.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.File

class AuthApiService(client: HttpClient) : BaseApiService(client) {
    companion object {
        private const val TAG = "AuthApiService"
    }

    suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): AuthResponse {
        Log.d(TAG, "Starting signUp request for username: $username, email: $email")

        val allErrors = mutableListOf<String>()
        var lastResponseBody: String? = null

        // Try each URL until one works
        for ((index, url) in possibleUrls.withIndex()) {
            try {
                Log.d(TAG, "Trying URL $index: $url/register")

                val requestBody = SignUpRequest(username, email, password)
                Log.d(TAG, "Request body: $requestBody")

                val httpResponse = client.post("$url/register") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                    setBody(requestBody)
                }

                Log.d(TAG, "Received response with status ${httpResponse.status} from $url")

                // Success case (created or ok) – parse token
                if (httpResponse.status == HttpStatusCode.Created || httpResponse.status == HttpStatusCode.OK) {
                    val tokenResp: TokenResponse = httpResponse.body()
                    Log.d(TAG, "Registration successful with URL: $url")
                    return AuthResponse(success = true, token = tokenResp.access_token, message = "Registration successful")
                }

                // Failure case – try to parse structured error first
                try {
                    val error = httpResponse.body<ErrorResponse>()
                    Log.e(TAG, "Server returned error: ${error.message}")
                    return AuthResponse(false, message = error.message)
                } catch (ex: Exception) {
                    // Fallback to raw text
                    val rawError = httpResponse.bodyAsText()
                    Log.e(TAG, "Unable to parse structured error. Raw: $rawError")
                    return AuthResponse(false, message = rawError)
                }
            } catch (e: ClientRequestException) {
                Log.e(TAG, "Client error with URL $url: ${e.response.status} - ${e.message}")

                // Try to get detailed error response
                try {
                    lastResponseBody = e.response.body<String>()
                    Log.e(TAG, "Full error response body: $lastResponseBody")
                } catch (bodyException: Exception) {
                    Log.e(TAG, "Could not read error response body: ${bodyException.message}")
                }

                // Handle 422 validation errors specifically
                if (e.response.status == HttpStatusCode.UnprocessableEntity) {
                    try {
                        // Try to parse as ErrorResponse first
                        val errorResponse = e.response.body<ErrorResponse>()
                        Log.e(TAG, "Validation error details: ${errorResponse.message}")
                        if (!errorResponse.errors.isNullOrEmpty()) {
                            Log.e(TAG, "Validation errors: ${errorResponse.errors}")
                            // Extract the first detailed validation message
                            val firstError = errorResponse.errors.first()
                            val detailedMessage = firstError.msg
                            Log.d(TAG, "Using detailed validation message: $detailedMessage")
                            return AuthResponse(false, message = detailedMessage)
                        }
                        return AuthResponse(false, message = errorResponse.message)
                    } catch (bodyException: Exception) {
                        Log.e(TAG, "Could not parse validation error response: ${bodyException.message}")
                        // Try to get raw response text to extract the actual error
                        try {
                            val rawBody = lastResponseBody ?: "Unknown validation error"
                            Log.d(TAG, "Raw error response: $rawBody")

                            // pass raw server message to be formatted client side
                            return AuthResponse(false, message = rawBody)
                        } catch (ex: Exception) {
                            return AuthResponse(false, message = "Please check your input and try again")
                        }
                    }
                }

                // Handle 409 conflict (user already exists)
                if (e.response.status == HttpStatusCode.Conflict) {
                    try {
                        val errorResponse = e.response.body<ErrorResponse>()
                        Log.e(TAG, "Conflict error: ${errorResponse.message}")
                        return AuthResponse(false, message = errorResponse.message)
                    } catch (bodyException: Exception) {
                        return AuthResponse(false, message = "User with this username or email already exists.")
                    }
                }

                // For other 4xx errors, try to extract meaningful error message
                if (e.response.status.value in 400..499) {
                    try {
                        val errorResponse = e.response.body<ErrorResponse>()
                        Log.e(TAG, "4xx error response: ${errorResponse.message}")
                        allErrors.add("URL $index (${e.response.status}): ${errorResponse.message}")

                        // If this is the last URL, return the error
                        if (index == possibleUrls.size - 1) {
                            return AuthResponse(false, message = errorResponse.message)
                        }
                    } catch (bodyException: Exception) {
                        val errorMsg = "Registration failed with status ${e.response.status}"
                        allErrors.add("URL $index: $errorMsg")
                        Log.e(TAG, errorMsg)

                        // If this is the last URL, return a generic error
                        if (index == possibleUrls.size - 1) {
                            return AuthResponse(false, message = errorMsg)
                        }
                    }
                }

                // For connection-related errors, continue to next URL
                allErrors.add("URL $index: Connection failed - ${e.message}")
                continue
            } catch (e: ServerResponseException) {
                Log.e(TAG, "Server error with URL $url: ${e.response.status} - ${e.message}")

                try {
                    lastResponseBody = e.response.body<String>()
                    Log.e(TAG, "Server error response body: $lastResponseBody")
                } catch (bodyException: Exception) {
                    Log.e(TAG, "Could not read server error response body: ${bodyException.message}")
                }

                val errorMsg = "Server error (${e.response.status}): ${e.message}"
                allErrors.add("URL $index: $errorMsg")

                // For 5xx errors, try to get server error message but don't continue to other URLs
                try {
                    val errorResponse = e.response.body<ErrorResponse>()
                    return AuthResponse(false, message = "Server error: ${errorResponse.message}")
                } catch (bodyException: Exception) {
                    return AuthResponse(false, message = errorMsg)
                }
            } catch (e: RedirectResponseException) {
                Log.e(TAG, "Redirect error with URL $url: ${e.response.status} - ${e.message}")
                allErrors.add("URL $index: Unexpected redirect - ${e.message}")
                continue
            } catch (e: SerializationException) {
                Log.e(TAG, "Serialization error with URL $url: ${e.message}")
                allErrors.add("URL $index: Response format error - ${e.message}")
                continue
            } catch (e: Exception) {
                Log.e(TAG, "General error with URL $url: ${e.message}")
                allErrors.add("URL $index: ${e.message}")
                continue
            }
        }

        // All URLs failed - create detailed error message
        Log.e(TAG, "All URLs failed for signUp. Errors: $allErrors")
        val detailedError = if (allErrors.isNotEmpty()) {
            "Connection failed. Attempted servers:\n${allErrors.joinToString("\n")}"
        } else {
            "Could not connect to server. Please check your network connection."
        }

        return AuthResponse(false, message = detailedError)
    }

    suspend fun signIn(
        username: String,
        password: String,
    ): AuthResponse {
        Log.d(TAG, "Starting signIn request for username: $username")

        val allErrors = mutableListOf<String>()
        var lastResponseBody: String? = null

        // Try each URL until one works
        for ((index, url) in possibleUrls.withIndex()) {
            try {
                Log.d(TAG, "Trying URL $index: $url/token")

                val response: TokenResponse = client.post("$url/token") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    }
                    setBody("username=$username&password=$password")
                }.body()

                Log.d(TAG, "Login successful with URL: $url")
                return AuthResponse(success = true, token = response.access_token, message = "Login successful")
            } catch (e: ClientRequestException) {
                Log.e(TAG, "Client error with URL $url: ${e.response.status} - ${e.message}")

                // Try to get detailed error response
                try {
                    lastResponseBody = e.response.body<String>()
                    Log.e(TAG, "Full error response body: $lastResponseBody")
                } catch (bodyException: Exception) {
                    Log.e(TAG, "Could not read error response body: ${bodyException.message}")
                }

                // Handle 401 unauthorized specifically
                if (e.response.status == HttpStatusCode.Unauthorized) {
                    try {
                        val errorResponse = e.response.body<ErrorResponse>()
                        Log.e(TAG, "Authentication error details: ${errorResponse.message}")
                        return AuthResponse(false, message = errorResponse.message)
                    } catch (bodyException: Exception) {
                        return AuthResponse(false, message = "Invalid username or password")
                    }
                }

                // Handle 422 validation errors
                if (e.response.status == HttpStatusCode.UnprocessableEntity) {
                    try {
                        val errorResponse = e.response.body<ErrorResponse>()
                        Log.e(TAG, "Validation error details: ${errorResponse.message}")
                        if (!errorResponse.errors.isNullOrEmpty()) {
                            Log.e(TAG, "Validation errors: ${errorResponse.errors}")
                            // Extract the first detailed validation message
                            val firstError = errorResponse.errors.first()
                            val detailedMessage = firstError.msg
                            Log.d(TAG, "Using detailed validation message: $detailedMessage")
                            return AuthResponse(false, message = detailedMessage)
                        }
                        return AuthResponse(false, message = errorResponse.message)
                    } catch (bodyException: Exception) {
                        val errorMsg = if (lastResponseBody != null) {
                            "Login failed: $lastResponseBody"
                        } else {
                            "Invalid login data. Please check your username and password."
                        }
                        return AuthResponse(false, message = errorMsg)
                    }
                }

                // For other 4xx errors, try to extract meaningful error message
                if (e.response.status.value in 400..499) {
                    try {
                        val errorResponse = e.response.body<ErrorResponse>()
                        Log.e(TAG, "4xx error response: ${errorResponse.message}")
                        allErrors.add("URL $index (${e.response.status}): ${errorResponse.message}")

                        // If this is the last URL, return the error
                        if (index == possibleUrls.size - 1) {
                            return AuthResponse(false, message = errorResponse.message)
                        }
                    } catch (bodyException: Exception) {
                        val errorMsg = "Login failed with status ${e.response.status}"
                        allErrors.add("URL $index: $errorMsg")
                        Log.e(TAG, errorMsg)

                        // If this is the last URL, return a generic error
                        if (index == possibleUrls.size - 1) {
                            return AuthResponse(false, message = errorMsg)
                        }
                    }
                }

                // For connection-related errors, continue to next URL
                allErrors.add("URL $index: Connection failed - ${e.message}")
                continue
            } catch (e: ServerResponseException) {
                Log.e(TAG, "Server error with URL $url: ${e.response.status} - ${e.message}")

                try {
                    lastResponseBody = e.response.body<String>()
                    Log.e(TAG, "Server error response body: $lastResponseBody")
                } catch (bodyException: Exception) {
                    Log.e(TAG, "Could not read server error response body: ${bodyException.message}")
                }

                val errorMsg = "Server error (${e.response.status}): ${e.message}"
                allErrors.add("URL $index: $errorMsg")

                // For 5xx errors, try to get server error message but don't continue to other URLs
                try {
                    val errorResponse = e.response.body<ErrorResponse>()
                    return AuthResponse(false, message = "Server error: ${errorResponse.message}")
                } catch (bodyException: Exception) {
                    return AuthResponse(false, message = errorMsg)
                }
            } catch (e: RedirectResponseException) {
                Log.e(TAG, "Redirect error with URL $url: ${e.response.status} - ${e.message}")
                allErrors.add("URL $index: Unexpected redirect - ${e.message}")
                continue
            } catch (e: SerializationException) {
                Log.e(TAG, "Serialization error with URL $url: ${e.message}")
                allErrors.add("URL $index: Response format error - ${e.message}")
                continue
            } catch (e: Exception) {
                Log.e(TAG, "General error with URL $url: ${e.message}")
                allErrors.add("URL $index: ${e.message}")
                continue
            }
        }

        // All URLs failed - create detailed error message
        Log.e(TAG, "All URLs failed for signIn. Errors: $allErrors")
        val detailedError = if (allErrors.isNotEmpty()) {
            "Connection failed. Attempted servers:\n${allErrors.joinToString("\n")}"
        } else {
            "Could not connect to server. Please check your network connection."
        }

        return AuthResponse(false, message = detailedError)
    }

    suspend fun getCurrentUser(token: String): User {
        Log.d(TAG, "Getting current user with token: ${token.take(10)}...")

        // Try each URL until one works
        for ((index, url) in possibleUrls.withIndex()) {
            try {
                Log.d(TAG, "Trying URL $index: $url/users/me")

                return client.get("$url/users/me") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }.body()
            } catch (e: ClientRequestException) {
                Log.e(TAG, "Client error with URL $url: ${e.response.status} - ${e.message}")

                // Handle 401 unauthorized specifically
                if (e.response.status == HttpStatusCode.Unauthorized) {
                    throw Exception("Invalid or expired token")
                }

                continue
            } catch (e: ServerResponseException) {
                Log.e(TAG, "Server error with URL $url: ${e.response.status} - ${e.message}")
                throw Exception("Server error: ${e.response.status}")
            } catch (e: RedirectResponseException) {
                Log.e(TAG, "Redirect error with URL $url: ${e.response.status} - ${e.message}")
                continue
            } catch (e: SerializationException) {
                Log.e(TAG, "Serialization error with URL $url: ${e.message}")
                continue
            } catch (e: Exception) {
                Log.e(TAG, "General error with URL $url: ${e.message}")
                continue
            }
        }

        Log.e(TAG, "All URLs failed for getCurrentUser")
        throw Exception("Could not connect to server")
    }

    // Onboarding API calls
    suspend fun completeOnboardingStepOne(token: String, userType: UserType): Boolean {
        Log.d(TAG, "Completing onboarding step one: $userType")

        for ((index, url) in possibleUrls.withIndex()) {
            try {
                Log.d(TAG, "Trying URL $index: $url/onboarding/step-one")

                val response: OnboardingResponse = client.post("$url/onboarding/step-one") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                    setBody(OnboardingStepOneRequest(userType))
                }.body()

                Log.d(TAG, "Step one completed successfully")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error completing step one with URL $url: ${e.message}")
                continue
            }
        }

        Log.e(TAG, "All URLs failed for onboarding step one")
        return false
    }

    suspend fun completeOnboarding(
        token: String,
        userType: UserType,
        fullName: String,
        phone: String,
        organizationName: String? = null,
        institutionName: InstitutionName? = null,
    ): Boolean {
        Log.d(TAG, "Completing onboarding for user type: $userType")

        for ((index, url) in possibleUrls.withIndex()) {
            try {
                Log.d(TAG, "Trying URL $index: $url/onboarding/complete")

                val requestBody = OnboardingCompleteRequest(
                    user_type = userType,
                    full_name = fullName,
                    phone = phone,
                    organization_name = organizationName,
                    institution_name = institutionName,
                )

                val response: OnboardingResponse = client.post("$url/onboarding/complete") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                    setBody(requestBody)
                }.body()

                Log.d(TAG, "Onboarding completed successfully")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error completing onboarding with URL $url: ${e.message}")
                continue
            }
        }

        Log.e(TAG, "All URLs failed for completing onboarding")
        return false
    }

    suspend fun getInstitutions(): List<InstitutionOption> {
        Log.d(TAG, "Getting available institutions")

        for ((index, url) in possibleUrls.withIndex()) {
            try {
                Log.d(TAG, "Trying URL $index: $url/institutions")

                return client.get("$url/institutions").body()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting institutions with URL $url: ${e.message}")
                continue
            }
        }

        Log.e(TAG, "All URLs failed for getting institutions")
        return emptyList()
    }

    suspend fun uploadProfilePicture(token: String, imageFile: File): ProfilePictureUploadResponse? {
        Log.d(TAG, "Uploading profile picture: ${imageFile.name}")

        for ((index, url) in possibleUrls.withIndex()) {
            try {
                Log.d(TAG, "Trying URL $index: $url/upload-profile-picture")

                val response: ProfilePictureUploadResponse = client.post("$url/upload-profile-picture") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    "file",
                                    imageFile.readBytes(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "image/jpeg")
                                        append(HttpHeaders.ContentDisposition, "filename=\"${imageFile.name}\"")
                                    },
                                )
                            },
                        ),
                    )
                }.body()

                Log.d(TAG, "Profile picture uploaded successfully: ${response.profile_picture_url}")
                return response
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading profile picture with URL $url: ${e.message}")
                continue
            }
        }

        Log.e(TAG, "All URLs failed for uploading profile picture")
        return null
    }

    suspend fun completeVolunteerOnboarding(
        token: String,
        volunteerProfile: DomainVolunteerProfile,
        profilePictureUrl: String? = null,
    ): Boolean {
        Log.d(TAG, "Completing volunteer onboarding")

        for ((index, url) in possibleUrls.withIndex()) {
            try {
                Log.d(TAG, "Trying URL $index: $url/onboarding/volunteer-complete")

                val requestBody = OnboardingCompleteRequest(
                    user_type = UserType.VOLUNTEER,
                    full_name = volunteerProfile.fullName,
                    phone = volunteerProfile.phone,
                    sex = volunteerProfile.sex.toData(),
                    description = volunteerProfile.description,
                    skills = volunteerProfile.skills,
                    age = volunteerProfile.age,
                    emergency_contact_name = volunteerProfile.emergencyContactName,
                    emergency_contact_phone = volunteerProfile.emergencyContactPhone,
                    location_area = volunteerProfile.locationArea,
                    has_drivers_license = volunteerProfile.hasDriversLicense,
                    disabilities = volunteerProfile.disabilities,
                )

                val response: OnboardingResponse = client.post("$url/onboarding/volunteer-complete") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                    setBody(requestBody)
                }.body()

                Log.d(TAG, "Volunteer onboarding completed successfully")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error completing volunteer onboarding with URL $url: ${e.message}")
                continue
            }
        }

        Log.e(TAG, "All URLs failed for completing volunteer onboarding")
        return false
    }
}
