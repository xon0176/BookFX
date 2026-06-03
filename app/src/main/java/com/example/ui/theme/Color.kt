package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

var isSystemInDarkMode by mutableStateOf(false)

val DarkBackground: Color
    get() = if (isSystemInDarkMode) Color(0xFF111827) else Color(0xFFF9FAFC)

val DarkSurface: Color
    get() = if (isSystemInDarkMode) Color(0xFF1F2937) else Color(0xFFFFFFFF)

val Primary: Color
    get() = if (isSystemInDarkMode) Color(0xFF60A5FA) else Color(0xFF2E6FF2)

val Secondary: Color
    get() = if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFFEF3C7)

val TextPrimary: Color
    get() = if (isSystemInDarkMode) Color(0xFFF9FAFC) else Color(0xFF111827)

val TextSecondary: Color
    get() = if (isSystemInDarkMode) Color(0xFF9CA3AF) else Color(0xFF8F9BB3)

val GreenAccent: Color
    get() = if (isSystemInDarkMode) Color(0xFF4ADE80) else Color(0xFF137333)

val RedAccent: Color
    get() = if (isSystemInDarkMode) Color(0xFFF87171) else Color(0xFFC5221F)

val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF5F5F5)
val LightPrimary = Color(0xFF6200EE)
val LightSecondary = Color(0xFF03DAC6)
val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0xFF757575)
