package com.example.qrscannerapp.common.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// =============================================================================
// TRANSITION ENGINE — ELEGANT & FLUID
// =============================================================================

// --- REFINED SPRING SPECS ---

private val NavigationSpringInt = spring<IntOffset>(
    dampingRatio = 0.92f,
    stiffness = 250f
)

private val AlphaEasing = CubicBezierEasing(0.33f, 0.0f, 0.67f, 1f)

val MicroSpring = spring<Float>(
    dampingRatio = 0.72f,
    stiffness = 600f
)

private val SlowElegantSpring = spring<Float>(
    dampingRatio = 0.9f,
    stiffness = 180f
)

// =============================================================================
// 1. SWIPE BACK — SMOOTH & FORGIVING
// =============================================================================

class SwipeBackState(
    private val onBack: () -> Unit,
    private val threshold: Float = 0.3f
) {
    var progress by mutableFloatStateOf(0f)
        private set

    var isGestureActive by mutableStateOf(false)
        private set

    private val animatableProgress = Animatable(0f)

    fun startGesture() {
        isGestureActive = true
        progress = 0f
    }

    fun updateProgress(delta: Float, totalWidth: Float) {
        progress = (delta / totalWidth).coerceIn(0f, 1.15f)
    }

    fun endGesture(scope: kotlinx.coroutines.CoroutineScope) {
        isGestureActive = false
        val shouldComplete = progress > threshold

        scope.launch {
            if (shouldComplete) {
                animatableProgress.snapTo(progress)
                animatableProgress.animateTo(
                    targetValue = 1.15f,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 280f)
                )
                onBack()
                animatableProgress.snapTo(0f)
                progress = 0f
            } else {
                animatableProgress.snapTo(progress)
                animatableProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f)
                )
                progress = 0f
            }
        }
    }

    fun cancelGesture() {
        isGestureActive = false
        progress = 0f
    }

    fun getAnimatedProgress(): Float {
        return if (isGestureActive) progress else animatableProgress.value
    }
}

@Composable
fun rememberSwipeBackState(onBack: () -> Unit): SwipeBackState {
    return remember { SwipeBackState(onBack) }
}

@Composable
fun Modifier.interactiveSwipeBack(
    swipeBackState: SwipeBackState,
    enabled: Boolean = true
): Modifier {
    val scope = rememberCoroutineScope()

    return this.then(
        if (enabled) {
            Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        swipeBackState.startGesture()
                    },
                    onDragEnd = {
                        swipeBackState.endGesture(scope)
                    },
                    onDragCancel = {
                        swipeBackState.cancelGesture()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeBackState.updateProgress(
                            swipeBackState.progress * size.width + dragAmount,
                            size.width.toFloat()
                        )
                    }
                )
            }
        } else {
            Modifier
        }
    )
}

// =============================================================================
// 2. TACTILE BUTTON — REFINED FEEL
// =============================================================================

@Composable
fun TactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scaleOnPress: Float = 0.96f,
    brightnessOnPress: Float = 0.85f,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleOnPress else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "ts"
    )

    val buttonAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) brightnessOnPress else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "tb"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = buttonAlpha
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when {
                            event.changes.any { it.pressed } -> isPressed = true
                            event.changes.any { !it.pressed } -> {
                                if (isPressed) {
                                    onClick()
                                    isPressed = false
                                }
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}

// =============================================================================
// 3. BOUNCY ICON — PLAYFUL BUT CONTROLLED
// =============================================================================

@Composable
fun BouncyIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isBouncing by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isBouncing) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 550f),
        label = "bs",
        finishedListener = { isBouncing = false }
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed && !it.previousPressed }) {
                            isBouncing = true
                            onClick()
                        }
                    }
                }
            }
    ) {
        content()
    }
}

// =============================================================================
// 4. SWIPE BACK INDICATOR — ELEGANT FADE
// =============================================================================

@Composable
fun SwipeBackIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (progress <= 0f) return

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "si"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(3.dp)
            .alpha((animatedProgress * 0.7f).coerceIn(0f, 1f))
            .background(Color.White.copy(alpha = animatedProgress.coerceIn(0f, 1f)))
    )
}

@Composable
fun BackArrowIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val arrowAlpha by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "aa"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                alpha = arrowAlpha
                translationX = -progress.coerceIn(0f, 1f) * 120f
            }
            .background(
                color = Color.White.copy(alpha = 0.25f * arrowAlpha),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u2039",
            color = Color.White.copy(alpha = arrowAlpha),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
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
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing) {
            infiniteRepeatable(animation = tween(1200, easing = LinearEasing))
        } else {
            spring(dampingRatio = 0.8f, stiffness = 250f)
        },
        label = "rr"
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                rotationZ = rotation
                alpha = (pullProgress * 2.2f).coerceIn(0f, 1f)
            }
            .background(
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(22.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isRefreshing) "\u21BB" else "\u2193",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    }
}

