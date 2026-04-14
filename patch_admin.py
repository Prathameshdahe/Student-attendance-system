import re

path = r'c:\Users\DELL\Desktop\smartattendance\frontend\app\src\main\java\com\smartattendance\smartattendance\ui\screens\AdminHomeScreen.kt'

with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# 1. State variable insertion
text = text.replace(
    'var activityLog  by remember { mutableStateOf<List<ActivityLogItem>>(emptyList()) }',
    'var activityLog  by remember { mutableStateOf<List<ActivityLogItem>>(emptyList()) }\n    var pendingRequests by remember { mutableStateOf<List<com.smartattendance.smartattendance.data.remote.ExitRequestDto>>(emptyList()) }'
)

# 2. Add polling effect below adminName LaunchedEffect
polling_block = """
        adminName = withContext(Dispatchers.IO) {
            SessionManager(context).getUserName() ?: "Admin"
        }
    }

    LaunchedEffect(Unit) {
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
"""

text = text.replace(
    '''        adminName = withContext(Dispatchers.IO) {
            SessionManager(context).getUserName() ?: "Admin"
        }
    }''',
    polling_block
)

# 3. Modify the Content to include PendingRequests List
ui_block = """            }

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
                                        AttendanceRepository(context).resolveExitRequest(req.request_id.toString(), action)
                                        pendingRequests = pendingRequests.filter { it.request_id != req.request_id }
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
    }"""

text = re.sub(
    r'            Spacer\(Modifier\.height\(12\.dp\)\)\s+if \(activityLog\.isEmpty\(\)\)[\s\S]*?item \{ Spacer\(Modifier\.height\(80\.dp\)\) \}\s+\}\s+\}\s+\}\s+\}',
    ui_block,
    text
)

# 4. Inject ExitRequestCard composable at the very end of file
card_composable = """

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
"""

text += card_composable

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)

print("Admin HomeScreen Patched")
