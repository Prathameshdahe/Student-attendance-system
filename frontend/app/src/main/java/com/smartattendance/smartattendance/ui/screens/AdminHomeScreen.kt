package com.smartattendance.smartattendance.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartattendance.smartattendance.ui.theme.BVPMaroon
import com.smartattendance.smartattendance.ui.theme.BVPNavy
import com.smartattendance.smartattendance.ui.theme.SuccessGreen
import com.smartattendance.smartattendance.ui.theme.ErrorRed
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class ActivityLogItem(
    val studentName: String,
    val rollNumber: String,
    val action: String, // "CHECK_IN" or "CHECK_OUT" or "UNAUTHORIZED_EXIT"
    val time: String,
    val status: String  // "SUCCESS" or "WARNING" or "ALERT"
)

@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    onScanQr: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var adminName by remember { mutableStateOf("Admin") }
    var presentCount by remember { mutableStateOf(0) }
    var totalStudents by remember { mutableStateOf(0) }
    var activityLog by remember { mutableStateOf<List<ActivityLogItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val adminDoc = firestore.collection("users").document(uid).get().await()
            adminName = adminDoc.getString("name") ?: "Admin"

            // Total student count
            val allStudents = firestore.collection("users")
                .whereEqualTo("role", "STUDENT").get().await()
            totalStudents = allStudents.size()

            // Today's attendance logs
            val logs = firestore.collection("attendance_logs")
                .whereEqualTo("date", today)
                .get().await()

            presentCount = logs.documents.count { it.getString("status") == "PRESENT" }

            // Build activity log from logs
            val items = mutableListOf<ActivityLogItem>()
            for (doc in logs.documents) {
                val studentId = doc.getString("student_id") ?: continue
                val studentDoc = firestore.collection("users").document(studentId).get().await()
                val name = studentDoc.getString("name") ?: "Unknown"
                val roll = studentDoc.getString("roll_number") ?: ""

                val checkIn = doc.getLong("check_in")
                val checkOut = doc.getLong("check_out")

                if (checkIn != null) {
                    items.add(
                        ActivityLogItem(
                            studentName = name,
                            rollNumber = roll,
                            action = "Check In",
                            time = formatTimestamp(checkIn),
                            status = "SUCCESS"
                        )
                    )
                }
                if (checkOut != null) {
                    items.add(
                        ActivityLogItem(
                            studentName = name,
                            rollNumber = roll,
                            action = "Check Out",
                            time = formatTimestamp(checkOut),
                            status = "SUCCESS"
                        )
                    )
                }
            }

            activityLog = items.sortedByDescending { it.time }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanQr,
                icon = {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = Color.White
                    )
                },
                text = { Text("Scan QR", color = Color.White) },
                containerColor = BVPMaroon,
                shape = RoundedCornerShape(12.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(padding)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BVPNavy)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Column {
                    Text(
                        text = "Good ${getGreeting()},",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = adminName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Admin • ${SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date())}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                IconButton(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Summary Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Present",
                    value = "$presentCount",
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Absent",
                    value = "${totalStudents - presentCount}",
                    color = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Total",
                    value = "$totalStudents",
                    color = BVPNavy,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Activity Log Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Today's Activity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BVPNavy
                    )
                )
                Text(
                    text = "${activityLog.size} events",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BVPMaroon)
                }
            } else if (activityLog.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = BVPNavy.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No activity yet today",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activityLog) { item ->
                        ActivityCard(item)
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) } // space for FAB
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    fontSize = 22.sp
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun ActivityCard(item: ActivityLogItem) {
    val accentColor = when (item.status) {
        "SUCCESS" -> SuccessGreen
        "WARNING" -> Color(0xFFFFA000)
        else -> ErrorRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Name and action
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.studentName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = BVPNavy
                    )
                )
                Text(
                    text = "${item.action}  •  Roll: ${item.rollNumber}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }

            // Time
            Text(
                text = item.time,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            )
        }
    }
}

fun formatTimestamp(millis: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))
}
