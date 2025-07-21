package com.example.gooddeedfeed.data.remote

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
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
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EventApiService @Inject constructor(
    client: HttpClient,
    private val dataStore: DataStore<Preferences>,
) : BaseApiService(client) {

    companion object {
        private const val TAG = "EventApiService"
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

    suspend fun createEvent(data: CreateEventData): EventDto {
        Log.d(TAG, "🚀 Starting createEvent request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for createEvent")
            throw Exception("No authentication session found")
        }

        return try {
            Log.d(TAG, "📤 Sending createEvent request with session authorization...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying createEvent URL: $baseUrl/events")
                client.post("$baseUrl/events") {
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                    contentType(ContentType.Application.Json)
                    setBody(data.toDto())
                }
            }

            Log.d(TAG, "📥 createEvent response status: ${response.status}")

            if (response.status.value in 200..299) {
                val eventDto: EventDto = response.body()
                Log.d(TAG, "✅ createEvent successful - Event ID: ${eventDto.id}")
                eventDto
            } else {
                Log.e(TAG, "❌ createEvent failed with status ${response.status}")
                throw Exception("Server returned ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ createEvent failed with exception", e)
            throw Exception("Failed to create event: ${e.message}")
        }
    }

    suspend fun updateEvent(id: Int, data: CreateEventData): EventDto {
        Log.d(TAG, "🚀 Starting updateEvent request for event ID: $id")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for updateEvent")
            throw Exception("No authentication session found")
        }

        return try {
            Log.d(TAG, "📤 Sending updateEvent request with session authorization...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying updateEvent URL: $baseUrl/events/$id")
                client.patch("$baseUrl/events/$id") {
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                    contentType(ContentType.Application.Json)
                    setBody(data.toDto())
                }
            }

            Log.d(TAG, "📥 updateEvent response status: ${response.status}")

            if (response.status.value in 200..299) {
                val eventDto: EventDto = response.body()
                Log.d(TAG, "✅ updateEvent successful - Event ID: ${eventDto.id}")
                eventDto
            } else {
                Log.e(TAG, "❌ updateEvent failed with status ${response.status}")
                throw Exception("Server returned ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateEvent failed with exception", e)
            throw Exception("Failed to update event: ${e.message}")
        }
    }

    suspend fun deleteEvent(id: Int) {
        Log.d(TAG, "🚀 Starting deleteEvent request for event ID: $id")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for deleteEvent")
            throw Exception("No authentication session found")
        }

        try {
            Log.d(TAG, "📤 Sending deleteEvent request with session authorization...")
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying deleteEvent URL: $baseUrl/events/$id")
                client.delete("$baseUrl/events/$id") {
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                }
            }
            Log.d(TAG, "✅ deleteEvent successful for event ID: $id")
        } catch (e: Exception) {
            Log.e(TAG, "❌ deleteEvent failed with exception", e)
            throw Exception("Failed to delete event: ${e.message}")
        }
    }

    suspend fun uploadEventImage(eventId: Int, file: java.io.File) {
        Log.d(TAG, "🚀 Starting uploadEventImage request for event ID: $eventId")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for uploadEventImage")
            throw Exception("No authentication session found")
        }

        try {
            Log.d(TAG, "📤 Sending uploadEventImage request with session authorization...")
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying uploadEventImage URL: $baseUrl/events/$eventId/upload-image")
                client.submitFormWithBinaryData(
                    url = "$baseUrl/events/$eventId/upload-image",
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
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                }
            }
            Log.d(TAG, "✅ uploadEventImage successful for event ID: $eventId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ uploadEventImage failed with exception", e)
            throw Exception("Failed to upload event image: ${e.message}")
        }
    }

    suspend fun uploadEventImageToCarousel(eventId: Int, file: java.io.File, isMain: Boolean) {
        Log.d(TAG, "🚀 Starting uploadEventImageToCarousel request for event ID: $eventId")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for uploadEventImageToCarousel")
            throw Exception("No authentication session found")
        }

        try {
            Log.d(TAG, "📤 Sending uploadEventImageToCarousel request with session authorization...")
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying uploadEventImageToCarousel URL: $baseUrl/events/$eventId/images/upload")
                client.submitFormWithBinaryData(
                    url = "$baseUrl/events/$eventId/images/upload",
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
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                }
            }
            Log.d(TAG, "✅ uploadEventImageToCarousel successful for event ID: $eventId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ uploadEventImageToCarousel failed with exception", e)
            throw Exception("Failed to upload event image to carousel: ${e.message}")
        }
    }

    suspend fun getEventImages(eventId: Int): List<EventImageDto> =
        client.get(buildUrl("events/$eventId/images")).body()

    suspend fun setMainEventImage(eventId: Int, imageId: Int) {
        Log.d(TAG, "🚀 Starting setMainEventImage request for event ID: $eventId, image ID: $imageId")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for setMainEventImage")
            throw Exception("No authentication session found")
        }

        try {
            Log.d(TAG, "📤 Sending setMainEventImage request with session authorization...")
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying setMainEventImage URL: $baseUrl/events/$eventId/images/$imageId/set-main")
                client.patch("$baseUrl/events/$eventId/images/$imageId/set-main") {
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                }
            }
            Log.d(TAG, "✅ setMainEventImage successful for event ID: $eventId, image ID: $imageId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ setMainEventImage failed with exception", e)
            throw Exception("Failed to set main event image: ${e.message}")
        }
    }

    suspend fun joinEvent(eventId: Int): Map<String, Any> {
        Log.d(TAG, "🚀 Starting joinEvent request for event ID: $eventId")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for joinEvent")
            throw Exception("No authentication session found")
        }

        return try {
            Log.d(TAG, "📤 Sending joinEvent request with session authorization...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying joinEvent URL: $baseUrl/events/$eventId/join")
                client.post("$baseUrl/events/$eventId/join") {
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                }
            }

            Log.d(TAG, "📥 joinEvent response status: ${response.status}")

            if (response.status.value in 200..299) {
                val responseBody: Map<String, Any> = response.body()
                Log.d(TAG, "✅ joinEvent successful - Event ID: $eventId")
                responseBody
            } else {
                Log.e(TAG, "❌ joinEvent failed with status ${response.status}")
                throw Exception("Server returned ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ joinEvent failed with exception", e)
            throw Exception("Failed to join event: ${e.message}")
        }
    }

    suspend fun leaveEvent(eventId: Int): Map<String, Any> {
        Log.d(TAG, "🚀 Starting leaveEvent request for event ID: $eventId")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for leaveEvent")
            throw Exception("No authentication session found")
        }

        return try {
            Log.d(TAG, "📤 Sending leaveEvent request with session authorization...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying leaveEvent URL: $baseUrl/events/$eventId/leave")
                client.post("$baseUrl/events/$eventId/leave") {
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                }
            }

            Log.d(TAG, "📥 leaveEvent response status: ${response.status}")

            if (response.status.value in 200..299) {
                val responseBody: Map<String, Any> = response.body()
                Log.d(TAG, "✅ leaveEvent successful - Event ID: $eventId")
                responseBody
            } else {
                Log.e(TAG, "❌ leaveEvent failed with status ${response.status}")
                throw Exception("Server returned ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ leaveEvent failed with exception", e)
            throw Exception("Failed to leave event: ${e.message}")
        }
    }

    suspend fun getMyJoinedEvents(): List<EventDto> {
        Log.d(TAG, "🚀 Starting getMyJoinedEvents request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for getMyJoinedEvents")
            throw Exception("No authentication session found")
        }

        return try {
            Log.d(TAG, "📤 Sending getMyJoinedEvents request with session authorization...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying getMyJoinedEvents URL: $baseUrl/users/me/joined-events")
                client.get("$baseUrl/users/me/joined-events") {
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                }
            }

            Log.d(TAG, "📥 getMyJoinedEvents response status: ${response.status}")

            if (response.status.value in 200..299) {
                val events: List<EventDto> = response.body()
                Log.d(TAG, "✅ getMyJoinedEvents successful - Found ${events.size} joined events")
                events
            } else {
                Log.e(TAG, "❌ getMyJoinedEvents failed with status ${response.status}")
                throw Exception("Server returned ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ getMyJoinedEvents failed with exception", e)
            throw Exception("Failed to get joined events: ${e.message}")
        }
    }
} 
