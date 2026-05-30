package com.example.qrscannerapp.common.ui

import android.app.ActivityManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import android.annotation.SuppressLint
import android.graphics.RuntimeShader
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import com.example.qrscannerapp.AppTheme
import com.example.qrscannerapp.R
import com.example.qrscannerapp.SettingsManager
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

private const val TAG = "AppBackground"

// =================================================================================
// ТЕМЫ
// =================================================================================

data class ShaderBackgroundTheme(
    val shaderResourceId: Int,
    val colors: List<Color>,
    val animationSpeed: Float = 0.3f,
    val complexity: Float = 1.5f,
    val brightness: Float = 1.0f,
    val piston1SpeedMult: Float = 2.0f,
    val piston2SpeedMult: Float = 1.5f,
    val rampMidPoint: Float = 0.3f,
    val rampCorePoint: Float = 0.6f,
    val density: Float = 1.0f,
    val breathRate: Float = 0.5f,
    val cellDensity: Float = 6.0f,
    val pulseStrength: Float = 0.6f,
    val shaderType: ShaderType = ShaderType.ENGINE
)

enum class ShaderType { ENGINE, NEBULA, VORONOI, SILK_DRAPE, AMBER_FLOW, EMBER_GLOW, AURORA, PLASMA }

// =============================================================================
// Тёмные темы
// =============================================================================

val EngineTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.engine_background,
    colors = listOf(
        Color(0.02f, 0.01f, 0.04f, 1.0f),
        Color(0.3f, 0.15f, 0.6f, 1.0f),
        Color(0.6f, 0.5f, 0.9f, 1.0f)
    ),
    animationSpeed = 0.25f,
    shaderType = ShaderType.ENGINE
)

val NebulaTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.nebula_background,
    colors = listOf(
        Color(0.0f, 0.01f, 0.09f, 1.0f),
        Color(0.0f, 0.18f, 0.45f, 1.0f),
        Color(0.0f, 0.52f, 0.90f, 1.0f)
    ),
    animationSpeed = 0.12f,
    complexity = 1.2f,
    density = 1.0f,
    shaderType = ShaderType.NEBULA
)

val VoronoiTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.voronoi_background,
    colors = listOf(
        Color(0.01f, 0.04f, 0.02f, 1.0f),
        Color(0.05f, 0.35f, 0.2f, 1.0f),
        Color(0.2f, 0.9f, 0.5f, 1.0f)
    ),
    animationSpeed = 0.2f,
    breathRate = 0.5f,
    cellDensity = 6.0f,
    pulseStrength = 0.6f,
    shaderType = ShaderType.VORONOI
)

// =============================================================================
// БЕЛАЯ ТЕМА
// =============================================================================
val WhiteTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.silk_drape,
    colors = listOf(
        Color(0.08f, 0.08f, 0.10f, 1.0f),
        Color(0.82f, 0.83f, 0.86f, 1.0f),
        Color(0.97f, 0.97f, 0.99f, 1.0f)
    ),
    animationSpeed = 0.15f,
    complexity = 1.3f,
    brightness = 1.1f,
    shaderType = ShaderType.SILK_DRAPE
)

// =============================================================================
// АВРОРА
// =============================================================================
val AuroraTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.aurora_borealis,
    colors = listOf(
        Color(0.0f,  0.68f, 0.28f, 1.0f),
        Color(0.0f,  0.52f, 0.88f, 1.0f),
        Color(0.55f, 0.0f,  0.88f, 1.0f)
    ),
    animationSpeed = 1.0f,
    complexity = 1.0f,
    brightness = 1.05f,
    shaderType = ShaderType.AURORA
)

// =============================================================================
// ПЛАЗМА
// =============================================================================
val PlasmaTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.plasma_field,
    colors = listOf(
        Color(0.06f, 0.0f,  0.18f, 1.0f),
        Color(0.75f, 0.0f,  0.62f, 1.0f),
        Color(0.0f,  0.88f, 1.0f,  1.0f)
    ),
    animationSpeed = 0.22f,
    complexity = 1.2f,
    brightness = 1.0f,
    shaderType = ShaderType.PLASMA
)

