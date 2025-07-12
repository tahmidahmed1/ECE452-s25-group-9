package com.example.gooddeedfeed

import android.app.Application
import android.os.Bundle
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
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

        // Initialize Firebase Analytics and log a startup event
        val analytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle().apply {
            putString("screen_name", "Main")
            putString("startup_reason", "app_launch")
            putLong("timestamp", System.currentTimeMillis())
        }
        analytics.logEvent("startup_event", bundle)
    }
}
