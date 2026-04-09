package com.smartattendance.smartattendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartattendance.smartattendance.ui.components.WaveHeader
import com.smartattendance.smartattendance.ui.theme.*

@Composable
fun WelcomeScreen(onNavigateToLogin: (String) -> Unit) {

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {

        // ── Wave Header ────────────────────────────────────────────────
        WaveHeader(height = 240.dp) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo circle with "SA" initials
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AuthPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "SmartAttend",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                )
            }
        }

        // ── Bottom Sheet Card ─────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset(y = 0.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Bharati Vidyapeeth\nCollege of Engineering",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Secure. Smart.\nPresent.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )
                )

                Spacer(Modifier.height(32.dp))

                // Role Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RoleCard(
                        title = "I'm a Student",
                        description = "Mark your daily attendance & track your record",
                        icon = Icons.Filled.School,
                        accentColor = StudentPrimary,
                        lightColor = StudentLight,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToLogin("STUDENT") }
                    )
                    RoleCard(
                        title = "I'm an Admin",
                        description = "Manage students, scan QR & view reports",
                        icon = Icons.Filled.AdminPanelSettings,
                        accentColor = AdminPrimary,
                        lightColor = AdminLight,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToLogin("ADMIN") }
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    "By continuing, you agree to our Terms of Service\nand Privacy Policy.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextHint,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    lightColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(lightColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                lineHeight = 16.sp
            )
        )
        Spacer(Modifier.height(12.dp))
        // Arrow chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accentColor)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                "→",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