// =============================================================================
// 6. CONFETTI — CANVAS-BASED, GRACEFUL
// =============================================================================

class ConfettiParticle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiEffect(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 30
) {
    var show by remember { mutableStateOf(false) }
    val animatedProgress = remember { Animatable(0f) }

    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                x = Random.nextFloat() * 0.5f + 0.25f,
                y = Random.nextFloat() * 0.3f + 0.25f,
                velocityX = (Random.nextFloat() - 0.5f) * 0.7f,
                velocityY = (Random.nextFloat() - 0.5f) * -0.7f - 0.25f,
                color = Color.hsl(
                    hue = Random.nextFloat() * 360f,
                    saturation = 0.75f,
                    lightness = 0.65f
                ),
                size = Random.nextFloat() * 7f + 3f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 540f
            )
        }
    }

    LaunchedEffect(trigger) {
        if (trigger) {
            show = true
            animatedProgress.snapTo(0f)
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1800, easing = FastOutSlowInEasing)
            )
            delay(600)
            show = false
        }
    }

    if (!show) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val progress = animatedProgress.value
                val fadeOut = if (progress > 0.65f) 1f - ((progress - 0.65f) / 0.35f) else 1f

                particles.forEach { particle ->
                    val cx = (particle.x + particle.velocityX * progress) * size.width
                    val cy = (particle.y + particle.velocityY * progress) * size.height
                    val rot = particle.rotation + particle.rotationSpeed * progress
                    val alpha = ((1f - progress * 0.8f) * fadeOut).coerceIn(0f, 1f)

                    drawParticle(cx, cy, particle.size, rot, particle.color, alpha)
                }
            }
    )
}

private fun DrawScope.drawParticle(
    x: Float, y: Float, size: Float, rotation: Float, color: Color, alpha: Float
) {
    val half = size / 2f
    val angle = Math.toRadians(rotation.toDouble())
    val cosA = cos(angle).toFloat()
    val sinA = sin(angle).toFloat()

    val p1 = androidx.compose.ui.geometry.Offset(
        x + (-half * cosA - (-half * 0.45f) * sinA),
        y + (-half * sinA + (-half * 0.45f) * cosA)
    )
    val p2 = androidx.compose.ui.geometry.Offset(
        x + (half * cosA - (-half * 0.45f) * sinA),
        y + (half * sinA + (-half * 0.45f) * cosA)
    )
    val p3 = androidx.compose.ui.geometry.Offset(
        x + (half * cosA - (half * 0.45f) * sinA),
        y + (half * sinA + (half * 0.45f) * cosA)
    )
    val p4 = androidx.compose.ui.geometry.Offset(
        x + (-half * cosA - (half * 0.45f) * sinA),
        y + (-half * sinA + (half * 0.45f) * cosA)
    )

    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        close()
    }
    drawPath(path, color.copy(alpha = alpha))
}

// =============================================================================
// 7. TILT CARD — BUTTERY SMOOTH
// =============================================================================

@Composable
fun TiltCard(
    modifier: Modifier = Modifier,
    maxTilt: Float = 8f,
    content: @Composable () -> Unit
) {
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .graphicsLayer {
                this.rotationX = rotationX.value
                this.rotationY = rotationY.value
                cameraDistance = 14f * density.density
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (change.pressed) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val ty = ((change.position.x - cx) / cx) * maxTilt
                            val tx = -((change.position.y - cy) / cy) * maxTilt

                            scope.launch {
                                rotationX.snapTo(tx)
                                rotationY.snapTo(ty)
                            }
                        } else {
                            scope.launch {
                                rotationX.animateTo(
                                    0f,
                                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f)
                                )
                                rotationY.animateTo(
                                    0f,
                                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f)
                                )
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}

// =============================================================================
// 8. PARALLAX SCROLL
// =============================================================================

@Composable
fun ParallaxImage(
    scrollOffset: Float,
    parallaxFactor: Float = 0.45f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedOffset by animateFloatAsState(
        targetValue = scrollOffset * parallaxFactor,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 200f),
        label = "po"
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationY = animatedOffset
        }
    ) {
        content()
    }
}

// =============================================================================
// 9. GLOW EFFECT — SOFT & ATMOSPHERIC
// =============================================================================

@Composable
fun GlowEffect(
    isActive: Boolean,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.25f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "ga"
    )

    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            drawCircle(
                color = color.copy(alpha = glowAlpha),
                radius = size.minDimension * 0.55f,
                center = center
            )
        }
    )
}

// =============================================================================
// 10. PULSATING RIPPLE — GENTLE PULSE
// =============================================================================

@Composable
fun PulsatingRipple(
    isActive: Boolean,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rip")

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rs"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ra"
    )

    if (isActive) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = rippleScale
                    scaleY = rippleScale
                    alpha = rippleAlpha
                }
                .background(color, CircleShape)
        )
    }
}

