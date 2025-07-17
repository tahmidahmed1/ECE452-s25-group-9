package com.example.gooddeedfeed.di

import com.example.gooddeedfeed.data.remote.LeaderboardApiService
import com.example.gooddeedfeed.data.repository.LeaderboardRepositoryImpl
import com.example.gooddeedfeed.domain.repository.LeaderboardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LeaderboardModule {

    @Provides
    @Singleton
    fun provideLeaderboardApiService(httpClient: HttpClient): LeaderboardApiService {
        return LeaderboardApiService(httpClient)
    }

    @Provides
    @Singleton
    fun provideLeaderboardRepository(
        leaderboardApiService: LeaderboardApiService,
    ): LeaderboardRepository {
        return LeaderboardRepositoryImpl(leaderboardApiService)
    }
} 
