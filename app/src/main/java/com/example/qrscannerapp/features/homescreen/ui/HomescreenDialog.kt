package com.example.qrscannerapp.features.homescreen.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * Центральное модальное окно в стиле iOS Focus/Do Not Disturb.
 * Появляется по центру экрана с анимацией scale + fade.
 * Компактное, не занимает весь экран.
 *
 * @param icon        Иконка в кружке сверху (опционально)
 * @param accentColor Цвет акцента — glow, кнопка, иконка
 * @param title       Заголовок
 * @param onExpand    Нажатие "Открыть" — переход на полный экран
 * @param onDismiss   Закрытие диалога
 * @param content     Содержимое между заголовком и кнопками
 */
@Composable
fun HomescreenDialog(
    title: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onExpand: (() -> Unit)? = null,
    expandLabel: String = "Открыть",
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    // Анимация появления — запускается сразу после показа
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(16) // один фрейм — чтобы Dialog успел отрисоваться
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "dialog_scale"
    )
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label         = "dialog_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress        = true,
            dismissOnClickOutside     = true,
            usePlatformDefaultWidth   = false  // сами управляем шириной
        )
    ) {
        // Затемнённый фон — клик закрывает
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * alpha))
                .clickable(
                    indication           = null,
                    interactionSource    = remember { MutableInteractionSource() },
                    onClick              = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Карточка — блокирует клик на фон
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(0.88f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        cameraDistance = 8f * density
                    }
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = {} // блокируем сквозной клик на фон
                    )
                    // Внешний glow
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.30f),
                                    accentColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.6f),
                                radius = size.maxDimension * 0.75f
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.6f),
                            radius = size.maxDimension * 0.75f
                        )
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF14141F),
                                Color(0xFF0C0C18)
                            )
                        )
                    )
                    // Gradient border
                    .drawBehind {
                        val r  = 28.dp.toPx()
                        val sw = 1.5f
                        val path = Path().apply {
                            addRoundRect(RoundRect(sw/2, sw/2, size.width-sw/2, size.height-sw/2, CornerRadius(r)))
                        }
                        drawPath(
                            path  = path,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    accentColor.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.08f)
                                ),
                                start = Offset(0f, 0f),
                                end   = Offset(size.width, size.height)
                            ),
                            style = Stroke(width = sw)
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Кнопка закрытия — правый верхний угол
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(1.dp))
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Закрыть",
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Иконка в цветном кружке
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = 0.50f),
                                                accentColor.copy(alpha = 0.15f),
                                                Color.Transparent
                                            ),
                                            radius = size.maxDimension
                                        ),
                                        radius = size.maxDimension
                                    )
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.85f),
                                            accentColor.copy(alpha = 0.55f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Заголовок
                    Text(
                        text       = title,
                        color      = Color.White,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Контент (задаётся снаружи)
                    content()

                    // Кнопка "Открыть" — если передан onExpand
                    if (onExpand != null) {
                        Spacer(Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .clickable {
                                    onDismiss()
                                    onExpand()
                                }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text       = expandLabel,
                                    color      = accentColor,
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ArrowForward,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}