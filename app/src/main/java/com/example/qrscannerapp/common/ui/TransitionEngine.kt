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
// SPRING СИСТЕМА — настроена под 120fps
//
// Принципы iOS ProMotion физики:
// 1. Нажатие реагирует за 1 кадр (8ms на 120fps) — нужен высокий stiffness
// 2. Все элементы одного действия заканчиваются ОДНОВРЕМЕННО
//    → alpha и scale всегда один и тот же spring
// 3. Исчезновение быстрее появления (iOS ratio ~0.75x)
// 4. Навигация без tween — только spring, время определяет физика
// 5. Интерактивные spring критически быстрее контентных
// =============================================================================

// Нажатие — мгновенный отклик, 1-2 кадра на 120fps
// iOS эквивалент: response=0.2, damping=0.82
internal val PressSpring = spring<Float>(
    dampingRatio = 0.70f,
    stiffness    = 750f   // было 550 — +36%, разница ощутима
)

// Навигация — чёткая, без overshooting
// iOS эквивалент: response=0.35, damping=0.90
internal val NavSpring = spring<Float>(
    dampingRatio = 0.90f,
    stiffness    = 420f
)

internal val NavSpringInt = spring<IntOffset>(
    dampingRatio = 0.90f,
    stiffness    = 420f
)

// Контент — плавное появление, никакого bounce
// iOS эквивалент: response=0.55, damping=0.95
internal val ContentSpring = spring<Float>(
    dampingRatio = 0.95f,
    stiffness    = 180f
)

// Bounce — иконки, success, упругие элементы
// iOS эквивалент: response=0.3, damping=0.55
internal val BouncySpring = spring<Float>(
    dampingRatio = 0.52f,
    stiffness    = 520f
)

// Падение — диалоги FALL_TOP / SLIDE_BOTTOM, средний bounce
internal val FallSpring = spring<Float>(
    dampingRatio = 0.60f,
    stiffness    = 380f
)

// Микро — tooltip, badge, мелкие детали
val MicroSpring = spring<Float>(
    dampingRatio = 0.75f,
    stiffness    = 700f
)

// =============================================================================
// SPYDER CONFIG — параметры от Spyder3000Engine через CompositionLocal
// Когда телефон греется, движок меняет множители → все анимации плавно адаптируются
// =============================================================================

data class SpyderConfig(
    val dampingMul: Float = 1f,    // >1 = быстрее затухает (меньше bounce)
    val stiffnessMul: Float = 1f,  // <1 = мягче пружина (медленнее)
    val throttleLevel: Int = 0     // 0=норма, 1=тепло, 2=горячо, 3=критично
)

val LocalSpyderConfig = staticCompositionLocalOf { SpyderConfig() }

@Composable
fun SpyderConfigProvider(
    config: SpyderConfig,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalSpyderConfig provides config) { content() }
}

// Единый easing — только для tween где spring не подходит (skeleton, beam)
// iOS использует именно эту кривую для большинства анимаций
internal val IOSEasing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f) // ease-in-out

// =============================================================================
// 1. SWIPE BACK — резиновый отклик
// =============================================================================

class SwipeBackState(
    private val onBack: () -> Unit,
    private val threshold: Float = 0.28f   // немного ниже — легче активировать
) {
    var progress by mutableFloatStateOf(0f)
        private set
    var isGestureActive by mutableStateOf(false)
        private set

    private val animatable = Animatable(0f)

    fun startGesture() { isGestureActive = true; progress = 0f }

    fun updateProgress(delta: Float, totalWidth: Float) {
        // Rubber band: сопротивление нарастает чем дальше тянешь
        val raw = delta / totalWidth
        progress = rubberBand(raw).coerceIn(0f, 1.15f)
    }

    // iOS rubber band формула: x / (1 + abs(x) * resistance)
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
                // Возврат пружинистый — iOS так и делает
                animatable.animateTo(0f, spring(0.75f, 450f))
                progress = 0f
            }
        }
    }

    fun cancelGesture() { isGestureActive = false; progress = 0f }
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
// 2. TACTILE BUTTON
// Ключевое исправление: alpha и scale — ОДИН spring PressSpring
// Раньше они могли закончиться в разное время → рывок
// =============================================================================

