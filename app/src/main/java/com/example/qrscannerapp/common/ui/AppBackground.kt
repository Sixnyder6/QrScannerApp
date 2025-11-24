// Файл: common/ui/AppBackground.kt
package com.example.qrscannerapp.common.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
// V-- ИЗМЕНЕНИЕ 1: Ненужные импорты удалены (Hilt, ViewModel, PerformanceClass) --V

private const val AGSL_WAVY_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform half4 color1;
    uniform half4 color2;
    uniform half4 color3;

    float random(float2 st) {
        return fract(sin(dot(st.xy, float2(12.9898, 78.233))) * 43758.5453123);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord.xy / iResolution.xy;
        float wave1 = sin(uv.x * 3.0 + uv.y * 5.0 + iTime * 0.1);
        float wave2 = cos(uv.x * 2.0 - uv.y * 4.0 + iTime * 0.2);
        float basePattern = (wave1 + wave2) * 0.5;
        float noise = random(uv + iTime * 0.01) * 0.1 - 0.05;
        float finalValue = basePattern + noise;
        half4 finalColor = mix(color1, color2, smoothstep(-0.5, 0.0, finalValue));
        finalColor = mix(finalColor, color3, smoothstep(0.0, 0.5, finalValue));
        return half4(finalColor.rgb, 1.0);
    }
"""

// V-- ИЗМЕНЕНИЕ 2: Сигнатура и логика функции полностью упрощены --V
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // "Мозг" отключен. Логика теперь зависит только от версии ОС.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Если ОС поддерживает шейдеры, всегда используем их.
        ShaderPoweredBackground(modifier, content)
    } else {
        // Для старых версий ОС используем запасную анимацию.
        LegacyGradientBackground(modifier, content)
    }
}

// Эта функция больше не используется, но оставим ее на случай будущих изменений.
@Composable
private fun StaticBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StaticBackgroundColor)
    ) {
        content()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderPoweredBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "time_transition")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_600_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val shader = remember { RuntimeShader(AGSL_WAVY_SHADER_SRC) }
    val shaderBrush = remember { ShaderBrush(shader) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                shader.setFloatUniform("iResolution", size.width, size.height)
                val color1 = ShaderColors[0]
                val color2 = ShaderColors[1]
                val color3 = ShaderColors[2]
                shader.setFloatUniform("color1", color1.red, color1.green, color1.blue, color1.alpha)
                shader.setFloatUniform("color2", color2.red, color2.green, color2.blue, color2.alpha)
                shader.setFloatUniform("color3", color3.red, color3.green, color3.blue, color3.alpha)

                onDrawBehind {
                    shader.setFloatUniform("iTime", time)
                    drawRect(shaderBrush)
                }
            }
    ) {
        content()
    }
}

@Composable
private fun LegacyGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "legacy_wave_transition")
    val animatedProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1_progress"
    )
    val animatedProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height
                val diagonal = width + height

                val shift2 = (animatedProgress2 * 2f - 1f) * diagonal
                val brush2 = Brush.linearGradient(
                    colors = WaveGradientColors2,
                    start = Offset(shift2, 0f),
                    end = Offset(0f, shift2)
                )
                drawRect(brush2)

                val shift1 = (animatedProgress1 * 2f - 1f) * diagonal
                val brush1 = Brush.linearGradient(
                    colors = WaveGradientColors1,
                    start = Offset(0f, shift1),
                    end = Offset(shift1, 0f)
                )
                drawRect(brush1)
            }
    ) {
        content()
    }
}