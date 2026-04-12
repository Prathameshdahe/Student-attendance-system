package com.smartattendance.smartattendance.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val rollNumber: String = "",
    val email: String = "",
    val role: String = "STUDENT" // STUDENT or ADMIN
)

data class Attendance(
    val id: String = "",
    val studentId: String = "",
    val date: String = "",       // yyyy-MM-dd
    val checkIn: Long? = null,
    val checkOut: Long? = null,
    val status: String = "ABSENT" // PRESENT, ABSENT, PARTIAL
)

data class ExitRequest(
    val id: String = "",
    val studentId: String = "",
    val reason: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, DENIED
    val requestedAt: Long = System.currentTimeMillis(),
    val exitTime: Long? = null,
    val returnTime: Long? = null
)
