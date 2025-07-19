package com.example.gooddeedfeed

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        if (!Places.isInitialized()) {
            Places.initialize(this, BuildConfig.GOOGLE_MAPS_API_KEY)
        }
    }
}
