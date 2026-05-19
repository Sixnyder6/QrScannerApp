package com.example.qrscannerapp.features.homescreen.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * WidgetCard — минималистичный Apple-стиль.
 *
 * Философия: карточка почти невидима. Она — контейнер для данных,
 * не объект дизайна сам по себе. Никакого glow, shimmer, gradient.
 * Единственное украшение — тонкая граница 0.5dp и frosted glass.
 *
 * accentColor сохранён в сигнатуре для совместимости с существующим кодом,
 * но используется только для тонкого tint border — очень сдержанно.
 */

// Основные цвета системы
private val CardBg           = Color(0xFF141416)
private val CardBorder       = Color(0xFF2C2C2E)
private val CardBorderActive = Color(0xFF3A3A3C)

private val BorderBrush  = Brush.verticalGradient(listOf(CardBorderActive, CardBorder, CardBorder))
private val BorderStroke = Stroke(width = 0.5f)

@Composable
fun WidgetCard(
    accentColor: Color,                       // сохранён для совместимости
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    glowStrength: Float = 0.5f,              // сохранён для совместимости, не используется
    sheenPhaseOffset: Float = 0f,            // сохранён для совместимости, не используется
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "scale"
    )

    val corner = 20.dp

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip  = true
                shape = RoundedCornerShape(corner)
            }
            .clip(RoundedCornerShape(corner))
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    blurRadius  = 20.dp,
                    tints       = listOf(HazeTint(color = Color.White.copy(alpha = 0.03f))),
                    noiseFactor = 0f
                )
            )
            .background(CardBg)
            // Единственный декор — тонкая граница
            .drawWithCache {
                val cPx  = corner.toPx()
                val half = 0.5f
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(half, half, size.width - half, size.height - half,
                            CornerRadius(cPx, cPx))
                    )
                }
                onDrawWithContent {
                    drawContent()
                    drawPath(path = path, brush = BorderBrush, style = BorderStroke)
                }
            }
            .then(
                if (onClick != null) Modifier.pointerInput(onClick) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            try { tryAwaitRelease() } finally { pressed = false }
                        },
                        onTap = { onClick() }
                    )
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            content()
        }
    }
}