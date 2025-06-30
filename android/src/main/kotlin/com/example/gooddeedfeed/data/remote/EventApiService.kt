package com.example.gooddeedfeed.data.remote

import com.example.gooddeedfeed.data.remote.dto.EventDto
import com.example.gooddeedfeed.data.remote.dto.toDto
import com.example.gooddeedfeed.domain.model.CreateEventData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class EventApiService(private val client: HttpClient) {
    companion object {
        private val possibleUrls = listOf(
            "http://10.0.2.2:8000",  // Android emulator
            "http://127.0.0.1:8000", // local
        )
    }

    private fun base(): String = possibleUrls.first()

    /* ----------------- Public CRUD ----------------- */

    suspend fun getAllEvents(): List<EventDto> = client.get("${base()}/events").body()

    suspend fun getOrganizerEvents(organizerId: Int): List<EventDto> =
        client.get("${base()}/organizers/$organizerId/events").body()

    suspend fun getEvent(id: Int): EventDto = client.get("${base()}/events/$id").body()

    suspend fun createEvent(token: String, data: CreateEventData): EventDto =
        client.post("${base()}/events") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(data.toDto())
        }.body()

    suspend fun updateEvent(token: String, id: Int, data: CreateEventData): EventDto =
        client.patch("${base()}/events/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(data.toDto())
        }.body()

    suspend fun deleteEvent(token: String, id: Int) {
        client.delete("${base()}/events/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
} 
