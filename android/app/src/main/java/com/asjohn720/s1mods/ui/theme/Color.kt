package com.asjohn720.s1mods.ui.theme

import androidx.compose.ui.graphics.Color

// Neutral Palette - Deep Slate Ramp
val BgDark = Color(0xFF0C0F14)
val SurfaceDark = Color(0xFF131822)
val SurfaceVariantDark = Color(0xFF1A212E)
val SurfaceHighlight = Color(0xFF222B3B)
val CardBorder = Color(0xFF242C3B)
val DividerColor = Color(0xFF1E2534)

// Purposeful Accents & States
val AccentMint = Color(0xFF10B981)          // Primary functional accent (connected, success)
val AccentMintSubtle = Color(0x1F10B981)    // Low-opacity tint for active states
val AccentSky = Color(0xFF38BDF8)           // Secondary accent for actionable inputs
val AccentSkySubtle = Color(0x1F38BDF8)
val StatusWarning = Color(0xFFF59E0B)       // Pre-flight & warning notifications
val StatusDanger = Color(0xFFEF4444)        // Error states & offline indicator

// Typography Roles (WCAG AA compliant contrast)
val TextPrimary = Color(0xFFF8FAFC)         // 96% contrast
val TextSecondary = Color(0xFF94A3B8)       // 60% contrast
val TextMuted = Color(0xFF64748B)           // 40% contrast

// Diagnostic Console
val TerminalBg = Color(0xFF090C10)
val TerminalBorder = Color(0xFF1B222D)

// Backwards-compatible aliases for existing references
val ElectricGreen = AccentMint
val CyberCyan = AccentSky
val ElectricYellow = StatusWarning
val DangerRed = StatusDanger
