package com.smartattendance.smartattendance.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartattendance.smartattendance.R
import com.smartattendance.smartattendance.ui.theme.*

@Composable
fun WelcomeScreen(onNavigateToLogin: (String) -> Unit) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Hero section ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // College Logo
            Image(
                painter = painterResource(id = R.drawable.logo_crest),
                contentDescription = "College Logo",
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))
            
            Text(
                "BVCOE, Pune",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Smart Attendance",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Smart Attendance — BVCOE Pune",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
            )
        }

        // ── Bottom sheet ──────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Who are you?",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Select your role to continue",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(Modifier.height(28.dp))

                // Role cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NiaRoleCard(
                        title = "Student",
                        subtitle = "View QR, attendance\n& leave records",
                        icon = Icons.Filled.School,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToLogin("STUDENT") }
                    )
                    NiaRoleCard(
                        title = "Faculty",
                        subtitle = "Scan QR, manage\nsessions & reports",
                        icon = Icons.Filled.AdminPanelSettings,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToLogin("ADMIN") }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Bharati Vidyapeeth College of Engineering, Pune",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
private fun NiaRoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    onContainerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Icon bubble
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(onContainerColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onContainerColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = onContainerColor.copy(alpha = 0.75f),
                    lineHeight = 17.sp
                )
            )

            Spacer(Modifier.height(14.dp))

            // CTA pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(onContainerColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Enter",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = onContainerColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = onContainerColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
