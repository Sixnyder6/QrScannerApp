// TransitionEngine — движок приложения
// Настроен под 120fps ProMotion физику
package com.example.qrscannerapp.common.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import com.example.qrscannerapp.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// =============================================================================
// TIERS OF ANIMATION PRIORITY
// Critical  — never throttled (press, swipe, scroll-linked)
// Content   — reduced stiffness when thermal level >= 2
// Decorative — disabled at thermal >= 2, reduced at thermal = 1
// =============================================================================

enum class AnimationTier {
    CRITICAL,   // press, swipe-back, scroll-linked, navigation
    CONTENT,    // staggered, floating entrance, counter, morph
    DECORATIVE  // confetti, glow, ripple, success burst, scanner beam
}

// =============================================================================
// ACTIVE ANIMATION TRACKER — global counter with tier-aware limits
// =============================================================================

object ActiveAnimationTracker {
    private val _counts = java.util.concurrent.ConcurrentHashMap<AnimationTier, Int>()
    private val limits = java.util.concurrent.ConcurrentHashMap<AnimationTier, Int>().also {
        it[AnimationTier.CRITICAL]   = Int.MAX_VALUE
        it[AnimationTier.CONTENT]    = 24
        it[AnimationTier.DECORATIVE] = 12
    }

    fun getCount(tier: AnimationTier): Int = _counts[tier] ?: 0
    fun getTotalCount(): Int = _counts.values.sum()

    fun canStart(tier: AnimationTier): Boolean =
        (_counts[tier] ?: 0) < (limits[tier] ?: Int.MAX_VALUE)

    fun register(tier: AnimationTier) {
        _counts.merge(tier, 1, Int::plus)
    }

    fun unregister(tier: AnimationTier) {
        _counts.computeIfPresent(tier) { _, v -> (v - 1).coerceAtLeast(0) }
    }

    fun updateLimits(profileTier: Int) {
        limits[AnimationTier.CONTENT] = when (profileTier) {
            0 -> 8; 1 -> 16; 2 -> 24; else -> 32
        }
        limits[AnimationTier.DECORATIVE] = when (profileTier) {
            0 -> 3; 1 -> 8; 2 -> 12; else -> 18
        }
    }

    fun resetAll() { _counts.clear() }
}

// =============================================================================
// SPRING SYSTEM — настроена под 120fps
// =============================================================================

internal val PressSpring = spring<Float>(
    dampingRatio = 0.70f,
    stiffness    = 750f
)

internal val NavSpring = spring<Float>(
    dampingRatio = 0.90f,
    stiffness    = 420f
)

internal val NavSpringInt = spring<IntOffset>(
    dampingRatio = 0.90f,
    stiffness    = 420f
)

internal val ContentSpring = spring<Float>(
    dampingRatio = 0.95f,
    stiffness    = 180f
)

internal val BouncySpring = spring<Float>(
    dampingRatio = 0.52f,
    stiffness    = 520f
)

internal val FallSpring = spring<Float>(
    dampingRatio = 0.60f,
    stiffness    = 380f
)

val MicroSpring = spring<Float>(
    dampingRatio = 0.75f,
    stiffness    = 700f
)

// =============================================================================
// SPYDER CONFIG
// =============================================================================

enum class NavTransitionType { ZOOM, SLIDE, FADE, RISE, ELASTIC, PUSH }

data class SpyderConfig(
    val dampingMul: Float = 1f,
    val stiffnessMul: Float = 1f,
    val throttleLevel: Int = 0,
    val navTransition: NavTransitionType = NavTransitionType.ZOOM,
    val screenEntranceDelay: Long = 60L,
    val pressScale: Float = 0.94f,
    val pressBrightness: Float = 0.82f,
    val reducedMotion: Boolean = false,
    // Новые поля для управления тирами
    val decorativeEnabled: Boolean = true,
    val shaderTickIntervalMs: Long = 66L,    // 66 = ~15fps для шейдеров, можно увеличить при throttle
    val maxContentAnimations: Int = 24,
    val maxDecorativeAnimations: Int = 12
)

val LocalSpyderConfig = staticCompositionLocalOf { SpyderConfig() }

@Composable
fun SpyderConfigProvider(
    config: SpyderConfig,
    content: @Composable () -> Unit
) {
    // Обновляем лимиты трекера при изменении конфига
    LaunchedEffect(config.maxContentAnimations, config.maxDecorativeAnimations) {
        ActiveAnimationTracker.updateLimits(
            when {
                config.maxDecorativeAnimations <= 3 -> 0
                config.maxDecorativeAnimations <= 8 -> 1
                config.maxDecorativeAnimations <= 12 -> 2
                else -> 3
            }
        )
    }
    CompositionLocalProvider(LocalSpyderConfig provides config) { content() }
}

internal val IOSEasing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

// =============================================================================
// 1. SWIPE BACK — резиновый отклик (CRITICAL TIER)
// =============================================================================

