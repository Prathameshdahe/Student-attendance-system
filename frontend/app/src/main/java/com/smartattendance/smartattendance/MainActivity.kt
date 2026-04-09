package com.smartattendance.smartattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.smartattendance.smartattendance.ui.screens.*
import com.smartattendance.smartattendance.ui.theme.SmartAttendanceTheme

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
    var selectedRole by remember { mutableStateOf("STUDENT") }

    NavHost(navController = navController, startDestination = "welcome") {

        // ── Screen 1: Welcome / Role Select ─────────────────────────────────
        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = { role ->
                    selectedRole = role
                    navController.navigate("login/$role")
                }
            )
        }

        // ── Screen 2: Login ──────────────────────────────────────────────────
        composable(
            route = "login/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "STUDENT"
            LoginScreen(
                role = role,
                onBack = { navController.popBackStack() },
                onLoginSuccess = { successRole ->
                    if (successRole == "ADMIN") {
                        navController.navigate("admin_home") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    } else {
                        navController.navigate("student_home") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Phone SMS OTP 2FA (Student only) ─────────────────────────────────
        composable(
            route = "otp/{phone}",
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneArg = backStackEntry.arguments?.getString("phone") ?: ""
            val phone = phoneArg.replace("P", "+")
            
            OtpScreen(
                phoneNumber = phone,
                onBack = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onVerified = {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                        .get().addOnSuccessListener { doc ->
                            val studentName = doc.getString("name") ?: "Student"
                            navController.navigate("success/STUDENT/$studentName") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                }
            )
        }

        // ── Success Screen ───────────────────────────────────────────────────
        composable(
            route = "success/{role}/{name}",
            arguments = listOf(
                navArgument("role") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: selectedRole
            val name = backStackEntry.arguments?.getString("name") ?: ""
            SuccessScreen(
                userName = name,
                role = role,
                onGoToDashboard = {
                    navController.navigate("student_home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        // ── Student Home ─────────────────────────────────────────────────────
        composable("student_home") {
            StudentHomeScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // ── Admin Home ───────────────────────────────────────────────────────
        composable("admin_home") {
            AdminHomeScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                },
                onScanQr = { navController.navigate("qr_scanner") }
            )
        }

        // ── QR Scanner (next milestone) ──────────────────────────────────────
        composable("qr_scanner") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("QR Scanner — Coming Next")
            }
        }
    }
}