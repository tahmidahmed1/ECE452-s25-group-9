package com.example.gooddeedfeed.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.gooddeedfeed.data.remote.NotificationApiService
import com.example.gooddeedfeed.data.repository.NotificationRepositoryImpl
import com.example.gooddeedfeed.domain.repository.NotificationRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl,
    ): NotificationRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseMessaging(): FirebaseMessaging {
            return FirebaseMessaging.getInstance()
        }

        @Provides
        @Singleton
        fun provideNotificationApiService(
            client: HttpClient,
            dataStore: DataStore<Preferences>,
        ): NotificationApiService {
            return NotificationApiService(client, dataStore)
        }
    }
}
