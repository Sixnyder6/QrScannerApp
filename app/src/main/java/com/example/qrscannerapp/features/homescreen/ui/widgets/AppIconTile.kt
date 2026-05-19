package com.example.qrscannerapp.features.homescreen.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AppIconTile — Apple iOS-стиль для складского приложения.
 *
 * Принципы:
 * - Фон иконки: тёмный нейтральный (#1C1C1E), почти чёрный
 * - Иконка: белая, чёткая, без tint
 * - Подпись: серая (#8E8E93) — системный серый Apple
 * - Акцент ТОЛЬКО если есть badge (непрочитанные, активные задачи)
 * - Никакого glow, shimmer, border-sweep
 * - Единственная анимация — scale при нажатии (0.92, spring)
 *
 * accentColor сохранён — используется только для badge.
 * phaseOffset сохранён для совместимости, не используется.
 */

// Apple системные цвета
private val IconBg       = Color(0xFF1C1C1E)
private val IconBgPress  = Color(0xFF2C2C2E)
private val IconFg       = Color(0xFFFFFFFF)
private val LabelColor   = Color(0xFF8E8E93)
private val BorderColor  = Color(0xFF2C2C2E)

private val IconHighlightBrush = Brush.verticalGradient(
    colorStops = arrayOf(
        0f   to Color.White.copy(alpha = 0.10f),
        0.4f to Color.White.copy(alpha = 0.03f),
        1f   to Color.Transparent
    )
)
private val IconHighlightStroke = Stroke(width = 0.5f)
private val BadgeBorderStroke   = Stroke(width = 1.5f)
private val BadgeBorderColor    = Color(0xFF000000).copy(alpha = 0.6f)

@Composable
fun AppIconTile(
    label: String,
    icon: ImageVector,
    accentColor: Color,                        // используется только для badge
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeColor: Color = accentColor,
    iconSize: Int = 56,
    phaseOffset: Float = 0f,                   // совместимость, не используется
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.92f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "scale"
    )

    val shape = RoundedCornerShape(16.dp)   // Apple icon corner — 16dp для 56dp иконки
    val currentBg = if (pressed) IconBgPress else IconBg

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try { tryAwaitRelease() } finally { pressed = false }
                    },
                    onTap = { onClick() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Иконка
            Box(
                modifier = Modifier
                    .size(iconSize.dp)
                    .clip(shape)
                    .background(currentBg)
                    // Тонкая граница — едва видна, даёт форму
                    .drawWithCache {
                        val cPx  = 16.dp.toPx()
                        val half = 0.5f
                        val path = Path().apply {
                            addRoundRect(RoundRect(
                                half, half,
                                size.width - half, size.height - half,
                                CornerRadius(cPx, cPx)
                            ))
                        }
                        onDrawWithContent {
                            drawContent()
                            drawPath(path = path, brush = IconHighlightBrush, style = IconHighlightStroke)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = label,
                    tint               = IconFg,
                    modifier           = Modifier.size((iconSize * 0.46f).dp)
                )
            }

            // Badge — единственное место где появляется цвет
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .graphicsLayer {
                            translationX =  (iconSize * 0.12f)
                            translationY = -(iconSize * 0.12f)
                        }
                        .size(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(badgeColor)
                        // Тонкая тёмная граница отделяет badge от фона
                        .drawBehind {
                            drawCircle(
                                color  = BadgeBorderColor,
                                radius = size.minDimension / 2f,
                                style  = BadgeBorderStroke
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = if (badgeText.length > 2) "•" else badgeText,
                        color      = Color.White,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign  = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Подпись — серая, нейтральная
        Text(
            text       = label,
            color      = LabelColor,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Normal,
            textAlign  = TextAlign.Center,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}