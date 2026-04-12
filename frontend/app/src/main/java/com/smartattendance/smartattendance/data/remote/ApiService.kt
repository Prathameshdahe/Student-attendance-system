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

// ─── Retrofit Interface ──────────────────────────────────────────────────────

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") bearerToken: String): UserProfile

    @POST("attendance/scan")
    suspend fun scanQr(@Header("Authorization") bearerToken: String, @Body request: ScanRequest): ScanResponse
}
