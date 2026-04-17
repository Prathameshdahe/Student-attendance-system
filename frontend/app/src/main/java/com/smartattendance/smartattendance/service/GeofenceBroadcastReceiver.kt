package com.smartattendance.smartattendance.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.smartattendance.smartattendance.data.local.DeviceIdentity
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transitionType = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            Geofence.GEOFENCE_TRANSITION_ENTER -> "RETURN"
            else -> return
        }

        val location = event.triggeringLocation
        saveEventToQueue(
            context = context,
            type = transitionType,
            timestamp = System.currentTimeMillis(),
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracyMeters = location?.accuracy,
            distanceFromCenterMeters = location?.let { distanceFromCampus(it) },
            networkType = currentNetworkType(context),
            deviceId = DeviceIdentity.getDeviceId(context)
        )

        if (isNetworkAvailable(context)) {
            sendQueuedEvents(context)
        }
    }

    private fun saveEventToQueue(
        context: Context,
        type: String,
        timestamp: Long,
        latitude: Double?,
        longitude: Double?,
        accuracyMeters: Float?,
        distanceFromCenterMeters: Float?,
        networkType: String,
        deviceId: String
    ) {
        val prefs = context.getSharedPreferences("geofence_queue", Context.MODE_PRIVATE)
        val existing = prefs.getString("events", "[]") ?: "[]"
        val arr = JSONArray(existing)

        val lastEvent = if (arr.length() > 0) arr.optJSONObject(arr.length() - 1) else null
        if (
            lastEvent != null &&
            lastEvent.optString("type") == type &&
            timestamp - lastEvent.optLong("timestamp", 0L) < 60_000L
        ) {
            return
        }

        val obj = JSONObject().apply {
            put("type", type)
            put("timestamp", timestamp)
            put("network_type", networkType)
            put("device_id", deviceId)
            if (latitude != null) put("latitude", latitude)
            if (longitude != null) put("longitude", longitude)
            if (accuracyMeters != null) put("accuracy_meters", accuracyMeters.toDouble())
            if (distanceFromCenterMeters != null) put("distance_from_center_meters", distanceFromCenterMeters.toDouble())
        }
        arr.put(obj)
        prefs.edit().putString("events", arr.toString()).apply()

        scheduleRetry(context)
    }

    private fun sendQueuedEvents(context: Context) {
        val request = OneTimeWorkRequestBuilder<GeofenceUploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun scheduleRetry(context: Context) {
        val request = PeriodicWorkRequestBuilder<GeofenceUploadWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "geofence_retry",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun currentNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return "OFFLINE"
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return "OFFLINE"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun distanceFromCampus(location: Location): Float {
        val campusLocation = Location("bvcoe-campus").apply {
            latitude = GeofenceManager.COLLEGE_LAT
            longitude = GeofenceManager.COLLEGE_LNG
        }
        return location.distanceTo(campusLocation)
    }
}
