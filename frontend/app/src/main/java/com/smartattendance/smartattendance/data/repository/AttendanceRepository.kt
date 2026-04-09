package com.smartattendance.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartattendance.smartattendance.data.model.Attendance
import com.smartattendance.smartattendance.data.model.User
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    /** Called by Admin when they scan the student's QR code */
    suspend fun markCheckIn(studentId: String): Result<Unit> {
        return try {
            val docId = "${studentId}_${todayDate}"
            val existing = firestore.collection("attendance_logs")
                .document(docId).get().await()

            if (existing.exists()) {
                Result.failure(Exception("Student already checked in today"))
            } else {
                val log = Attendance(
                    id = docId,
                    studentId = studentId,
                    date = todayDate,
                    checkIn = System.currentTimeMillis(),
                    status = "PRESENT"
                )
                firestore.collection("attendance_logs").document(docId).set(log).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Called by Admin when they scan the student's QR code a second time */
    suspend fun markCheckOut(studentId: String): Result<Unit> {
        return try {
            val docId = "${studentId}_${todayDate}"
            firestore.collection("attendance_logs").document(docId)
                .update("check_out", System.currentTimeMillis()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fetches today's attendance log for a specific student */
    suspend fun getTodayLog(studentId: String): Result<Attendance?> {
        return try {
            val docId = "${studentId}_${todayDate}"
            val doc = firestore.collection("attendance_logs").document(docId).get().await()
            Result.success(doc.toObject(Attendance::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fetches all students currently checked in (for Admin dashboard) */
    suspend fun getPresentStudents(): Result<List<User>> {
        return try {
            // Get all attendance logs for today
            val logs = firestore.collection("attendance_logs")
                .whereEqualTo("date", todayDate)
                .whereEqualTo("status", "PRESENT")
                .get().await()

            val studentIds = logs.documents.mapNotNull { it.getString("student_id") }

            if (studentIds.isEmpty()) return Result.success(emptyList())

            // Batch fetch student data
            val students = studentIds.map { uid ->
                firestore.collection("users").document(uid).get().await()
                    .toObject(User::class.java)
            }.filterNotNull()

            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
