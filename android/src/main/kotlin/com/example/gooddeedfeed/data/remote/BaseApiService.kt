package com.example.gooddeedfeed.data.remote

import io.ktor.client.HttpClient

/**
 * Base API service holding common configuration such as list of possible server URLs
 * (to support emulator/WSL/localhost environments) and generic helpers.
 */
abstract class BaseApiService(protected val client: HttpClient) {

    /** Potential base URLs – first items will be tried first. */
    protected val possibleUrls: List<String> = listOf(
        "http://10.0.2.2:9000/api", // Android emulator → Windows host
        "http://172.28.7.154:9000/api", // Current WSL IP
        "http://172.28.0.1:9000/api", // Windows host from WSL
        "http://localhost:9000/api", // Localhost
        "http://127.0.0.1:9000/api", // Loopback
    )

    /**
     * Helper to iterate over [possibleUrls] until [block] succeeds.
     * The [block] receives the composed full URL so it can append path segments.
     */
    protected suspend fun <T> withFallbackUrls(block: suspend (baseUrl: String) -> T): T {
        var lastException: Exception? = null
        for (url in possibleUrls) {
            try {
                return block(url)
            } catch (e: Exception) {
                lastException = e
                // Continue to next URL
            }
        }
        throw lastException ?: IllegalStateException("All URLs failed and no exception captured")
    }
} 