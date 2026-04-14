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

    var records  by remember { mutableStateOf<List<HistoryRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error    by remember { mutableStateOf<String?>(null) }

    // Derived stats
    val totalDays    = records.size
    val completedDays = records.count { it.status == "COMPLETED" }
    val pct = if (totalDays > 0) (completedDays * 100f / totalDays).toInt() else 0

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            AttendanceRepository(context).getMyHistory()
        }
        result.onSuccess { records = it }
              .onFailure { error = it.message }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Attendance History", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Your personal record", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
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

                records.isEmpty() -> {
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
                        "  ${records.size} Records",
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
                        itemsIndexed(records) { index, record ->
                            HistoryCard(record = record, index = index)
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
