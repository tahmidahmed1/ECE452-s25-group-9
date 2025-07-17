package com.example.gooddeedfeed.data.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L, // 5 seconds - more frequent updates for better accuracy
    ).apply {
        setMinUpdateIntervalMillis(2000L) // 2 seconds
        setMaxUpdateDelayMillis(10000L) // 10 seconds
        setMaxUpdateAgeMillis(30000L) // Don't use location older than 30 seconds
    }.build()

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Location?> = callbackFlow {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // Filter out obviously fake/test locations (like Google Plex HQ)
                    val isValidLocation = !(location.latitude == 37.4220 && location.longitude == -122.0841)
                    if (isValidLocation) {
                        trySend(location)
                    }
                    // If it's a fake location, we don't send it and let the system try again
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper(),
            )
        } catch (e: SecurityException) {
            // Handle permission denied
            trySend(null)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = try {
        val location = fusedLocationClient.lastLocation.await()
        // Filter out obviously fake/test locations (like Google Plex HQ)
        location?.let {
            // Google Plex HQ coordinates: 37.4220, -122.0841
            // If location is exactly at Google Plex, it's likely a mock location
            if (it.latitude == 37.4220 && it.longitude == -122.0841) {
                null // Return null to force a fresh location request
            } else {
                it
            }
        }
    } catch (e: Exception) {
        null
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000f // Convert to kilometers
    }
} 
