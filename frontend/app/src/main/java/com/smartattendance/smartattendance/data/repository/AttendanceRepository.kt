package com.smartattendance.smartattendance.data.repository

import android.content.Context
import com.smartattendance.smartattendance.data.local.DeviceIdentity
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.model.Attendance
import com.smartattendance.smartattendance.data.model.User
import com.smartattendance.smartattendance.data.remote.ApiClient
import com.smartattendance.smartattendance.data.remote.ExitRequest
import com.smartattendance.smartattendance.data.remote.ExitRequestDto
import com.smartattendance.smartattendance.data.remote.ExitResponse
import com.smartattendance.smartattendance.data.remote.GeofenceEventDto
import com.smartattendance.smartattendance.data.remote.GeofenceRequest
import com.smartattendance.smartattendance.data.remote.GeofenceResponse
import com.smartattendance.smartattendance.data.remote.HistoryRecord
import com.smartattendance.smartattendance.data.remote.LiveAttendanceDto
import com.smartattendance.smartattendance.data.remote.ResolveActionRequest
import com.smartattendance.smartattendance.data.remote.ScanRequest
import com.smartattendance.smartattendance.data.remote.ScanResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceRepository(context: Context) {

    private val session = SessionManager(context)
    private val deviceId = DeviceIdentity.getDeviceId(context)
    private val clientType = "android-app"

    private val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun getCurrentUserId(): String = session.getUserId() ?: ""

    fun getCurrentUserName(): String = session.getUserName() ?: "Student"

    fun getCurrentUserRole(): String = session.getRole() ?: "STUDENT"

    fun generateQrToken(): String {
        val uid = getCurrentUserId()
        return "${uid}::${todayDate}"
    }

    suspend fun scanQr(qrPayload: String): Result<ScanResponse> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            val adminId = getCurrentUserId()
            val request = ScanRequest(qr_payload = qrPayload, scanned_by = adminId)
            Result.success(ApiClient.api.scanQr("Bearer $token", clientType, deviceId, request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendGeofenceAlert(type: String): Result<GeofenceResponse> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            Result.success(
                ApiClient.api.sendGeofenceAlert(
                    "Bearer $token",
                    clientType,
                    deviceId,
                    GeofenceRequest(type = type, device_id = deviceId)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitExitRequest(reason: String): Result<ExitResponse> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            Result.success(ApiClient.api.submitExitRequest("Bearer $token", clientType, deviceId, ExitRequest(reason)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyHistory(): Result<List<HistoryRecord>> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            Result.success(ApiClient.api.getMyHistory("Bearer $token", clientType, deviceId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveAttendanceToday(): Result<List<LiveAttendanceDto>> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            Result.success(ApiClient.api.getLiveAttendanceToday("Bearer $token", clientType, deviceId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingExitRequests(): Result<List<ExitRequestDto>> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            Result.success(ApiClient.api.getPendingExitRequests("Bearer $token", clientType, deviceId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveExitRequest(reqId: String, action: String): Result<ExitResponse> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            Result.success(
                ApiClient.api.resolveExitRequest(
                    "Bearer $token",
                    clientType,
                    deviceId,
                    reqId,
                    ResolveActionRequest(action)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyExitRequests(): Result<List<ExitRequestDto>> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            Result.success(ApiClient.api.getMyExitRequests("Bearer $token", clientType, deviceId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExitRequestHistory(): Result<List<ExitRequestDto>> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            Result.success(ApiClient.api.getExitRequestHistory("Bearer $token", clientType, deviceId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGeofenceEvents(): Result<List<GeofenceEventDto>> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            Result.success(ApiClient.api.getGeofenceEvents("Bearer $token", clientType, deviceId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyGeofenceEvents(): Result<List<GeofenceEventDto>> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            Result.success(ApiClient.api.getMyGeofenceEvents("Bearer $token", clientType, deviceId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markCheckIn(studentId: String): Result<Unit> {
        return Result.failure(NotImplementedError("Backend QR check-in is used instead"))
    }

    suspend fun markCheckOut(studentId: String): Result<Unit> {
        return Result.failure(NotImplementedError("Backend QR check-out is used instead"))
    }

    suspend fun getTodayLog(studentId: String): Result<Attendance?> {
        return Result.success(null)
    }

    suspend fun getPresentStudents(): Result<List<User>> {
        return Result.success(emptyList())
    }
}
