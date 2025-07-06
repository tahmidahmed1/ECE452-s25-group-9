package com.example.gooddeedfeed

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.google.android.libraries.places.api.Places

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!Places.isInitialized()) {
            Places.initialize(this, BuildConfig.GOOGLE_MAPS_API_KEY)
        }
    }
}
