package com.example.qrscannerapp.common.ui

import android.app.ActivityManager
import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.annotation.RawRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
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

// =================================================================================
// ТЕМЫ
// =================================================================================

data class ShaderBackgroundTheme(
    @RawRes val shaderResourceId: Int,
    val colors: List<Color>,
    val animationSpeed: Float = 0.3f,
    val complexity: Float = 1.5f,
    val brightness: Float = 1.0f,
    // Engine-specific
    val piston1SpeedMult: Float = 2.0f,
    val piston2SpeedMult: Float = 1.5f,
    val rampMidPoint: Float = 0.3f,
    val rampCorePoint: Float = 0.6f,
    // Nebula-specific
    val density: Float = 1.0f,
    // Voronoi-specific
    val breathRate: Float = 0.5f,
    val cellDensity: Float = 6.0f,
    val pulseStrength: Float = 0.6f,
    // Metadata
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
        Color(0.0f, 0.01f, 0.09f, 1.0f),   // глубокий тёмный синий (дно)
        Color(0.0f, 0.18f, 0.45f, 1.0f),   // средний океанский синий
        Color(0.0f, 0.52f, 0.90f, 1.0f)    // светлый голубой (поверхность)
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
// БЕЛАЯ ТЕМА — Шёлковая ткань, стекающая по поверхности
// =============================================================================
val WhiteTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.silk_drape,
    colors = listOf(
        Color(0.08f, 0.08f, 0.10f, 1.0f),  // Глубокие складки: тёмный графит
        Color(0.82f, 0.83f, 0.86f, 1.0f),  // Базовый шёлк: светло-серый с голубизной
        Color(0.97f, 0.97f, 0.99f, 1.0f)   // Блики: почти белый, атласный
    ),
    animationSpeed = 0.15f,
    complexity = 1.3f,
    brightness = 1.1f,
    shaderType = ShaderType.SILK_DRAPE
)

// =============================================================================
// АВРОРА — Северное сияние
// =============================================================================
val AuroraTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.aurora_borealis,
    colors = listOf(
        Color(0.0f,  0.68f, 0.28f, 1.0f),  // aurora green
        Color(0.0f,  0.52f, 0.88f, 1.0f),  // aurora blue/teal
        Color(0.55f, 0.0f,  0.88f, 1.0f)   // aurora violet
    ),
    animationSpeed = 1.0f,
    complexity = 1.0f,
    brightness = 1.05f,
    shaderType = ShaderType.AURORA
)

// =============================================================================
// ПЛАЗМА — Электрическое поле
// =============================================================================
val PlasmaTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.plasma_field,
    colors = listOf(
        Color(0.06f, 0.0f,  0.18f, 1.0f),  // глубокий индиго
        Color(0.75f, 0.0f,  0.62f, 1.0f),  // горячая маджента
        Color(0.0f,  0.88f, 1.0f,  1.0f)   // электрик-циан
    ),
    animationSpeed = 0.22f,
    complexity = 1.2f,
    brightness = 1.0f,
    shaderType = ShaderType.PLASMA
)

// =============================================================================
// КРАСНО-ПЕРСИКОВАЯ ТЕМА — Жар / тёмное бордо → красный → персик
// =============================================================================
val EmberTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.ember_glow,
    colors = listOf(
        Color(0.12f, 0.01f, 0.02f, 1.0f),  // тёмное бордо
        Color(0.60f, 0.04f, 0.06f, 1.0f),  // насыщенный красный
        Color(0.95f, 0.52f, 0.30f, 1.0f)   // тёплый персик
    ),
    animationSpeed = 0.14f,
    complexity = 1.3f,
    brightness = 1.0f,
    shaderType = ShaderType.EMBER_GLOW
)

