package com.example.gooddeedfeed.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.gooddeedfeed.data.remote.LostFoundApiService
import com.example.gooddeedfeed.data.repository.LostFoundRepositoryImpl
import com.example.gooddeedfeed.domain.repository.LostFoundRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LostFoundModule {

    @Provides
    @Singleton
    fun provideLostFoundApiService(
        httpClient: HttpClient,
        dataStore: DataStore<Preferences>,
    ): LostFoundApiService {
        return LostFoundApiService(httpClient, dataStore)
    }

    @Provides
    @Singleton
    fun provideLostFoundRepository(
        apiService: LostFoundApiService,
    ): LostFoundRepository {
        return LostFoundRepositoryImpl(apiService)
    }
}
