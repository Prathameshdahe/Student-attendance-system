package com.smartattendance.smartattendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartattendance.smartattendance.ui.theme.*

// ─── Shared Auth Text Field — M3 token-based ─────────────────────────────────

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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.outline
                    )
                )
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            trailingIcon = trailingIcon,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
            )
        )
        if (supportingText != null && isError) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

// ─── Primary Button (legacy compat) ──────────────────────────────────────────

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.38f)
        ),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled && !isLoading,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// ─── Role Badge ───────────────────────────────────────────────────────────────

@Composable
fun RoleBadge(role: String) {
    val (bg, fg, label) = when (role.uppercase()) {
        "ADMIN" -> Triple(AdminLight, AdminPrimary, "Faculty / Admin")
        else    -> Triple(StudentLight, StudentPrimary, "Student")
    }
    Surface(color = bg, shape = CircleShape) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = fg,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// ─── Wave Header (legacy — kept for SuccessScreen / SignUpScreen) ─────────────

@Composable
fun WaveHeader(
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    onBack: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 36.dp, start = 8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        content()
    }
}

// ─── Step Progress Bar ────────────────────────────────────────────────────────

@Composable
fun StepProgressBar(currentStep: Int, totalSteps: Int = 3, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index < currentStep) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}
