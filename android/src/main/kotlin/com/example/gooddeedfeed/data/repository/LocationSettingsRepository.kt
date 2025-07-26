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
        private val NOTIFICATION_PROMPT_SHOWN_KEY = booleanPreferencesKey("notification_prompt_shown")
    }

    val isLocationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[LOCATION_ENABLED_KEY] ?: true // Default to true
    }

    val isNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true // Default to true
    }

    val hasNotificationPromptBeenShown: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATION_PROMPT_SHOWN_KEY] ?: false // Default to false
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

    suspend fun setNotificationPromptShown(shown: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_PROMPT_SHOWN_KEY] = shown
        }
    }

    suspend fun updateAllSettings(
        locationEnabled: Boolean,
        notificationsEnabled: Boolean,
    ) {
        dataStore.edit { preferences ->
            preferences[LOCATION_ENABLED_KEY] = locationEnabled
            preferences[NOTIFICATIONS_ENABLED_KEY] = notificationsEnabled
        }
    }
} 
