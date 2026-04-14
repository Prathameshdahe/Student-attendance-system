package com.smartattendance.smartattendance.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// ─── Request / Response DTOs ────────────────────────────────────────────────

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val token: String,
    val role: String,
    val name: String,
    val email: String,
    val user_id: String
)

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val roll_number: String
)

data class ScanRequest(val qr_payload: String, val scanned_by: String)

data class ScanResponse(
    val status: String,
    val message: String,
    val student_name: String? = null,
    val roll: String? = null,
    val time: String? = null
)

data class GeofenceRequest(val type: String)

data class GeofenceResponse(val status: String, val message: String)

data class ExitRequest(val reason: String)

data class ExitResponse(val status: String, val message: String)

data class ExitRequestDto(
    val id: Int? = null,
    val request_id: Int? = null,
    val student_id: String? = null,
    val name: String? = null,
    val roll: String? = null,
    val reason: String,
    val status: String? = null,
    val time: String? = null
)

data class ResolveActionRequest(val action: String)
data class HistoryRecord(
    val date: String,
    val status: String,
    val time_in: String,
    val time_out: String
)

// ─── Retrofit Interface ──────────────────────────────────────────────────────

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") bearerToken: String): UserProfile

    @POST("attendance/scan")
    suspend fun scanQr(@Header("Authorization") bearerToken: String, @Body request: ScanRequest): ScanResponse

    @POST("attendance/geofence-alert")
    suspend fun sendGeofenceAlert(@Header("Authorization") bearerToken: String, @Body request: GeofenceRequest): GeofenceResponse

    @POST("attendance/exit-requests")
    suspend fun submitExitRequest(@Header("Authorization") bearerToken: String, @Body request: ExitRequest): ExitResponse

    @GET("attendance/history/me")
    suspend fun getMyHistory(@Header("Authorization") bearerToken: String): List<HistoryRecord>

    @GET("attendance/exit-requests/pending")
    suspend fun getPendingExitRequests(@Header("Authorization") bearerToken: String): List<ExitRequestDto>

    @POST("attendance/exit-requests/{id}/resolve")
    suspend fun resolveExitRequest(@Header("Authorization") bearerToken: String, @retrofit2.http.Path("id") reqId: String, @Body action: ResolveActionRequest): ExitResponse

    @GET("attendance/exit-requests/me")
    suspend fun getMyExitRequests(@Header("Authorization") bearerToken: String): List<ExitRequestDto>
}
