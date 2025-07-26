package com.example.gooddeedfeed.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
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

    /**
     * Determine whether the given [conversationId] should currently be considered deleted.
     * We store the lastMessage snapshot at the moment when the user deleted the conversation.
     * If the *current* snapshot differs (meaning a new message arrived) we automatically
     * restore the conversation so that it shows up again.
     */
    suspend fun isConversationDeleted(
        userId: Int,
        conversationId: String,
        currentSnapshot: String,
    ): Boolean {
        val key = deletedConversationsKey(userId)
        var deleted = false

        dataStore.edit { preferences ->
            val deletedSet = preferences[key]?.toMutableSet() ?: mutableSetOf()

            // Find any entry that matches this conversation (regardless of snapshot)
            val existingEntry = deletedSet.firstOrNull { it.startsWith("$conversationId::") }

            if (existingEntry != null) {
                val storedSnapshot = existingEntry.substringAfter("::")

                if (storedSnapshot == currentSnapshot) {
                    // Still matches → keep hidden
                    deleted = true
                } else {
                    // Snapshot changed → new message arrived → remove the deletion entry
                    deletedSet.remove(existingEntry)
                    preferences[key] = deletedSet
                    deleted = false
                }
            }
        }

        return deleted
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

    /** Store a deletion record as "conversationId::lastMessageSnapshot" */
    suspend fun deleteConversation(
        userId: Int,
        conversationId: String,
        lastMessageSnapshot: String,
    ) {
        val key = deletedConversationsKey(userId)

        dataStore.edit { preferences ->
            val deletedSet = preferences[key]?.toMutableSet() ?: mutableSetOf()

            // Remove any previous entry irrespective of snapshot then add new one
            deletedSet.removeIf { it.startsWith("$conversationId::") }
            deletedSet.add("$conversationId::$lastMessageSnapshot")

            preferences[key] = deletedSet
        }
    }

    suspend fun restoreConversation(userId: Int, conversationId: String) {
        val key = deletedConversationsKey(userId)

        dataStore.edit { preferences ->
            val deletedSet = preferences[key]?.toMutableSet() ?: mutableSetOf()
            deletedSet.removeIf { it.startsWith("$conversationId::") }
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