@Composable
fun TactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scaleOnPress: Float = 0.94f,      // iOS использует 0.94, не 0.96
    brightnessOnPress: Float = 0.82f,
    content: @Composable () -> Unit
) {
    val spyder = LocalSpyderConfig.current
    var isPressed by remember { mutableStateOf(false) }

    val adaptedPressSpring = spring<Float>(
        dampingRatio = (0.70f * spyder.dampingMul).coerceIn(0.3f, 1.0f),
        stiffness    = (750f  * spyder.stiffnessMul).coerceAtLeast(100f)
    )
    // Один animationSpec на оба параметра — заканчиваются синхронно
    val animValue by animateFloatAsState(
        targetValue   = if (isPressed && enabled) 0f else 1f,
        animationSpec = adaptedPressSpring,
        label         = "press"
    )
    // animValue 0→1: scale = scaleOnPress→1, alpha = brightnessOnPress→1
    val scale = scaleOnPress + (1f - scaleOnPress) * animValue
    val alpha = brightnessOnPress + (1f - brightnessOnPress) * animValue

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
// 3. BOUNCY ICON — упругий отклик
// =============================================================================

@Composable
fun BouncyIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spyder = LocalSpyderConfig.current
    var bouncing by remember { mutableStateOf(false) }

    val adaptedBouncySpring = spring<Float>(
        dampingRatio = (0.52f * spyder.dampingMul).coerceIn(0.3f, 1.0f),
        stiffness    = (520f  * spyder.stiffnessMul).coerceAtLeast(100f)
    )
    val scale by animateFloatAsState(
        targetValue      = if (bouncing) 1.20f else 1f,
        animationSpec    = adaptedBouncySpring,
        label            = "bounce",
        finishedListener = { bouncing = false }
    )

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
// 6. CONFETTI
// =============================================================================

data class ConfettiParticle(
    val x: Float, val y: Float,
    val velocityX: Float, val velocityY: Float,
    val color: Color, val size: Float,
    val rotation: Float, val rotationSpeed: Float
)

@Composable
fun ConfettiEffect(trigger: Boolean, modifier: Modifier = Modifier, particleCount: Int = 28) {
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
        if (trigger) {
            show = true; progress.snapTo(0f)
            progress.animateTo(1f, tween(1600, easing = IOSEasing))
            delay(400); show = false
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
// 7. TILT CARD
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
// 9. GLOW EFFECT
// GPU: bloom rendering + breathing animation via shader.
// CPU: spring для toggle (1 float) + iTime тик 15fps.
// Fallback (API < 33): оригинальная реализация.
// =============================================================================

@Composable
fun GlowEffect(isActive: Boolean, color: Color = Color.White, modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlowEffectShader(isActive, color, modifier)
    } else {
        GlowEffectLegacy(isActive, color, modifier)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun GlowEffectShader(isActive: Boolean, color: Color, modifier: Modifier) {
    val context = LocalContext.current
    val shader = remember {
        context.resources.openRawResource(R.raw.glow_effect)
            .bufferedReader().use { it.readText() }
            .let { RuntimeShader(it) }
    }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }
    // Spring: 1 scalar на кадр во время toggle — CPU cost ничтожен
    val glowAlpha = animateFloatAsState(
        targetValue   = if (isActive) 0.22f else 0f,
        animationSpec = ContentSpring, label = "glow"
    )
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val t0 = System.currentTimeMillis()
        while (true) {
            delay(66L)
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
// 10. PULSATING RIPPLE
// GPU: вся математика scale/alpha/easing внутри шейдера.
// CPU: только delay(66) → iTime. Нет rememberInfiniteTransition, нет 60fps recomp.
// Fallback (API < 33): оригинальная реализация.
// =============================================================================

@Composable
fun PulsatingRipple(isActive: Boolean, color: Color = Color.White, modifier: Modifier = Modifier) {
    if (!isActive) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PulsatingRippleShader(color, modifier)
    } else {
        PulsatingRippleLegacy(color, modifier)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun PulsatingRippleShader(color: Color, modifier: Modifier) {
    val context = LocalContext.current
    val shader = remember {
        context.resources.openRawResource(R.raw.pulsating_ripple)
            .bufferedReader().use { it.readText() }
            .let { RuntimeShader(it) }
    }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val t0 = System.currentTimeMillis()
        while (true) {
            delay(66L)
            time = (System.currentTimeMillis() - t0) / 1000f
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
    Box(modifier = modifier
        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
        .background(color, CircleShape))
}

// =============================================================================
// NAVIGATION TRANSITIONS
// Ключевое: убраны все tween на основных переходах, только spring
// Исчезновение на 25% быстрее появления (iOS правило)
// =============================================================================

fun iosSlideIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, NavSpringInt) +
            fadeIn(animationSpec = spring(0.95f, 380f), initialAlpha = 0f)
}

fun iosSlideOutParallax(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left,
        spring(0.92f, 420f), targetOffset = { -(it * 0.25f).toInt() }) +
            fadeOut(tween(180, easing = IOSEasing), targetAlpha = 0.6f)
}

fun iosPopEnterParallax(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right,
        spring(0.92f, 420f), initialOffset = { -(it * 0.25f).toInt() }) +
            fadeIn(tween(240, easing = IOSEasing), initialAlpha = 0.6f)
}

fun iosPopSlideOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, NavSpringInt) +
            fadeOut(animationSpec = spring(0.95f, 380f))
}

// Zoom переходы — для модальных экранов
fun macZoomIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(spring(0.95f, 360f), initialAlpha = 0f) +
            scaleIn(spring(0.88f, 420f), initialScale = 0.93f)
}

