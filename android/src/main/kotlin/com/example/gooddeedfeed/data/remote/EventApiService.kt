package com.example.gooddeedfeed.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

@Serializable
data class EventResponse(
    val id: Int,
    val title: String,
    val description: String,
    val organization_id: Int,
    val organization_name: String,
    val location: String,
    val date: String,
    val start_time: String,
    val end_time: String,
    val max_volunteers: Int,
    val current_volunteers: Int,
    val category: String,
    val requirements: List<String> = emptyList(),
    val status: String,
    val created_at: String,
    val updated_at: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

class EventApiService(private val client: HttpClient) {
    companion object {
        private val possibleUrls = listOf(
            "http://10.0.2.2:9000/api",
            "http://localhost:9000/api",
            "http://127.0.0.1:9000/api",
        )
        private val baseUrl = possibleUrls.first()
    }

    suspend fun getEvents(): List<EventResponse> {
        for (url in possibleUrls) {
            try {
                return client.get("$url/events") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                }.body()
            } catch (e: Exception) {
                // Try next URL
            }
        }
        throw Exception("Failed to fetch events from any URL")
    }
} 
