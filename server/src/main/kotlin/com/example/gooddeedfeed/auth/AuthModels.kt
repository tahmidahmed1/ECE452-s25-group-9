package com.example.gooddeedfeed.auth

import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(val username: String, val password: String)

@Serializable
data class SignInRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val success: Boolean, val token: String? = null, val message: String? = null)

@Serializable
data class UserPrincipal(val id: Int, val username: String) : io.ktor.server.auth.Principal
