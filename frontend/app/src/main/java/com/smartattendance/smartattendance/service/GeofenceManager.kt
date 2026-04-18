package com.smartattendance.smartattendance.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    companion object {
        const val GEOFENCE_ID = "COLLEGE_CAMPUS"
        const val COLLEGE_LAT = 18.458444
        const val COLLEGE_LNG = 73.855922
        const val GEOFENCE_RADIUS = 325f
        const val BOUNDARY_BUFFER_METERS = 30f
        const val MAX_ACCEPTABLE_ACCURACY_METERS = 85f
        const val NOTIFICATION_RESPONSIVENESS_MS = 20_000
    }

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    fun startGeofencing() {
        if (
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("Geofence", "Fine location permission not granted.")
            return
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("Geofence", "Background location permission not granted.")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(COLLEGE_LAT, COLLEGE_LNG, GEOFENCE_RADIUS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_EXIT or
                    Geofence.GEOFENCE_TRANSITION_ENTER
            )
            .setNotificationResponsiveness(NOTIFICATION_RESPONSIVENESS_MS)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val pendingIntent = getGeofencePendingIntent()
        geofencingClient.removeGeofences(pendingIntent)
            .addOnCompleteListener {
                geofencingClient.addGeofences(request, pendingIntent)
                    .addOnSuccessListener {
                        Log.d("Geofence", "BVCOE campus geofence registered successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Geofence", "Geofence registration failed: ${e.message}")
                    }
            }
    }

    fun stopGeofencing() {
        geofencingClient.removeGeofences(listOf(GEOFENCE_ID))
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
