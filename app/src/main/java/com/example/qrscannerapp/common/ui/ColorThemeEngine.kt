package com.example.qrscannerapp.common.ui

import androidx.compose.ui.graphics.Color

// Расширяемый движок тем с оптимизацией
object ColorThemeEngine {
    // Базовые темы
    val RedTheme = ColorPalette(
        primary = Color(0xFFFF6B6B),
        secondary = Color(0xFFFCA311),
        background = Color(0xFF1A1A2E)
    )

    val GreenTheme = ColorPalette(
        primary = Color(0xFF2ECC71),
        secondary = Color(0xFF27AE60),
        background = Color(0xFF141E30)
    )

    val PurpleTheme = ColorPalette(
        primary = Color(0xFF9C27B0),
        secondary = Color(0xFF673AB7),
        background = Color(0xFF120136)
    )

    // Новые темы
    val WhiteTheme = ColorPalette(
        primary = Color(0xFFF1F1F1),
        secondary = Color(0xFFE0E0E0),
        background = Color(0xFFFFFFFF)
    )

    val YellowTheme = ColorPalette(
        primary = Color(0xFFFFC107),
        secondary = Color(0xFFFFD54F),
        background = Color(0xFFFFF8E1)
    )

    // Структура палитры для расширяемости
    data class ColorPalette(
        val primary: Color,
        val secondary: Color,
        val background: Color,
        val onPrimary: Color = primary.getContrastColor(),
        val onSecondary: Color = secondary.getContrastColor()
    )

    // Утилита для контраста
    private fun Color.getContrastColor(): Color {
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
        return if (luminance > 0.5) Color.Black else Color.White
    }

    // Функция для динамической темизации
    fun applyTheme(theme: ColorPalette): ColorPalette = theme
}