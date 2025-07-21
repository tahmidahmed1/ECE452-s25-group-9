package com.example.gooddeedfeed.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.gooddeedfeed.data.remote.BadgeApiService
import com.example.gooddeedfeed.data.repository.BadgeRepositoryImpl
import com.example.gooddeedfeed.domain.repository.BadgeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BadgeModule {

    @Binds
    @Singleton
    abstract fun bindBadgeRepository(
        badgeRepositoryImpl: BadgeRepositoryImpl,
    ): BadgeRepository

    companion object {
        @Provides
        @Singleton
        fun provideBadgeApiService(
            httpClient: HttpClient,
            dataStore: DataStore<Preferences>
        ): BadgeApiService {
            return BadgeApiService(httpClient, dataStore)
        }
    }
} 
