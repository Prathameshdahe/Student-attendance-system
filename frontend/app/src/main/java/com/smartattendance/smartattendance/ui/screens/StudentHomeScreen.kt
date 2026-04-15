package com.smartattendance.smartattendance.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
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
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.remote.ExitRequestDto
import com.smartattendance.smartattendance.data.remote.GeofenceEventDto
import com.smartattendance.smartattendance.data.repository.AttendanceRepository
import com.smartattendance.smartattendance.service.GeofenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(onLogout: () -> Unit, onViewHistory: () -> Unit = {}) {
    val context = LocalContext.current
    val repo = remember(context) { AttendanceRepository(context) }
    val geofenceManager = remember(context) { GeofenceManager(context) }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayDisplay = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())

    var studentName by remember { mutableStateOf("...") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var latestRequest by remember { mutableStateOf<ExitRequestDto?>(null) }
    var recentGeofenceEvents by remember { mutableStateOf<List<GeofenceEventDto>>(emptyList()) }
    var geofenceEnabled by remember { mutableStateOf(false) }
    val hasPendingReq = latestRequest?.status == "PENDING"
    var showExitDialog by remember { mutableStateOf(false) }
    var exitReason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        geofenceEnabled =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (geofenceEnabled) {
            geofenceManager.startGeofencing()
        }
    }

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
        val (name, token, hasLocationPermission) = withContext(Dispatchers.IO) {
            val session = SessionManager(context)
            Triple(
                session.getUserName() ?: "Student",
                repo.generateQrToken(),
                hasLocationPermission(context)
            )
        }
        studentName = name
        qrBitmap = generateQrBitmap(token)
        geofenceEnabled = hasLocationPermission
        if (hasLocationPermission) {
            geofenceManager.startGeofencing()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val reqs = repo.getMyExitRequests().getOrNull()
                if (!reqs.isNullOrEmpty()) {
                    // Prioritise PENDING over everything else — then show latest
                    val pending = reqs.firstOrNull { it.status == "PENDING" }
                    latestRequest = pending ?: reqs.first()
                } else {
                    latestRequest = null
                }
                repo.getMyGeofenceEvents().getOrNull()?.let { recentGeofenceEvents = it }
            } catch (e: Exception) {}
            delay(5000)
        }
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

            Spacer(Modifier.height(12.dp))
            StudentStatusBanner(latestRequest = latestRequest, geofenceEnabled = geofenceEnabled)
            Spacer(Modifier.height(16.dp))
            
            // ── Stat row ──────────────────────────────────────────────────

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KiwiStatCard("Request", latestRequest?.status.orEmpty().ifBlank { "Clear" }, Modifier.weight(1f))
                KiwiStatCard("Today", SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()), Modifier.weight(1f))
                KiwiStatCard("Alerts", recentGeofenceEvents.count { it.event_type == "EXIT" }.toString(), Modifier.weight(1f))
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

            Spacer(Modifier.height(20.dp))

            // ── Exit Pass Success Banner ─────────────────────────────
            AnimatedVisibility(
                visible = showSuccess,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit  = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD1FAE5)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                        Text(
                            "Exit pass submitted! Awaiting faculty approval.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (showSuccess) 16.dp else 0.dp))

            // ── Exit Pass Button ─────────────────────────────────────
            Button(
                onClick = { if (!hasPendingReq) showExitDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !hasPendingReq,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasPendingReq) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                    contentColor   = Color.White,
                    disabledContainerColor = Color(0xFFF59E0B).copy(alpha = 0.6f),
                    disabledContentColor   = Color.White
                )
            ) {
                Icon(
                    if (hasPendingReq) Icons.Filled.PendingActions else Icons.Filled.DirectionsWalk,
                    null, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasPendingReq) "Exit Pass Pending…" else "Request Campus Exit Pass",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))

            // ── View History Button ───────────────────────────────────
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.History, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Full History", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))
            RequestPreviewCard(request = latestRequest)
            Spacer(Modifier.height(12.dp))
            GeofencePreviewCard(events = recentGeofenceEvents, geofenceEnabled = geofenceEnabled)
            Spacer(Modifier.height(32.dp))
        }

        // ── Exit Request Dialog ────────────────────────────────────────────
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSubmitting) showExitDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.DirectionsWalk, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Text("Request Exit Pass")
                    }
                },
                text = {
                    Column {
                        Text(
                            "Please specify your reason for leaving the campus.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = exitReason,
                            onValueChange = { exitReason = it },
                            label = { Text("Reason (e.g., Medical, Lunch)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmitting
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (exitReason.isNotBlank()) {
                                coroutineScope.launch {
                                    isSubmitting = true
                                    val reason = exitReason.trim()
                                    repo.submitExitRequest(reason).onSuccess {
                                        latestRequest = ExitRequestDto(
                                            reason = reason,
                                            status = "PENDING",
                                            status_label = "Awaiting faculty approval",
                                            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                        )
                                        showExitDialog = false
                                        exitReason     = ""
                                        showSuccess    = true
                                        kotlinx.coroutines.delay(4000); showSuccess = false
                                    }.onFailure {
                                        android.widget.Toast.makeText(context, it.message ?: "Request failed", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isSubmitting && exitReason.isNotBlank()
                    ) {
                        if (isSubmitting)
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text("Submit Request")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }, enabled = !isSubmitting) {
                        Text("Cancel")
                    }
                }
            )
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

@Composable
private fun RequestPreviewCard(request: ExitRequestDto?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Latest Request", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(6.dp))
            if (request == null) {
                Text(
                    "Your latest faculty decision will appear here.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            } else {
                Text(request.reason, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(4.dp))
                Text(
                    "${request.status_label ?: request.status ?: "PENDING"} • ${request.time ?: "--"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                if (!request.resolved_by_name.isNullOrBlank() || !request.resolution_time.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Faculty: ${request.resolved_by_name ?: "Faculty"} • ${request.resolution_time ?: "--"}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun GeofencePreviewCard(events: List<GeofenceEventDto>, geofenceEnabled: Boolean) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Campus Boundary Activity", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(6.dp))
            when {
                !geofenceEnabled -> {
                    Text(
                        "Allow location access so exit and return events are tracked automatically.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                events.isEmpty() -> {
                    Text(
                        "No geofence movement has been recorded yet.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                else -> {
                    events.take(3).forEach { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    event.note ?: event.event_type,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    event.date ?: "",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Text(
                                event.time ?: "--",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
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

@Composable
private fun StudentStatusBanner(latestRequest: ExitRequestDto?, geofenceEnabled: Boolean) {
    val (icon, label, bg, fg) = when (latestRequest?.status) {
        "PENDING" -> listOf(Icons.Filled.PendingActions, "Exit request is pending faculty approval", Color(0xFFFEF3C7), Color(0xFFF59E0B))
        "APPROVED" -> listOf(Icons.Filled.CheckCircle, "Exit request approved${latestRequest.resolution_time?.let { " • $it" } ?: ""}", Color(0xFFD1FAE5), Color(0xFF10B981))
        "DENIED" -> listOf(Icons.Filled.Cancel, "Exit request denied${latestRequest.resolution_time?.let { " • $it" } ?: ""}", Color(0xFFFEE2E2), Color(0xFFEF4444))
        else -> if (geofenceEnabled) {
            listOf(Icons.Filled.LocationOn, "Campus boundary monitoring is active", Color(0xFFE0E7FF), Color(0xFF6366F1))
        } else {
            listOf(Icons.Filled.LocationOn, "Allow location access to enable geofence history", Color(0xFFF3F4F6), Color(0xFF6B7280))
        }
    }

    @Suppress("UNCHECKED_CAST")
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        color = bg as Color
    ) {
        Row(
            modifier = Modifier.padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, null, tint = fg as Color, modifier = Modifier.size(18.dp))
            Text(label as String, style = MaterialTheme.typography.bodySmall.copy(color = fg, fontWeight = FontWeight.SemiBold))
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun StatusBanner(latestRequest: ExitRequestDto?, geofenceEnabled: Boolean) {
    val (icon, label, bg, fg) = when (latestRequest?.status) {
        "PENDING" -> listOf(Icons.Filled.PendingActions, "Exit Pass Pending — Awaiting", Color(0xFFFEF3C7), Color(0xFFF59E0B))
        "APPROVED" -> listOf(Icons.Filled.CheckCircle, "Exit Pass APPROVED — Clear to leave", Color(0xFFD1FAE5), Color(0xFF10B981))
        "DENIED" -> listOf(Icons.Filled.Cancel, "Exit Pass DENIED — See faculty", Color(0xFFFEE2E2), Color(0xFFEF4444))
        else -> listOf(Icons.Filled.LocationOn, "You're on campus  •  Geofence Active", Color(0xFFE0E7FF), Color(0xFF6366F1))
    }

    @Suppress("UNCHECKED_CAST")
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        color = bg as Color
    ) {
        Row(
            modifier = Modifier.padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, null, tint = fg as Color, modifier = Modifier.size(18.dp))
            Text(label as String, style = MaterialTheme.typography.bodySmall.copy(color = fg, fontWeight = FontWeight.SemiBold))
        }
    }
}