fun macZoomExitShrink(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    // Исчезновение быстрее: tween 200 vs 260 у появления
    fadeOut(tween(200, easing = IOSEasing), targetAlpha = 0.5f) +
            scaleOut(tween(200, easing = IOSEasing), targetScale = 0.96f)
}

fun macZoomPopEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(260, easing = IOSEasing), initialAlpha = 0.5f) +
            scaleIn(tween(260, easing = IOSEasing), initialScale = 0.96f)
}

fun macZoomPopExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(200, easing = IOSEasing)) +
            scaleOut(spring(0.88f, 420f), targetScale = 0.93f)
}

// Fade для homescreen — мягче zoom, база не должна "прыгать"
fun smoothFadeIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(spring(0.95f, 240f), initialAlpha = 0f) +
            scaleIn(spring(0.92f, 280f), initialScale = 0.98f)
}

fun smoothFadeOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(180, easing = IOSEasing)) +
            scaleOut(tween(180, easing = IOSEasing), targetScale = 0.99f)
}

// =============================================================================
// STAGGERED CONTENT
// =============================================================================

@Composable
fun StaggeredColumn(
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
    baseDelayMs: Long = 30L,       // немного быстрее — iOS стаггер короче
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
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(initialDelayMs + index * baseDelayMs); visible = true }

        // alpha и translY — один spring, заканчиваются синхронно
        val animV by animateFloatAsState(
            targetValue   = if (visible) 1f else 0f,
            animationSpec = ContentSpring, label = "stagger_$index"
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
    val ready = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs); ready.value = true }
    AnimatedContent(ready.value,
        transitionSpec = {
            fadeIn(ContentSpring) togetherWith fadeOut(spring(0.92f, 280f))
        }, label = "smooth") { isReady -> if (isReady) content() else skeleton() }
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
// 11. ANIMATED COUNTER
// =============================================================================

@Composable
fun AnimatedCounter(
    value: Int, modifier: Modifier = Modifier,
    suffix: String = "", prefix: String = "",
    color: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val animValue = remember { Animatable(value.toFloat()) }
    LaunchedEffect(value) { animValue.animateTo(value.toFloat(), spring(0.82f, 240f)) }
    Text("$prefix${animValue.value.toInt()}$suffix",
        color = color, fontSize = fontSize, fontWeight = fontWeight, modifier = modifier)
}

