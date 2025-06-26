package com.example.gooddeedfeed.presentation.ui.components

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray

object ToastUtils {

    private const val TAG = "ToastUtils"

    private val URL_REGEX = Regex("http[s]?://[^\\s]+")
    private val URL_INDEX_REGEX = Regex("URL \\d+:")
    private val MULTI_SPACE_REGEX = Regex("\\s+")

    fun showSuccessToast(context: Context, message: String) {
        val cleanMessage = extractActualMessage(message)
        Log.d(TAG, "Success toast: $cleanMessage")
        ToastManager.showSuccess(cleanMessage)
    }

    fun showErrorToast(context: Context, message: String) {
        val cleanMessage = extractActualMessage(message)
        Log.d(TAG, "Error toast: $cleanMessage")
        ToastManager.showError(cleanMessage)
    }

    fun showInfoToast(context: Context, message: String) {
        val cleanMessage = extractActualMessage(message)
        Log.d(TAG, "Info toast: $cleanMessage")
        ToastManager.showInfo(cleanMessage)
    }

    private fun extractActualMessage(message: String): String {
        Log.d(TAG, "Original message: $message")

        val jsonExtracted = extractFromJson(message)
        val cleanMessage = cleanUpMessage(jsonExtracted)
        val finalMessage = categorizeAndFormatMessage(cleanMessage)

        Log.d(TAG, "Cleaned message: $finalMessage")
        return finalMessage
    }

    private fun cleanUpMessage(message: String): String {
        return message
            .replace(URL_REGEX, "")
            .replace(URL_INDEX_REGEX, "")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
    }

    private fun categorizeAndFormatMessage(cleanMessage: String): String {
        return when {
            isServerValidationError(cleanMessage) -> handleServerValidationError(cleanMessage)
            isDevModeError(cleanMessage) -> handleDevModeError(cleanMessage)
            isCleanMessage(cleanMessage) -> cleanMessage
            isAuthenticationError(cleanMessage) -> "Invalid username or password. Please try again."
            isTokenError(cleanMessage) -> "Authentication failed. Please try signing in again."
            isConflictError(cleanMessage) -> "An account with this username or email already exists."
            isServerError(cleanMessage) -> "Server is temporarily unavailable. Please try again later."
            isConnectionError(cleanMessage) -> "Unable to connect to server. Please check your internet connection and try again."
            else -> cleanMessage // fallback: show whatever we have after cleanup
        }
    }

    private fun isServerValidationError(message: String): Boolean {
        return message.contains("illegal input", ignoreCase = true) ||
            message.contains("fields [", ignoreCase = true) ||
            message.contains("value is not a valid", ignoreCase = true) ||
            message.contains("valid email address", ignoreCase = true) ||
            message.contains("must have an @", ignoreCase = true)
    }

    private fun handleServerValidationError(message: String): String {
        val lower = message.lowercase()
        return when {
            (lower.contains("email") && lower.contains("period")) ->
                "Please enter a valid email address that contains a period after the @ sign (e.g., example@domain.com)"
            lower.contains("email") || lower.contains("@-sign") || lower.contains("@ sign") ->
                "Please enter a valid email address with one @ sign"
            lower.contains("password") ->
                "Password is required and must meet minimum requirements"
            lower.contains("username") ->
                "Username is required and must meet minimum requirements"
            else -> "Please check your input and try again"
        }
    }

    private fun isConnectionError(message: String): Boolean {
        return message.contains("Connection failed") ||
            message.contains("could not connect") ||
            message.contains("Attempted servers")
    }

    private fun isAuthenticationError(message: String): Boolean {
        return message.contains("unauthorized") ||
            message.contains("invalid username") ||
            message.contains("invalid password")
    }

    private fun isConflictError(message: String): Boolean {
        return message.contains("already exists") || message.contains("conflict")
    }

    private fun isServerError(message: String): Boolean {
        return message.contains("server error") ||
            message.contains("internal server") ||
            message.contains("500") ||
            message.contains("503")
    }

    private fun isDevModeError(message: String): Boolean {
        return message.contains("dev mode", ignoreCase = true) ||
            message.contains("dev_", ignoreCase = true)
    }

    private fun handleDevModeError(message: String): String {
        return when {
            message.contains("sign-in failed", ignoreCase = true) ->
                "Development sign-in failed. Server may be unavailable. Details: ${message.substringAfter(":")}"
            message.contains("connection", ignoreCase = true) ->
                "Cannot connect to development server. Please check if the server is running."
            message.contains("token", ignoreCase = true) ->
                "Development authentication failed. Please try again or restart the server."
            else -> "Development mode error: $message"
        }
    }

    private fun isTokenError(message: String): Boolean {
        return message.contains("no authentication token", ignoreCase = true) ||
            message.contains("invalid token", ignoreCase = true) ||
            message.contains("expired token", ignoreCase = true) ||
            message.contains("authentication token", ignoreCase = true)
    }

    private fun isCleanMessage(message: String): Boolean {
        return !message.contains("failed") &&
            !message.contains("error") &&
            message.length < 150 &&
            message.split(" ").size < 20
    }

    private fun extractFromJson(raw: String): String {
        val trimmed = raw.trim()
        return try {
            val element = Json.parseToJsonElement(trimmed)
            when (element) {
                is JsonObject -> {
                    element["message"]?.jsonPrimitive?.content ?: raw
                }
                is JsonArray -> {
                    if (element.isNotEmpty()) {
                        val first = element[0]
                        if (first is JsonObject && first["message"] is JsonPrimitive) {
                            first["message"]!!.jsonPrimitive.content
                        } else raw
                    } else raw
                }
                else -> raw
            }
        } catch (e: Exception) {
            raw
        }
    }
}
