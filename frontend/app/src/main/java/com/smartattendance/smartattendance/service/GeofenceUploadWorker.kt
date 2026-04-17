package com.smartattendance.smartattendance.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartattendance.smartattendance.data.local.DeviceIdentity
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.remote.ApiClient
import com.smartattendance.smartattendance.data.remote.GeofenceEventRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import retrofit2.HttpException

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
            ?: return Result.failure()
        val deviceId = DeviceIdentity.getDeviceId(context)

        return try {
            val remaining = mutableListOf<String>()

            for (i in 0 until arr.length()) {
                val event = arr.getJSONObject(i)
                try {
                    val response = ApiClient.api.postGeofenceEvent(
                        token = "Bearer $token",
                        clientType = "android-app",
                        deviceId = deviceId,
                        body = GeofenceEventRequest(
                            type = event.getString("type"),
                            timestamp = event.getLong("timestamp"),
                            latitude = event.optDoubleOrNull("latitude"),
                            longitude = event.optDoubleOrNull("longitude"),
                            accuracy_meters = event.optDoubleOrNull("accuracy_meters")?.toFloat(),
                            distance_from_center_meters = event.optDoubleOrNull("distance_from_center_meters")?.toFloat(),
                            device_id = event.optString("device_id").ifBlank { deviceId },
                            network_type = event.optString("network_type").ifBlank { "UNKNOWN" }
                        )
                    )

                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }
                } catch (e: Exception) {
                    remaining += event.toString()
                    Log.e("GeofenceWorker", "Upload failed for queued event: ${e.message}")
                }
            }

            val updatedQueue = JSONArray()
            remaining.forEach { updatedQueue.put(org.json.JSONObject(it)) }
            prefs.edit().putString("events", updatedQueue.toString()).apply()

            if (remaining.isEmpty()) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e("GeofenceWorker", "Upload failed: ${e.message}")
            Result.retry()
        }
    }

    private fun org.json.JSONObject.optDoubleOrNull(key: String): Double? {
        return if (has(key) && !isNull(key)) optDouble(key) else null
    }
}
