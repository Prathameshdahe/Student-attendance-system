package com.smartattendance.smartattendance.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    companion object {
        const val GEOFENCE_ID = "COLLEGE_CAMPUS"
        // Temporary test coordinates (18°27'35.27"N 73°50'57.46"E)
        const val COLLEGE_LAT = 18.459797
        const val COLLEGE_LNG = 73.849294
        const val GEOFENCE_RADIUS = 100f // 100 meters buffer for testing
    }

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    fun startGeofencing() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("Geofence", "Permission not granted.")
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
            .setLoiteringDelay(30000) // 30 sec buffer before triggering exit to prevent GPS bounce
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL or GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, getGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d("Geofence", "Geo-fence registered successfully around BVCOE.")
            }
            .addOnFailureListener { e ->
                Log.e("Geofence", "Geo-fence failed: ${e.message}")
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
