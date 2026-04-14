package com.smartattendance.smartattendance.data.repository

import android.content.Context
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.model.Attendance
import com.smartattendance.smartattendance.data.model.User
import java.text.SimpleDateFormat
import java.util.*
import com.smartattendance.smartattendance.data.remote.ApiClient
import com.smartattendance.smartattendance.data.remote.ScanRequest
import com.smartattendance.smartattendance.data.remote.ScanResponse
import com.smartattendance.smartattendance.data.remote.GeofenceRequest
import com.smartattendance.smartattendance.data.remote.GeofenceResponse
import com.smartattendance.smartattendance.data.remote.ExitRequest
import com.smartattendance.smartattendance.data.remote.ExitResponse
import com.smartattendance.smartattendance.data.remote.HistoryRecord

/**
 * Placeholder AttendanceRepository — Firebase removed.
 * QR check-in/check-out logic will be wired to backend API endpoints in the next milestone.
 */
class AttendanceRepository(context: Context) {

    private val session = SessionManager(context)

    private val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    /** Returns the current user's ID from the stored JWT session */
    fun getCurrentUserId(): String = session.getUserId() ?: ""

    /** Returns the current user's name */
    fun getCurrentUserName(): String = session.getUserName() ?: "Student"

    /** Returns the current user's role */
    fun getCurrentUserRole(): String = session.getRole() ?: "STUDENT"

    /**
     * Generates a local QR token payload for the student.
     * Format: "studentId::uuid" — validated by the Admin scanner against the backend.
     */
    fun generateQrToken(): String {
        val uid = getCurrentUserId()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return "${uid}::${date}"
    }

    suspend fun scanQr(qrPayload: String): Result<ScanResponse> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            val adminId = getCurrentUserId()
            val req = ScanRequest(qr_payload = qrPayload, scanned_by = adminId)
            val response = ApiClient.api.scanQr("Bearer $token", req)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendGeofenceAlert(type: String): Result<GeofenceResponse> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            val req = GeofenceRequest(type = type)
            val response = ApiClient.api.sendGeofenceAlert("Bearer $token", req)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitExitRequest(reason: String): Result<ExitResponse> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            val req = ExitRequest(reason = reason)
            val response = ApiClient.api.submitExitRequest("Bearer $token", req)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyHistory(): Result<List<com.smartattendance.smartattendance.data.remote.HistoryRecord>> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            val response = ApiClient.api.getMyHistory("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingExitRequests(): Result<List<com.smartattendance.smartattendance.data.remote.ExitRequestDto>> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            val response = ApiClient.api.getPendingExitRequests("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveExitRequest(reqId: String, action: String): Result<com.smartattendance.smartattendance.data.remote.ExitResponse> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            val response = ApiClient.api.resolveExitRequest("Bearer $token", reqId, com.smartattendance.smartattendance.data.remote.ResolveActionRequest(action))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyExitRequests(): Result<List<com.smartattendance.smartattendance.data.remote.ExitRequestDto>> {
        return try {
            val token = session.getToken() ?: return Result.failure(Exception("Unauthorized"))
            val response = ApiClient.api.getMyExitRequests("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Stub methods — to be replaced with Retrofit calls in milestone 2 ────

    suspend fun markCheckIn(studentId: String): Result<Unit> {
        return Result.failure(NotImplementedError("Backend QR check-in coming soon"))
    }

    suspend fun markCheckOut(studentId: String): Result<Unit> {
        return Result.failure(NotImplementedError("Backend QR check-out coming soon"))
    }

    suspend fun getTodayLog(studentId: String): Result<Attendance?> {
        return Result.success(null)
    }

    suspend fun getPresentStudents(): Result<List<User>> {
        return Result.success(emptyList())
    }
}