class SwipeBackState(
    private val onBack: () -> Unit,
    private val threshold: Float = 0.28f
) {
    var progress by mutableFloatStateOf(0f)
        private set
    var isGestureActive by mutableStateOf(false)
        private set

    private val animatable = Animatable(0f)

    fun startGesture() {
        isGestureActive = true
        progress = 0f
        ActiveAnimationTracker.register(AnimationTier.CRITICAL)
    }

    fun updateProgress(delta: Float, totalWidth: Float) {
        val raw = delta / totalWidth
        progress = rubberBand(raw).coerceIn(0f, 1.15f)
    }

    private fun rubberBand(x: Float, resistance: Float = 0.55f): Float {
        return if (x <= 1f) x else x / (1f + (x - 1f) * resistance)
    }

    fun endGesture(scope: kotlinx.coroutines.CoroutineScope) {
        isGestureActive = false
        val shouldComplete = progress > threshold
        scope.launch {
            if (shouldComplete) {
                animatable.snapTo(progress)
                animatable.animateTo(1.15f, spring(0.90f, 380f))
                onBack()
                animatable.snapTo(0f); progress = 0f
            } else {
                animatable.snapTo(progress)
                animatable.animateTo(0f, spring(0.75f, 450f))
                progress = 0f
            }
            ActiveAnimationTracker.unregister(AnimationTier.CRITICAL)
        }
    }

    fun cancelGesture() {
        isGestureActive = false
        progress = 0f
        ActiveAnimationTracker.unregister(AnimationTier.CRITICAL)
    }

    fun getAnimatedProgress() = if (isGestureActive) progress else animatable.value
}

@Composable
fun rememberSwipeBackState(onBack: () -> Unit) = remember { SwipeBackState(onBack) }

@Composable
fun Modifier.interactiveSwipeBack(
    swipeBackState: SwipeBackState,
    enabled: Boolean = true
): Modifier {
    val scope = rememberCoroutineScope()
    return this.then(if (enabled) Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart  = { swipeBackState.startGesture() },
            onDragEnd    = { swipeBackState.endGesture(scope) },
            onDragCancel = { swipeBackState.cancelGesture() },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                swipeBackState.updateProgress(
                    swipeBackState.getAnimatedProgress() * size.width + dragAmount,
                    size.width.toFloat()
                )
            }
        )
    } else Modifier)
}

// =============================================================================
// 2. TACTILE BUTTON (CRITICAL TIER — never throttled)
// =============================================================================

@Composable
fun TactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scaleOnPress: Float = Float.NaN,
    brightnessOnPress: Float = Float.NaN,
    content: @Composable () -> Unit
) {
    val spyder = LocalSpyderConfig.current
    val effectiveScale      = if (scaleOnPress.isNaN())      spyder.pressScale      else scaleOnPress
    val effectiveBrightness = if (brightnessOnPress.isNaN()) spyder.pressBrightness else brightnessOnPress
    var isPressed by remember { mutableStateOf(false) }

    // CRITICAL: всегда используем PressSpring без адаптации под throttle
    val adaptedPressSpring = spring<Float>(
        dampingRatio = 0.70f,
        stiffness    = 750f
    )

    val animValue by animateFloatAsState(
        targetValue   = if (isPressed && enabled) 0f else 1f,
        animationSpec = adaptedPressSpring,
        label         = "press"
    )

    val scale = effectiveScale + (1f - effectiveScale) * animValue
    val alpha = effectiveBrightness + (1f - effectiveBrightness) * animValue

    // Регистрируем только когда анимация активна
    LaunchedEffect(isPressed) {
        if (isPressed) {
            ActiveAnimationTracker.register(AnimationTier.CRITICAL)
        } else {
            ActiveAnimationTracker.unregister(AnimationTier.CRITICAL)
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when {
                            event.changes.any { it.pressed }  -> isPressed = true
                            event.changes.any { !it.pressed } -> {
                                if (isPressed) { onClick(); isPressed = false }
                            }
                        }
                    }
                }
            }
    ) { content() }
}

// =============================================================================
// 3. BOUNCY ICON (CONTENT TIER)
// =============================================================================

@Composable
fun BouncyIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spyder = LocalSpyderConfig.current
    var bouncing by remember { mutableStateOf(false) }

    val tier = AnimationTier.CONTENT
    val throttle = spyder.throttleLevel

    // CONTENT: снижаем stiffness при throttle >= 2
    val effectiveDamping = when {
        throttle >= 3 -> 0.70f
        throttle >= 2 -> 0.62f
        else -> 0.52f
    }
    val effectiveStiffness = when {
        throttle >= 3 -> 300f
        throttle >= 2 -> 400f
        else -> 520f
    }

    val adaptedBouncySpring = spring<Float>(
        dampingRatio = (effectiveDamping * spyder.dampingMul).coerceIn(0.3f, 1.0f),
        stiffness    = (effectiveStiffness * spyder.stiffnessMul).coerceAtLeast(100f)
    )

    val scale by animateFloatAsState(
        targetValue      = if (bouncing && !spyder.reducedMotion) 1.20f else 1f,
        animationSpec    = adaptedBouncySpring,
        label            = "bounce",
        finishedListener = {
            bouncing = false
            ActiveAnimationTracker.unregister(tier)
        }
    )

    LaunchedEffect(bouncing) {
        if (bouncing) ActiveAnimationTracker.register(tier)
    }

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed && !it.previousPressed }) {
                            bouncing = true; onClick()
                        }
                    }
                }
            }
    ) { content() }
}

// =============================================================================
// 4. SWIPE BACK INDICATORS
// =============================================================================

