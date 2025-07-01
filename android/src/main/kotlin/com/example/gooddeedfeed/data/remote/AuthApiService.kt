package com.example.gooddeedfeed.data.remote

import com.example.gooddeedfeed.data.mapper.toData
import com.example.gooddeedfeed.data.remote.dto.AuthResponse
import com.example.gooddeedfeed.data.remote.dto.ErrorResponse
import com.example.gooddeedfeed.data.remote.dto.InstitutionName
import com.example.gooddeedfeed.data.remote.dto.InstitutionOption
import com.example.gooddeedfeed.data.remote.dto.OnboardingCompleteRequest
import com.example.gooddeedfeed.data.remote.dto.OnboardingResponse
import com.example.gooddeedfeed.data.remote.dto.OnboardingStepOneRequest
import com.example.gooddeedfeed.data.remote.dto.ProfilePictureUploadResponse
import com.example.gooddeedfeed.data.remote.dto.SignUpRequest
import com.example.gooddeedfeed.data.remote.dto.TokenResponse
import com.example.gooddeedfeed.data.remote.dto.User
import com.example.gooddeedfeed.data.remote.dto.UserType
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
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
        val allErrors = mutableListOf<String>()
        var lastResponseBody: String? = null

        // Try each URL until one works
        for ((index, url) in possibleUrls.withIndex()) {
            try {
                val requestBody = SignUpRequest(username, email, password)

                val httpResponse = client.post("$url/register") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                    setBody(requestBody)
                }

                // Success case (created or ok) – parse token
                if (httpResponse.status == HttpStatusCode.Created || httpResponse.status == HttpStatusCode.OK) {
                    val tokenResp: TokenResponse = httpResponse.body()
                    return AuthResponse(success = true, token = tokenResp.access_token, message = "Registration successful")
                }

                // Failure case – try to parse structured error first
                try {
                    val error = httpResponse.body<ErrorResponse>()
                    return AuthResponse(false, message = error.message)
                } catch (ex: Exception) {
                    // Fallback to raw text
                    val rawError = httpResponse.bodyAsText()
                    return AuthResponse(false, message = rawError)
                }
            } catch (e: ClientRequestException) {
                // Try to get detailed error response
                try {
                    lastResponseBody = e.response.body<String>()
                } catch (bodyException: Exception) {
                }

                // Handle 422 validation errors specifically
                if (e.response.status == HttpStatusCode.UnprocessableEntity) {
                    try {
                        // Try to parse as ErrorResponse first
                        val errorResponse = e.response.body<ErrorResponse>()
                        if (!errorResponse.errors.isNullOrEmpty()) {
                            // Extract the first detailed validation message
                            val firstError = errorResponse.errors.first()
                            val detailedMessage = firstError.msg
                            return AuthResponse(false, message = detailedMessage)
                        }
                        return AuthResponse(false, message = errorResponse.message)
                    } catch (bodyException: Exception) {
                        // Try to get raw response text to extract the actual error
                        try {
                            val rawBody = lastResponseBody ?: "Unknown validation error"

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
                        return AuthResponse(false, message = errorResponse.message)
                    } catch (bodyException: Exception) {
                        return AuthResponse(false, message = "User with this username or email already exists.")
                    }
                }

                // For other 4xx errors, try to extract meaningful error message
                if (e.response.status.value in 400..499) {
                    try {
                        val errorResponse = e.response.body<ErrorResponse>()
                        allErrors.add("URL $index (${e.response.status}): ${errorResponse.message}")

                        // If this is the last URL, return the error
                        if (index == possibleUrls.size - 1) {
                            return AuthResponse(false, message = errorResponse.message)
                        }
                    } catch (bodyException: Exception) {
                        val errorMsg = "Registration failed with status ${e.response.status}"
                        allErrors.add("URL $index: $errorMsg")

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
                try {
                    lastResponseBody = e.response.body<String>()
                } catch (bodyException: Exception) {
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
                allErrors.add("URL $index: Unexpected redirect - ${e.message}")
                continue
            } catch (e: SerializationException) {
                allErrors.add("URL $index: Response format error - ${e.message}")
                continue
            } catch (e: Exception) {
                allErrors.add("URL $index: ${e.message}")
                continue
            }
        }

        // All URLs failed - create detailed error message
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
        val allErrors = mutableListOf<String>()
        var lastResponseBody: String? = null

        // Try each URL until one works
        for ((index, url) in possibleUrls.withIndex()) {
            try {
                val response: TokenResponse = client.post("$url/token") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    }
                    setBody("username=$username&password=$password")
                }.body()

                return AuthResponse(success = true, token = response.access_token, message = "Login successful")
            } catch (e: ClientRequestException) {
                // Try to get detailed error response
                try {
                    lastResponseBody = e.response.body<String>()
                } catch (bodyException: Exception) {
                }

                // Handle 401 unauthorized specifically
                if (e.response.status == HttpStatusCode.Unauthorized) {
                    try {
                        val errorResponse = e.response.body<ErrorResponse>()
                        return AuthResponse(false, message = errorResponse.message)
                    } catch (bodyException: Exception) {
                        return AuthResponse(false, message = "Invalid username or password")
                    }
                }

                // Handle 422 validation errors
                if (e.response.status == HttpStatusCode.UnprocessableEntity) {
                    try {
                        val errorResponse = e.response.body<ErrorResponse>()
                        if (!errorResponse.errors.isNullOrEmpty()) {
                            // Extract the first detailed validation message
                            val firstError = errorResponse.errors.first()
                            val detailedMessage = firstError.msg
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
                        allErrors.add("URL $index (${e.response.status}): ${errorResponse.message}")

                        // If this is the last URL, return the error
                        if (index == possibleUrls.size - 1) {
                            return AuthResponse(false, message = errorResponse.message)
                        }
                    } catch (bodyException: Exception) {
                        val errorMsg = "Login failed with status ${e.response.status}"
                        allErrors.add("URL $index: $errorMsg")

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
                try {
                    lastResponseBody = e.response.body<String>()
                } catch (bodyException: Exception) {
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
                allErrors.add("URL $index: Unexpected redirect - ${e.message}")
                continue
            } catch (e: SerializationException) {
                allErrors.add("URL $index: Response format error - ${e.message}")
                continue
            } catch (e: Exception) {
                allErrors.add("URL $index: ${e.message}")
                continue
            }
        }

        // All URLs failed - create detailed error message
        val detailedError = if (allErrors.isNotEmpty()) {
            "Connection failed. Attempted servers:\n${allErrors.joinToString("\n")}"
        } else {
            "Could not connect to server. Please check your network connection."
        }

        return AuthResponse(false, message = detailedError)
    }

    suspend fun getCurrentUser(token: String): User {
        // Try each URL until one works
        for ((index, url) in possibleUrls.withIndex()) {
            try {
                return client.get("$url/users/me") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }.body()
            } catch (e: ClientRequestException) {
                // Handle 401 unauthorized specifically
                if (e.response.status == HttpStatusCode.Unauthorized) {
                    throw Exception("Invalid or expired token")
                }

                continue
            } catch (e: ServerResponseException) {
                throw Exception("Server error: ${e.response.status}")
            } catch (e: RedirectResponseException) {
                continue
            } catch (e: SerializationException) {
                continue
            } catch (e: Exception) {
                continue
            }
        }

        throw Exception("Could not connect to server")
    }

    // Onboarding API calls
    suspend fun completeOnboardingStepOne(token: String, userType: UserType): Boolean {
        for ((index, url) in possibleUrls.withIndex()) {
            try {
                val response: OnboardingResponse = client.post("$url/onboarding/step-one") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                    setBody(OnboardingStepOneRequest(userType))
                }.body()

                return true
            } catch (e: Exception) {
                continue
            }
        }

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
        for ((index, url) in possibleUrls.withIndex()) {
            try {
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

                return true
            } catch (e: Exception) {
                continue
            }
        }

        return false
    }

    suspend fun getInstitutions(): List<InstitutionOption> {
        for ((index, url) in possibleUrls.withIndex()) {
            try {
                return client.get("$url/institutions").body()
            } catch (e: Exception) {
                continue
            }
        }

        return emptyList()
    }

    suspend fun uploadProfilePicture(token: String, imageFile: File): ProfilePictureUploadResponse? {
        for ((index, url) in possibleUrls.withIndex()) {
            try {
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

                return response
            } catch (e: Exception) {
                continue
            }
        }

        return null
    }

    suspend fun completeVolunteerOnboarding(
        token: String,
        volunteerProfile: DomainVolunteerProfile,
        profilePictureUrl: String? = null,
    ): Boolean {
        for ((index, url) in possibleUrls.withIndex()) {
            try {
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

                return true
            } catch (e: Exception) {
                continue
            }
        }

        return false
    }

    // ------------------ Profile Update ------------------

    suspend fun updateUserProfile(token: String, update: DomainUserUpdate): Boolean {
        for ((index, url) in possibleUrls.withIndex()) {
            try {
                // Prepare payload with snake_case keys expected by backend
                val payload = buildJsonObject {
                    update.fullName?.let { put("full_name", it) }
                    update.phone?.let { put("phone", it) }
                    update.organizationName?.let { put("organization_name", it) }
                    update.institutionName?.let { put("institution_name", it.name) }

                    update.sex?.let { put("sex", it.name.lowercase()) }
                    update.description?.let { put("description", it) }
                    update.skills?.let { put("skills", Json.encodeToJsonElement(it)) }
                    update.age?.let { put("age", it) }
                    update.emergencyContactName?.let { put("emergency_contact_name", it) }
                    update.emergencyContactPhone?.let { put("emergency_contact_phone", it) }
                    update.locationArea?.let { put("location_area", it) }
                    update.hasDriversLicense?.let { put("has_drivers_license", it) }
                    update.disabilities?.let { put("disabilities", it) }
                }

                client.put("$url/users/me") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                    setBody(payload)
                }.body<User>()

                return true
            } catch (e: Exception) {
                continue
            }
        }

        return false
    }
}
