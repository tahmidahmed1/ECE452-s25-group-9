package com.example.gooddeedfeed.di

import com.example.gooddeedfeed.data.remote.ChatApiService
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
    fun provideChatApiService(client: HttpClient): ChatApiService = ChatApiService(client)
} 