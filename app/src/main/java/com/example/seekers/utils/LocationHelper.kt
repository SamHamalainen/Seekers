package com.example.seekers.utils

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

/**
 * LocationHelper: Contains functions and variables that facilitate the acquisition of the current location
 */

object LocationHelper {
    // Sets the parameters for acquiring the current location
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 15 * 1000
        isWaitForAccurateLocation = false
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    // Requests the tracking of the current location given a callback
    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(
        client: FusedLocationProviderClient,
        locationCallback: LocationCallback
    ) {
        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // Stops tracking location
    fun removeLocationUpdates(
        client: FusedLocationProviderClient,
        locationCallback: LocationCallback
    ) {
        client.removeLocationUpdates(locationCallback)
        Log.d("DEBUG", "removed location updates")
    }
}