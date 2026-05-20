package com.example.qrscannerapp.features.homescreen.ui.widgets

import android.annotation.SuppressLint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import com.example.qrscannerapp.R
import kotlinx.coroutines.delay

/**
 * Глобальный shimmer-движок.
 *
 * CPU передаёт только iTime раз в 66мс (≈15fps).
 * Вся математика позиции/яркости луча — в shimmer.agsl на GPU.
 *
 * Каждый виджет вызывает .shimmerSheen(phaseOffset = X) —
 * сдвиг задаёт когда у него начинается цикл, блики не синхронны.
 *
 * Использование:
 *   // В HomescreenScreen оборачиваем контент:
 *   ProvideShimmerPhase { /* виджеты */ }
 *
 *   // В виджете:
 *   Modifier.shimmerSheen(phaseOffset = 0.3f)
 *   // или с кастомным периодом для skeleton-loader:
 *   Modifier.shimmerSheen(phaseOffset = 0f, period = 2.2f, angle = 0f)
 */

private const val SHIMMER_PERIOD_SECONDS = 8.0f

/**
 * CompositionLocal несёт iTime (секунды с запуска провайдера).
 * Default — заглушка 0f (если ProvideShimmerPhase не вызван).
 */
val LocalShimmerPhase = compositionLocalOf<State<Float>> {
    object : State<Float> { override val value = 0f }
}

/**
 * Единый shimmer-таймер для всего homescreen.
 * Один LaunchedEffect, тик раз в 66мс — без InfiniteTransition и recomposition.
 */
@Composable
fun ProvideShimmerPhase(content: @Composable () -> Unit) {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val t0 = System.currentTimeMillis()
        while (true) {
            delay(66L)
            time.floatValue = (System.currentTimeMillis() - t0) / 1000f
        }
    }
    CompositionLocalProvider(LocalShimmerPhase provides time, content = content)
}

/**
 * Composable Modifier: рисует shimmer-блик поверх контента.
 * API 33+ → RuntimeShader (GPU). Ниже → Brush.linearGradient (CPU fallback).
 *
 * [shimmerTime] — из LocalShimmerPhase.current (берётся автоматически)
 * [phaseOffset] — 0..1, сдвигает начало цикла (разные блики у разных виджетов)
 * [period]      — длина цикла в секундах (8.0 = медленный sheen, 2.2 = скелетон)
 * [angle]       — угол луча в радианах (0.61 ≈ 35°, 0.0 = горизонталь)
 * [intensity]   — яркость пика блика (0..1)
 */
@SuppressLint("NewApi")
@Composable
fun Modifier.shimmerSheen(
    shimmerTime: State<Float> = LocalShimmerPhase.current,
    phaseOffset: Float = 0f,
    period: Float = SHIMMER_PERIOD_SECONDS,
    angle: Float = 0.61f,
    intensity: Float = 0.28f
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        val shader = remember {
            context.resources.openRawResource(R.raw.shimmer)
                .bufferedReader().use { it.readText() }
                .let { RuntimeShader(it) }
        }
        this.shimmerOverlayShader(shader, shimmerTime, phaseOffset, period, angle, intensity)
    } else {
        this.shimmerOverlayLegacy(shimmerTime, phaseOffset, period, intensity)
    }
}

// GPU-путь: весь расчёт в шейдере, CPU передаёт только iTime
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Modifier.shimmerOverlayShader(
    shader: RuntimeShader,
    shimmerTime: State<Float>,
    phaseOffset: Float,
    period: Float,
    angle: Float,
    intensity: Float
): Modifier = this.drawWithCache {
    shader.setFloatUniform("iResolution",   size.width, size.height)
    shader.setFloatUniform("u_phaseOffset", phaseOffset)
    shader.setFloatUniform("u_period",      period)
    shader.setFloatUniform("u_angle",       angle)
    shader.setFloatUniform("u_intensity",   intensity)
    val shaderBrush = ShaderBrush(shader)
    onDrawWithContent {
        drawContent()
        shader.setFloatUniform("iTime", shimmerTime.value)  // state read → автоматический redraw
        drawRect(shaderBrush)
    }
}

// CPU fallback: горизонтальный gradient-sweep
private fun Modifier.shimmerOverlayLegacy(
    shimmerTime: State<Float>,
    phaseOffset: Float,
    period: Float,
    intensity: Float
): Modifier = this.drawWithContent {
    drawContent()
    val phase  = (shimmerTime.value / period + phaseOffset) % 1f
    val x      = (phase * 2f - 0.5f) * size.width  // -0.5w → 1.5w
    val hw     = size.width * 0.5f
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = intensity * 0.55f),
                Color.White.copy(alpha = intensity),
                Color.White.copy(alpha = intensity * 0.55f),
                Color.Transparent
            ),
            start = Offset(x - hw, 0f),
            end   = Offset(x + hw, 0f)
        )
    )
}