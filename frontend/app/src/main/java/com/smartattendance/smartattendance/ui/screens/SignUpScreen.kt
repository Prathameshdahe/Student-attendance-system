package com.smartattendance.smartattendance.ui.screens

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartattendance.smartattendance.ui.components.*
import com.smartattendance.smartattendance.ui.theme.*

@Composable
fun SignUpScreen(
    role: String,
    onBack: () -> Unit,
    onContinue: (name: String, email: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var rollOrEmployeeId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val emailValid = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val emailInvalid = email.isNotEmpty() && !emailValid
    val passwordStrength = getPasswordStrength(password)
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword

    val canContinue = fullName.isNotBlank() && emailValid && rollOrEmployeeId.isNotBlank()
            && password.length >= 6 && passwordsMatch

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Wave Header
            WaveHeader(height = 160.dp, onBack = onBack) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 16.dp)
                ) {
                    StepProgressBar(currentStep = 1, modifier = Modifier.padding(start = 0.dp, bottom = 10.dp, end = 48.dp))
                    Text(
                        "Create Account",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        "Step 1 of 3 — Basic Info",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceWhite, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                // Full Name
                AuthTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full Name",
                    placeholder = "Rahul Sharma"
                )

                Spacer(Modifier.height(16.dp))

                // Email with live validation
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    placeholder = "you@example.com",
                    keyboardType = KeyboardType.Email,
                    isError = emailInvalid,
                    supportingText = if (emailInvalid) "Enter a valid email address" else null,
                    trailingIcon = {
                        AnimatedVisibility(visible = email.isNotEmpty()) {
                            Icon(
                                imageVector = if (emailValid) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = if (emailValid) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                // Roll Number / Employee ID
                AuthTextField(
                    value = rollOrEmployeeId,
                    onValueChange = { rollOrEmployeeId = it },
                    label = if (role == "ADMIN") "Employee ID" else "Roll Number",
                    placeholder = if (role == "ADMIN") "EMP-001" else "2021BVUCOE001"
                )

                Spacer(Modifier.height(16.dp))

                // Password + Strength Meter
                AuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "Min. 6 characters",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                if (password.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    PasswordStrengthBar(strength = passwordStrength)
                }

                Spacer(Modifier.height(16.dp))

                // Confirm Password
                AuthTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    placeholder = "Re-enter password",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                    supportingText = if (confirmPassword.isNotEmpty() && !passwordsMatch) "Passwords don't match" else null,
                    trailingIcon = {
                        if (confirmPassword.isNotEmpty()) {
                            Icon(
                                imageVector = if (passwordsMatch) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = if (passwordsMatch) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Spacer(Modifier.height(28.dp))

                PrimaryButton(
                    text = "Continue →",
                    color = AuthPurple,
                    enabled = canContinue,
                    onClick = { onContinue(fullName, email) }
                )
            }
        }
    }
}

// ─── Password Strength Bar ───────────────────────────────────────────────────

enum class PasswordStrength(val label: String, val color: Color, val fraction: Float) {
    WEAK("Weak", Color(0xFFEF4444), 0.33f),
    MEDIUM("Medium", Color(0xFFF59E0B), 0.66f),
    STRONG("Strong", Color(0xFF22C55E), 1f)
}

fun getPasswordStrength(password: String): PasswordStrength {
    if (password.length < 6) return PasswordStrength.WEAK
    val hasUpper = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    return when {
        hasUpper && hasDigit && hasSpecial -> PasswordStrength.STRONG
        hasUpper || hasDigit              -> PasswordStrength.MEDIUM
        else                              -> PasswordStrength.WEAK
    }
}

@Composable
fun PasswordStrengthBar(strength: PasswordStrength) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PasswordStrength.entries.forEach { s ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (strength.fraction >= s.fraction) s.color else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Strength: ${strength.label}",
            style = MaterialTheme.typography.labelSmall.copy(
                color = strength.color,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
