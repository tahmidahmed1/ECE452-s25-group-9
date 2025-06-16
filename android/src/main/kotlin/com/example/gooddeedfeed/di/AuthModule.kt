package com.example.gooddeedfeed.di

import android.content.Context
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
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    @Provides
    @Singleton
    fun provideAuthApiService(client: HttpClient): AuthApiService = AuthApiService(client)

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        api: AuthApiService,
    ): AuthRepository = AuthRepositoryImpl(context, api)

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
