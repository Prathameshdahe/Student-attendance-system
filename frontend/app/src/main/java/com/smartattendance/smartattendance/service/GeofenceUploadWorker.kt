package com.smartattendance.smartattendance.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.remote.ApiClient
import com.smartattendance.smartattendance.data.remote.GeofenceEventRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class GeofenceUploadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("geofence_queue", Context.MODE_PRIVATE)
        val pending = prefs.getString("events", "[]") ?: "[]"
        val arr = JSONArray(pending)
        
        if (arr.length() == 0) return Result.success()

        val sessionManager = SessionManager(context)
        val token = withContext(Dispatchers.IO) { sessionManager.getToken() }
            ?: return Result.failure() // not logged in

        return try {
            // Send each queued event
            for (i in 0 until arr.length()) {
                val event = arr.getJSONObject(i)
                ApiClient.api.postGeofenceEvent(
                    "Bearer $token",
                    GeofenceEventRequest(
                        transition_type = event.getString("type"),
                        timestamp = event.getLong("timestamp"),
                        date = event.getString("date")
                    )
                )
            }
            // Clear queue on success
            prefs.edit().putString("events", "[]").apply()
            Result.success()
        } catch (e: Exception) {
            Log.e("GeofenceWorker", "Upload failed: ${e.message}")
            Result.retry() // WorkManager retries with backoff
        }
    }
}
