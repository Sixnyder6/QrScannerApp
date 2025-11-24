// File: LoadingScreen.kt

package com.example.qrscannerapp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// import androidx.compose.foundation.background // <-- ЭТОТ ИМПОРТ БОЛЬШЕ НЕ НУЖЕН
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
// import androidx.compose.runtime.remember // <-- ЭТОТ ИМПОРТ БОЛЬШЕ НЕ НУЖЕН
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
// import androidx.compose.ui.graphics.Brush // <-- ЭТОТ ИМПОРТ БОЛЬШЕ НЕ НУЖЕН
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.common.ui.AppBackground // V-- НОВЫЙ ИМПОРТ --V

@Composable
fun LoadingScreen() {
    // V-- НАЧАЛО ИЗМЕНЕНИЙ: ИНТЕГРАЦИЯ APPBACKGROUND --V
    // val staticBrush = remember { ... } // <-- ЭТОТ БЛОК УДАЛЕН

    AppBackground {
        // Добавляем Box для центрирования контента, так как AppBackground отвечает только за фон.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Создаем анимацию "дыхания"
            val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")

            // Анимируем масштаб (увеличение/уменьшение)
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )

            // Анимируем прозрачность
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_alpha"
            )

            // Создаем стилизованный логотип-плейсхолдер
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer { // Применяем анимации
                        scaleX = scale
                        scaleY = scale
                    }
                    .alpha(alpha)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = StardustItemBg.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "QS",
                    color = StardustTextSecondary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
}