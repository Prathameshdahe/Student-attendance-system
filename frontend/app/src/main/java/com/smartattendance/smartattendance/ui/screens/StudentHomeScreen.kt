package com.smartattendance.smartattendance.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayDisplay = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())

    var studentName by remember { mutableStateOf("...") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        val (name, token) = withContext(Dispatchers.IO) {
            val session = SessionManager(context)
            val repo = AttendanceRepository(context)
            Pair(session.getUserName() ?: "Student", repo.generateQrToken())
        }
        studentName = name
        qrBitmap = generateQrBitmap(token)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Good ${getGreeting()}, ${studentName.split(" ").firstOrNull() ?: studentName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            todayDisplay,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Stat row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KiwiStatCard("Status", "Present", Modifier.weight(1f))
                KiwiStatCard("Today", SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()), Modifier.weight(1f))
                KiwiStatCard("Geofence", "Active", Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            // ── QR card ───────────────────────────────────────────────────
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Daily Attendance QR",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        "Show this to your faculty to mark attendance",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    if (qrBitmap != null) {
                        Surface(
                            modifier = Modifier
                                .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha * 0.3f)
                        ) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "Attendance QR Code",
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.size(224.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Status indicator
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Valid for $today  •  Resets at midnight",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun KiwiStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
            )
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        val bmp = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
        for (x in 0 until bitMatrix.width)
            for (y in 0 until bitMatrix.height)
                bmp.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        bmp
    } catch (e: Exception) { null }
}