// =============================================================================
// ЖЁЛТАЯ ТЕМА — Янтарная смола / жидкое солнце
// =============================================================================
val YellowTheme = ShaderBackgroundTheme(
    shaderResourceId = R.raw.amber_flow,
    colors = listOf(
        Color(0.06f, 0.03f, 0.01f, 1.0f),  // Глубокий коричневый
        Color(0.22f, 0.14f, 0.04f, 1.0f),  // Бронза/янтарь
        Color(0.75f, 0.55f, 0.12f, 1.0f)   // Золотое свечение
    ),
    animationSpeed = 0.18f,
    complexity = 1.6f,
    brightness = 1.1f,
    shaderType = ShaderType.AMBER_FLOW
)

// Маппинг enum → тема
fun AppTheme.toShaderTheme(): ShaderBackgroundTheme = when (this) {
    AppTheme.ENGINE  -> EngineTheme
    AppTheme.NEBULA  -> NebulaTheme
    AppTheme.VORONOI -> VoronoiTheme
    AppTheme.WHITE   -> WhiteTheme
    AppTheme.YELLOW  -> YellowTheme
    AppTheme.EMBER   -> EmberTheme
    AppTheme.AURORA  -> AuroraTheme
    AppTheme.PLASMA  -> PlasmaTheme
    AppTheme.RIVE    -> EngineTheme // шейдер не используется, но when должен быть exhaustive
}

// =================================================================================
// APP BACKGROUND — точка входа
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

    // Автодетект: низкая память ИЛИ пользователь отключил анимации в настройках телефона
    val isLowPerf = remember {
        val am = context.getSystemService(ActivityManager::class.java)
        val animScale = Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        )
        am.isLowRamDevice || animScale == 0f
    }

    if (currentTheme == AppTheme.RIVE) {
        RiveBackground(modifier, content)
    } else if (!isLowPerf && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ShaderPoweredBackground(modifier, shaderTheme, content)
    } else {
        LegacyGradientBackground(modifier, shaderTheme.colors, content)
    }
}