// =============================================================================
// КРАСНО-ПЕРСИКОВАЯ
// =============================================================================
val EmberTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.ember_glow,
    colors = listOf(
        Color(0.12f, 0.01f, 0.02f, 1.0f),
        Color(0.60f, 0.04f, 0.06f, 1.0f),
        Color(0.95f, 0.52f, 0.30f, 1.0f)
    ),
    animationSpeed = 0.14f,
    complexity = 1.3f,
    brightness = 1.0f,
    shaderType = ShaderType.EMBER_GLOW
)

// =============================================================================
// ЖЁЛТАЯ ТЕМА
// =============================================================================
val YellowTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.amber_flow,
    colors = listOf(
        Color(0.06f, 0.03f, 0.01f, 1.0f),
        Color(0.22f, 0.14f, 0.04f, 1.0f),
        Color(0.75f, 0.55f, 0.12f, 1.0f)
    ),
    animationSpeed = 0.18f,
    complexity = 1.6f,
    brightness = 1.1f,
    shaderType = ShaderType.AMBER_FLOW
)

fun AppTheme.toShaderTheme(): ShaderBackgroundTheme = when (this) {
    AppTheme.ENGINE  -> EngineTheme
    AppTheme.NEBULA  -> NebulaTheme
    AppTheme.VORONOI -> VoronoiTheme
    AppTheme.WHITE   -> WhiteTheme
    AppTheme.YELLOW  -> YellowTheme
    AppTheme.EMBER   -> EmberTheme
    AppTheme.AURORA  -> AuroraTheme
    AppTheme.PLASMA  -> PlasmaTheme
    AppTheme.RIVE    -> EngineTheme
}

// =================================================================================
// QUALITY LEVELS
// =================================================================================

private enum class BackgroundQuality {
    SHADER_60FPS,   // шейдер 60fps — полное качество
    SHADER_30FPS,   // шейдер 30fps — уменьшенная частота
    SHADER_15FPS,   // шейдер 15fps — минимум для плавности
    LEGACY          // только градиенты, шейдер не грузим
}

/**
 * Проверяет, поддерживает ли устройство AGSL на GPU (hardware accelerated).
 * На Android 14+ (API 34) — гарантированно GPU.
 * На Android 13 (API 33) — зависит от вендора (Samsung GPU, Pixel GPU — да, остальные — CPU).
 * На Android < 13 — нет AGSL.
 */
private fun isAgslGpuSupported(): Boolean {
    return when {
        Build.VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE -> true  // Android 14+
        Build.VERSION.SDK_INT < VERSION_CODES.TIRAMISU -> false          // Android < 13
        else -> {
            // Android 13 — проверяем по производителю/модели GPU
            val manufacturer = Build.MANUFACTURER.lowercase()
            val model = Build.MODEL.lowercase()
            // Известные проблемные вендоры на Android 13
            val cpuOnlyVendors = listOf("xiaomi", "redmi", "huawei", "honor", "oppo", "vivo", "realme", "tecno", "infinix")
            val isKnownBad = cpuOnlyVendors.any { manufacturer.contains(it) }
            if (isKnownBad) {
                Log.d(TAG, "GPU AGSL disabled: $manufacturer $model known CPU-only on API 33")
                false
            } else {
                true // Pixel, Samsung, OnePlus, Sony обычно нормально
            }
        }
    }
}

/**
 * Определяет, является ли текущая тема "тяжёлой" для шейдера.
 * Nebula, Aurora, Plasma — имеют fbm/noise, требуют больше GPU.
 */
private val heavyShaders = setOf(ShaderType.NEBULA, ShaderType.AURORA, ShaderType.PLASMA, ShaderType.VORONOI)

// =================================================================================
// FPS-BASED QUALITY SELECTOR
// =================================================================================