@Composable
fun SwipeBackIndicator(progress: Float, modifier: Modifier = Modifier) {
    if (progress <= 0f) return
    Box(modifier = modifier.fillMaxHeight().width(3.dp)
        .alpha((progress * 0.7f).coerceIn(0f, 1f))
        .background(Color.White.copy(alpha = progress.coerceIn(0f, 1f))))
}

@Composable
fun BackArrowIndicator(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(40.dp)
            .graphicsLayer {
                alpha        = progress.coerceIn(0f, 1f)
                translationX = -progress.coerceIn(0f, 1f) * 120f
            }
            .background(Color.White.copy(alpha = 0.25f * progress.coerceIn(0f, 1f)),
                RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("‹", color = Color.White.copy(alpha = progress.coerceIn(0f, 1f)),
            fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

// =============================================================================
// 5. PULL-TO-REFRESH
// =============================================================================

@Composable
fun PullToRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    modifier: Modifier = Modifier
) {
    val spyder = LocalSpyderConfig.current
    val infiniteTransition = rememberInfiniteTransition(label = "ptr")
    val spinRotation by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Restart),
        label         = "spin"
    )
    val pullRotation by animateFloatAsState(
        targetValue   = (pullProgress * 180f).coerceIn(0f, 180f),
        animationSpec = spring(0.85f, 300f), label = "pull"
    )

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) ActiveAnimationTracker.register(AnimationTier.CONTENT)
        else ActiveAnimationTracker.unregister(AnimationTier.CONTENT)
    }

    Box(
        modifier = modifier.size(44.dp)
            .graphicsLayer {
                rotationZ = if (isRefreshing) spinRotation else pullRotation
                alpha     = (pullProgress * 2.2f).coerceIn(0f, 1f)
            }
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(if (isRefreshing) "↻" else "↓",
            color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp, fontWeight = FontWeight.Light)
    }
}

// =============================================================================
// 6. CONFETTI (DECORATIVE TIER — disabled at throttle >= 2)
// =============================================================================

data class ConfettiParticle(
    val x: Float, val y: Float,
    val velocityX: Float, val velocityY: Float,
    val color: Color, val size: Float,
    val rotation: Float, val rotationSpeed: Float
)

@Composable
fun ConfettiEffect(trigger: Boolean, modifier: Modifier = Modifier, particleCount: Int = 28) {
    val spyder = LocalSpyderConfig.current
    val tier = AnimationTier.DECORATIVE

    // DECORATIVE: disabled at throttle >= 2
    if (!spyder.decorativeEnabled || spyder.throttleLevel >= 2) return

    var show by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                x = Random.nextFloat() * 0.5f + 0.25f,
                y = Random.nextFloat() * 0.3f + 0.25f,
                velocityX = (Random.nextFloat() - 0.5f) * 0.7f,
                velocityY = (Random.nextFloat() - 0.5f) * -0.7f - 0.25f,
                color = Color.hsl(Random.nextFloat() * 360f, 0.75f, 0.65f),
                size = Random.nextFloat() * 7f + 3f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 480f
            )
        }
    }

    LaunchedEffect(trigger) {
        if (trigger && spyder.decorativeEnabled && ActiveAnimationTracker.canStart(tier)) {
            ActiveAnimationTracker.register(tier)
            show = true; progress.snapTo(0f)
            progress.animateTo(1f, tween(1600, easing = IOSEasing))
            delay(400); show = false
            ActiveAnimationTracker.unregister(tier)
        }
    }

    if (!show) return
    Box(modifier = modifier.fillMaxSize().drawWithContent {
        drawContent()
        val p = progress.value
        val fade = if (p > 0.65f) 1f - ((p - 0.65f) / 0.35f) else 1f
        particles.forEach { particle ->
            val cx = (particle.x + particle.velocityX * p) * size.width
            val cy = (particle.y + particle.velocityY * p) * size.height
            drawParticle(cx, cy, particle.size,
                particle.rotation + particle.rotationSpeed * p,
                particle.color, ((1f - p * 0.8f) * fade).coerceIn(0f, 1f))
        }
    })
}

private fun DrawScope.drawParticle(
    x: Float, y: Float, size: Float, rotation: Float, color: Color, alpha: Float
) {
    val half = size / 2f
    val angle = Math.toRadians(rotation.toDouble())
    val cosA = cos(angle).toFloat(); val sinA = sin(angle).toFloat()
    fun pt(dx: Float, dy: Float) = Offset(x + dx * cosA - dy * sinA, y + dx * sinA + dy * cosA)
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(pt(-half, -half * 0.45f).x, pt(-half, -half * 0.45f).y)
        lineTo(pt( half, -half * 0.45f).x, pt( half, -half * 0.45f).y)
        lineTo(pt( half,  half * 0.45f).x, pt( half,  half * 0.45f).y)
        lineTo(pt(-half,  half * 0.45f).x, pt(-half,  half * 0.45f).y)
        close()
    }
    drawPath(path, color.copy(alpha = alpha))
}

// =============================================================================
// 7. TILT CARD (CRITICAL — linked to touch)
// =============================================================================

