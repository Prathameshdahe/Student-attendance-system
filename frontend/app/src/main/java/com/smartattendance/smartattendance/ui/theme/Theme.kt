package com.smartattendance.smartattendance.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Light color scheme ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = Indigo40,
    onPrimary            = Color.White,
    primaryContainer     = Indigo90,
    onPrimaryContainer   = Indigo10,
    secondary            = Teal40,
    onSecondary          = Color.White,
    secondaryContainer   = Teal90,
    onSecondaryContainer = Teal10,
    tertiary             = AdminGreen,
    onTertiary           = Color.White,
    tertiaryContainer    = AdminGreenCont,
    onTertiaryContainer  = Color(0xFF002113),
    error                = ErrorRed,
    onError              = Color.White,
    errorContainer       = ErrorRedLight,
    onErrorContainer     = Color(0xFF410002),
    background           = NeutralGray99,
    onBackground         = NeutralGray10,
    surface              = NeutralGray99,
    onSurface            = NeutralGray10,
    surfaceVariant       = NeutralGray90,
    onSurfaceVariant     = Color(0xFF44465A),
    outline              = Color(0xFF757899),
    outlineVariant       = NeutralGray87,
    inverseSurface       = NeutralGray20,
    inverseOnSurface     = NeutralGray95,
    inversePrimary       = Indigo80,
    surfaceTint          = Indigo40,
)

// ── Dark color scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = Indigo80,
    onPrimary            = Indigo20,
    primaryContainer     = Indigo30,
    onPrimaryContainer   = Indigo90,
    secondary            = Teal80,
    onSecondary          = Teal20,
    secondaryContainer   = Teal30,
    onSecondaryContainer = Teal90,
    tertiary             = Color(0xFF6DD58C),
    onTertiary           = Color(0xFF003919),
    tertiaryContainer    = AdminGreen,
    onTertiaryContainer  = Color(0xFFB8F1CA),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    background           = NeutralGray10,
    onBackground         = NeutralGray90,
    surface              = NeutralGray12,
    onSurface            = NeutralGray87,
    surfaceVariant       = NeutralGray20,
    onSurfaceVariant     = NeutralGray87,
    outline              = Color(0xFF8E90A8),
    outlineVariant       = NeutralGray20,
    inverseSurface       = NeutralGray87,
    inverseOnSurface     = NeutralGray17,
    inversePrimary       = Indigo40,
    surfaceTint          = Indigo80,
)

// ── Theme ─────────────────────────────────────────────────────────────────────
@Composable
fun SmartAttendanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,        // off by default — use our brand palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KiwiTypography,
        content = content
    )
}