private class FpsStabilizer(
    private val shaderType: ShaderType
) {
    private var lastFps = 60f
    private var stableSince = 0L
    private var currentQuality = BackgroundQuality.SHADER_60FPS

    fun update(fps: Float, isGpuShader: Boolean): BackgroundQuality {
        val now = System.currentTimeMillis()

        // Адаптивный порог: для тяжёлых шейдеров выше терпимость
        val deviationThreshold = if (shaderType in heavyShaders) 15f else 10f

        if (abs(fps - lastFps) > deviationThreshold) {
            lastFps = fps
            stableSince = now
        }

        val stableDuration = now - stableSince
        val isStable = stableDuration > 2000L

        if (!isStable) return currentQuality

        // Если шейдер на CPU — сразу LEGACY, GPU не спасёт
        if (!isGpuShader) {
            if (currentQuality != BackgroundQuality.LEGACY) {
                currentQuality = BackgroundQuality.LEGACY
                Log.w(TAG, "AGSL on CPU detected, falling back to LEGACY")
            }
            return currentQuality
        }

        val target = when {
            fps > 52f -> BackgroundQuality.SHADER_60FPS
            fps > 40f -> BackgroundQuality.SHADER_30FPS
            fps > 30f -> BackgroundQuality.SHADER_15FPS
            else -> BackgroundQuality.LEGACY
        }

        if (target != currentQuality) {
            currentQuality = target
            Log.d(TAG, "Quality changed to $currentQuality (fps=$fps, type=${shaderType})")
        }
        return currentQuality
    }

    fun forceReset(quality: BackgroundQuality) {
        currentQuality = quality
        lastFps = 60f
        stableSince = 0L
    }
}

// =================================================================================
// APP BACKGROUND
// =================================================================================

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    val currentTheme by settingsManager.appThemeFlow.collectAsState(
        initial = AppTheme.ENGINE
    )
    val shaderTheme = currentTheme.toShaderTheme()

    // ── Системные проверки ──
    val am = remember { context.getSystemService(ActivityManager::class.java) }
    val isLowRam = remember { am.isLowRamDevice }
    val animOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val hasShaderApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val isGpuShader = remember { isAgslGpuSupported() }

    // Стабилизатор — создаётся под конкретный тип шейдера
    val stabilizer = remember(shaderTheme.shaderType) { FpsStabilizer(shaderTheme.shaderType) }
    var quality by remember { mutableStateOf(BackgroundQuality.SHADER_60FPS) }

    // FPS монитор — считаем кадры на том же drawWithCache, где рисуется шейдер
    // Передаём frameCount через callback, который будет вызываться из ShaderBackgroundWithQuality
    if (hasShaderApi && !isLowRam && !animOff && isGpuShader) {
        ShaderBackgroundWithQualityAndFps(
            shaderTheme = shaderTheme,
            stabilizer = stabilizer,
            onQualityChanged = { quality = it },
            modifier = modifier,
            content = content
        )
    } else {
        // Без шейдеров — Legacy или Rive
        if (currentTheme == AppTheme.RIVE) {
            RiveBackground(modifier, content)
        } else {
            LegacyGradientBackground(modifier, shaderTheme.colors, content)
        }
    }
}

// =================================================================================
// SHADER BACKGROUND — с FPS мониторингом ВНУТРИ шейдерного draw
// =================================================================================

