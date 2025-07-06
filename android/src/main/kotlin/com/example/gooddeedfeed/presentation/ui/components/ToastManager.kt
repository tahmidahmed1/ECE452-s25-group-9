package com.example.gooddeedfeed.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ToastManager {
    private val _toastData = MutableStateFlow<ToastData?>(null)
    val toastData: StateFlow<ToastData?> = _toastData.asStateFlow()

    fun showSuccess(message: String, duration: Long = 3000L) {
        _toastData.value = ToastData(
            message = message,
            type = ToastType.SUCCESS,
            duration = duration,
        )
    }

    fun showError(message: String, duration: Long = 5000L) {
        _toastData.value = ToastData(
            message = message,
            type = ToastType.ERROR,
            duration = duration,
        )
    }

    fun showWarning(message: String, duration: Long = 4000L) {
        _toastData.value = ToastData(
            message = message,
            type = ToastType.WARNING,
            duration = duration,
        )
    }

    fun showInfo(message: String, duration: Long = 3000L) {
        _toastData.value = ToastData(
            message = message,
            type = ToastType.INFO,
            duration = duration,
        )
    }

    fun dismiss() {
        _toastData.value = null
    }
}

@Composable
fun rememberToastState(): State<ToastData?> {
    return ToastManager.toastData.collectAsStateWithLifecycle()
} 
