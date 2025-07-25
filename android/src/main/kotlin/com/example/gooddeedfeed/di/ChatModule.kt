package com.example.gooddeedfeed.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.gooddeedfeed.data.remote.ChatApiService
import com.example.gooddeedfeed.data.repository.ConversationPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideChatApiService(
        client: HttpClient,
        dataStore: DataStore<Preferences>
    ): ChatApiService = ChatApiService(client, dataStore)

    @Provides
    @Singleton
    fun provideConversationPreferencesRepository(
        dataStore: DataStore<Preferences>
    ): ConversationPreferencesRepository = ConversationPreferencesRepository(dataStore)
} 
