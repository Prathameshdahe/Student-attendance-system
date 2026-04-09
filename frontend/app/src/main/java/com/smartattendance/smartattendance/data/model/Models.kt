package com.smartattendance.smartattendance.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val name: String = "",
    @get:PropertyName("roll_number") @set:PropertyName("roll_number")
    var rollNumber: String = "",
    val email: String = "",
    val role: String = "STUDENT", // STUDENT or ADMIN
    @get:PropertyName("face_embedding") @set:PropertyName("face_embedding")
    var faceEmbedding: List<Double>? = null
)

data class Attendance(
    val id: String = "",
    @get:PropertyName("student_id") @set:PropertyName("student_id")
    var studentId: String = "",
    val date: String = "", // yyyy-MM-dd
    @get:PropertyName("check_in") @set:PropertyName("check_in")
    var checkIn: Long? = null,
    @get:PropertyName("check_out") @set:PropertyName("check_out")
    var checkOut: Long? = null,
    val status: String = "ABSENT" // PRESENT, ABSENT, PARTIAL
)

data class ExitRequest(
    val id: String = "",
    @get:PropertyName("student_id") @set:PropertyName("student_id")
    var studentId: String = "",
    val reason: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, DENIED
    @get:PropertyName("requested_at") @set:PropertyName("requested_at")
    var requestedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("exit_time") @set:PropertyName("exit_time")
    var exitTime: Long? = null,
    @get:PropertyName("return_time") @set:PropertyName("return_time")
    var returnTime: Long? = null
)