// =================================================================================
// SHADER BACKGROUND
// =================================================================================

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderPoweredBackground(
    modifier: Modifier = Modifier,
    theme: ShaderBackgroundTheme,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current

    val shader = remember(theme.shaderResourceId) {
        val shaderSrc = context.resources.openRawResource(theme.shaderResourceId)
            .bufferedReader().use { it.readText() }
        RuntimeShader(shaderSrc)
    }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    // ~15fps для фона — шейдер не требует 60fps, экономим GPU в 4x
    var time by remember { mutableFloatStateOf(0f) }
    var pulsatingBrightness by remember { mutableFloatStateOf(0.375f) }
    var pulsatingRadius by remember { mutableFloatStateOf(0.20f) }

    LaunchedEffect(Unit) {
        val startMs = System.currentTimeMillis()
        while (true) {
            delay(66L)
            val elapsed = (System.currentTimeMillis() - startMs) / 1000f
            time = elapsed
            if (theme.shaderType == ShaderType.ENGINE) {
                pulsatingBrightness = 0.375f + 0.075f * sin(elapsed * 0.7854f)
                pulsatingRadius     = 0.20f  + 0.02f  * sin(elapsed * 0.6283f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                // Статические юниформы — только при смене размера/темы, не каждый кадр
                shader.setFloatUniform("iResolution", size.width, size.height)
                when (theme.shaderType) {
                    ShaderType.ENGINE -> {
                        shader.setFloatUniform("backgroundColor",
                            theme.colors[0].red, theme.colors[0].green,
                            theme.colors[0].blue, theme.colors[0].alpha)
                        shader.setFloatUniform("midEnergyColor",
                            theme.colors[1].red, theme.colors[1].green,
                            theme.colors[1].blue, theme.colors[1].alpha)
                        shader.setFloatUniform("coreEnergyColor",
                            theme.colors[2].red, theme.colors[2].green,
                            theme.colors[2].blue, theme.colors[2].alpha)
                        shader.setFloatUniform("animationSpeed",   theme.animationSpeed)
                        shader.setFloatUniform("piston1SpeedMult", theme.piston1SpeedMult)
                        shader.setFloatUniform("piston2SpeedMult", theme.piston2SpeedMult)
                        shader.setFloatUniform("rampMidPoint",     theme.rampMidPoint)
                        shader.setFloatUniform("rampCorePoint",    theme.rampCorePoint)
                    }
                    ShaderType.NEBULA -> {
                        shader.setFloatUniform("flowSpeed",   theme.animationSpeed)
                        shader.setFloatUniform("complexity",  theme.complexity)
                        shader.setFloatUniform("nebulaColorA",
                            theme.colors[0].red, theme.colors[0].green, theme.colors[0].blue)
                        shader.setFloatUniform("nebulaColorB",
                            theme.colors[2].red, theme.colors[2].green, theme.colors[2].blue)
                        shader.setFloatUniform("density", theme.density)
                    }
                    ShaderType.VORONOI -> {
                        shader.setFloatUniform("breathRate",   theme.breathRate)
                        shader.setFloatUniform("cellDensity",  theme.cellDensity)
                        shader.setFloatUniform("tissueColor",
                            theme.colors[0].red, theme.colors[0].green, theme.colors[0].blue)
                        shader.setFloatUniform("glowColor",
                            theme.colors[2].red, theme.colors[2].green, theme.colors[2].blue)
                        shader.setFloatUniform("pulseStrength", theme.pulseStrength)
                    }
                    ShaderType.SILK_DRAPE -> {
                        shader.setFloatUniform("flowSpeed",  theme.animationSpeed)
                        shader.setFloatUniform("complexity", theme.complexity)
                        shader.setFloatUniform("deepFoldColor",
                            theme.colors[0].red, theme.colors[0].green, theme.colors[0].blue)
                        shader.setFloatUniform("silkBaseColor",
                            theme.colors[1].red, theme.colors[1].green, theme.colors[1].blue)
                        shader.setFloatUniform("highlightColor",
                            theme.colors[2].red, theme.colors[2].green, theme.colors[2].blue)
                        shader.setFloatUniform("brightness", theme.brightness)
                    }
                    ShaderType.AMBER_FLOW -> {
                        shader.setFloatUniform("flowSpeed",  theme.animationSpeed)
                        shader.setFloatUniform("complexity", theme.complexity)
                        shader.setFloatUniform("baseColor",
                            theme.colors[0].red, theme.colors[0].green, theme.colors[0].blue)
                        shader.setFloatUniform("midColor",
                            theme.colors[1].red, theme.colors[1].green, theme.colors[1].blue)
                        shader.setFloatUniform("glowColor",
                            theme.colors[2].red, theme.colors[2].green, theme.colors[2].blue)
                        shader.setFloatUniform("brightness", theme.brightness)
                    }
                    ShaderType.EMBER_GLOW,
                    ShaderType.AURORA,
                    ShaderType.PLASMA -> {
                        shader.setFloatUniform("flowSpeed",   theme.animationSpeed)
                        shader.setFloatUniform("complexity",  theme.complexity)
                        shader.setFloatUniform("layerColorA",
                            theme.colors[0].red, theme.colors[0].green, theme.colors[0].blue)
                        shader.setFloatUniform("layerColorB",
                            theme.colors[1].red, theme.colors[1].green, theme.colors[1].blue)
                        shader.setFloatUniform("layerColorC",
                            theme.colors[2].red, theme.colors[2].green, theme.colors[2].blue)
                        shader.setFloatUniform("brightness", theme.brightness)
                    }
                }
                // Динамические юниформы — только iTime (+ brightness/radius для Engine)
                onDrawBehind {
                    shader.setFloatUniform("iTime", time)
                    if (theme.shaderType == ShaderType.ENGINE) {
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
// LEGACY BACKGROUND (Android < 13)
// =================================================================================

@Composable
private fun LegacyGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "legacy_transition")

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
                            center = Offset(size.width * progress3, size.height * (1f - progress2)),
                            radius = size.width * (1.2f + progress2 * 0.5f)
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(coreColor.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(size.width * (1f - progress1), size.height * progress3),
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