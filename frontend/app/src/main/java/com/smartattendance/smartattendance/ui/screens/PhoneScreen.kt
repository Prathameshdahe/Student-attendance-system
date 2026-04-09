package com.smartattendance.smartattendance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartattendance.smartattendance.ui.components.*
import com.smartattendance.smartattendance.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class OtpSendState { IDLE, SENDING, SENT }

@Composable
fun PhoneScreen(
    onBack: () -> Unit,
    onOtpSent: (String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var otpState by remember { mutableStateOf(OtpSendState.IDLE) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Wave Header
            WaveHeader(height = 160.dp, onBack = onBack) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 16.dp)
                ) {
                    StepProgressBar(
                        currentStep = 2,
                        modifier = Modifier.padding(end = 48.dp, bottom = 10.dp)
                    )
                    Text(
                        "Enter your phone\nnumber",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            lineHeight = 32.sp
                        )
                    )
                }
            }

            // Form Card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceWhite, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Text(
                    "A 6-digit OTP will be sent to verify\nyour number.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    "Phone Number",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Country code chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🇮🇳", fontSize = 20.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "+91",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phone = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "98765 43210",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextHint)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuthPurple,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "OTP expires in 10 minutes",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextHint)
                )

                Spacer(Modifier.height(32.dp))

                PrimaryButton(
                    text = when (otpState) {
                        OtpSendState.IDLE    -> "Send OTP"
                        OtpSendState.SENDING -> "Sending..."
                        OtpSendState.SENT    -> "OTP Sent ✓"
                    },
                    color = if (otpState == OtpSendState.SENT) SuccessGreen else AuthPurple,
                    isLoading = otpState == OtpSendState.SENDING,
                    enabled = phone.length == 10 && otpState == OtpSendState.IDLE,
                    onClick = {
                        scope.launch {
                            otpState = OtpSendState.SENDING
                            delay(1500)
                            otpState = OtpSendState.SENT
                            delay(600)
                            onOtpSent("+91$phone")
                        }
                    }
                )

                AnimatedVisibility(visible = otpState == OtpSendState.SENT) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "A 6-digit code was sent to +91 ${phone.take(4)}XXXXXX",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SuccessGreen,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
