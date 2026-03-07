package com.graberhub.companion.ui.theme

import androidx.compose.ui.graphics.Color

// Convert the long values from decompiled source to hex colors
// 4278430196L = 0xFF0074F4 -> actually let me compute properly
// The decompiled code used Color(long) which is ARGB as unsigned long

val ShedBlue = Color(0xFF003B74)
val ShedBlueDark = Color(0xFF002451)
val ShedBlueLight = Color(0xFFB3D4FC)
val ShedPink = Color(0xFFEC5281)
val ShedPinkLight = Color(0xFFFFC9DC)
val BackgroundWhite = Color(0xFFFFFFFF)
val BackgroundGray = Color(0xFFF5F5FA)
val SurfaceWhite = Color(0xFFFFFFFF)
val CardBackground = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF6B7280)
val TextHint = Color(0xFFB0B8C3)
val DividerColor = Color(0xFFF0F0F5)
val BorderLight = Color(0xFFE5E7EB)
val CheckGreen = Color(0xFF22C55E)
val CheckGreenLight = Color(0xFFDCFCE7)
val KidPink = Color(0xFFEC5281)
val KidBlue = Color(0xFF003B74)
val KidPurple = Color(0xFFA78BFA)
val KidGreen = Color(0xFF4ADE80)
val ShadowColor = Color(0x1A000000)

// Preset colors for kid color picker (12 presets)
val PresetColors = listOf(
    "#ec5281" to "Pink",
    "#03a9f4" to "Blue",
    "#a78bfa" to "Purple",
    "#4ade80" to "Green",
    "#facc15" to "Yellow",
    "#fb923c" to "Orange",
    "#ef4444" to "Red",
    "#2dd4bf" to "Teal",
    "#6366f1" to "Indigo",
    "#fb7185" to "Rose",
    "#34d399" to "Emerald",
    "#38bdf8" to "Sky"
)
