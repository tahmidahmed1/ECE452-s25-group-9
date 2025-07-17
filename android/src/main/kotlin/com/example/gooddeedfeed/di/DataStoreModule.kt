package com.example.gooddeedfeed.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.gooddeedfeed.data.repository.LocationSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Extension property to create the DataStore instance lazily on the Context
private val Context.authDataStore by preferencesDataStore(name = "auth")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.authDataStore

    @Provides
    @Singleton
    fun provideLocationSettingsRepository(dataStore: DataStore<Preferences>): LocationSettingsRepository =
        LocationSettingsRepository(dataStore)
} 