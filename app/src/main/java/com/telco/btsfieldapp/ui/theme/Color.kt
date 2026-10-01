package com.telco.btsfieldapp.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Neo-Banking Bright Green Palette ─────────────────────────────────────────
// Primary brand: deep forest green
val PrimaryGreen      = Color(0xFF126B36)  // main brand / CTAs / top bar
val PrimaryGreenDark = Color(0xFF0D5228)  // pressed / dark variant
val PrimaryGreenLight= Color(0xFFE8F5E9)  // soft tint backgrounds

// Vibrant accent: bright green for highlights
val AccentGreen       = Color(0xFF1E9E4F)
val AccentGreenLight  = Color(0xFFD1FAE5)

// Background / surface: clean and bright
val BackgroundLight   = Color(0xFFF7F8FA)  // off-white canvas
val SurfaceLight     = Color(0xFFFFFFFF)    // pure white cards
val OnSurfaceLight    = Color(0xFF1C1F22)   // dark charcoal — primary text
val OnSurfaceVariantLight = Color(0xFF7A808A) // medium gray — secondary text

// Dark theme
val BackgroundDark   = Color(0xFF111827)    // deep navy-dark
val SurfaceDark      = Color(0xFF1F2937)    // dark card surface
val OnSurfaceDark     = Color(0xFFF9FAFB)   // near white
val OnSurfaceVariantDark = Color(0xFF9CA3AF) // muted gray

// ─── Section accent colors (used per-section in BTS forms) ───────────────────
val SectionGreen    = Color(0xFF126B36)  // Ground Equipment
val SectionBlue     = Color(0xFF2563EB)  // GPS / RRU
val SectionAmber     = Color(0xFFD97706)  // Shelter / Power
val SectionPurple    = Color(0xFF7C3AED)  // AC / Antenna
val SectionRed       = Color(0xFFDC2626)  // Battery / Critical
val SectionCyan      = Color(0xFF0891B2)  // Media
val SectionPink      = Color(0xFFDB2777)  // Slab / Redundant
val SectionTeal      = Color(0xFF0D9488)  // Extra

// ─── Semantic Colors ─────────────────────────────────────────────────────────
val SuccessColor     = Color(0xFF16A34A)  // bright green
val WarningColor     = Color(0xFFCA8A04)  // amber
val ErrorColor       = Color(0xFFDC2626)  // red
val InfoColor        = Color(0xFF2563EB)  // blue

val StatusActive     = Color(0xFF16A34A)
val StatusInactive   = Color(0xFF9CA3AF)
val StatusPending    = Color(0xFFCA8A04)
val StatusCritical   = Color(0xFFDC2626)

// ─── Pastel Chip Colors ───────────────────────────────────────────────────────
val ChipGreen   = Color(0xFFBEF264)
val ChipYellow  = Color(0xFFFDE047)
val ChipBlue    = Color(0xFF93C5FD)
val ChipOrange  = Color(0xFFFCA5A5)
val ChipPurple  = Color(0xFFC4B5FD)
val ChipPink    = Color(0xFFFBCFE8)

// ─── Aliases for existing code ──────────────────────────────────────────────
// Keep old aliases so existing screen files don't break
val PrimaryCoral      = PrimaryGreen
val PrimaryCoralDark = PrimaryGreenDark
val PrimaryCoralLight= PrimaryGreenLight
val SecondaryTeal    = AccentGreen
val SecondaryAmber    = WarningColor
val SecondaryAmberLight = Color(0xFFFEF9C3)
val AccentTeal       = AccentGreen
val AccentTealLight  = AccentGreenLight

// Old aliases for legacy callers
val BackgroundLight_alias = BackgroundLight
val SurfaceLight_alias   = SurfaceLight
val OnSurfaceLight_alias  = OnSurfaceLight
val OnSurfaceVariantLight_alias = OnSurfaceVariantLight
val BackgroundDark_alias  = BackgroundDark
val SurfaceDark_alias     = SurfaceDark
val OnSurfaceDark_alias   = OnSurfaceDark
val OnSurfaceVariantDark_alias = OnSurfaceVariantDark
val SuccessColor_alias    = SuccessColor
val WarningColor_alias   = WarningColor
val ErrorColor_alias     = ErrorColor
val InfoColor_alias      = InfoColor
val StatusActive_alias   = StatusActive
val StatusInactive_alias  = StatusInactive
val StatusPending_alias   = StatusPending
val StatusCritical_alias  = StatusCritical
