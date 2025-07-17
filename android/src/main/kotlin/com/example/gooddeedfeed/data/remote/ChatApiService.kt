package com.example.gooddeedfeed.data.remote

import io.ktor.client.HttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chat API service extending BaseApiService to provide chat-specific functionality
 */
@Singleton
class ChatApiService @Inject constructor(client: HttpClient) : BaseApiService(client) {
    // This class inherits the withFallbackUrls method from BaseApiService
    // which is what ChatViewModel needs
} 