package com.smartattendance.smartattendance.ui.theme

import androidx.compose.ui.graphics.Color

// ── KIWI Design System — NIA-inspired token palette ─────────────────────────

// Primary — Deep Indigo (NIA-style, not generic blue)
val Indigo10  = Color(0xFF0A0033)
val Indigo20  = Color(0xFF190063)
val Indigo30  = Color(0xFF2C0099)
val Indigo40  = Color(0xFF4C2FF0)   // primary light
val Indigo80  = Color(0xFFB8ADFF)   // primary dark
val Indigo90  = Color(0xFFE3DFFF)   // primary container light
val Indigo95  = Color(0xFFF3F0FF)

// Secondary — Teal accent (NIA tonal)
val Teal10    = Color(0xFF001F26)
val Teal20    = Color(0xFF003A44)
val Teal30    = Color(0xFF005060)
val Teal40    = Color(0xFF006878)
val Teal80    = Color(0xFF4FD8EB)
val Teal90    = Color(0xFFB4EEFF)

// Neutral surfaces — NIA-style blue-grey
val NeutralGray10  = Color(0xFF12131A)
val NeutralGray12  = Color(0xFF1A1B25)
val NeutralGray17  = Color(0xFF22233A)
val NeutralGray20  = Color(0xFF2A2C3E)
val NeutralGray87  = Color(0xFFDADCEF)
val NeutralGray90  = Color(0xFFE3E4F4)
val NeutralGray95  = Color(0xFFF0F1FF)
val NeutralGray99  = Color(0xFFFCFBFF)

// Role colours
val AdminGreen     = Color(0xFF10B981)
val AdminGreenCont = Color(0xFFD1FAE5)
val StudentBlue    = Color(0xFF2563EB)
val StudentBlueCont= Color(0xFFEFF6FF)

// Semantic
val ErrorRed       = Color(0xFFBA1A1A)
val ErrorRedLight  = Color(0xFFFFDAD6)
val SuccessGreen   = Color(0xFF146C2E)
val SuccessGreenLight = Color(0xFFB8F1CA)
val WarningAmber   = Color(0xFFB56A00)

// Legacy aliases (kept for backward-compat)
val AppBackground   = NeutralGray99
val SurfaceWhite    = Color(0xFFFFFFFF)
val WaveBackground  = NeutralGray95
val AuthPurple      = Indigo40
val AuthPurpleLight = Indigo95
val TextPrimary     = NeutralGray10
val TextSecondary   = Color(0xFF44465A)
val TextHint        = Color(0xFF757899)
val BVPMaroon       = Color(0xFFA61F21)
val BVPNavy         = NeutralGray17
val BVPGold         = Color(0xFFFFB800)
val OffWhite        = NeutralGray99
val AdminPrimary    = AdminGreen
val AdminLight      = AdminGreenCont
val StudentPrimary  = StudentBlue
val StudentLight    = StudentBlueCont

// M3 compat (no longer needed but kept for buildcompat)
val Purple80        = Color(0xFFD0BCFF)
val PurpleGrey80    = Color(0xFFCCC2DC)
val Pink80          = Color(0xFFEFB8C8)
val Purple40        = Color(0xFF6650a4)
val PurpleGrey40    = Color(0xFF625b71)
val Pink40          = Color(0xFF7D5260)