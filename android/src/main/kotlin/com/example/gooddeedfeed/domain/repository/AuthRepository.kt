package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.data.remote.AuthResponse
import com.example.gooddeedfeed.data.remote.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signUp(
        username: String,
        password: String,
    ): AuthResponse

    suspend fun signIn(
        username: String,
        password: String,
    ): AuthResponse

    suspend fun signOut()

    fun getToken(): Flow<String?>

    suspend fun getCurrentUser(): User?
}
