package com.example.gooddeedfeed.data.remote

import android.util.Log
import com.example.gooddeedfeed.data.remote.dto.EventDto
import com.example.gooddeedfeed.data.remote.dto.EventImageDto
import com.example.gooddeedfeed.data.remote.dto.toDto
import com.example.gooddeedfeed.domain.model.CreateEventData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class EventApiService(client: HttpClient) : BaseApiService(client) {

    private fun buildUrl(path: String): String = "${possibleUrls.first()}/$path"

    /* ----------------- Public CRUD ----------------- */

    suspend fun getAllEvents(
        lat: Double? = null,
        lon: Double? = null,
        radiusKm: Float = 50f,
        category: String? = null,
        onlyAvailable: Boolean = false,
        almostFull: Boolean = false,
        minKarmaPoints: Int = 1,
        maxKarmaPoints: Int = 200,
        dateFilter: String? = null,
    ): List<EventDto> {
        return client.get(buildUrl("events")) {
            url {
                if (lat != null && lon != null) {
                    parameters.append("lat", lat.toString())
                    parameters.append("lon", lon.toString())
                    parameters.append("radius_km", radiusKm.toString())
                }

                category?.let { parameters.append("category", it) }
                if (onlyAvailable) parameters.append("only_available", "true")
                if (almostFull) parameters.append("almost_full", "true")
                parameters.append("min_karma_points", minKarmaPoints.toString())
                parameters.append("max_karma_points", maxKarmaPoints.toString())
                dateFilter?.let { parameters.append("date_filter", it) }
            }
        }.body()
    }

    suspend fun getOrganizerEvents(organizerId: Int): List<EventDto> {
        val events: List<EventDto> = client.get(buildUrl("organizers/$organizerId/events")).body()
        Log.d("EventAPI", "Fetched ${events.size} events for organizer $organizerId")
        events.forEachIndexed { index, event ->
            Log.d("EventAPI", "Event $index: id=${event.id}, title=${event.title}, images=${event.images.size}")
            event.images.forEachIndexed { imgIndex, image ->
                Log.d("EventAPI", "  Image $imgIndex: id=${image.id}, url=${image.image_url}, isMain=${image.is_main}")
            }
        }
        return events
    }

    suspend fun getEvent(id: Int): EventDto {
        val event: EventDto = client.get(buildUrl("events/$id")).body()
        Log.d("EventAPI", "Fetched event: id=${event.id}, title=${event.title}, images=${event.images.size}")
        event.images.forEachIndexed { index, image ->
            Log.d("EventAPI", "  Image $index: id=${image.id}, url=${image.image_url}, isMain=${image.is_main}")
        }
        return event
    }

    suspend fun createEvent(token: String, data: CreateEventData): EventDto {
        return try {
            val response = client.post(buildUrl("events")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(data.toDto())
            }

            if (response.status.value in 200..299) {
                response.body<EventDto>()
            } else {
                throw Exception("Server returned ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            throw Exception("Failed to create event: ${e.message}")
        }
    }

    suspend fun updateEvent(token: String, id: Int, data: CreateEventData): EventDto {
        return try {
            val response = client.patch(buildUrl("events/$id")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(data.toDto())
            }

            if (response.status.value in 200..299) {
                response.body<EventDto>()
            } else {
                throw Exception("Server returned ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            throw Exception("Failed to update event: ${e.message}")
        }
    }

    suspend fun deleteEvent(token: String, id: Int) {
        client.delete(buildUrl("events/$id")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun uploadEventImage(token: String, eventId: Int, file: java.io.File) {
        client.submitFormWithBinaryData(
            url = buildUrl("events/$eventId/upload-image"),
            formData = formData {
                append(
                    "file",
                    file.readBytes(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                    },
                )
            },
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun uploadEventImageToCarousel(token: String, eventId: Int, file: java.io.File, isMain: Boolean) {
        client.submitFormWithBinaryData(
            url = buildUrl("events/$eventId/images/upload"),
            formData = formData {
                append(
                    "file",
                    file.readBytes(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                    },
                )
                append("is_main", isMain.toString())
            },
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun getEventImages(eventId: Int): List<EventImageDto> = 
        client.get(buildUrl("events/$eventId/images")).body()
} 