// =============================================================================
// 12. SHAKE — один spring с bounce вместо цикла tween
// iOS встряхивание это один physics-based spring, не серия tween
// =============================================================================

@Composable
fun Modifier.shake(trigger: Boolean, onFinished: () -> Unit = {}): Modifier {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        // Один импульс → spring сам создаёт затухающие колебания
        offsetX.snapTo(14f)
        offsetX.animateTo(
            targetValue   = 0f,
            animationSpec = spring(dampingRatio = 0.38f, stiffness = 680f)
        )
        onFinished()
    }
    return this.graphicsLayer { translationX = offsetX.value }
}

// =============================================================================
// 13. SUCCESS BURST
// =============================================================================

@Composable
fun SuccessBurst(
    trigger: Boolean, modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CAF50)
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        scale.snapTo(0.2f); alpha.snapTo(0.6f)
        kotlinx.coroutines.coroutineScope {
            launch { scale.animateTo(2.0f, spring(0.75f, 300f)) }
            launch { alpha.animateTo(0f, tween(450, easing = IOSEasing)) }
        }
    }
    Box(modifier = modifier
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }
        .background(color, CircleShape))
}

// =============================================================================
// 14. DOTS LOADER
// =============================================================================

@Composable
fun DotsLoader(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    dotSize: androidx.compose.ui.unit.Dp = 8.dp
) {
    val t = rememberInfiniteTransition(label = "dots")
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
// 15. ANIMATED PROGRESS BAR
// =============================================================================

@Composable
fun AnimatedProgressBar(
    progress: Float, modifier: Modifier = Modifier,
    trackColor: Color = Color.White.copy(alpha = 0.12f),
    fillColor: Color = Color(0xFF6A5AE0),
    cornerRadius: androidx.compose.ui.unit.Dp = 6.dp
) {
    val animProg by animateFloatAsState(progress.coerceIn(0f, 1f),
        spring(0.88f, 240f), label = "bar")
    Box(modifier = modifier.height(8.dp)
        .clip(RoundedCornerShape(cornerRadius)).background(trackColor)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animProg)
            .background(fillColor, RoundedCornerShape(cornerRadius)))
    }
}

// =============================================================================
// 16. MORPH BUTTON
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
    // alpha переходы — одинаковый tween, синхронно
    val contentAlpha by animateFloatAsState(
        if (isLoading) 0f else 1f, tween(200, easing = IOSEasing), label = "mb_a")
    val spinnerAlpha by animateFloatAsState(
        if (isLoading) 1f else 0f, tween(200, easing = IOSEasing), label = "mb_s")

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
// 17. SCANNER BEAM — замедляется у краёв
// =============================================================================

@Composable
fun ScannerBeam(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6A5AE0),
    isActive: Boolean = true
) {
    val t = rememberInfiniteTransition(label = "beam")
    val position by t.animateFloat(0.05f, 0.95f,
        infiniteRepeatable(
            // EaseInOut: луч ускоряется в середине, замедляется у краёв
            tween(1800, easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)),
            RepeatMode.Reverse
        ), "beam_pos")
    val beamAlpha by animateFloatAsState(
        if (isActive) 1f else 0f, tween(260, easing = IOSEasing), label = "beam_a")
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
// 18. FLOATING ENTRANCE
// =============================================================================

@Composable
fun FloatingEntrance(
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    offsetY: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable () -> Unit
) {
    var triggered by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    LaunchedEffect(Unit) { if (delayMs > 0) delay(delayMs.toLong()); triggered = true }

    // alpha и translY — один ContentSpring, заканчиваются вместе
    val animV by animateFloatAsState(
        targetValue   = if (triggered) 1f else 0f,
        animationSpec = ContentSpring, label = "float"
    )
    Box(modifier = modifier.graphicsLayer {
        alpha        = animV
        translationY = (1f - animV) * with(density) { offsetY.toPx() }
    }) { content() }
}