package com.smartattendance.smartattendance.ui.screens

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.smartattendance.smartattendance.R
import com.smartattendance.smartattendance.ui.components.*
import com.smartattendance.smartattendance.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    role: String,
    onBack: () -> Unit,
    onLoginSuccess: (role: String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val emailValid = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val emailInvalid = email.isNotEmpty() && !emailValid
    val roleColor = if (role == "ADMIN") AdminPrimary else StudentPrimary

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Wave Header with BVP logo ─────────────────────────────
            WaveHeader(height = 200.dp, onBack = onBack) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // BVP College crest logo
                    Image(
                        painter = painterResource(id = R.drawable.logo_crest),
                        contentDescription = "Bharati Vidyapeeth",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "BVCOE, Pune",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                }
                // Role badge + heading at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 20.dp)
                ) {
                    RoleBadge(role = role)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sign in to continue",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                }
            }

            // ── Form Card ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceWhite, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {

                // Info banner for students (admin adds accounts)
                if (role == "STUDENT") {
                    Surface(
                        color = StudentLight,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = StudentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Your account is created by the Admin. Contact admin if you don't have credentials.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = StudentPrimary,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // Email Field
                AuthTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = null },
                    label = "Email Address",
                    placeholder = if (role == "ADMIN") "admin@bvucoep.edu.in" else "rollno-branch@bvucoep.edu.in",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    trailingIcon = {
                        AnimatedVisibility(visible = email.isNotEmpty()) {
                            Icon(
                                imageVector = if (emailValid) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = if (emailValid) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    isError = emailInvalid,
                    supportingText = if (emailInvalid) "Enter a valid email address" else null
                )

                Spacer(Modifier.height(16.dp))

                // Password Field
                AuthTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = null },
                    label = "Password",
                    placeholder = "••••••••",
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = { focusManager.clearFocus() },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                              else Icons.Filled.Visibility,
                                contentDescription = "Toggle password",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                TextButton(
                    onClick = { /* TODO: Password reset */ },
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                ) {
                    Text(
                        "Forgot Password?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = roleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Error Banner
                AnimatedVisibility(visible = errorMsg != null) {
                    Surface(
                        color = ErrorRedLight,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Sign In Button → Firebase Auth → Direct Dashboard ──
                PrimaryButton(
                    text = "Sign In",
                    color = roleColor,
                    isLoading = isLoading,
                    enabled = emailValid && password.length >= 6,
                    onClick = {
                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                isLoading = false
                                onLoginSuccess(role)
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMsg = when {
                                    e.message?.contains("password") == true ->
                                        "Wrong password. Please try again."
                                    e.message?.contains("no user") == true ||
                                    e.message?.contains("identifier") == true ->
                                        "No account found for this email."
                                    else -> e.localizedMessage ?: "Login failed. Try again."
                                }
                            }
                    }
                )

                if (role == "STUDENT") {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = null,
                            tint = AuthPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "A one-time verification code will be sent to your email after sign-in.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                        )
                    }
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(24.dp))

                Text(
                    "Bharati Vidyapeeth College of Engineering, Pune",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextHint,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─── Shared Auth Text Field ──────────────────────────────────────────────────

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(color = TextHint))
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            trailingIcon = trailingIcon,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AuthPurple,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                errorBorderColor = ErrorRed,
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite
            )
        )
        if (supportingText != null && isError) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall.copy(color = ErrorRed),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}
