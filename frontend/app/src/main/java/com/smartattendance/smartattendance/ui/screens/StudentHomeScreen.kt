package com.smartattendance.smartattendance.ui.screens

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.smartattendance.smartattendance.ui.theme.BVPMaroon
import com.smartattendance.smartattendance.ui.theme.BVPNavy
import com.smartattendance.smartattendance.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StudentHomeScreen(
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var studentName by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var attendancePercent by remember { mutableStateOf(0) }
    var checkInStatus by remember { mutableStateOf("Not Checked In") }
    var qrToken by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Pulsing animation for QR ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qr_pulse"
    )

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        try {
            // Fetch student profile
            val userDoc = firestore.collection("users").document(uid).get().await()
            studentName = userDoc.getString("name") ?: "Student"
            rollNumber = userDoc.getString("roll_number") ?: ""

            // Fetch today's QR token from Firestore (written by Python backend daily)
            val tokenDoc = firestore.collection("qr_tokens").document(today).get().await()
            val token = tokenDoc.getString("token") ?: ""
            qrToken = "${uid}::${token}" // Embed student UID + daily token to make it unique

            // Generate QR bitmap
            if (qrToken.isNotBlank()) {
                qrBitmap = generateQrBitmap(qrToken)
            }

            // Fetch check-in status
            val attendanceDoc = firestore.collection("attendance_logs")
                .document("${uid}_${today}").get().await()
            if (attendanceDoc.exists()) {
                checkInStatus = "Checked In ✓"
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Exit request dialog */ },
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Request Exit",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                },
                text = {
                    Text(
                        "Request Exit",
                        color = androidx.compose.ui.graphics.Color.White
                    )
                },
                containerColor = BVPMaroon,
                shape = RoundedCornerShape(12.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFFF8F9FA))
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
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
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = if (studentName.isNotBlank()) studentName else "Loading...",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    )
                    if (rollNumber.isNotBlank()) {
                        Text(
                            text = "Roll No: $rollNumber",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
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
                        tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Status",
                    value = if (checkInStatus == "Checked In ✓") "Present" else "Absent",
                    valueColor = if (checkInStatus == "Checked In ✓") SuccessGreen else BVPMaroon,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Today",
                    value = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Geofence",
                    value = "Active",
                    valueColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // QR Code Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.QrCode,
                            contentDescription = null,
                            tint = BVPMaroon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your Daily QR Code",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BVPNavy
                            )
                        )
                    }

                    Text(
                        text = "Show this to your Admin to mark attendance",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = androidx.compose.ui.graphics.Color.Gray
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    if (isLoading) {
                        CircularProgressIndicator(color = BVPMaroon, modifier = Modifier.size(60.dp))
                    } else if (qrBitmap != null) {
                        // Pulsing QR ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .border(
                                    3.dp,
                                    BVPMaroon.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "Attendance QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    } else {
                        // Fallback: No token generated yet
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(
                                    BVPNavy.copy(alpha = 0.05f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.QrCode,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = BVPNavy.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "QR Not Ready",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BVPNavy.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live indicator dot
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Valid for ${today}  •  Refreshes at midnight",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = androidx.compose.ui.graphics.Color.Gray,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // space for FAB
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = BVPNavy,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
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
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    fontSize = 13.sp
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = androidx.compose.ui.graphics.Color.Gray,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

fun getGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Morning"
        in 12..16 -> "Afternoon"
        else -> "Evening"
    }
}

fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
