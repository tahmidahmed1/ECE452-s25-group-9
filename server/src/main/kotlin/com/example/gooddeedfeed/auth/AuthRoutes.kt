package com.example.gooddeedfeed.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/signup") {
            val req = call.receive<SignUpRequest>()
            val result = authService.signUp(req)
            if (result.success) {
                call.respond(HttpStatusCode.Created, result)
            } else {
                call.respond(HttpStatusCode.BadRequest, result)
            }
        }
        post("/signin") {
            val req = call.receive<SignInRequest>()
            val result = authService.signIn(req)
            if (result.success) {
                call.respond(HttpStatusCode.OK, result)
            } else {
                call.respond(HttpStatusCode.Unauthorized, result)
            }
        }
        authenticate("auth-jwt") {
            get("/me") {
                val user = call.principal<UserPrincipal>()
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}
