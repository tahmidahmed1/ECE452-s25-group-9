package com.example.gooddeedfeed.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.gooddeedfeed.DatabaseFactory
import com.example.gooddeedfeed.UserTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

interface AuthService {
    fun signUp(request: SignUpRequest): AuthResponse

    fun signIn(request: SignInRequest): AuthResponse

    fun getUserByUsername(username: String): UserPrincipal?
}

class DatabaseAuthService(private val jwtSecret: String) : AuthService {
    override fun signUp(request: SignUpRequest): AuthResponse =
        runBlocking {
            DatabaseFactory.dbQuery {
                if (UserTable.select { UserTable.username eq request.username }.count() > 0) {
                    return@dbQuery AuthResponse(false, message = "User already exists")
                }
                val hash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
                val userId = UserTable.insert {
                    it[username] = request.username
                    it[passwordHash] = hash
                } get UserTable.id
                val token = generateToken(request.username, userId)
                AuthResponse(true, token = token)
            }
        }

    override fun signIn(request: SignInRequest): AuthResponse =
        runBlocking {
            DatabaseFactory.dbQuery {
                val user = UserTable.select { UserTable.username eq request.username }
                    .firstOrNull()
                    ?: return@dbQuery AuthResponse(false, message = "User not found")
                val hash = user[UserTable.passwordHash]
                val id = user[UserTable.id]
                val result = BCrypt.verifyer().verify(request.password.toCharArray(), hash)
                if (result.verified) {
                    AuthResponse(true, token = generateToken(request.username, id))
                } else {
                    AuthResponse(false, message = "Invalid credentials")
                }
            }
        }

    override fun getUserByUsername(username: String): UserPrincipal? =
        runBlocking {
            DatabaseFactory.dbQuery {
                val user = UserTable.select { UserTable.username eq username }
                    .firstOrNull()
                    ?: return@dbQuery null
                UserPrincipal(user[UserTable.id], user[UserTable.username])
            }
        }

    private fun generateToken(
        username: String,
        id: Int,
    ): String {
        return JWT.create()
            .withClaim("username", username)
            .withClaim("id", id)
            .sign(Algorithm.HMAC256(jwtSecret))
    }
}
