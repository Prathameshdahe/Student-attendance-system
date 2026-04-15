package com.smartattendance.smartattendance.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartattendance.smartattendance.data.remote.ExitRequestDto
import com.smartattendance.smartattendance.data.remote.GeofenceEventDto
import com.smartattendance.smartattendance.data.remote.HistoryRecord
import com.smartattendance.smartattendance.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Indigo      = Color(0xFF6366F1)
private val IndigoLight = Color(0xFFEEF2FF)
private val GreenColor  = Color(0xFF10B981)
private val GreenLight2 = Color(0xFFD1FAE5)
private val AmberColor  = Color(0xFFF59E0B)
private val AmberLight2 = Color(0xFFFEF3C7)
private val Gray100     = Color(0xFFF8F7FF)
private val Gray500     = Color(0xFF6B7280)
private val Gray900     = Color(0xFF111827)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { AttendanceRepository(context) }

    var records  by remember { mutableStateOf<List<HistoryRecord>>(emptyList()) }
    var requestHistory by remember { mutableStateOf<List<ExitRequestDto>>(emptyList()) }
    var geofenceHistory by remember { mutableStateOf<List<GeofenceEventDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error    by remember { mutableStateOf<String?>(null) }

    // Derived stats
    val totalDays    = records.size
    val completedDays = records.count { it.status == "COMPLETED" }
    val pct = if (totalDays > 0) (completedDays * 100f / totalDays).toInt() else 0

    LaunchedEffect(Unit) {
        val attendanceResult = withContext(Dispatchers.IO) { repo.getMyHistory() }
        val requestResult = withContext(Dispatchers.IO) { repo.getMyExitRequests() }
        val geofenceResult = withContext(Dispatchers.IO) { repo.getMyGeofenceEvents() }

        attendanceResult.onSuccess { records = it }
            .onFailure { error = it.message }
        requestResult.onSuccess { requestHistory = it }
        geofenceResult.onSuccess { geofenceHistory = it }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Activity History", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Attendance, requests, and geofence activity", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // ── Summary Header Card ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn(tween(400)) + expandVertically(tween(400))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 16.dp, 20.dp, 0.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(Indigo, Color(0xFF818CF8)))
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Overall Attendance",
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$pct%",
                                color = Color.White,
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(8.dp))
                            // Mini progress bar
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(pct / 100f)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "$completedDays complete days out of $totalDays scanned",
                                color = Color.White.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        // Circular %
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$pct",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Records or States ───────────────────────────────────────────────
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = Indigo, modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
                            Text("Loading your history…", color = Gray500, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                error != null -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.WifiOff, null, tint = AmberColor, modifier = Modifier.size(48.dp))
                            Text("Couldn't load history", fontWeight = FontWeight.Bold, color = Gray900)
                            Text(error ?: "Unknown error", color = Gray500, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }

                records.isEmpty() && requestHistory.isEmpty() && geofenceHistory.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = IndigoLight, modifier = Modifier.size(80.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.EventNote, null, tint = Indigo, modifier = Modifier.size(40.dp))
                                }
                            }
                            Text("No records yet", fontWeight = FontWeight.Bold, color = Gray900, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Your attendance history will appear here once you've been scanned in by a faculty member.",
                                color = Gray500, style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        "  Your latest activity",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Gray500, fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (records.isNotEmpty()) {
                            item {
                                SectionHeader("Attendance")
                            }
                            itemsIndexed(records) { index, record ->
                                HistoryCard(record = record, index = index)
                            }
                        }
                        item {
                            SectionHeader("Exit Requests")
                        }
                        if (requestHistory.isEmpty()) {
                            item { EmptySectionCard("No exit requests submitted yet.") }
                        } else {
                            itemsIndexed(requestHistory) { _, request ->
                                ExitRequestHistoryCard(request = request)
                            }
                        }
                        item {
                            SectionHeader("Geofence Activity")
                        }
                        if (geofenceHistory.isEmpty()) {
                            item { EmptySectionCard("No geofence events recorded yet.") }
                        } else {
                            itemsIndexed(geofenceHistory) { _, event ->
                                GeofenceHistoryCard(event = event)
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(record: HistoryRecord, index: Int) {
    val isCompleted = record.status == "COMPLETED"
    val isIn        = record.status == "IN"

    val accent = when {
        isCompleted -> GreenColor
        isIn        -> Indigo
        else        -> AmberColor
    }
    val accentBg = when {
        isCompleted -> GreenLight2
        isIn        -> IndigoLight
        else        -> AmberLight2
    }
    val label = when {
        isCompleted -> "Complete"
        isIn        -> "Checked In"
        else        -> record.status
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date bubble
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentBg,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val parts = record.date.split("-")
                        Text(
                            if (parts.size == 3) parts[2] else "—",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = accent, fontSize = 16.sp)
                        )
                        Text(
                            if (parts.size == 3) monthShort(parts[1].toIntOrNull() ?: 1) else "",
                            style = MaterialTheme.typography.labelSmall.copy(color = accent.copy(alpha = 0.7f), fontSize = 9.sp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.date,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Gray900)
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TimeChip(label = "In", time = record.time_in, color = Indigo)
                    TimeChip(label = "Out", time = record.time_out, color = GreenColor)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentBg
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = accent, fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Gray900),
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun EmptySectionCard(message: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall.copy(color = Gray500),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ExitRequestHistoryCard(request: ExitRequestDto) {
    val accent = when (request.status) {
        "APPROVED" -> GreenColor
        "DENIED" -> MaterialTheme.colorScheme.error
        else -> AmberColor
    }
    val accentBg = when (request.status) {
        "APPROVED" -> GreenLight2
        "DENIED" -> MaterialTheme.colorScheme.errorContainer
        else -> AmberLight2
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(request.reason, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Surface(shape = RoundedCornerShape(8.dp), color = accentBg) {
                    Text(
                        request.status_label ?: request.status ?: "PENDING",
                        style = MaterialTheme.typography.labelSmall.copy(color = accent, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Submitted: ${request.date ?: "--"} • ${request.time ?: "--"}",
                style = MaterialTheme.typography.bodySmall.copy(color = Gray500)
            )
            if (!request.resolved_by_name.isNullOrBlank() || !request.resolution_time.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Faculty: ${request.resolved_by_name ?: "Faculty"} • ${request.resolution_time ?: "--"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Gray500)
                )
            }
        }
    }
}

@Composable
private fun GeofenceHistoryCard(event: GeofenceEventDto) {
    val accent = if (event.event_type == "EXIT") AmberColor else Indigo
    val accentBg = if (event.event_type == "EXIT") AmberLight2 else IndigoLight

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = accentBg, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (event.event_type == "EXIT") Icons.Filled.DirectionsWalk else Icons.Filled.LocationOn,
                        null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.note ?: event.event_type,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Gray900)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${event.date ?: "--"} • ${event.time ?: "--"}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Gray500)
                )
            }
        }
    }
}

@Composable
private fun TimeChip(label: String, time: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(color.copy(alpha = 0.5f)))
        Text(
            "$label: $time",
            style = MaterialTheme.typography.labelSmall.copy(color = Gray500, fontSize = 10.sp)
        )
    }
}

private fun monthShort(month: Int): String = listOf(
    "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
).getOrElse(month) { "—" }
