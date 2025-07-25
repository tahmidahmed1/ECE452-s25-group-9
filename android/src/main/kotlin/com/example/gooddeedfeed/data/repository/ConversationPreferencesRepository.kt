package com.example.gooddeedfeed.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val STARRED_CONVERSATIONS_KEY = stringSetPreferencesKey("starred_conversations")
        private val DELETED_CONVERSATIONS_KEY = stringSetPreferencesKey("deleted_conversations")
    }

    private fun starredConversationsKey(userId: Int) = stringSetPreferencesKey("starred_conversations_$userId")
    private fun deletedConversationsKey(userId: Int) = stringSetPreferencesKey("deleted_conversations_$userId")

    suspend fun isConversationStarred(userId: Int, conversationId: String): Boolean {
        val key = starredConversationsKey(userId)
        return dataStore.data.first()[key]?.contains(conversationId) ?: false
    }

    suspend fun isConversationDeleted(userId: Int, conversationId: String): Boolean {
        val key = deletedConversationsKey(userId)
        return dataStore.data.first()[key]?.contains(conversationId) ?: false
    }

    suspend fun toggleConversationStar(userId: Int, conversationId: String): Boolean {
        val key = starredConversationsKey(userId)
        var isStarred = false
        
        dataStore.edit { preferences ->
            val starredSet = preferences[key]?.toMutableSet() ?: mutableSetOf()
            if (starredSet.contains(conversationId)) {
                starredSet.remove(conversationId)
                isStarred = false
            } else {
                starredSet.add(conversationId)
                isStarred = true
            }
            preferences[key] = starredSet
        }
        
        return isStarred
    }

    suspend fun deleteConversation(userId: Int, conversationId: String) {
        val key = deletedConversationsKey(userId)
        
        dataStore.edit { preferences ->
            val deletedSet = preferences[key]?.toMutableSet() ?: mutableSetOf()
            deletedSet.add(conversationId)
            preferences[key] = deletedSet
        }
    }

    suspend fun restoreConversation(userId: Int, conversationId: String) {
        val key = deletedConversationsKey(userId)
        
        dataStore.edit { preferences ->
            val deletedSet = preferences[key]?.toMutableSet() ?: mutableSetOf()
            deletedSet.remove(conversationId)
            preferences[key] = deletedSet
        }
    }

    fun getStarredConversations(userId: Int): Flow<Set<String>> {
        val key = starredConversationsKey(userId)
        return dataStore.data.map { preferences ->
            preferences[key] ?: emptySet()
        }
    }

    fun getDeletedConversations(userId: Int): Flow<Set<String>> {
        val key = deletedConversationsKey(userId)
        return dataStore.data.map { preferences ->
            preferences[key] ?: emptySet()
        }
    }
}