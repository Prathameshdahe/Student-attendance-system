package com.smartattendance.smartattendance.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.smartattendance.smartattendance.data.repository.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e("Geofence", "Error: ${geofencingEvent.errorCode}")
            return
        }

        val repo = AttendanceRepository(context)
        
        when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d("Geofence", "UNAUTHORIZED_EXIT detected!")
                CoroutineScope(Dispatchers.IO).launch {
                    val result = repo.sendGeofenceAlert("UNAUTHORIZED_EXIT")
                    result.onSuccess {
                        Log.d("Geofence", "Backend notified of EXIT successfully.")
                    }.onFailure { e ->
                        Log.e("Geofence", "Failed to notify backend: ${e.message}")
                    }
                }
            }
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d("Geofence", "Student RETURNED to campus.")
                CoroutineScope(Dispatchers.IO).launch {
                    val result = repo.sendGeofenceAlert("RETURN_TO_CAMPUS")
                    result.onSuccess {
                        Log.d("Geofence", "Backend notified of RETURN successfully.")
                    }.onFailure { e ->
                        Log.e("Geofence", "Failed to notify backend: ${e.message}")
                    }
                }
            }
        }
    }
}
