package com.example.qrscannerapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// =================================================================================
// ЦВЕТОВАЯ СХЕМА — на базе Stardust палитры
// =================================================================================

private val StardustDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7B61FF),          // StardustPrimary
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D2E80),
    onPrimaryContainer = Color(0xFFE0D9FF),

    secondary = Color(0xFFFF9800),        // StardustSecondary
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4D3000),
    onSecondaryContainer = Color(0xFFFFDDB3),

    tertiary = Color(0xFF00BFA5),
    onTertiary = Color.Black,

    error = Color(0xFFFF5252),            // StardustError
    onError = Color.White,
    errorContainer = Color(0xFF4D1515),
    onErrorContainer = Color(0xFFFFB4AB),

    background = Color(0xFF0A0A0F),       // Почти чёрный, как AppBackground
    onBackground = Color(0xFFEAEAF0),     // StardustTextPrimary

    surface = Color(0xFF121218),          // Чуть светлее фона
    onSurface = Color(0xFFEAEAF0),
    surfaceVariant = Color(0xFF1A1A24),
    onSurfaceVariant = Color(0xFFA0A0B0), // StardustTextSecondary

    outline = Color(0xFF2A2A3A),          // StardustItemBg
    outlineVariant = Color(0xFF1E1E2A),

    inverseSurface = Color(0xFFEAEAF0),
    inverseOnSurface = Color(0xFF0A0A0F),
    inversePrimary = Color(0xFF5A40CC)
)

// =================================================================================
// ТЕМА — всегда тёмная (приложение тёмное по дизайну)
// =================================================================================

@Composable
fun QrScannerAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StardustDarkColorScheme,
        typography = Typography,
        content = content
    )
}