// =============================================================================
// NAVIGATION TRANSITIONS — ELEVATED
// =============================================================================

fun iosSlideIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = NavigationSpringInt,
        initialOffset = { it }
    ) + fadeIn(animationSpec = tween(400, easing = AlphaEasing))
}

fun iosSlideOutParallax(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 250f),
        targetOffset = { -(it * 0.3f).toInt() }
    ) + fadeOut(animationSpec = tween(400, easing = AlphaEasing), targetAlpha = 0.5f)
}

fun iosPopEnterParallax(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 250f),
        initialOffset = { -(it * 0.3f).toInt() }
    ) + fadeIn(animationSpec = tween(400, easing = AlphaEasing), initialAlpha = 0.5f)
}

fun iosPopSlideOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = NavigationSpringInt,
        targetOffset = { it }
    )
}

fun macZoomIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
            scaleIn(animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f), initialScale = 0.9f)
}

fun macZoomExitShrink(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(250, easing = FastOutLinearInEasing), targetAlpha = 0.4f) +
            scaleOut(animationSpec = tween(250, easing = FastOutLinearInEasing), targetScale = 0.94f)
}

fun macZoomPopEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing), initialAlpha = 0.4f) +
            scaleIn(animationSpec = tween(300, easing = FastOutSlowInEasing), initialScale = 0.94f)
}

fun macZoomPopExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(250, easing = FastOutLinearInEasing)) +
            scaleOut(animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f), targetScale = 0.9f)
}

fun smoothFadeIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing)) +
            scaleIn(animationSpec = spring(dampingRatio = 0.9f, stiffness = 250f), initialScale = 0.97f)
}

fun smoothFadeOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(250, easing = FastOutLinearInEasing)) +
            scaleOut(animationSpec = tween(250, easing = FastOutLinearInEasing), targetScale = 0.98f)
}

// =============================================================================
// STAGGERED CONTENT
// =============================================================================

@Composable
fun StaggeredColumn(
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
    baseDelayMs: Long = 40L,
    initialDelayMs: Long = 60L,
    content: @Composable StaggeredScope.() -> Unit
) {
    val scope = remember(itemCount) { StaggeredScopeImpl(itemCount, baseDelayMs, initialDelayMs) }
    Column(modifier = modifier) {
        scope.content()
    }
}

interface StaggeredScope {
    @Composable
    fun StaggeredItem(index: Int, modifier: Modifier, content: @Composable () -> Unit)
}

private class StaggeredScopeImpl(
    private val itemCount: Int,
    private val baseDelayMs: Long,
    private val initialDelayMs: Long
) : StaggeredScope {

    @Composable
    override fun StaggeredItem(index: Int, modifier: Modifier, content: @Composable () -> Unit) {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(initialDelayMs + (index * baseDelayMs))
            visible = true
        }

        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = SlowElegantSpring,
            label = "sa"
        )
        val translationY by animateFloatAsState(
            targetValue = if (visible) 0f else 20f,
            animationSpec = SlowElegantSpring,
            label = "sy"
        )

        Box(
            modifier = modifier.graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
            }
        ) {
            content()
        }
    }
}

// =============================================================================
// HELPERS
// =============================================================================

@Composable
fun SpringAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = SlowElegantSpring) +
                slideInVertically(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 220f),
                    initialOffsetY = { it / 8 }
                ),
        exit = fadeOut(animationSpec = tween(200)) +
                slideOutVertically(
                    animationSpec = tween(200),
                    targetOffsetY = { it / 10 }
                ),
        content = content
    )
}

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, cornerRadius: Int = 12) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color.White.copy(alpha = shimmerAlpha))
    )
}

@Composable
fun SkeletonList(itemCount: Int = 5, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBlock(
            modifier = Modifier.fillMaxWidth(0.35f).height(18.dp),
            cornerRadius = 8
        )
        Spacer(modifier = Modifier.height(6.dp))
        repeat(itemCount) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth().height(68.dp))
        }
    }
}

@Composable
fun SmoothScreen(
    delayMs: Long = 100L,
    skeleton: @Composable () -> Unit = { SkeletonList() },
    content: @Composable () -> Unit
) {
    val ready = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMs)
        ready.value = true
    }

    AnimatedContent(
        targetState = ready.value,
        transitionSpec = {
            fadeIn(animationSpec = SlowElegantSpring) togetherWith
                    fadeOut(animationSpec = spring(dampingRatio = 0.9f, stiffness = 250f))
        },
        label = "smooth_screen"
    ) { isReady ->
        if (isReady) content() else skeleton()
    }
}

@Composable
fun TransitionScrim(visible: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.12f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 220f),
        label = "scrim"
    )
    if (alpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = alpha))
        )
    }
}