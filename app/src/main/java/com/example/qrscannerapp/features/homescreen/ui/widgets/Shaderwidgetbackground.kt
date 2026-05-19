package com.example.qrscannerapp.features.homescreen.ui.widgets

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.withFrameNanos

/**
 * Modifier-расширение: рисует nebula-шейдер как фон виджета.
 *
 * Использует тот же AGSL-код что и AppBackground (nebula_background),
 * но цвета берёт из accentColor виджета — тёмный тон акцента как colorA,
 * насыщенный как colorB.
 *
 * Требует Android 13+ (API 33). На более старых — ничего не рисует,
 * WidgetCard сам покажет свой gradient-фон.
 *
 * 15fps (delay 66ms) — достаточно для медленного органичного движения,
 * не нагружает GPU.
 */

// AGSL-код nebula-шейдера встроен строкой — не нужен отдельный raw-файл для виджета.
// Это тот же nebula_background.glsl из проекта, скопирован сюда чтобы виджет
// не зависел от R.raw и мог использоваться независимо.
private val NEBULA_AGSL = """
uniform float2 iResolution;
uniform float  iTime;
uniform float  flowSpeed;
uniform float  complexity;
uniform float3 nebulaColorA;
uniform float3 nebulaColorB;
uniform float  density;

half hash(float2 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * (p.x + p.y));
}

half noise(float2 x) {
    float2 i = floor(x); float2 f = fract(x);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

half fbm(float2 p) {
    half v = 0.0; half a = 0.5; float2 shift = float2(100.0);
    mat2 rot = mat2(cos(0.3), sin(0.3), -sin(0.3), cos(0.3));
    v += a * noise(p); p = rot * p * 2.0 + shift; a *= 0.5;
    v += a * noise(p); p = rot * p * 2.0 + shift; a *= 0.5;
    v += a * noise(p);
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord.xy / iResolution.xy;
    float2 p  = (fragCoord.xy - 0.5 * iResolution.xy) / iResolution.y;
    float  t  = iTime * flowSpeed;

    float2 q = float2(
        fbm(p + float2(0.10 * t, 0.0)),
        fbm(p + float2(0.0, 0.06 * t) + float2(3.2, 1.5))
    );
    float2 r = float2(
        fbm(p + complexity * q + float2(1.7, 9.2) + float2(0.12 * t, 0.03 * t)),
        fbm(p + complexity * q + float2(8.3, 2.8) + float2(0.05 * t, 0.08 * t))
    );

    float f = fbm(p + complexity * 0.8 * r);

    float depth = 1.0 - uv.y;
    float blend = clamp(f * 1.3 + (1.0 - depth) * 0.5, 0.0, 1.0);
    float3 color = mix(nebulaColorA, nebulaColorB, smoothstep(0.0, 1.0, blend));

    float shimmer = smoothstep(0.62, 0.82, f) * (1.0 - depth * 0.8) * 0.14;
    color += nebulaColorB * shimmer;

    float vig = 1.0 - length(uv - 0.5) * 0.35;
    color *= vig;

    return half4(color * density, 1.0);
}
""".trimIndent()

/**
 * Создаёт и кэширует RuntimeShader для виджета.
 * Один объект на composable — не пересоздаётся при рекомпозиции.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun rememberWidgetShader(): RuntimeShader {
    return remember { RuntimeShader(NEBULA_AGSL) }
}

/**
 * Modifier который рисует nebula-шейдер как фон.
 *
 * Вызывай так:
 *   Box(
 *       modifier = Modifier
 *           .widgetShaderBackground(shader, accentColor, time)
 *   )
 *
 * [shader]      — из rememberWidgetShader()
 * [accentColor] — акцент виджета (Color(0xFF8B7FFF) и т.п.)
 * [time]        — текущее время в секундах (из виджет-таймера)
 * [flowSpeed]   — скорость течения, дефолт 0.08 (очень медленно)
 * [complexity]  — сложность завихрений, дефолт 1.2
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.widgetShaderBackground(
    shader: RuntimeShader,
    accentColor: Color,
    time: State<Float>,
    flowSpeed: Float = 0.08f,
    complexity: Float = 1.2f
): Modifier {
    // Цвета из акцента вычисляются один раз при создании модификатора
    val aR = accentColor.red   * 0.08f
    val aG = accentColor.green * 0.08f
    val aB = accentColor.blue  * 0.08f
    val bR = accentColor.red   * 0.65f
    val bG = accentColor.green * 0.65f
    val bB = accentColor.blue  * 0.65f

    // drawBehind — canvas-слой подписывается на snapshot при каждом чтении time.value
    // → при изменении time перерисовывается только canvas, без recomposition
    return this.drawBehind {
        shader.setFloatUniform("iResolution",  size.width, size.height)
        shader.setFloatUniform("flowSpeed",    flowSpeed)
        shader.setFloatUniform("complexity",   complexity)
        shader.setFloatUniform("nebulaColorA", aR, aG, aB)
        shader.setFloatUniform("nebulaColorB", bR, bG, bB)
        shader.setFloatUniform("density",      1.0f)
        shader.setFloatUniform("iTime",        time.value)
        drawRect(ShaderBrush(shader))
    }
}

/**
 * Таймер для виджета — синхронизирован с vsync через withFrameNanos.
 * Обновляется каждый кадр (60fps). Стартует с момента первой композиции.
 */
@Composable
fun rememberWidgetTime(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startNs = System.nanoTime()
        while (true) {
            withFrameNanos { frameNs ->
                time.floatValue = (frameNs - startNs) / 1_000_000_000f
            }
        }
    }
    return time
}