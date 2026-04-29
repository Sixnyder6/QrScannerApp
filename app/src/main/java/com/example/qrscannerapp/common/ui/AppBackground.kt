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
    import com.example.qrscannerapp.BackgroundTheme
    import com.example.qrscannerapp.R
    import com.example.qrscannerapp.SettingsManager

    // =================================================================================
    // ТЕМЫ
    // =================================================================================

    data class ShaderBackgroundTheme(
        @RawRes val shaderResourceId: Int,
        val colors: List<Color>,
        val animationSpeed: Float = 0.3f,
        val piston1SpeedMult: Float = 2.0f,
        val piston2SpeedMult: Float = 1.5f,
        val rampMidPoint: Float = 0.3f,
        val rampCorePoint: Float = 0.6f,
        // [НОВОЕ] Тип шейдера — Engine/Nebula/Voronoi применяют разные uniform'ы
        val shaderType: ShaderType = ShaderType.ENGINE
    )

    enum class ShaderType { ENGINE, NEBULA, VORONOI }

    // Тема по умолчанию — движок
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

    // Старый псевдоним — чтобы не ломать существующий код
    val DeepSpaceTheme = EngineTheme

    // Туманность
    val NebulaTheme = ShaderBackgroundTheme(
        shaderResourceId = R.raw.nebula_background,
        colors = listOf(
            Color(0.0f, 0.02f, 0.08f, 1.0f),
            Color(0.1f, 0.3f, 0.7f, 1.0f),
            Color(0.4f, 0.7f, 1.0f, 1.0f)
        ),
        animationSpeed = 0.15f,
        shaderType = ShaderType.NEBULA
    )

    // Биосфера / Voronoi
    val VoronoiTheme = ShaderBackgroundTheme(
        shaderResourceId = R.raw.voronoi_background,
        colors = listOf(
            Color(0.01f, 0.04f, 0.02f, 1.0f),
            Color(0.05f, 0.35f, 0.2f, 1.0f),
            Color(0.2f, 0.9f, 0.5f, 1.0f)
        ),
        animationSpeed = 0.2f,
        shaderType = ShaderType.VORONOI
    )

    // Маппинг enum → тема
    fun BackgroundTheme.toShaderTheme(): ShaderBackgroundTheme = when (this) {
        BackgroundTheme.ENGINE  -> EngineTheme
        BackgroundTheme.NEBULA  -> NebulaTheme
        BackgroundTheme.VORONOI -> VoronoiTheme
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

        // Реактивно читаем тему из DataStore
        val backgroundTheme by settingsManager.backgroundThemeFlow.collectAsState(
            initial = BackgroundTheme.ENGINE
        )
        val shaderTheme = backgroundTheme.toShaderTheme()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        // Пересоздаём шейдер при смене темы (ключ = resourceId)
        val shader = remember(theme.shaderResourceId) {
            val shaderSrc = context.resources.openRawResource(theme.shaderResourceId)
                .bufferedReader().use { it.readText() }
            RuntimeShader(shaderSrc)
        }
        val shaderBrush = remember(shader) { ShaderBrush(shader) }

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
                    onDrawBehind {
                        shader.setFloatUniform("iResolution", size.width, size.height)
                        shader.setFloatUniform("iTime", time)

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
                                shader.setFloatUniform("animationSpeed", theme.animationSpeed)
                                shader.setFloatUniform("piston1SpeedMult", theme.piston1SpeedMult)
                                shader.setFloatUniform("piston2SpeedMult", theme.piston2SpeedMult)
                                shader.setFloatUniform("rampMidPoint", theme.rampMidPoint)
                                shader.setFloatUniform("rampCorePoint", theme.rampCorePoint)
                                shader.setFloatUniform("energyBrightness", pulsatingBrightness)
                                shader.setFloatUniform("mainOrbitRadius", pulsatingRadius)
                            }
                            ShaderType.NEBULA -> {
                                shader.setFloatUniform("flowSpeed", theme.animationSpeed)
                                shader.setFloatUniform("complexity", 1.5f)
                                shader.setFloatUniform("nebulaColorA",
                                    theme.colors[0].red, theme.colors[0].green, theme.colors[0].blue)
                                shader.setFloatUniform("nebulaColorB",
                                    theme.colors[2].red, theme.colors[2].green, theme.colors[2].blue)
                                shader.setFloatUniform("density", 1.0f)
                            }
                            ShaderType.VORONOI -> {
                                shader.setFloatUniform("breathRate", 0.5f)
                                shader.setFloatUniform("cellDensity", 6.0f)
                                shader.setFloatUniform("tissueColor",
                                    theme.colors[0].red, theme.colors[0].green, theme.colors[0].blue)
                                shader.setFloatUniform("glowColor",
                                    theme.colors[2].red, theme.colors[2].green, theme.colors[2].blue)
                                shader.setFloatUniform("pulseStrength", 0.6f)
                            }
                        }

                        drawRect(shaderBrush)
                    }
                }
        ) {
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
        val infiniteTransition = rememberInfiniteTransition(label = "super_legacy_transition")

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