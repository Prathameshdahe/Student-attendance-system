import re

path = r'c:\Users\DELL\Desktop\smartattendance\frontend\app\src\main\java\com\smartattendance\smartattendance\ui\screens\StudentHomeScreen.kt'

with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# 1. State substitutions
text = text.replace(
    '    var geofenceReady by remember { mutableStateOf(false) }',
    '    var geofenceReady by remember { mutableStateOf(false) }\n    var exitStatus by remember { mutableStateOf<String?>(null) }\n    val hasPendingReq = exitStatus == "PENDING"\n    var showExitDialog by remember { mutableStateOf(false) }\n    var exitReason by remember { mutableStateOf("") }\n    var isSubmitting by remember { mutableStateOf(false) }\n    var showSuccess by remember { mutableStateOf(false) }'
)

# 2. Add polling effect
polling_block = """
        if (allGranted) { geofenceManager.startGeofencing(); geofenceReady = true }
        else permissionLauncher.launch(permissionsNeeded.toTypedArray())
    }

    LaunchedEffect(Unit) {
        val repo = AttendanceRepository(context)
        while(true) {
            try {
                val reqs = repo.getMyExitRequests().getOrNull()
                if (!reqs.isNullOrEmpty()) {
                    exitStatus = reqs.first().status?.replace("D", "D")
                } else {
                    exitStatus = null
                }
            } catch (e: Exception) {}
            kotlinx.coroutines.delay(5000)
        }
    }
"""
text = text.replace(
    '        if (allGranted) { geofenceManager.startGeofencing(); geofenceReady = true }\n        else permissionLauncher.launch(permissionsNeeded.toTypedArray())\n    }',
    polling_block
)

# 3. Inject View Attendance and Exit Request Buttons
# We find "Spacer(Modifier.height(32.dp))\n        }\n    }\n}"

missing_ui = """            Spacer(Modifier.height(20.dp))

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
                Text("View Attendance History", fontWeight = FontWeight.SemiBold)
            }

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
                                    val repo = AttendanceRepository(context)
                                    repo.submitExitRequest(exitReason.trim()).onSuccess {
                                        exitStatus = "PENDING"
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
"""

text = re.sub(r'            Spacer\(Modifier\.height\(32\.dp\)\)\s+\}\s+\}\s+\}', missing_ui, text)

# Status banner injection
banner_ui = """
            Spacer(Modifier.height(12.dp))
            StatusBanner(exitStatus = exitStatus)
            Spacer(Modifier.height(16.dp))
            
            // ── Stat row ──────────────────────────────────────────────────
"""
text = text.replace('            Spacer(Modifier.height(8.dp))\n\n            // ── Stat row ──────────────────────────────────────────────────', banner_ui)

# Add StatusBanner func at bottom
banner_func = """
@Composable
private fun StatusBanner(exitStatus: String?) {
    val (icon, label, bg, fg) = when (exitStatus) {
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
"""
text += banner_func

# Finally fix missing imports
if "androidx.compose.material.icons.filled.DirectionsWalk" not in text:
    text = text.replace("import androidx.compose.material.icons.filled.QrCode", "import androidx.compose.material.icons.filled.QrCode\nimport androidx.compose.material.icons.filled.DirectionsWalk\nimport androidx.compose.material.icons.filled.History\nimport androidx.compose.material.icons.filled.PendingActions\nimport androidx.compose.material.icons.filled.LocationOn\nimport androidx.compose.material.icons.filled.CheckCircle\nimport androidx.compose.material.icons.filled.Cancel")

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)

print("Restored StudentHomeScreen!")
