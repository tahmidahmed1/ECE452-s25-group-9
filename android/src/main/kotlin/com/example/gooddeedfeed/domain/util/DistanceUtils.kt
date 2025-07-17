package com.example.gooddeedfeed.domain.util

import android.location.Location

/**
 * Calculates the distance between two lat/lng points in kilometres.
 */
fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0] / 1000f
} 
