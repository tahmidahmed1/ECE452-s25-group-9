package com.example.gooddeedfeed.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        private val LOCATION_ENABLED_KEY = booleanPreferencesKey("location_enabled")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val SHARE_PROFILE_PICTURE_KEY = booleanPreferencesKey("share_profile_picture")
    }

    val isLocationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[LOCATION_ENABLED_KEY] ?: true // Default to true
    }

    val isNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true // Default to true
    }

    val isShareProfilePictureEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SHARE_PROFILE_PICTURE_KEY] ?: true // Default to true
    }

    suspend fun setLocationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOCATION_ENABLED_KEY] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setShareProfilePictureEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHARE_PROFILE_PICTURE_KEY] = enabled
        }
    }

    suspend fun updateAllSettings(
        locationEnabled: Boolean,
        notificationsEnabled: Boolean,
        shareProfilePictureEnabled: Boolean,
    ) {
        dataStore.edit { preferences ->
            preferences[LOCATION_ENABLED_KEY] = locationEnabled
            preferences[NOTIFICATIONS_ENABLED_KEY] = notificationsEnabled
            preferences[SHARE_PROFILE_PICTURE_KEY] = shareProfilePictureEnabled
        }
    }
} 