@Composable
fun TiltCard(modifier: Modifier = Modifier, maxTilt: Float = 6f, content: @Composable () -> Unit) {
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    Box(modifier = modifier
        .graphicsLayer {
            rotationX = rotX.value; rotationY = rotY.value
            cameraDistance = 14f * density.density
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (change.pressed) {
                        val cx = size.width / 2f; val cy = size.height / 2f
                        scope.launch { rotX.snapTo(-((change.position.y - cy) / cy) * maxTilt) }
                        scope.launch { rotY.snapTo( ((change.position.x - cx) / cx) * maxTilt) }
                    } else {
                        scope.launch { rotX.animateTo(0f, spring(0.68f, 420f)) }
                        scope.launch { rotY.animateTo(0f, spring(0.68f, 420f)) }
                    }
                }
            }
        }
    ) { content() }
}

// =============================================================================
// 8. PARALLAX IMAGE
// =============================================================================

@Composable
fun ParallaxImage(
    scrollOffset: Float, parallaxFactor: Float = 0.45f,
    modifier: Modifier = Modifier, content: @Composable () -> Unit
) {
    val offset by animateFloatAsState(scrollOffset * parallaxFactor,
        spring(0.90f, 220f), label = "par")
    Box(modifier = modifier.graphicsLayer { translationY = offset }) { content() }
}

// =============================================================================
// 9. GLOW EFFECT (DECORATIVE TIER)
// =============================================================================

@Composable
fun GlowEffect(isActive: Boolean, color: Color = Color.White, modifier: Modifier = Modifier) {
    val spyder = LocalSpyderConfig.current
    val tier = AnimationTier.DECORATIVE

    // DECORATIVE: disabled at throttle >= 2
    if (!spyder.decorativeEnabled || spyder.throttleLevel >= 2) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlowEffectShader(isActive, color, modifier, spyder.shaderTickIntervalMs)
    } else {
        GlowEffectLegacy(isActive, color, modifier)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun GlowEffectShader(
    isActive: Boolean,
    color: Color,
    modifier: Modifier,
    tickIntervalMs: Long
) {
    val context = LocalContext.current
    val shader = remember {
        context.resources.openRawResource(R.raw.glow_effect)
            .bufferedReader().use { it.readText() }
            .let { RuntimeShader(it) }
    }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    val glowAlpha = animateFloatAsState(
        targetValue   = if (isActive) 0.22f else 0f,
        animationSpec = ContentSpring, label = "glow"
    )

    var time by remember { mutableFloatStateOf(0f) }
    val tier = AnimationTier.DECORATIVE

    LaunchedEffect(isActive) {
        if (isActive) ActiveAnimationTracker.register(tier)
        else ActiveAnimationTracker.unregister(tier)
    }

    LaunchedEffect(Unit) {
        val t0 = System.currentTimeMillis()
        while (true) {
            delay(tickIntervalMs)  // адаптивный интервал из конфига
            time = (System.currentTimeMillis() - t0) / 1000f
        }
    }

    Box(modifier = modifier.drawWithCache {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("u_color", color.red, color.green, color.blue)
        onDrawBehind {
            shader.setFloatUniform("iTime", time)
            shader.setFloatUniform("u_alpha", glowAlpha.value)
            drawRect(shaderBrush)
        }
    })
}

@Composable
private fun GlowEffectLegacy(isActive: Boolean, color: Color, modifier: Modifier) {
    val glowAlpha by animateFloatAsState(
        targetValue   = if (isActive) 0.22f else 0f,
        animationSpec = ContentSpring, label = "glow"
    )
    Box(modifier = modifier.drawWithContent {
        drawContent()
        drawCircle(color.copy(alpha = glowAlpha), size.minDimension * 0.55f, center)
    })
}

// =============================================================================
// 10. PULSATING RIPPLE (DECORATIVE TIER)
// =============================================================================

@Composable
fun PulsatingRipple(isActive: Boolean, color: Color = Color.White, modifier: Modifier = Modifier) {
    val spyder = LocalSpyderConfig.current
    val tier = AnimationTier.DECORATIVE

    if (!isActive) return
    if (!spyder.decorativeEnabled || spyder.throttleLevel >= 2) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PulsatingRippleShader(color, modifier, spyder.shaderTickIntervalMs)
    } else {
        PulsatingRippleLegacy(color, modifier)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun PulsatingRippleShader(color: Color, modifier: Modifier, tickIntervalMs: Long) {
    val context = LocalContext.current
    val shader = remember {
        context.resources.openRawResource(R.raw.pulsating_ripple)
            .bufferedReader().use { it.readText() }
            .let { RuntimeShader(it) }
    }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }
    var time by remember { mutableFloatStateOf(0f) }

    val tier = AnimationTier.DECORATIVE
    LaunchedEffect(Unit) {
        ActiveAnimationTracker.register(tier)
        val t0 = System.currentTimeMillis()
        try {
            while (true) {
                delay(tickIntervalMs)
                time = (System.currentTimeMillis() - t0) / 1000f
            }
        } finally {
            ActiveAnimationTracker.unregister(tier)
        }
    }

    Box(modifier = modifier.drawWithCache {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("u_color", color.red, color.green, color.blue)
        onDrawBehind {
            shader.setFloatUniform("iTime", time)
            drawRect(shaderBrush)
        }
    })
}

@Composable
private fun PulsatingRippleLegacy(color: Color, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "ripple")
    val scale by t.animateFloat(0.88f, 1.12f,
        infiniteRepeatable(tween(1400, easing = IOSEasing), RepeatMode.Reverse), "rs")
    val alpha by t.animateFloat(0.20f, 0f,
        infiniteRepeatable(tween(1400, easing = IOSEasing), RepeatMode.Restart), "ra")

    val tier = AnimationTier.DECORATIVE
    LaunchedEffect(Unit) {
        ActiveAnimationTracker.register(tier)
    }
    DisposableEffect(Unit) {
        onDispose { ActiveAnimationTracker.unregister(tier) }
    }

    Box(modifier = modifier
        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
        .background(color, CircleShape))
}

