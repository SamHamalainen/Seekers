package com.example.seekers.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log

import com.google.android.gms.location.*

object LocationHelper {
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 15 * 1000
        isWaitForAccurateLocation = false
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    fun checkPermissions(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

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

    fun removeLocationUpdates(
        client: FusedLocationProviderClient,
        locationCallback: LocationCallback
    ) {
        client.removeLocationUpdates(locationCallback)
        Log.d("DEBUG", "removed location updates")
    }
}