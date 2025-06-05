package com.example.gooddeedfeed.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gooddeedfeed.data.remote.AuthApiService
import com.example.gooddeedfeed.data.remote.AuthResponse
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")
private val TOKEN_KEY = stringPreferencesKey("jwt_token")

class AuthRepositoryImpl(
    private val context: Context,
    private val api: AuthApiService,
) : AuthRepository {
    override suspend fun signUp(
        username: String,
        password: String,
    ): AuthResponse {
        val resp = api.signUp(username, password)
        resp.token?.let { token -> saveToken(token) }
        return resp
    }

    override suspend fun signIn(
        username: String,
        password: String,
    ): AuthResponse {
        val resp = api.signIn(username, password)
        resp.token?.let { token -> saveToken(token) }
        return resp
    }

    override suspend fun signOut() {
        saveToken("")
    }

    override fun getToken(): Flow<String?> = context.dataStore.data.map { preferences -> preferences[TOKEN_KEY] }

    override suspend fun getCurrentUser(): User? {
        val token = context.dataStore.data.map { preferences -> preferences[TOKEN_KEY] }.firstOrNull()
        return if (token.isNullOrEmpty()) {
            null
        } else {
            try {
                api.getCurrentUser(token)
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences -> preferences[TOKEN_KEY] = token }
    }
}