@SuppressLint("NewApi")
@RequiresApi(VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderBackgroundWithQualityAndFps(
    shaderTheme: ShaderBackgroundTheme,
    stabilizer: FpsStabilizer,
    onQualityChanged: (BackgroundQuality) -> Unit,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Lazy load шейдера
    val context = LocalContext.current
    val shader = remember(shaderTheme.shaderResourceId, shaderTheme.shaderType) {
        try {
            context.resources.openRawResource(shaderTheme.shaderResourceId)
                .bufferedReader().use { it.readText() }
                .let { RuntimeShader(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load shader ${shaderTheme.shaderResourceId}", e)
            null
        }
    }

    // Если шейдер не загрузился — fallback на Legacy
    if (shader == null) {
        LegacyGradientBackground(modifier, shaderTheme.colors, content)
        return
    }

    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    // ── Состояния ──
    var time by remember { mutableFloatStateOf(0f) }
    val useEnginePulse = shaderTheme.shaderType == ShaderType.ENGINE
    var pulsatingBrightness by remember { mutableFloatStateOf(0.375f) }
    var pulsatingRadius by remember { mutableFloatStateOf(0.20f) }

    // ── FPS счётчик ──
    var frameCount by remember { mutableIntStateOf(0) }
    var lastCheck by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var localQuality by remember { mutableStateOf(BackgroundQuality.SHADER_60FPS) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            val now = System.currentTimeMillis()
            val elapsed = (now - lastCheck) / 1000f
            val fps = frameCount / elapsed.coerceAtLeast(0.5f)
            Log.d(TAG, "Shader FPS: $fps (frames: $frameCount in ${elapsed}s)")
            val newQuality = stabilizer.update(fps, isGpuShader = true)
            if (newQuality != localQuality) {
                localQuality = newQuality
                onQualityChanged(newQuality)
            }
            frameCount = 0
            lastCheck = now
        }
    }

    // Тик времени — адаптивная частота под качество
    val actualTickMs = when (localQuality) {
        BackgroundQuality.SHADER_60FPS -> 16L
        BackgroundQuality.SHADER_30FPS -> 33L
        BackgroundQuality.SHADER_15FPS -> 66L
        BackgroundQuality.LEGACY -> 66L // fallback, но сюда не дойдём
    }

    LaunchedEffect(actualTickMs) {
        val startMs = System.currentTimeMillis()
        while (true) {
            delay(actualTickMs)
            val elapsed = (System.currentTimeMillis() - startMs) / 1000f
            time = elapsed
            if (useEnginePulse) {
                pulsatingBrightness = 0.375f + 0.075f * sin(elapsed * 0.7854f)
                pulsatingRadius = 0.20f + 0.02f * sin(elapsed * 0.6283f)
            }
        }
    }

    // Если качество упало до LEGACY — переключаемся на градиенты
    if (localQuality == BackgroundQuality.LEGACY) {
        LegacyGradientBackground(modifier, shaderTheme.colors, content)
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                // ═════════════════════════════════════════════════════
                // Статические uniform'ы — только при смене размера
                // ═════════════════════════════════════════════════════
                shader.setFloatUniform("iResolution", size.width, size.height)
                when (shaderTheme.shaderType) {
                    ShaderType.ENGINE -> {
                        shader.setFloatUniform("backgroundColor",
                            shaderTheme.colors[0].red, shaderTheme.colors[0].green,
                            shaderTheme.colors[0].blue, shaderTheme.colors[0].alpha)
                        shader.setFloatUniform("midEnergyColor",
                            shaderTheme.colors[1].red, shaderTheme.colors[1].green,
                            shaderTheme.colors[1].blue, shaderTheme.colors[1].alpha)
                        shader.setFloatUniform("coreEnergyColor",
                            shaderTheme.colors[2].red, shaderTheme.colors[2].green,
                            shaderTheme.colors[2].blue, shaderTheme.colors[2].alpha)
                        shader.setFloatUniform("animationSpeed",   shaderTheme.animationSpeed)
                        shader.setFloatUniform("piston1SpeedMult", shaderTheme.piston1SpeedMult)
                        shader.setFloatUniform("piston2SpeedMult", shaderTheme.piston2SpeedMult)
                        shader.setFloatUniform("rampMidPoint",     shaderTheme.rampMidPoint)
                        shader.setFloatUniform("rampCorePoint",    shaderTheme.rampCorePoint)
                    }
                    ShaderType.NEBULA -> {
                        shader.setFloatUniform("flowSpeed",   shaderTheme.animationSpeed)
                        shader.setFloatUniform("complexity",  shaderTheme.complexity)
                        shader.setFloatUniform("nebulaColorA",
                            shaderTheme.colors[0].red, shaderTheme.colors[0].green, shaderTheme.colors[0].blue)
                        shader.setFloatUniform("nebulaColorB",
                            shaderTheme.colors[2].red, shaderTheme.colors[2].green, shaderTheme.colors[2].blue)
                        shader.setFloatUniform("density", shaderTheme.density)
                    }
                    ShaderType.VORONOI -> {
                        shader.setFloatUniform("breathRate",   shaderTheme.breathRate)
                        shader.setFloatUniform("cellDensity",  shaderTheme.cellDensity)
                        shader.setFloatUniform("tissueColor",
                            shaderTheme.colors[0].red, shaderTheme.colors[0].green, shaderTheme.colors[0].blue)
                        shader.setFloatUniform("glowColor",
                            shaderTheme.colors[2].red, shaderTheme.colors[2].green, shaderTheme.colors[2].blue)
                        shader.setFloatUniform("pulseStrength", shaderTheme.pulseStrength)
                    }
                    ShaderType.SILK_DRAPE -> {
                        shader.setFloatUniform("flowSpeed",  shaderTheme.animationSpeed)
                        shader.setFloatUniform("complexity", shaderTheme.complexity)
                        shader.setFloatUniform("deepFoldColor",
                            shaderTheme.colors[0].red, shaderTheme.colors[0].green, shaderTheme.colors[0].blue)
                        shader.setFloatUniform("silkBaseColor",
                            shaderTheme.colors[1].red, shaderTheme.colors[1].green, shaderTheme.colors[1].blue)
                        shader.setFloatUniform("highlightColor",
                            shaderTheme.colors[2].red, shaderTheme.colors[2].green, shaderTheme.colors[2].blue)
                        shader.setFloatUniform("brightness", shaderTheme.brightness)
                    }
                    ShaderType.AMBER_FLOW -> {
                        shader.setFloatUniform("flowSpeed",  shaderTheme.animationSpeed)
                        shader.setFloatUniform("complexity", shaderTheme.complexity)
                        shader.setFloatUniform("baseColor",
                            shaderTheme.colors[0].red, shaderTheme.colors[0].green, shaderTheme.colors[0].blue)
                        shader.setFloatUniform("midColor",
                            shaderTheme.colors[1].red, shaderTheme.colors[1].green, shaderTheme.colors[1].blue)
                        shader.setFloatUniform("glowColor",
                            shaderTheme.colors[2].red, shaderTheme.colors[2].green, shaderTheme.colors[2].blue)
                        shader.setFloatUniform("brightness", shaderTheme.brightness)
                    }
                    ShaderType.EMBER_GLOW,
                    ShaderType.AURORA,
                    ShaderType.PLASMA -> {
                        shader.setFloatUniform("flowSpeed",   shaderTheme.animationSpeed)
                        shader.setFloatUniform("complexity",  shaderTheme.complexity)
                        shader.setFloatUniform("layerColorA",
                            shaderTheme.colors[0].red, shaderTheme.colors[0].green, shaderTheme.colors[0].blue)
                        shader.setFloatUniform("layerColorB",
                            shaderTheme.colors[1].red, shaderTheme.colors[1].green, shaderTheme.colors[1].blue)
                        shader.setFloatUniform("layerColorC",
                            shaderTheme.colors[2].red, shaderTheme.colors[2].green, shaderTheme.colors[2].blue)
                        shader.setFloatUniform("brightness", shaderTheme.brightness)
                    }
                }

                // ═════════════════════════════════════════════════════
                // onDrawBehind — РИСУЕМ ШЕЙДЕР И СЧИТАЕМ КАДРЫ
                // ═════════════════════════════════════════════════════
                onDrawBehind {
                    // ✅ СЧЁТЧИК КАДРОВ ПРЯМО ЗДЕСЬ — измеряет реальные кадры шейдера
                    frameCount++

                    shader.setFloatUniform("iTime", time)
                    if (useEnginePulse) {
                        shader.setFloatUniform("energyBrightness", pulsatingBrightness)
                        shader.setFloatUniform("mainOrbitRadius",  pulsatingRadius)
                    }
                    drawRect(shaderBrush)
                }
            }
    ) {
        content()
    }
}

// =================================================================================
// RIVE BACKGROUND
// =================================================================================

@Composable
private fun RiveBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                RiveAnimationView(ctx).also { view ->
                    view.fit = Fit.COVER
                    view.setRiveResource(R.raw.riv_background)
                    view.play()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        content()
    }
}

