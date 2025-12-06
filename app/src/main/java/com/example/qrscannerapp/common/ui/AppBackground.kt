// Файл: common/ui/AppBackground.kt
package com.example.qrscannerapp.common.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RawRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import com.example.qrscannerapp.R

data class ShaderBackgroundTheme(
    @RawRes val shaderResourceId: Int,
    val colors: List<Color>,
    val animationSpeed: Float = 0.3f,
    val piston1SpeedMult: Float = 2.0f,
    val piston2SpeedMult: Float = 1.5f,
    val rampMidPoint: Float = 0.3f,
    val rampCorePoint: Float = 0.6f
)

val DeepSpaceTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.engine_background,
    colors = listOf(
        Color(0.02f, 0.01f, 0.04f, 1.0f),
        Color(0.3f, 0.15f, 0.6f, 1.0f),
        Color(0.6f, 0.5f, 0.9f, 1.0f)
    ),
    animationSpeed = 0.25f
)

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    theme: ShaderBackgroundTheme = DeepSpaceTheme,
    content: @Composable BoxScope.() -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ShaderPoweredBackground(modifier, theme, content)
    } else {
        // Устройства со старой версией Android теперь будут использовать
        // новую, значительно улучшенную версию Legacy-фона.
        LegacyGradientBackground(modifier, theme.colors, content)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderPoweredBackground(
    modifier: Modifier = Modifier,
    theme: ShaderBackgroundTheme,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val shader = remember {
        val shaderSrc = context.resources.openRawResource(theme.shaderResourceId)
            .bufferedReader().use { it.readText() }
        RuntimeShader(shaderSrc)
    }
    val shaderBrush = remember { ShaderBrush(shader) }
    val infiniteTransition = rememberInfiniteTransition(label = "shader_master_transition")

    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 3600f,
        animationSpec = infiniteRepeatable(tween(3_600_000, easing = LinearEasing)),
        label = "time"
    )
    val pulsatingBrightness by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "brightness"
    )
    val pulsatingRadius by infiniteTransition.animateFloat(
        initialValue = 0.18f, targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "radius"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("backgroundColor", theme.colors[0].red, theme.colors[0].green, theme.colors[0].blue, theme.colors[0].alpha)
                shader.setFloatUniform("midEnergyColor", theme.colors[1].red, theme.colors[1].green, theme.colors[1].blue, theme.colors[1].alpha)
                shader.setFloatUniform("coreEnergyColor", theme.colors[2].red, theme.colors[2].green, theme.colors[2].blue, theme.colors[2].alpha)
                shader.setFloatUniform("animationSpeed", theme.animationSpeed)
                shader.setFloatUniform("piston1SpeedMult", theme.piston1SpeedMult)
                shader.setFloatUniform("piston2SpeedMult", theme.piston2SpeedMult)
                shader.setFloatUniform("rampMidPoint", theme.rampMidPoint)
                shader.setFloatUniform("rampCorePoint", theme.rampCorePoint)
                onDrawBehind {
                    shader.setFloatUniform("iTime", time)
                    shader.setFloatUniform("energyBrightness", pulsatingBrightness)
                    shader.setFloatUniform("mainOrbitRadius", pulsatingRadius)
                    drawRect(shaderBrush)
                }
            }
    ) {
        content()
    }
}

// --- ИЗМЕНЕНИЕ НАЧАЛОСЬ ЗДЕСЬ ---
// Мы полностью заменяем старый Legacy-фон на новый, многослойный,
// чтобы он выглядел достойно на флагманах вроде S10+.

@Composable
private fun LegacyGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "super_legacy_transition")

    // Анимируем несколько параметров для создания сложного, не повторяющегося движения
    val progress1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse)
    )
    val progress2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse)
    )
    val progress3 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse)
    )

    // Безопасно извлекаем цвета из темы, предоставляя значения по умолчанию
    val baseColor = colors.getOrNull(0) ?: Color.Black
    val midColor = colors.getOrNull(1) ?: Color.Blue
    val coreColor = colors.getOrNull(2) ?: Color.Magenta

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    // Слой 1: Основной темный фон, чтобы избежать плоского черного цвета.
                    drawRect(brush = Brush.verticalGradient(listOf(baseColor, baseColor.copy(alpha = 0.8f))))

                    // Слой 2: Большое, медленное "дыхание" света для атмосферы.
                    val radius2 = size.width * (1.2f + progress2 * 0.5f)
                    val center2 = Offset(size.width * progress3, size.height * (1f - progress2))
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(midColor.copy(alpha = 0.3f), Color.Transparent),
                            center = center2,
                            radius = radius2
                        )
                    )

                    // Слой 3: Более яркое и быстрое "ядро", имитирующее центр шейдера.
                    val radius1 = size.width * (0.8f + progress1 * 0.3f)
                    val center1 = Offset(size.width * (1f - progress1), size.height * progress3)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(coreColor.copy(alpha = 0.4f), Color.Transparent),
                            center = center1,
                            radius = radius1
                        ),
                        // Режим смешивания 'Plus' красиво складывает цвета, делая их ярче при пересечении.
                        blendMode = BlendMode.Plus
                    )
                }
            }
    ) {
        content()
    }
}