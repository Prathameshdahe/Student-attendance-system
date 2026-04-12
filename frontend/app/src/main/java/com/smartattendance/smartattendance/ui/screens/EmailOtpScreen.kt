package com.smartattendance.smartattendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * EmailOtpScreen — deprecated. Email OTP via Firebase was removed.
 * Authentication is now handled by email+password via the backend API.
 * This file is kept as a stub to prevent import errors in any remaining references.
 */
@Composable
fun EmailOtpScreen(
    email: String = "",
    role: String = "",
    onBack: () -> Unit = {},
    onVerified: (role: String) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Email OTP deprecated — use email+password login.")
    }
}
