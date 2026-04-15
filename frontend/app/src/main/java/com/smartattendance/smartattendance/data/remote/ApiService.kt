package com.smartattendance.smartattendance.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

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

data class GeofenceEventRequest(
    val transition_type: String,
    val timestamp: Long,
    val date: String
)

data class GeofenceEventResponse(
    val status: String,
    val message: String
)

data class ExitRequestDto(
    val id: String? = null,
    val request_id: String? = null,
    val student_id: String? = null,
    val name: String? = null,
    val roll: String? = null,
    val reason: String,
    val status: String? = null,
    val status_label: String? = null,
    val date: String? = null,
    val time: String? = null,
    val created_at: String? = null,
    val resolved_at: String? = null,
    val resolution_time: String? = null,
    val resolved_by_name: String? = null,
    val left_campus_at: String? = null,
    val left_campus_time: String? = null,
    val returned_campus_at: String? = null,
    val returned_campus_time: String? = null
)

data class ResolveActionRequest(val action: String)

data class HistoryRecord(
    val date: String,
    val status: String,
    val time_in: String,
    val time_out: String
)

data class LiveAttendanceDto(
    val id: String,
    val student_id: String,
    val name: String,
    val roll: String? = null,
    val status: String,
    val time_in: String? = null,
    val time_out: String? = null
)

data class GeofenceEventDto(
    val id: String,
    val student_id: String? = null,
    val name: String? = null,
    val roll: String? = null,
    val request_id: String? = null,
    val event_type: String,
    val permission_status: String? = null,
    val source_type: String? = null,
    val reason: String? = null,
    val note: String? = null,
    val time: String? = null,
    val date: String? = null,
    val created_at: String? = null
)

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") bearerToken: String): UserProfile

    @POST("attendance/scan")
    suspend fun scanQr(@Header("Authorization") bearerToken: String, @Body request: ScanRequest): ScanResponse

    @POST("attendance/geofence-alert")
    suspend fun sendGeofenceAlert(@Header("Authorization") bearerToken: String, @Body request: GeofenceRequest): GeofenceResponse

    @POST("attendance/geofence-events")
    suspend fun postGeofenceEvent(
        @Header("Authorization") token: String,
        @Body body: GeofenceEventRequest
    ): retrofit2.Response<GeofenceEventResponse>

    @POST("attendance/exit-requests")
    suspend fun submitExitRequest(@Header("Authorization") bearerToken: String, @Body request: ExitRequest): ExitResponse

    @GET("attendance/history/me")
    suspend fun getMyHistory(@Header("Authorization") bearerToken: String): List<HistoryRecord>

    @GET("attendance/live/today")
    suspend fun getLiveAttendanceToday(@Header("Authorization") bearerToken: String): List<LiveAttendanceDto>

    @GET("attendance/exit-requests/pending")
    suspend fun getPendingExitRequests(@Header("Authorization") bearerToken: String): List<ExitRequestDto>

    @POST("attendance/exit-requests/{id}/resolve")
    suspend fun resolveExitRequest(
        @Header("Authorization") bearerToken: String,
        @retrofit2.http.Path("id") reqId: String,
        @Body action: ResolveActionRequest
    ): ExitResponse

    @GET("attendance/exit-requests/me")
    suspend fun getMyExitRequests(
        @Header("Authorization") bearerToken: String,
        @Query("limit") limit: Int = 20
    ): List<ExitRequestDto>

    @GET("attendance/exit-requests/history")
    suspend fun getExitRequestHistory(
        @Header("Authorization") bearerToken: String,
        @Query("limit") limit: Int = 20
    ): List<ExitRequestDto>

    @GET("attendance/geofence-events")
    suspend fun getGeofenceEvents(
        @Header("Authorization") bearerToken: String,
        @Query("limit") limit: Int = 20
    ): List<GeofenceEventDto>

    @GET("attendance/geofence-events/me")
    suspend fun getMyGeofenceEvents(
        @Header("Authorization") bearerToken: String,
        @Query("limit") limit: Int = 20
    ): List<GeofenceEventDto>
}
