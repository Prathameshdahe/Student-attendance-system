package com.smartattendance.smartattendance.ui.screens

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartattendance.smartattendance.R
import com.smartattendance.smartattendance.data.repository.AuthRepository
import com.smartattendance.smartattendance.ui.components.AuthTextField
import com.smartattendance.smartattendance.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    role: String,
    onBack: () -> Unit,
    onLoginSuccess: (role: String) -> Unit
) {
    val context = LocalContext.current
    val authRepo = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val emailValid = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val emailInvalid = email.isNotEmpty() && !emailValid

    val isAdmin = role == "ADMIN"
    val roleLabel = if (isAdmin) "Faculty / Admin" else "Student"
    val roleBg = if (isAdmin) MaterialTheme.colorScheme.tertiaryContainer
                 else MaterialTheme.colorScheme.secondaryContainer
    val roleOn = if (isAdmin) MaterialTheme.colorScheme.onTertiaryContainer
                 else MaterialTheme.colorScheme.onSecondaryContainer
    val primaryColor = if (isAdmin) MaterialTheme.colorScheme.tertiary
                       else MaterialTheme.colorScheme.secondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ───────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Centered Logo and Subtitle
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_crest),
                        contentDescription = "College Logo",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "BVCOE, Pune",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                // Left-aligned Role chip and Title
                Surface(
                    shape = CircleShape,
                    color = roleBg
                ) {
                    Text(
                        roleLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = roleOn,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Sign in to continue",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Form card ─────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = null },
                        label = "Email",
                        placeholder = if (isAdmin) "admin@bvucoep.edu.in" else "rollno-branch@bvucoep.edu.in",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        trailingIcon = {
                            AnimatedVisibility(visible = email.isNotEmpty()) {
                                Icon(
                                    imageVector = if (emailValid) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (emailValid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        isError = emailInvalid,
                        supportingText = if (emailInvalid) "Enter a valid email address" else null
                    )

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
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Error ─────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = errorMsg != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut(),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            errorMsg ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Sign in button ─────────────────────────────────────────────
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        val result = authRepo.login(email, password)
                        isLoading = false
                        result.fold(
                            onSuccess = { user ->
                                if (user.role != role) {
                                    authRepo.logout()
                                    errorMsg = "Access denied. This account is not a $roleLabel."
                                } else {
                                    onLoginSuccess(user.role)
                                }
                            },
                            onFailure = { e ->
                                errorMsg = when {
                                    e.message?.contains("401") == true -> "Invalid email or password."
                                    e.message?.contains("connect") == true ||
                                    e.message?.contains("timeout") == true ||
                                    e.message?.contains("Unable to resolve") == true ->
                                        "Cannot reach server. Check your WiFi connection."
                                    else -> "Login failed. Please try again."
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                enabled = emailValid && password.length >= 6 && !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = if (isAdmin) MaterialTheme.colorScheme.onTertiary
                                   else MaterialTheme.colorScheme.onSecondary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.surface
                    )
                } else {
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            Text(
                "Bharati Vidyapeeth College of Engineering, Pune",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }
    }
}
