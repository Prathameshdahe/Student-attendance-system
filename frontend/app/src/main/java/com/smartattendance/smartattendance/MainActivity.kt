package com.smartattendance.smartattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartattendance.smartattendance.data.local.SessionManager
import com.smartattendance.smartattendance.data.repository.AuthRepository
import com.smartattendance.smartattendance.ui.screens.*
import com.smartattendance.smartattendance.ui.theme.SmartAttendanceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartAttendanceTheme {
                SmartAttendanceApp()
            }
        }
    }
}

@Composable
fun SmartAttendanceApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // null = still loading, "" = not logged in, "student_home" / "admin_home" = logged in
    var startDest by remember { mutableStateOf<String?>(null) }

    // Check session off the main thread to avoid ANR from EncryptedSharedPreferences crypto
    LaunchedEffect(Unit) {
        val dest = withContext(Dispatchers.IO) {
            try {
                val session = SessionManager(context)
                if (session.isLoggedIn()) {
                    if (session.getRole() == "ADMIN") "admin_home" else "student_home"
                } else {
                    "welcome"
                }
            } catch (e: Exception) {
                "welcome"
            }
        }
        startDest = dest
    }

    // Show loading spinner until session check completes
    if (startDest == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = startDest!!) {

        // ── Welcome / Role Select ─────────────────────────────────────────────
        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = { role ->
                    navController.navigate("login/$role")
                }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable(
            route = "login/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "STUDENT"
            LoginScreen(
                role = role,
                onBack = { navController.popBackStack() },
                onLoginSuccess = { successRole ->
                    val dest = if (successRole == "ADMIN") "admin_home" else "student_home"
                    navController.navigate(dest) {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        // ── Student Home ──────────────────────────────────────────────────────
        composable("student_home") {
            StudentHomeScreen(
                onLogout = {
                    // Clear session off main thread too
                    val repo = AuthRepository(context)
                    repo.logout()
                    navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // ── Admin Home ────────────────────────────────────────────────────────
        composable("admin_home") {
            AdminHomeScreen(
                onLogout = {
                    val repo = AuthRepository(context)
                    repo.logout()
                    navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                },
                onScanQr = { navController.navigate("qr_scanner") }
            )
        }

        // ── QR Scanner ────────────────────────────────────────────────────────
        composable("qr_scanner") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("QR Scanner — Coming Soon")
            }
        }
    }
}