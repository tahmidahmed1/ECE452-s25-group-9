package com.example.gooddeedfeed

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.gooddeedfeed.auth.DatabaseAuthService
import com.example.gooddeedfeed.auth.authRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.routing.routing

fun Application.configureAuth() {
    if (pluginOrNull(Authentication) != null) return

    val jwtSecret = System.getProperty("ktor.jwt.secret")
        ?: throw IllegalStateException(
            "JWT secret not found as system property 'ktor.jwt.secret'. Ensure it's set via -Dktor.jwt.secret.",
        )
    val authService = DatabaseAuthService(jwtSecret)

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JWT.require(Algorithm.HMAC256(jwtSecret)).build())
            validate { cred ->
                val username = cred.payload.getClaim("username").asString()
                val id = cred.payload.getClaim("id").asInt()
                if (username != null && id != null) {
                    authService.getUserByUsername(username)
                } else {
                    null
                }
            }
        }
    }

    routing {
        authRoutes(authService)
    }
}