// =============================================================================
// NAVIGATION TRANSITIONS
// =============================================================================

fun iosSlideIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, NavSpringInt) +
            fadeIn(animationSpec = spring(0.95f, 380f), initialAlpha = 0f)
}

fun iosSlideOutParallax(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left,
        tween(70, easing = IOSEasing), targetOffset = { -(it * 0.25f).toInt() }) +
            fadeOut(tween(70, easing = IOSEasing), targetAlpha = 0.6f)
}

fun iosPopEnterParallax(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right,
        spring(0.92f, 420f), initialOffset = { -(it * 0.25f).toInt() }) +
            fadeIn(tween(240, easing = IOSEasing), initialAlpha = 0.6f)
}

fun iosPopSlideOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(70, easing = IOSEasing)) +
            fadeOut(tween(70, easing = IOSEasing))
}

fun macZoomIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(spring(0.95f, 360f), initialAlpha = 0f) +
            scaleIn(spring(0.88f, 420f), initialScale = 0.93f)
}

fun macZoomExitShrink(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(70, easing = IOSEasing)) +
            scaleOut(tween(70, easing = IOSEasing), targetScale = 0.94f)
}

fun macZoomPopEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(260, easing = IOSEasing), initialAlpha = 0f) +
            scaleIn(tween(260, easing = IOSEasing), initialScale = 0.94f)
}

fun macZoomPopExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(70, easing = IOSEasing)) +
            scaleOut(tween(70, easing = IOSEasing), targetScale = 0.93f)
}

fun smoothFadeIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(spring(0.95f, 240f), initialAlpha = 0f) +
            scaleIn(spring(0.92f, 280f), initialScale = 0.98f)
}

fun smoothFadeOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(70, easing = IOSEasing)) +
            scaleOut(tween(70, easing = IOSEasing), targetScale = 0.99f)
}

fun homescreenFadeIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(spring(dampingRatio = 0.94f, stiffness = 130f), initialAlpha = 0f)
}

fun homescreenFadeOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(70, easing = IOSEasing))
}

fun riseIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up,
        spring(0.86f, 360f)) +
            fadeIn(spring(0.92f, 320f), initialAlpha = 0f)
}

fun riseOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(70, easing = IOSEasing)) +
            scaleOut(tween(70, easing = IOSEasing), targetScale = 0.97f)
}

fun risePopEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(240, easing = IOSEasing), initialAlpha = 0.4f) +
            scaleIn(tween(240, easing = IOSEasing), initialScale = 0.97f)
}

fun risePopExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down,
        tween(70, easing = IOSEasing)) +
            fadeOut(tween(70, easing = IOSEasing))
}

fun elasticIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    scaleIn(spring(dampingRatio = 0.65f, stiffness = 460f), initialScale = 0.82f) +
            fadeIn(spring(dampingRatio = 0.80f, stiffness = 420f), initialAlpha = 0f)
}

fun elasticOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    scaleOut(tween(70, easing = IOSEasing), targetScale = 0.95f) +
            fadeOut(tween(70, easing = IOSEasing))
}

fun elasticPopEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    scaleIn(tween(220, easing = IOSEasing), initialScale = 0.95f) +
            fadeIn(tween(220, easing = IOSEasing), initialAlpha = 0.4f)
}

fun elasticPopExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    scaleOut(tween(70, easing = IOSEasing), targetScale = 0.82f) +
            fadeOut(tween(70, easing = IOSEasing))
}

fun pushIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left,
        spring(0.90f, 400f)) +
            fadeIn(spring(0.95f, 380f), initialAlpha = 0.2f)
}

fun pushOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left,
        tween(70, easing = IOSEasing)) +
            fadeOut(tween(70, easing = IOSEasing))
}

fun pushPopEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right,
        spring(0.90f, 400f)) +
            fadeIn(tween(240, easing = IOSEasing), initialAlpha = 0.2f)
}

fun pushPopExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right,
        tween(70, easing = IOSEasing)) +
            fadeOut(tween(70, easing = IOSEasing))
}

data class NavTransitionSet(
    val enter:    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
    val exit:     AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
    val popExit:  AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
)

fun navTransitionsFor(type: NavTransitionType) = when (type) {
    NavTransitionType.ZOOM    -> NavTransitionSet(macZoomIn(), macZoomExitShrink(), macZoomPopEnter(), macZoomPopExit())
    NavTransitionType.SLIDE   -> NavTransitionSet(iosSlideIn(), iosSlideOutParallax(), iosPopEnterParallax(), iosPopSlideOut())
    NavTransitionType.FADE    -> NavTransitionSet(smoothFadeIn(), smoothFadeOut(), smoothFadeIn(), smoothFadeOut())
    NavTransitionType.RISE    -> NavTransitionSet(riseIn(), riseOut(), risePopEnter(), risePopExit())
    NavTransitionType.ELASTIC -> NavTransitionSet(elasticIn(), elasticOut(), elasticPopEnter(), elasticPopExit())
    NavTransitionType.PUSH    -> NavTransitionSet(pushIn(), pushOut(), pushPopEnter(), pushPopExit())
}

