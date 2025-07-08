package com.example.gooddeedfeed

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!Places.isInitialized()) {
            Places.initialize(this, BuildConfig.GOOGLE_MAPS_API_KEY)
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.e("FirebaseInit", "Firebase NOT initialized!")
        } else {
            Log.d("FirebaseInit", "Firebase initialized successfully")

            // Optional: Get FCM token for push notification testing
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                        return@addOnCompleteListener
                    }

                    // Get new FCM token
                    val token = task.result
                    Log.d("FCM", "FCM registration token: $token")
                }
        }
    }
}
