package com.example.gooddeedfeed.data.remote

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.remote.dto.CreateLostFoundItemDto
import com.example.gooddeedfeed.data.remote.dto.LostFoundImageUploadResponseDto
import com.example.gooddeedfeed.data.remote.dto.LostFoundItemDto
import com.example.gooddeedfeed.data.remote.dto.LostFoundItemsResponseDto
import com.example.gooddeedfeed.data.remote.dto.UpdateLostFoundItemDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

class LostFoundApiService @Inject constructor(
    client: HttpClient,
    private val dataStore: DataStore<Preferences>,
) : BaseApiService(client) {

    companion object {
        private const val TAG = "LostFoundApiService"
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
    }

    private suspend fun getSessionIdFromDataStore(): String? {
        return try {
            val sessionId = dataStore.data.first()[SESSION_ID_KEY]
            Log.d(TAG, "🔍 Session ID from DataStore: ${if (sessionId != null) "Found" else "Not found"}")
            sessionId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get session ID from DataStore", e)
            null
        }
    }

    private fun buildUrl(path: String): String = "${possibleUrls.first()}/$path"

    suspend fun getLostFoundItems(
        itemType: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): LostFoundItemsResponseDto {
        Log.d(TAG, "🔄 Getting lost and found items")
        Log.d(TAG, "📝 Parameters - itemType: $itemType, limit: $limit, offset: $offset")
        
        val url = buildUrl("lost-found")
        Log.d(TAG, "🌐 Request URL: $url")
        
        return try {
            val response = client.get(url) {
                parameter("item_type", itemType)
                parameter("limit", limit)
                parameter("offset", offset)
                getSessionIdFromDataStore()?.let { sessionId ->
                    header("Authorization", "Bearer $sessionId")
                    Log.d(TAG, "🔐 Added Authorization header with session")
                }
            }.body<LostFoundItemsResponseDto>()
            
            Log.d(TAG, "✅ Successfully retrieved ${response.items.size} items, total: ${response.totalCount}")
            response
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get lost and found items", e)
            throw e
        }
    }

    suspend fun getLostFoundItem(itemId: Int): LostFoundItemDto {
        return client.get(buildUrl("lost-found/$itemId")) {
            getSessionIdFromDataStore()?.let { sessionId ->
                header("Authorization", "Bearer $sessionId")
            }
        }.body()
    }

    suspend fun createLostFoundItem(item: CreateLostFoundItemDto): LostFoundItemDto {
        Log.d(TAG, "🚀 Creating lost and found item")
        Log.d(TAG, "📝 Item data - title: ${item.title}, type: ${item.itemType}, location: ${item.location}")
        Log.d(TAG, "📝 Item data - description: ${item.description}, reward: ${item.reward}, expiryDays: ${item.expiryDays}")
        Log.d(TAG, "📝 Item data - tags: ${item.tags}")
        
        val url = buildUrl("lost-found")
        Log.d(TAG, "🌐 POST URL: $url")
        
        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(item)
                getSessionIdFromDataStore()?.let { sessionId ->
                    header("Authorization", "Bearer $sessionId")
                    Log.d(TAG, "🔐 Added Authorization header with session")
                } ?: Log.w(TAG, "⚠️ No session ID found - request might fail")
            }.body<LostFoundItemDto>()
            
            Log.d(TAG, "✅ Successfully created lost and found item with ID: ${response.id}")
            Log.d(TAG, "📋 Created item - title: ${response.title}, isActive: ${response.isActive}")
            response
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create lost and found item", e)
            Log.e(TAG, "🔍 Exception details: ${e.message}")
            throw e
        }
    }

    suspend fun updateLostFoundItem(itemId: Int, item: UpdateLostFoundItemDto): LostFoundItemDto {
        return client.put(buildUrl("lost-found/$itemId")) {
            contentType(ContentType.Application.Json)
            setBody(item)
            getSessionIdFromDataStore()?.let { sessionId ->
                header("Authorization", "Bearer $sessionId")
            }
        }.body()
    }

    suspend fun deleteLostFoundItem(itemId: Int): Map<String, String> {
        return client.delete(buildUrl("lost-found/$itemId")) {
            getSessionIdFromDataStore()?.let { sessionId ->
                header("Authorization", "Bearer $sessionId")
            }
        }.body()
    }

    suspend fun uploadLostFoundImage(itemId: Int, imageFile: File): LostFoundImageUploadResponseDto {
        return client.post(buildUrl("lost-found/$itemId/images")) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", imageFile.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "image/*")
                            append(HttpHeaders.ContentDisposition, "filename=\"${imageFile.name}\"")
                        })
                    }
                )
            )
            getSessionIdFromDataStore()?.let { sessionId ->
                header("Authorization", "Bearer $sessionId")
            }
        }.body()
    }

    suspend fun deleteLostFoundImage(itemId: Int, imageId: Int): Map<String, String> {
        return client.delete(buildUrl("lost-found/$itemId/images/$imageId")) {
            getSessionIdFromDataStore()?.let { sessionId ->
                header("Authorization", "Bearer $sessionId")
            }
        }.body()
    }
}