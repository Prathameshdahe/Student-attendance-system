package com.smartattendance.smartattendance.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartattendance.smartattendance.ui.components.PrimaryButton
import com.smartattendance.smartattendance.ui.components.RoleBadge
import com.smartattendance.smartattendance.ui.components.StepProgressBar
import com.smartattendance.smartattendance.ui.components.WaveHeader
import com.smartattendance.smartattendance.ui.theme.*

@Composable
fun SuccessScreen(
    userName: String,
    role: String,
    onGoToDashboard: () -> Unit
) {
    // Animated scale for checkmark pop-in
    val scaleAnim = remember { Animatable(0f) }
    val ringScale = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        ringScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = EaseOutCirc)
        )
    }

    // Pulsing ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ring"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Wave header — compact, step 3
            WaveHeader(height = 140.dp) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 16.dp)
                ) {
                    StepProgressBar(
                        currentStep = 3,
                        modifier = Modifier.padding(end = 48.dp, bottom = 10.dp)
                    )
                    Text(
                        "All Done!",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                }
            }

            // Content card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceWhite, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Outer pulsing ring
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow ring
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(ringScale.value)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.1f))
                                .border(2.dp, SuccessGreen.copy(alpha = 0.3f), CircleShape)
                        )
                        // Middle ring
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .scale(ringScale.value)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.15f))
                        )
                        // Inner checkmark circle
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .scale(scaleAnim.value)
                                .clip(CircleShape)
                                .background(SuccessGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Success",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    Text(
                        text = "Welcome, ${userName.split(" ").firstOrNull() ?: userName}! 🎉",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Your account is verified and ready.\nStart tracking attendance with ease.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    )

                    Spacer(Modifier.height(20.dp))

                    RoleBadge(role = role)

                    Spacer(Modifier.height(32.dp))

                    // Account info card
                    Surface(
                        color = AppBackground,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow(label = "Account Status", value = "✓ Verified", valueColor = SuccessGreen)
                            Spacer(Modifier.height(8.dp))
                            InfoRow(
                                label = "Role",
                                value = if (role == "ADMIN") "Administrator" else "Student",
                                valueColor = if (role == "ADMIN") AdminPrimary else StudentPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            InfoRow(label = "Institution", value = "BVCOE, Pune", valueColor = TextPrimary)
                        }
                    }
                }

                // Bottom
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PrimaryButton(
                        text = "Go to Dashboard →",
                        color = if (role == "ADMIN") AdminPrimary else StudentPrimary,
                        onClick = onGoToDashboard
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Bharati Vidyapeeth College of Engineering, Pune",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextHint,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = valueColor,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
