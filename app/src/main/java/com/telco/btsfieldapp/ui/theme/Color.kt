package com.telco.btsfieldapp.ui.theme

import androidx.compose.ui.graphics.Color

// ─── DailyMe-Inspired Warm Palette ────────────────────────────────────────────
// Backgrounds: warm cream instead of cold gray
val BackgroundLight   = Color(0xFFFFF8F0)   // warm cream
val SurfaceLight     = Color(0xFFFFFFFF)     // pure white cards
val OnSurfaceLight   = Color(0xFF1C1917)     // warm near-black
val OnSurfaceVariantLight = Color(0xFF78716C) // warm gray

// Primary: coral / warm orange (replaces teal)
val PrimaryCoral     = Color(0xFFF97316)     // warm coral-orange
val PrimaryCoralDark = Color(0xFFEA580C)     // deeper orange
val PrimaryCoralLight= Color(0xFFFED7AA)     // soft peach

// Secondary: warm amber (replaces teal)
val SecondaryAmber   = Color(0xFFF59E0B)     // warm amber
val SecondaryAmberLight = Color(0xFFFEF3C7) // pale amber

// Accent: soft teal for balance
val AccentTeal       = Color(0xFF14B8A6)
val AccentTealLight  = Color(0xFF99F6E4)

// Dark theme
val BackgroundDark   = Color(0xFF1C1917)     // warm dark
val SurfaceDark      = Color(0xFF292524)     // warm surface dark
val OnSurfaceDark    = Color(0xFFF5F5F4)     // warm white
val OnSurfaceVariantDark = Color(0xFFA8A29E) // warm gray

// ─── Semantic Colors ────────────────────────────────────────────────────────────
val SuccessColor     = Color(0xFF22C55E)
val WarningColor     = Color(0xFFF59E0B)
val ErrorColor       = Color(0xFFEF4444)
val InfoColor        = Color(0xFF3B82F6)

val StatusActive     = Color(0xFF22C55E)
val StatusInactive   = Color(0xFFA8A29E)
val StatusPending    = Color(0xFFF59E0B)
val StatusCritical   = Color(0xFFEF4444)

// ─── Pastel Chips (DailyMe-style) ──────────────────────────────────────────────
val ChipGreen       = Color(0xFFBEF264)
val ChipYellow      = Color(0xFFFDE047)
val ChipBlue        = Color(0xFF93C5FD)
val ChipOrange      = Color(0xFFFCA5A5)
val ChipPurple      = Color(0xFFC4B5FD)
val ChipPink        = Color(0xFFFBCFE8)

// ─── Backward-compat aliases (screens reference these) ───────────────────────────
val PrimaryGreen    = PrimaryCoral
val PrimaryGreenDark= PrimaryCoralDark
val PrimaryGreenLight=PrimaryCoralLight
val SecondaryTeal  = AccentTeal
val BackgroundLight_alias = BackgroundLight
val SurfaceLight_alias = SurfaceLight
val OnSurfaceLight_alias = OnSurfaceLight
val OnSurfaceVariantLight_alias = OnSurfaceVariantLight
val BackgroundDark_alias = BackgroundDark
val SurfaceDark_alias = SurfaceDark
val OnSurfaceDark_alias = OnSurfaceDark
val OnSurfaceVariantDark_alias = OnSurfaceVariantDark
val SuccessColor_alias = SuccessColor
val WarningColor_alias = WarningColor
val ErrorColor_alias = ErrorColor
val InfoColor_alias = InfoColor
val StatusActive_alias = StatusActive
val StatusInactive_alias = StatusInactive
val StatusPending_alias = StatusPending
val StatusCritical_alias = StatusCritical