// =============================================================================
// STAGGERED CONTENT (CONTENT TIER)
// =============================================================================

@Composable
fun StaggeredColumn(
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
    baseDelayMs: Long = 30L,
    initialDelayMs: Long = 40L,
    content: @Composable StaggeredScope.() -> Unit
) {
    val scope = remember(itemCount) { StaggeredScopeImpl(itemCount, baseDelayMs, initialDelayMs) }
    Column(modifier = modifier) { scope.content() }
}

interface StaggeredScope {
    @Composable fun StaggeredItem(index: Int, modifier: Modifier, content: @Composable () -> Unit)
}

private class StaggeredScopeImpl(
    private val itemCount: Int,
    private val baseDelayMs: Long,
    private val initialDelayMs: Long
) : StaggeredScope {
    @Composable
    override fun StaggeredItem(index: Int, modifier: Modifier, content: @Composable () -> Unit) {
        val spyder = LocalSpyderConfig.current
        var visible by remember { mutableStateOf(false) }
        val tier = AnimationTier.CONTENT

        // CONTENT: снижаем задержку при throttle чтобы быстрее закончить
        val effectiveDelay = when {
            spyder.throttleLevel >= 3 -> (initialDelayMs + index * baseDelayMs) / 3
            spyder.throttleLevel >= 2 -> (initialDelayMs + index * baseDelayMs) / 2
            else -> initialDelayMs + index * baseDelayMs
        }

        LaunchedEffect(Unit) {
            delay(effectiveDelay)
            if (ActiveAnimationTracker.canStart(tier)) {
                ActiveAnimationTracker.register(tier)
                visible = true
            }
        }

        // CONTENT: более быстрый spring при throttle
        val effectiveSpring = when {
            spyder.throttleLevel >= 3 -> spring(0.92f, 300f)
            spyder.throttleLevel >= 2 -> spring(0.93f, 240f)
            else -> ContentSpring
        }

        val animV by animateFloatAsState(
            targetValue   = if (visible) 1f else 0f,
            animationSpec = effectiveSpring,
            label         = "stagger_$index",
            finishedListener = {
                if (!visible) ActiveAnimationTracker.unregister(tier)
            }
        )

        Box(modifier = modifier.graphicsLayer {
            alpha        = animV
            translationY = (1f - animV) * 14f
        }) { content() }
    }
}

// =============================================================================
// HELPERS
// =============================================================================

@Composable
fun SpringAnimatedVisibility(
    visible: Boolean, modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val spyder = LocalSpyderConfig.current
    val tier = AnimationTier.CONTENT

    LaunchedEffect(visible) {
        if (visible) {
            if (ActiveAnimationTracker.canStart(tier)) {
                ActiveAnimationTracker.register(tier)
            }
        } else {
            ActiveAnimationTracker.unregister(tier)
        }
    }

    AnimatedVisibility(
        visible = visible, modifier = modifier,
        enter = fadeIn(ContentSpring) +
                slideInVertically(spring(0.88f, 260f)) { it / 10 },
        exit  = fadeOut(tween(160, easing = IOSEasing)) +
                slideOutVertically(tween(160, easing = IOSEasing)) { it / 12 },
        content = content
    )
}

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, cornerRadius: Int = 12) {
    val spyder = LocalSpyderConfig.current
    // DECORATIVE: скелетон отключается при throttle >= 3
    if (spyder.throttleLevel >= 3) {
        Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color.White.copy(alpha = 0.05f)))
        return
    }

    val t = rememberInfiniteTransition(label = "sk")
    val alpha by t.animateFloat(0.04f, 0.11f,
        infiniteRepeatable(tween(850, easing = IOSEasing), RepeatMode.Reverse), "sk_a")
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius.dp))
        .background(Color.White.copy(alpha = alpha)))
}

@Composable
fun SkeletonList(itemCount: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonBlock(Modifier.fillMaxWidth(0.35f).height(18.dp), 8)
        Spacer(Modifier.height(6.dp))
        repeat(itemCount) { SkeletonBlock(Modifier.fillMaxWidth().height(68.dp)) }
    }
}

@Composable
fun SmoothScreen(
    delayMs: Long = 60L,
    skeleton: @Composable () -> Unit = { SkeletonList() },
    content: @Composable () -> Unit
) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs); ready = true }
    if (ready) content() else Box(Modifier.fillMaxSize()) { skeleton() }
}

@Composable
fun TransitionScrim(visible: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 0.12f else 0f,
        animationSpec = spring(0.90f, 260f), label = "scrim"
    )
    if (alpha > 0f) Box(modifier = modifier.fillMaxSize()
        .background(Color.Black.copy(alpha = alpha)))
}

// =============================================================================
// 11. ANIMATED COUNTER (CONTENT TIER)
// =============================================================================

