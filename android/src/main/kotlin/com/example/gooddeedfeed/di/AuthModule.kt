package com.example.gooddeedfeed.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.remote.AuthApiService
import com.example.gooddeedfeed.data.repository.AuthRepositoryImpl
import com.example.gooddeedfeed.data.services.LocationService
import com.example.gooddeedfeed.domain.repository.AuthRepository
import com.example.gooddeedfeed.domain.usecase.GetCurrentUserUseCase
import com.example.gooddeedfeed.domain.usecase.SignInUseCase
import com.example.gooddeedfeed.domain.usecase.SignOutUseCase
import com.example.gooddeedfeed.domain.usecase.SignUpUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    private const val TAG = "AuthModule"
    private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")

    @Provides
    @Singleton
    fun provideHttpClient(dataStore: DataStore<Preferences>): HttpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d(TAG, "🌐 HTTP: $message")
                    }
                }
                level = LogLevel.INFO
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        Log.d(TAG, "🔐 Auth interceptor: Loading token for authentication...")
                        val token = runBlocking {
                            try {
                                val tokenValue = dataStore.data.first()[JWT_TOKEN_KEY]
                                Log.d(TAG, "🔍 Auth interceptor: Token from DataStore: ${if (tokenValue != null) "Found (${tokenValue.take(20)}...)" else "Not found"}")
                                tokenValue
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Auth interceptor: Failed to load token from DataStore", e)
                                null
                            }
                        }

                        if (token != null) {
                            Log.d(TAG, "✅ Auth interceptor: Token loaded successfully, will add to request")
                            BearerTokens(token, "") // Empty string for refresh token since server doesn't support it
                        } else {
                            Log.d(TAG, "ℹ️ Auth interceptor: No token found - requests will not be authenticated")
                            null
                        }
                    }

                    // No refreshTokens block since server doesn't support token refresh
                    // If token expires, user will need to sign in again
                }
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30000 // 30 seconds
                connectTimeoutMillis = 30000 // 30 seconds
                socketTimeoutMillis = 30000 // 30 seconds
            }
        }

    @Provides
    @Singleton
    fun provideAuthApiService(client: HttpClient, dataStore: DataStore<Preferences>): AuthApiService = AuthApiService(client, dataStore)

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: AuthApiService,
        dataStore: DataStore<Preferences>,
    ): AuthRepository = AuthRepositoryImpl(api, dataStore)

    @Provides
    fun provideSignUpUseCase(repo: AuthRepository) = SignUpUseCase(repo)

    @Provides
    fun provideSignInUseCase(repo: AuthRepository) = SignInUseCase(repo)

    @Provides
    fun provideSignOutUseCase(repo: AuthRepository) = SignOutUseCase(repo)

    @Provides
    fun provideGetCurrentUserUseCase(repo: AuthRepository) = GetCurrentUserUseCase(repo)

    @Provides
    @Singleton
    fun provideLocationService(@ApplicationContext context: Context): LocationService = LocationService(context)
}
