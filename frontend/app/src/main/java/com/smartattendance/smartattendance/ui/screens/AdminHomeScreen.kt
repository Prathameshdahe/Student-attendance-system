package com.smartattendance.smartattendance.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.smartattendance.smartattendance.data.local.DeviceIdentity
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.remote.ExitRequestDto
import com.smartattendance.smartattendance.data.remote.GeofenceEventDto
import com.smartattendance.smartattendance.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

data class ActivityLogItem(
    val studentName: String,
    val rollNumber: String,
    val action: String,
    val time: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    onScanQr: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val todayDisplay = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date())

    var adminName by remember { mutableStateOf("...") }
    val adminDeviceId = remember(context) { DeviceIdentity.getDeviceId(context) }
    val repo = remember(context) { AttendanceRepository(context) }
    var activityLog by remember { mutableStateOf<List<ActivityLogItem>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<ExitRequestDto>>(emptyList()) }
    var requestHistory by remember { mutableStateOf<List<ExitRequestDto>>(emptyList()) }
    var geofenceEvents by remember { mutableStateOf<List<GeofenceEventDto>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(true) }
    var resolvingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lastSeenGeofenceId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect("fetchAdmin") {
        adminName = withContext(Dispatchers.IO) {
            SessionManager(context).getUserName() ?: "Admin"
        }
    }

    suspend fun refreshDashboard() {
        isRefreshing = true
        val live = repo.getLiveAttendanceToday().getOrNull().orEmpty()
        pendingRequests = repo.getPendingExitRequests().getOrNull().orEmpty()
        requestHistory = repo.getExitRequestHistory().getOrNull().orEmpty()
        val latestGeofenceEvents = repo.getGeofenceEvents().getOrNull().orEmpty()
        val newestAlert = latestGeofenceEvents.firstOrNull { it.should_alert }
        if (lastSeenGeofenceId != null && newestAlert != null && newestAlert.id != lastSeenGeofenceId) {
            Toast.makeText(
                context,
                newestAlert.note ?: "Campus exit alert",
                Toast.LENGTH_LONG
            ).show()
        }
        lastSeenGeofenceId = newestAlert?.id ?: lastSeenGeofenceId
        geofenceEvents = latestGeofenceEvents
        activityLog = live.map { record ->
            ActivityLogItem(
                studentName = record.name,
                rollNumber = record.roll ?: "-",
                action = if (record.status == "IN") "On Campus" else "Checked Out",
                time = record.time_out ?: record.time_in ?: "--",
                status = if (record.status == "IN") "SUCCESS" else "WARNING"
            )
        }
        isRefreshing = false
    }

    LaunchedEffect("pollDashboard") {
        while (true) {
            try {
                refreshDashboard()
            } catch (_: Exception) {
                isRefreshing = false
            }
            delay(5000)
        }
    }
    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            scope.launch {
                try {
                    val parts = result.contents.split("::")
                    if (parts.size != 2) {
                        Toast.makeText(context, "Invalid QR Format", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val scanRes = repo.scanQr(result.contents)
                    
                    scanRes.onSuccess { data ->
                        when (data.status) {
                            "DUPLICATE" -> {
                                Toast.makeText(context, "⚠️ Student has already checked out today", Toast.LENGTH_LONG).show()
                            }
                            "INVALID" -> {
                                Toast.makeText(context, "❌ ${data.message}", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                val newAction = if (data.status == "CHECK_IN") "Check In" else "Check Out"
                                val newEntry = ActivityLogItem(
                                    studentName = data.student_name ?: "Unknown Student",
                                    rollNumber = data.roll ?: "—",
                                    action = newAction,
                                    time = data.time ?: SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                                    status = "SUCCESS"
                                )
                                activityLog = listOf(newEntry) + activityLog
                                Toast.makeText(context, "✅ ${data.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.onFailure { err ->
                        Toast.makeText(context, "Server Error: ${err.message}", Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(context, "Scan error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            adminName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Faculty  •  $todayDisplay",
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    barcodeLauncher.launch(
                        ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("Scan student's Daily QR Code")
                            setCameraId(0)
                            setBeepEnabled(true)
                            setBarcodeImageEnabled(false)
                        }
                    )
                },
                icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
                text = { Text("Scan QR", style = MaterialTheme.typography.labelLarge) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Summary chips row ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KiwiStatCard("Present", "${activityLog.count { it.action == "On Campus" }}", Modifier.weight(1f))
                KiwiStatCard("Alerts", "${geofenceEvents.count { it.should_alert }}", Modifier.weight(1f))
                KiwiStatCard("Scanned", "${activityLog.size}", Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Trusted Admin Device Key", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        adminDeviceId,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Use this value for ADMIN_ALLOWED_DEVICE_IDS in Render if you want to lock admin access to this Vivo phone.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Section header ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Today's Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        "${activityLog.size} events",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pendingRequests.isNotEmpty()) {
                    item {
                        Text("Pending Exit Requests", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    }
                    items(pendingRequests) { request ->
                        ExitRequestCard(
                            req = request,
                            isResolving = resolvingIds.contains(request.request_id ?: request.id.orEmpty()),
                            onResolve = { action ->
                                val requestId = request.request_id ?: request.id
                                if (!requestId.isNullOrBlank()) {
                                    scope.launch {
                                        resolvingIds = resolvingIds + requestId
                                        repo.resolveExitRequest(requestId, action)
                                        resolvingIds = resolvingIds - requestId
                                        refreshDashboard()
                                    }
                                }
                            }
                        )
                    }
                }

                if (geofenceEvents.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Recent Geofence Activity", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    }
                    items(geofenceEvents.take(5)) { event ->
                        GeofenceAlertCard(event = event)
                    }
                }

                if (requestHistory.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Recent Request Decisions", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    }
                    items(requestHistory.filter { it.status != "PENDING" }.take(5)) { request ->
                        ExitHistoryCard(request = request)
                    }
                }

                if (activityLog.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(72.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text("No scans yet", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Spacer(Modifier.height(4.dp))
                                Text("Tap the Scan QR button to mark attendance", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                        }
                    }
                } else {
                    item {
                        Text("Recent Check-Ins", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.height(4.dp))
                    }
                    items(activityLog) { item -> KiwiActivityCard(item) }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun KiwiActivityCard(item: ActivityLogItem) {
    val statusColor = when (item.status) {
        "SUCCESS" -> MaterialTheme.colorScheme.tertiary
        "WARNING" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    val statusBg = when (item.status) {
        "SUCCESS" -> MaterialTheme.colorScheme.tertiaryContainer
        "WARNING" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Surface(shape = CircleShape, color = statusBg, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.studentName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    "${item.action}  •  Roll: ${item.rollNumber}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    item.time,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ExitRequestCard(
    req: ExitRequestDto,
    isResolving: Boolean,
    onResolve: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(req.name ?: "Unknown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(req.time ?: "", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
            Text("Roll: ${req.roll ?: "-"}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            Spacer(Modifier.height(6.dp))
            Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(8.dp)) {
                Text(
                    req.reason,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB45309)),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onResolve("DENY") }, enabled = !isResolving) {
                    Text("Deny", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onResolve("APPROVE") },
                    enabled = !isResolving,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text(if (isResolving) "Saving..." else "Approve", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun GeofenceAlertCard(event: GeofenceEventDto) {
    val accent = when {
        event.should_alert && event.alert_level == "critical" -> Color(0xFFEF4444)
        event.should_alert -> Color(0xFFF59E0B)
        event.event_type == "RETURN" -> Color(0xFF6366F1)
        else -> Color(0xFF10B981)
    }
    val chipColor = when {
        event.should_alert && event.alert_level == "critical" -> Color(0xFFFEE2E2)
        event.should_alert -> Color(0xFFFEF3C7)
        event.event_type == "RETURN" -> Color(0xFFE0E7FF)
        else -> Color(0xFFD1FAE5)
    }
    val eventLabel = when {
        event.should_alert && event.alert_level == "critical" -> "Denied Exit"
        event.should_alert -> "Unauthorized Exit"
        event.event_type == "RETURN" -> "Returned"
        event.permission_status == "APPROVED" -> "Approved Exit"
        event.permission_status == "PENDING" -> "Pending Request"
        else -> event.event_type
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = chipColor, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (event.event_type == "EXIT") Icons.AutoMirrored.Filled.DirectionsWalk else Icons.Filled.LocationOn,
                        null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.name ?: "Student", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(event.note ?: event.event_type, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                val metaParts = buildList {
                    event.date?.takeIf { it.isNotBlank() }?.let { add(it) }
                    event.time?.takeIf { it.isNotBlank() }?.let { add(it) }
                    event.network_type?.takeIf { it.isNotBlank() }?.let { add(it) }
                    event.distance_from_center_meters?.let { add("${it.toInt()} m from center") }
                }
                if (metaParts.isNotEmpty()) {
                    Text(
                        metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
            Text(eventLabel, style = MaterialTheme.typography.labelSmall.copy(color = accent, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun ExitHistoryCard(request: ExitRequestDto) {
    val accent = if (request.status == "APPROVED") Color(0xFF10B981) else MaterialTheme.colorScheme.error
    val background = if (request.status == "APPROVED") Color(0xFFD1FAE5) else MaterialTheme.colorScheme.errorContainer
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(request.name ?: "Student", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Surface(shape = RoundedCornerShape(8.dp), color = background) {
                    Text(
                        request.status_label ?: request.status ?: "-",
                        style = MaterialTheme.typography.labelSmall.copy(color = accent, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(request.reason, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            Text(
                "${request.time ?: "--"}${request.resolution_time?.let { " • $it" } ?: ""}",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))