@Composable
fun AnimatedCounter(
    value: Int, modifier: Modifier = Modifier,
    suffix: String = "", prefix: String = "",
    color: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val spyder = LocalSpyderConfig.current
    val animValue = remember { Animatable(value.toFloat()) }

    val effectiveSpring = when {
        spyder.throttleLevel >= 3 -> spring<Float>(0.88f, 320f)
        spyder.throttleLevel >= 2 -> spring<Float>(0.86f, 280f)
        else -> spring<Float>(0.82f, 240f)
    }

    LaunchedEffect(value) {
        animValue.animateTo(value.toFloat(), effectiveSpring)
    }
    Text("$prefix${animValue.value.toInt()}$suffix",
        color = color, fontSize = fontSize, fontWeight = fontWeight, modifier = modifier)
}

// =============================================================================
// 12. SHAKE (CONTENT TIER)
// =============================================================================

@Composable
fun Modifier.shake(trigger: Boolean, onFinished: () -> Unit = {}): Modifier {
    val offsetX = remember { Animatable(0f) }
    val tier = AnimationTier.CONTENT

    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        ActiveAnimationTracker.register(tier)
        offsetX.snapTo(14f)
        offsetX.animateTo(
            targetValue   = 0f,
            animationSpec = spring(dampingRatio = 0.38f, stiffness = 680f)
        )
        ActiveAnimationTracker.unregister(tier)
        onFinished()
    }
    return this.graphicsLayer { translationX = offsetX.value }
}

// =============================================================================
// 13. SUCCESS BURST (DECORATIVE TIER)
// =============================================================================

@Composable
fun SuccessBurst(
    trigger: Boolean, modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CAF50)
) {
    val spyder = LocalSpyderConfig.current
    val tier = AnimationTier.DECORATIVE

    if (!spyder.decorativeEnabled || spyder.throttleLevel >= 2) return

    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        if (!ActiveAnimationTracker.canStart(tier)) return@LaunchedEffect
        ActiveAnimationTracker.register(tier)
        scale.snapTo(0.2f); alpha.snapTo(0.6f)
        kotlinx.coroutines.coroutineScope {
            launch { scale.animateTo(2.0f, spring(0.75f, 300f)) }
            launch { alpha.animateTo(0f, tween(450, easing = IOSEasing)) }
        }
        ActiveAnimationTracker.unregister(tier)
    }

    Box(modifier = modifier
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }
        .background(color, CircleShape))
}

// =============================================================================
// 14. DOTS LOADER (DECORATIVE TIER)
// =============================================================================

@Composable
fun DotsLoader(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    dotSize: androidx.compose.ui.unit.Dp = 8.dp
) {
    val spyder = LocalSpyderConfig.current
    if (spyder.throttleLevel >= 3) return  // skip entirely at max throttle

    val t = rememberInfiniteTransition(label = "dots")
    val tier = AnimationTier.DECORATIVE

    LaunchedEffect(Unit) { ActiveAnimationTracker.register(tier) }
    DisposableEffect(Unit) {
        onDispose { ActiveAnimationTracker.unregister(tier) }
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        listOf(0, 140, 280).forEachIndexed { i, delayMs ->
            val scale by t.animateFloat(0.5f, 1f,
                infiniteRepeatable(tween(460, delayMs, IOSEasing), RepeatMode.Reverse), "dot_$i")
            Box(Modifier.size(dotSize)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(color, CircleShape))
        }
    }
}

// =============================================================================
// 15. ANIMATED PROGRESS BAR (CONTENT TIER)
// =============================================================================

@Composable
fun AnimatedProgressBar(
    progress: Float, modifier: Modifier = Modifier,
    trackColor: Color = Color.White.copy(alpha = 0.12f),
    fillColor: Color = Color(0xFF6A5AE0),
    cornerRadius: androidx.compose.ui.unit.Dp = 6.dp
) {
    val spyder = LocalSpyderConfig.current
    val effectiveSpring = when {
        spyder.throttleLevel >= 2 -> spring<Float>(0.90f, 300f)
        else -> spring<Float>(0.88f, 240f)
    }

    val animProg by animateFloatAsState(progress.coerceIn(0f, 1f),
        effectiveSpring, label = "bar")
    Box(modifier = modifier.height(8.dp)
        .clip(RoundedCornerShape(cornerRadius)).background(trackColor)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animProg)
            .background(fillColor, RoundedCornerShape(cornerRadius)))
    }
}

// =============================================================================
// 16. MORPH BUTTON (CONTENT TIER)
// =============================================================================

@Composable
fun MorphButton(
    onClick: () -> Unit, isLoading: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF6A5AE0),
    content: @Composable () -> Unit
) {
    val cornerPercent by animateIntAsState(
        if (isLoading) 50 else 16, spring(0.80f, 320f), label = "mb_c")
    val contentAlpha by animateFloatAsState(
        if (isLoading) 0f else 1f, tween(200, easing = IOSEasing), label = "mb_a")
    val spinnerAlpha by animateFloatAsState(
        if (isLoading) 1f else 0f, tween(200, easing = IOSEasing), label = "mb_s")

    val tier = AnimationTier.CONTENT
    LaunchedEffect(isLoading) {
        if (isLoading) ActiveAnimationTracker.register(tier)
        else ActiveAnimationTracker.unregister(tier)
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerPercent))
            .background(containerColor)
            .pointerInput(isLoading) {
                if (!isLoading) awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed && !it.previousPressed }) onClick()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.alpha(contentAlpha)) { content() }
        CircularProgressIndicator(Modifier.size(22.dp).alpha(spinnerAlpha),
            color = Color.White, strokeWidth = 2.5.dp)
    }
}

