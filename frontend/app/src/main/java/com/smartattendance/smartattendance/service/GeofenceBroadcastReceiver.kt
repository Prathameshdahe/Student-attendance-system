package com.smartattendance.smartattendance.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transitionType = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_EXIT  -> "EXIT"
            Geofence.GEOFENCE_TRANSITION_ENTER -> "RETURN"
            else -> return
        }

        // Step 1: Save locally FIRST — never lose the event
        saveEventToQueue(context, transitionType)

        // Step 2: Try sending immediately
        if (isNetworkAvailable(context)) {
            sendQueuedEvents(context)
        }
        // If no network → WorkManager picks it up when internet returns
    }

    private fun saveEventToQueue(context: Context, type: String) {
        val prefs = context.getSharedPreferences("geofence_queue", Context.MODE_PRIVATE)
        val existing = prefs.getString("events", "[]") ?: "[]"
        val arr = JSONArray(existing)
        val obj = JSONObject().apply {
            put("type", type)
            put("timestamp", System.currentTimeMillis())
            put("date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
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
        val request = PeriodicWorkRequestBuilder<GeofenceUploadWorker>(
            15, TimeUnit.MINUTES
        )
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

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
