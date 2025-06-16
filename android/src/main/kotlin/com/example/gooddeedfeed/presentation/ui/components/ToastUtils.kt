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
        
        // Clean up the message - remove URLs and extra whitespace
        var cleanMessage = message
            .replace(Regex("http[s]?://[^\\s]+"), "") // Remove URLs
            .replace(Regex("URL \\d+:"), "") // Remove "URL 0:" patterns
            .replace(Regex("\\s+"), " ") // Replace multiple whitespace with single space
            .trim()
        
        // Extract meaningful content from various error patterns
        cleanMessage = when {
            // Case 1: Direct server validation message (what we want)
            cleanMessage.contains("valid email address") -> {
                cleanMessage
            }
            
            // Case 2: Server error with JSON response containing the real message
            cleanMessage.contains("illegal input") || cleanMessage.contains("fields [") -> {
                // Look for patterns like "illegal input: fields [access_token, t..."
                // This usually means the server returned an actual validation message that got truncated
                when {
                    cleanMessage.contains("email") || cleanMessage.contains("@") -> 
                        "Please enter a valid email address with an @ sign"
                    cleanMessage.contains("password") -> 
                        "Password is required and must meet minimum requirements"
                    cleanMessage.contains("username") -> 
                        "Username is required and must meet minimum requirements"
                    else -> "Please check your input and try again"
                }
            }
            
            // Case 3: Connection/Network errors
            cleanMessage.contains("Connection failed") || 
            cleanMessage.contains("could not connect") ||
            cleanMessage.contains("Attempted servers") -> 
                "Unable to connect to server. Please check your internet connection and try again."
            
            // Case 4: Authentication errors
            cleanMessage.contains("unauthorized") ||
            cleanMessage.contains("invalid username") ||
            cleanMessage.contains("invalid password") ->
                "Invalid username or password. Please try again."
            
            // Case 5: User already exists
            cleanMessage.contains("already exists") ||
            cleanMessage.contains("conflict") ->
                "An account with this username or email already exists."
            
            // Case 6: Server errors
            cleanMessage.contains("server error") ||
            cleanMessage.contains("internal server") ||
            cleanMessage.contains("500") ||
            cleanMessage.contains("503") ->
                "Server is temporarily unavailable. Please try again later."
            
            // Case 7: If it's already a clean, readable message, keep it
            !cleanMessage.contains("failed") && 
            !cleanMessage.contains("error") && 
            cleanMessage.length < 150 && 
            cleanMessage.split(" ").size < 20 ->
                cleanMessage
            
            // Case 8: Default fallback
            else -> "Something went wrong. Please try again."
        }
        
        Log.d(TAG, "Cleaned message: $cleanMessage")
        return cleanMessage
    }
} 