// =============================================================================
// 17. SCANNER BEAM (DECORATIVE TIER)
// =============================================================================

@Composable
fun ScannerBeam(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6A5AE0),
    isActive: Boolean = true
) {
    val spyder = LocalSpyderConfig.current
    if (!spyder.decorativeEnabled || spyder.throttleLevel >= 2) return

    val t = rememberInfiniteTransition(label = "beam")
    val position by t.animateFloat(0.05f, 0.95f,
        infiniteRepeatable(
            tween(1800, easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)),
            RepeatMode.Reverse
        ), "beam_pos")
    val beamAlpha by animateFloatAsState(
        if (isActive) 1f else 0f, tween(260, easing = IOSEasing), label = "beam_a")

    val tier = AnimationTier.DECORATIVE
    LaunchedEffect(isActive) {
        if (isActive) ActiveAnimationTracker.register(tier)
        else ActiveAnimationTracker.unregister(tier)
    }

    if (beamAlpha == 0f) return

    Box(modifier = modifier.alpha(beamAlpha).drawWithContent {
        drawContent()
        val y = position * size.height
        drawRect(color.copy(alpha = 0.06f),
            Offset(0f, (y - 24.dp.toPx()).coerceAtLeast(0f)),
            Size(size.width, 48.dp.toPx()))
        drawLine(color.copy(alpha = 0.85f), Offset(0f, y), Offset(size.width, y),
            2.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.40f),
            Offset(size.width * 0.25f, y), Offset(size.width * 0.75f, y),
            1.dp.toPx(), StrokeCap.Round)
    })
}

// =============================================================================
// 18. FLOATING ENTRANCE (CONTENT TIER)
// =============================================================================

@Composable
fun FloatingEntrance(
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    offsetY: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val spyder = LocalSpyderConfig.current
    var triggered by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val tier = AnimationTier.CONTENT

    val effectiveDelay = when {
        spyder.throttleLevel >= 3 -> (delayMs / 3).toLong()
        spyder.throttleLevel >= 2 -> (delayMs / 2).toLong()
        else -> delayMs.toLong()
    }

    LaunchedEffect(Unit) {
        if (effectiveDelay > 0) delay(effectiveDelay)
        if (ActiveAnimationTracker.canStart(tier)) {
            ActiveAnimationTracker.register(tier)
            triggered = true
        }
    }

    val effectiveSpring = when {
        spyder.throttleLevel >= 3 -> spring(0.92f, 300f)
        spyder.throttleLevel >= 2 -> spring(0.93f, 240f)
        else -> ContentSpring
    }

    val animV by animateFloatAsState(
        targetValue   = if (triggered) 1f else 0f,
        animationSpec = effectiveSpring,
        label         = "float",
        finishedListener = {
            if (!triggered) ActiveAnimationTracker.unregister(tier)
        }
    )

    Box(modifier = modifier.graphicsLayer {
        alpha        = animV
        translationY = (1f - animV) * with(density) { offsetY.toPx() }
    }) { content() }
}

// =============================================================================
// 19. SCREEN SHELL — стандартный враппер для всех экранов
// Теперь поддерживает явный сигнал готовности через ScreenReadyNotifier
// =============================================================================

object ScreenReadyNotifier {
    private val _readyStates = mutableMapOf<String, MutableState<Boolean>>()

    fun register(screenId: String): MutableState<Boolean> {
        return _readyStates.getOrPut(screenId) { mutableStateOf(false) }
    }

    fun signalReady(screenId: String) {
        _readyStates[screenId]?.value = true
    }

    fun isReady(screenId: String): Boolean {
        return _readyStates[screenId]?.value ?: true
    }

    fun reset(screenId: String) {
        _readyStates[screenId]?.value = false
    }

    fun unregister(screenId: String) {
        _readyStates.remove(screenId)
    }
}

@Composable
fun ScreenShell(
    delayMs: Long = -1L,
    screenId: String = "default",
    skeleton: @Composable () -> Unit = { SkeletonList() },
    content: @Composable () -> Unit
) {
    val spyder = LocalSpyderConfig.current
    val effective = if (delayMs < 0L) spyder.screenEntranceDelay else delayMs

    if (spyder.reducedMotion || effective == 0L) {
        content()
        return
    }

    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(effective)
        ready = true
    }

    DisposableEffect(screenId) {
        onDispose { ScreenReadyNotifier.unregister(screenId) }
    }

    if (ready) content() else skeleton()
}

// =============================================================================
// UTILITY: Трекинг активных анимаций в композиции
// =============================================================================

@Composable
fun TrackAnimationLifecycle(tier: AnimationTier, isActive: Boolean) {
    LaunchedEffect(isActive) {
        if (isActive) ActiveAnimationTracker.register(tier)
        else ActiveAnimationTracker.unregister(tier)
    }
    DisposableEffect(Unit) {
        onDispose {
            if (isActive) ActiveAnimationTracker.unregister(tier)
        }
    }
}