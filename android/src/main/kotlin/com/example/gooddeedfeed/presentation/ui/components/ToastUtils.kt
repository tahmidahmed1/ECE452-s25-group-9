package com.example.gooddeedfeed.presentation.ui.components

import android.content.Context
import android.util.Log

object ToastUtils {
    
    private const val TAG = "ToastUtils"
    
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
    
    /**
     * Extract the actual meaningful message from technical error responses
     */
    private fun extractActualMessage(message: String): String {
        Log.d(TAG, "Original message: $message")
        
        val cleanMessage = cleanUpMessage(message)
        val finalMessage = categorizeAndFormatMessage(cleanMessage)
        
        Log.d(TAG, "Cleaned message: $finalMessage")
        return finalMessage
    }
    
    /**
     * Clean up message by removing URLs and extra whitespace
     */
    private fun cleanUpMessage(message: String): String {
        return message
            .replace(Regex("http[s]?://[^\\s]+"), "") // Remove URLs
            .replace(Regex("URL \\d+:"), "") // Remove "URL 0:" patterns
            .replace(Regex("\\s+"), " ") // Replace multiple whitespace with single space
            .trim()
    }
    
    /**
     * Categorize and format error message based on content
     */
    private fun categorizeAndFormatMessage(cleanMessage: String): String {
        return when {
            // Direct server validation message (preferred)
            cleanMessage.contains("valid email address") -> cleanMessage
            
            // Dev mode specific errors
            isDevModeError(cleanMessage) -> handleDevModeError(cleanMessage)
            
            // Server validation errors
            isServerValidationError(cleanMessage) -> handleServerValidationError(cleanMessage)
            
            // Connection/Network errors
            isConnectionError(cleanMessage) -> "Unable to connect to server. Please check your internet connection and try again."
            
            // Authentication errors
            isAuthenticationError(cleanMessage) -> "Invalid username or password. Please try again."
            
            // User already exists
            isConflictError(cleanMessage) -> "An account with this username or email already exists."
            
            // Server errors
            isServerError(cleanMessage) -> "Server is temporarily unavailable. Please try again later."
            
            // Token/Auth errors
            isTokenError(cleanMessage) -> "Authentication failed. Please try signing in again."
            
            // Clean, readable message
            isCleanMessage(cleanMessage) -> cleanMessage
            
            // Default fallback with more context
            else -> "Unexpected error occurred: $cleanMessage"
        }
    }
    
    private fun isServerValidationError(message: String): Boolean {
        return message.contains("illegal input") || message.contains("fields [")
    }
    
    private fun handleServerValidationError(message: String): String {
        return when {
            message.contains("email") || message.contains("@") -> 
                "Please enter a valid email address with an @ sign"
            message.contains("password") -> 
                "Password is required and must meet minimum requirements"
            message.contains("username") -> 
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
}