// =================================================================================
// LEGACY BACKGROUND (Android < 13, низкая производительность, CPU шейдер)
// =================================================================================

@Composable
private fun LegacyGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "legacy_bg")

    val progress1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 15000, easing = LinearEasing),
            RepeatMode.Reverse
        )
    )
    val progress2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 22000, easing = LinearEasing),
            RepeatMode.Reverse
        )
    )
    val progress3 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 18000, easing = LinearEasing),
            RepeatMode.Reverse
        )
    )

    val baseColor = colors.getOrNull(0) ?: Color.Black
    val midColor  = colors.getOrNull(1) ?: Color.Blue
    val coreColor = colors.getOrNull(2) ?: Color.Magenta

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    drawRect(brush = Brush.verticalGradient(
                        listOf(baseColor, baseColor.copy(alpha = 0.8f))
                    ))
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(midColor.copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(
                                size.width * progress3,
                                size.height * (1f - progress2)
                            ),
                            radius = size.width * (1.2f + progress2 * 0.5f)
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(coreColor.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(
                                size.width * (1f - progress1),
                                size.height * progress3
                            ),
                            radius = size.width * (0.8f + progress1 * 0.3f)
                        ),
                        blendMode = BlendMode.Plus
                    )
                }
            }
    ) {
        content()
    }
}