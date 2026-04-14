import re

path = r'c:\Users\DELL\Desktop\smartattendance\frontend\app\src\main\java\com\smartattendance\smartattendance\ui\screens\StudentHomeScreen.kt'

with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# 1. State variable replacements
text = text.replace(
    'var hasPendingReq by remember { mutableStateOf(false) }',
    'var exitStatus by remember { mutableStateOf<String?>(null) }\n    val hasPendingReq = exitStatus == "PENDING"'
)

# 2. Add polling effect just before Scaffold
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
                    exitStatus = reqs.first().status?.replace("D", "D") # e.g. "APPROVED"
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

# 3. Modify success block status setting
text = text.replace('hasPendingReq = true', 'exitStatus = "PENDING"')

# 4. Modify StatusBanner call
text = text.replace('StatusBanner(hasPendingReq = hasPendingReq)', 'StatusBanner(exitStatus = exitStatus)')

# 5. Modify StatusBanner definition
banner_old = """@Composable
private fun StatusBanner(hasPendingReq: Boolean) {
    val (icon, label, bg, fg) = if (hasPendingReq)
        listOf(Icons.Filled.PendingActions, "Exit Pass Pending — Awaiting Approval", AmberLight, Amber)
    else
        listOf(Icons.Filled.LocationOn, "You're on campus  •  Geofence Active", IndigoLight, Indigo)"""

banner_new = """@Composable
private fun StatusBanner(exitStatus: String?) {
    val (icon, label, bg, fg) = when (exitStatus) {
        "PENDING" -> listOf(Icons.Filled.PendingActions, "Exit Pass Pending — Awaiting", AmberLight, Amber)
        "APPROVED" -> listOf(Icons.Filled.CheckCircle, "Exit Pass APPROVED — Clear to leave", GreenLight, Green)
        "DENIED" -> listOf(Icons.Filled.Cancel, "Exit Pass DENIED — See faculty", RedLight, Red)
        else -> listOf(Icons.Filled.LocationOn, "You're on campus  •  Geofence Active", IndigoLight, Indigo)
    }"""
text = text.replace(banner_old, banner_new)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)

print("Patched StudentHomeScreen.kt successfully")
