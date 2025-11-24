// Файл: common/ui/CommonUI.kt
package com.example.qrscannerapp.common.ui

import androidx.compose.ui.graphics.Color

/**
 * Этот файл является "мозгом графики", хранящим общие визуальные константы.
 */

// Цвета для "наследуемой" (legacy) анимации на старых API
val WaveGradientColors1 = listOf(
    Color(0x33FFFFFF),
    Color(0x1AFFFFFF),
    Color(0x00FFFFFF),
    Color(0x1AFFFFFF),
    Color(0x33FFFFFF)
)

val WaveGradientColors2 = listOf(
    Color(0xFF121212),
    Color(0xFF222222),
    Color(0xFF282828),
    Color(0xFF222222),
    Color(0xFF121212)
)

// Специализированный список цветов для AGSL-шейдера.
val ShaderColors = listOf(
    Color(0xFF121212), // color1: Самый темный
    Color(0xFF222222), // color2: Темно-серый
    Color(0xFF282828)  // color3: Промежуточный
)

// V-- НОВАЯ КОНСТАНТА --V
/**
 * Цвет для полностью статичного фона, используемый в режиме
 * PerformanceClass.NONE для максимальной экономии ресурсов.
 */
val StaticBackgroundColor = ShaderColors[0] // Используем самый темный базовый цвет
// ^-- КОНЕЦ НОВОЙ КОНСТАНТЫ --^