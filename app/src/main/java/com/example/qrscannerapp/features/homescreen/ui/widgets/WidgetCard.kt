package com.example.qrscannerapp.features.homescreen.ui.widgets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun WidgetCard(
    accentColor: Color,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    glowStrength: Float = 0.5f,
    sheenFraction: Float = 0f,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var tiltX   by remember { mutableStateOf(0f) }
    var tiltY   by remember { mutableStateOf(0f) }
    var cardW   by remember { mutableStateOf(1f) }
    var cardH   by remember { mutableStateOf(1f) }

    val animTiltX by animateFloatAsState(
        targetValue   = tiltX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "tiltX"
    )
    val animTiltY by animateFloatAsState(
        targetValue   = tiltY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "tiltY"
    )
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label         = "scale"
    )

    val corner = 22.dp

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationX = animTiltX * 12f
                rotationY = animTiltY * 12f
                cameraDistance = 8f * density
                clip  = true
                shape = RoundedCornerShape(corner)
            }
            // Внешний glow
            .drawBehind {
                cardW = size.width
                cardH = size.height
                if (glowStrength > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.60f * glowStrength),
                                accentColor.copy(alpha = 0.25f * glowStrength),
                                accentColor.copy(alpha = 0.08f * glowStrength),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height),
                            radius = size.width * 0.90f
                        ),
                        center = Offset(size.width * 0.5f, size.height),
                        radius = size.width * 0.90f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.22f * glowStrength),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.2f, 0f),
                            radius = size.maxDimension * 0.55f
                        ),
                        center = Offset(size.width * 0.2f, 0f),
                        radius = size.maxDimension * 0.55f
                    )
                }
            }
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    blurRadius  = 14.dp,
                    tints       = listOf(HazeTint(color = Color.White.copy(alpha = 0.02f))),
                    noiseFactor = 0.02f
                )
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.09f),
                        Color(0xFF05050E)
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .drawWithContent {
                drawContent()
                drawThickBorder(corner.toPx(), accentColor)
                drawShimmer(sheenFraction)
            }
            // ЕДИНСТВЕННЫЙ pointerInput — обрабатывает и tap и drag
            // Если onClick == null — только parallax, без tap-реакции
            .pointerInput(onClick) {
                detectDragGestures(
                    onDragStart = {
                        cardW = size.width.toFloat()
                        cardH = size.height.toFloat()
                    },
                    onDrag = { change, _ ->
                        val nx = ((change.position.x - cardW / 2f) / (cardW / 2f)).coerceIn(-1f, 1f)
                        val ny = ((change.position.y - cardH / 2f) / (cardH / 2f)).coerceIn(-1f, 1f)
                        tiltY =  nx
                        tiltX = -ny
                    },
                    onDragEnd    = { tiltX = 0f; tiltY = 0f },
                    onDragCancel = { tiltX = 0f; tiltY = 0f }
                )
            }
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(onClick) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                try { tryAwaitRelease() } finally {
                                    pressed = false
                                    tiltX = 0f
                                    tiltY = 0f
                                }
                            },
                            onTap = { onClick() }
                        )
                    }
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

private fun DrawScope.drawThickBorder(cornerPx: Float, accentColor: Color) {
    val strokeW = 2.5f
    val half    = strokeW / 2f

    // Чистый Compose Path — без native canvas, без артефактов
    val borderPath = Path().apply {
        addRoundRect(
            RoundRect(
                left         = half,
                top          = half,
                right        = size.width  - half,
                bottom       = size.height - half,
                cornerRadius = CornerRadius(cornerPx, cornerPx)
            )
        )
    }

    drawPath(
        path  = borderPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                accentColor.copy(alpha = 0.75f),
                accentColor.copy(alpha = 0.95f),
                accentColor.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.16f)
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width, size.height)
        ),
        style = Stroke(
            width = strokeW,
            join  = androidx.compose.ui.graphics.StrokeJoin.Round,
            cap   = androidx.compose.ui.graphics.StrokeCap.Round
        )
    )

    // Верхняя рефракционная линия
    drawLine(
        color       = Color.White.copy(alpha = 0.30f),
        start       = Offset(size.width * 0.12f, 1.5f),
        end         = Offset(size.width * 0.88f, 1.5f),
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawShimmer(sheenFraction: Float) {
    val alpha = when {
        sheenFraction < 0.15f || sheenFraction > 0.45f -> 0f
        sheenFraction in 0.15f..0.20f -> (sheenFraction - 0.15f) / 0.05f * 0.14f
        sheenFraction in 0.40f..0.45f -> (0.45f - sheenFraction) / 0.05f * 0.14f
        else -> 0.14f
    }
    if (alpha <= 0f) return
    val sweepX  = (sheenFraction - 0.5f) * 2.8f
    val centerX = sweepX * size.width + size.width * 0.5f
    val hw      = size.width * 0.18f
    val tiltY   = size.height * 0.15f
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = alpha),
                Color.White.copy(alpha = alpha * 1.6f),
                Color.White.copy(alpha = alpha),
                Color.Transparent
            ),
            start = Offset(centerX - hw, -tiltY),
            end   = Offset(centerX + hw, size.height + tiltY)
        ),
        start       = Offset(centerX - hw, 0f),
        end         = Offset(centerX + hw, size.height),
        strokeWidth = hw * 2f
    )
}