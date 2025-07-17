package com.example.gooddeedfeed.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.gooddeedfeed.data.remote.SubscriptionApiService
import com.example.gooddeedfeed.data.repository.SubscriptionRepository
import com.example.gooddeedfeed.data.repository.SubscriptionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SubscriptionModule {

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    companion object {
        @Provides
        @Singleton
        fun provideSubscriptionApiService(
            client: HttpClient,
            dataStore: DataStore<Preferences>
        ): SubscriptionApiService = SubscriptionApiService(client, dataStore)
    }
} 