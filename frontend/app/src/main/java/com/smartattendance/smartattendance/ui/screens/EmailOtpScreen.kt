package com.smartattendance.smartattendance.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartattendance.smartattendance.ui.components.*
import com.smartattendance.smartattendance.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

// ── Backend URL — Use your laptop's local IP for physical phone testing ──────
private const val BACKEND_URL = "http://192.168.0.115:8000"

private enum class EmailOtpState { IDLE, SENDING, SENT, VERIFYING, ERROR_SEND, ERROR_VERIFY, SUCCESS }

@Composable
fun EmailOtpScreen(
    email: String,
    onBack: () -> Unit,
    onVerified: (studentName: String) -> Unit
) {
    val otpDigits = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    var state by remember { mutableStateOf(EmailOtpState.IDLE) }
    var errorText by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Shake animation for wrong OTP
    val shakeTarget = if (state == EmailOtpState.ERROR_VERIFY) 12f else 0f
    val shakeOffset by animateFloatAsState(
        targetValue = shakeTarget,
        animationSpec = if (state == EmailOtpState.ERROR_VERIFY)
            spring(dampingRatio = Spring.DampingRatioHighBouncy)
        else spring(),
        label = "shake"
    )

    // Auto-send OTP on screen load
    LaunchedEffect(Unit) {
        sendOtp(email,
            onSending = { state = EmailOtpState.SENDING },
            onSent = {
                state = EmailOtpState.SENT
                // Start countdown
                scope.launch {
                    while (secondsLeft > 0) {
                        delay(1000)
                        secondsLeft--
                    }
                    canResend = true
                }
                // Focus first box
                focusRequesters[0].requestFocus()
            },
            onError = {
                state = EmailOtpState.ERROR_SEND
                errorText = "Failed to send OTP. Check server connection."
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {

            WaveHeader(height = 160.dp, onBack = onBack) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 16.dp)
                ) {
                    StepProgressBar(currentStep = 2, modifier = Modifier.padding(end = 48.dp, bottom = 10.dp))
                    Text(
                        "2-Factor Verification",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceWhite, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Status indicator
                when (state) {
                    EmailOtpState.IDLE, EmailOtpState.SENDING -> {
                        CircularProgressIndicator(color = AuthPurple, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Sending OTP to your email…",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                    EmailOtpState.ERROR_SEND -> {
                        Surface(
                            color = ErrorRedLight,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                errorText,
                                style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            text = "Retry",
                            color = ErrorRed,
                            onClick = {
                                state = EmailOtpState.SENDING
                                scope.launch {
                                    sendOtp(email,
                                        onSending = {},
                                        onSent = { state = EmailOtpState.SENT },
                                        onError = {
                                            state = EmailOtpState.ERROR_SEND
                                            errorText = "Still can't reach server. Is the backend running?"
                                        }
                                    )
                                }
                            }
                        )
                    }
                    else -> {
                        // OTP sent — show boxes
                        Text(
                            "Enter the 6-digit code sent to",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            email,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AuthPurple,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(Modifier.height(28.dp))

                        // 6 OTP Digit Boxes
                        Row(
                            modifier = Modifier.graphicsLayer { translationX = shakeOffset },
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            otpDigits.forEachIndexed { index, digit ->
                                val boxColor = when (state) {
                                    EmailOtpState.ERROR_VERIFY -> ErrorRed
                                    EmailOtpState.SUCCESS      -> SuccessGreen
                                    else -> if (digit.isNotEmpty()) AuthPurple else Color(0xFFE2E8F0)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(2.dp, boxColor, RoundedCornerShape(10.dp))
                                        .background(
                                            when (state) {
                                                EmailOtpState.ERROR_VERIFY -> ErrorRedLight
                                                EmailOtpState.SUCCESS -> SuccessGreenLight
                                                else -> SurfaceWhite
                                            },
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = digit,
                                        onValueChange = { newVal ->
                                            val clean = newVal.filter { it.isDigit() }.take(1)
                                            otpDigits[index] = clean
                                            if (state == EmailOtpState.ERROR_VERIFY) state = EmailOtpState.SENT
                                            if (clean.isNotEmpty() && index < 5) {
                                                focusRequesters[index + 1].requestFocus()
                                            } else if (clean.isEmpty() && index > 0) {
                                                focusRequesters[index - 1].requestFocus()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .focusRequester(focusRequesters[index]),
                                        enabled = state == EmailOtpState.SENT || state == EmailOtpState.ERROR_VERIFY,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.titleLarge.copy(
                                            color = boxColor,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.Center) { innerTextField() }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Error message
                        AnimatedVisibility(visible = state == EmailOtpState.ERROR_VERIFY) {
                            Text(
                                "Incorrect code. Please try again.",
                                style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Countdown / Resend
                        if (!canResend) {
                            Text(
                                "Resend OTP in ${String.format("%02d", secondsLeft)}s",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        } else {
                            TextButton(onClick = {
                                canResend = false
                                secondsLeft = 60
                                otpDigits.fill("")
                                state = EmailOtpState.SENDING
                                scope.launch {
                                    sendOtp(email,
                                        onSending = {},
                                        onSent = {
                                            state = EmailOtpState.SENT
                                            scope.launch {
                                                while (secondsLeft > 0) {
                                                    delay(1000)
                                                    secondsLeft--
                                                }
                                                canResend = true
                                            }
                                        },
                                        onError = { state = EmailOtpState.ERROR_SEND }
                                    )
                                }
                            }) {
                                Text(
                                    "Resend OTP",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = AuthPurple,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // Verify Button
                        PrimaryButton(
                            text = "Verify",
                            color = AuthPurple,
                            isLoading = state == EmailOtpState.VERIFYING,
                            enabled = otpDigits.all { it.isNotEmpty() } &&
                                    state != EmailOtpState.VERIFYING &&
                                    state != EmailOtpState.SUCCESS,
                            onClick = {
                                state = EmailOtpState.VERIFYING
                                val enteredOtp = otpDigits.joinToString("")
                                scope.launch {
                                    verifyOtp(
                                        email = email,
                                        otp = enteredOtp,
                                        onSuccess = {
                                            state = EmailOtpState.SUCCESS
                                            scope.launch {
                                                // Fetch student name from Firestore
                                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                                val name = try {
                                                    val doc = FirebaseFirestore.getInstance()
                                                        .collection("users").document(uid).get().await()
                                                    doc.getString("name") ?: "Student"
                                                } catch (e: Exception) { "Student" }
                                                delay(500)
                                                onVerified(name)
                                            }
                                        },
                                        onError = {
                                            state = EmailOtpState.ERROR_VERIFY
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── OkHttp calls to Python backend ─────────────────────────────────────────

private val httpClient = OkHttpClient()

private fun sendOtp(
    email: String,
    onSending: () -> Unit,
    onSent: () -> Unit,
    onError: () -> Unit
) {
    onSending()
    val body = JSONObject().put("email", email).toString()
    val request = Request.Builder()
        .url("$BACKEND_URL/auth/send-otp")
        .post(body.toRequestBody("application/json".toMediaType()))
        .build()

    httpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { onError() }
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) onSent() else onError()
        }
    })
}

private fun verifyOtp(
    email: String,
    otp: String,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val body = JSONObject().put("email", email).put("otp", otp).toString()
    val request = Request.Builder()
        .url("$BACKEND_URL/auth/verify-otp")
        .post(body.toRequestBody("application/json".toMediaType()))
        .build()

    httpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { onError() }
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) onSuccess() else onError()
        }
    })
}
