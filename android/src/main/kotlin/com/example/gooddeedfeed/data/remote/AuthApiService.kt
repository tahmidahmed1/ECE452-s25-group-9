package com.example.gooddeedfeed.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(val username: String, val password: String)

@Serializable
data class SignInRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val success: Boolean, val token: String? = null, val message: String? = null)

@Serializable
data class User(val id: Int, val username: String)

class AuthApiService(private val client: HttpClient) {
    suspend fun signUp(
        username: String,
        password: String,
    ): AuthResponse {
        return client.post("http://10.0.2.2:8080/auth/signup") {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            setBody(SignUpRequest(username, password))
        }.body()
    }

    suspend fun signIn(
        username: String,
        password: String,
    ): AuthResponse {
        return client.post("http://10.0.2.2:8080/auth/signin") {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            setBody(SignInRequest(username, password))
        }.body()
    }

    suspend fun getCurrentUser(token: String): User {
        return client.get("http://10.0.2.2:8080/auth/me") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.body()
    }
}
