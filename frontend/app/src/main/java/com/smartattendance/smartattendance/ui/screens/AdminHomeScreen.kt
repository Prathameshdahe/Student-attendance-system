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
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.remote.ExitRequestDto
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
    var activityLog by remember { mutableStateOf<List<ActivityLogItem>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<ExitRequestDto>>(emptyList()) }

    LaunchedEffect("fetchAdmin") {
        adminName = withContext(Dispatchers.IO) {
            SessionManager(context).getUserName() ?: "Admin"
        }
    }

    LaunchedEffect("pollExitRequests") {
        val repo = AttendanceRepository(context)
        while(true) {
            try {
                val reqs = repo.getPendingExitRequests().getOrNull()
                if (reqs != null) {
                    pendingRequests = reqs
                }
            } catch (e: Exception) {}
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
                    
                    val repo = AttendanceRepository(context)
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
                KiwiStatCard("Present", "${activityLog.count { it.action == "Check In" }}", Modifier.weight(1f))
                KiwiStatCard("Absent", "—", Modifier.weight(1f))
                KiwiStatCard("Scanned", "${activityLog.size}", Modifier.weight(1f))
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
                        Spacer(Modifier.height(4.dp))
                    }
                    items(pendingRequests) { req ->
                        ExitRequestCard(
                            req = req,
                            onResolve = { action ->
                                scope.launch {
                                    try {
                                        val reqId = (req.request_id ?: req.id ?: 0).toString()
                                        AttendanceRepository(context).resolveExitRequest(reqId, action)
                                        pendingRequests = pendingRequests.filter {
                                            (it.request_id ?: it.id) != (req.request_id ?: req.id)
                                        }
                                    } catch (e: Exception) {}
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
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

fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))


@Composable
fun ExitRequestCard(
    req: com.smartattendance.smartattendance.data.remote.ExitRequestDto,
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
            Text("Roll: ${req.roll}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
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
                TextButton(onClick = { onResolve("DENY") }) {
                    Text("Deny", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onResolve("APPROVE") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                    Text("Approve", color = Color.White)
                }
            }
        }
    }
}
