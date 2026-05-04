package com.example.qrscannerapp.common.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.delay

// =============================================================================
// TRANSITION ENGINE — iOS-STYLE SMOOTH NAVIGATION
// =============================================================================
//
// Что делает:
// 1) slideInFromRight / slideOutToRight — переходы "как на айфоне"
// 2) Parallax-эффект: текущий экран уходит влево на 25% и затемняется
// 3) Spring-кривые вместо линейных tween — пружинистость как у iOS
// 4) StaggeredContent — контент экрана появляется по очереди (stagger)
// 5) 120fps-ready: используем spring() вместо tween — нет привязки к duration
// =============================================================================

// ─── iOS-LIKE SPRING SPEC ────────────────────────────────────────────────────
// Эти параметры максимально близки к UIKit spring animation

/**
 * Пружина для базовых элементов
 */
private val NavigationSpring = spring<Float>(
    dampingRatio = 0.86f,
    stiffness = 300f
)

/**
 * Пружина для навигационных переходов.
 * Чуть более плотная пружина: меньше "желе", более собранное движение
 */
private val NavigationSpringInt = spring<IntOffset>(
    dampingRatio = 0.9f,
    stiffness = 280f
)

/**
 * Специальная кривая для прозрачности, чтобы она совпадала с пружиной по времени
 */
private val AlphaEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Пружина для появления контента внутри экрана (stagger).
 * Более мягкая — красиво "приземляется"
 */
private val ContentSpring = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = 220f
)

/**
 * Быстрая пружина для мелких элементов (иконки, бейджи)
 */
val SnapSpring = spring<Float>(
    dampingRatio = 0.75f,
    stiffness = 500f
)

// ─── NAVIGATION TRANSITIONS ─────────────────────────────────────────────────

/**
 * FORWARD: новый экран слайдит справа → налево
 * ФИКС: Плавное появление альфы синхронизировано с движением.
 */
fun iosSlideIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = NavigationSpringInt,
        initialOffset = { fullWidth -> fullWidth } // Начинаем ровно из-за правого края
    ) + fadeIn(
        animationSpec = tween(350, easing = AlphaEasing) // Растянули fade, чтобы не было "вспышки"
    )
}

/**
 * FORWARD: текущий экран уходит влево (parallax).
 * ФИКС: Уменьшили параллакс до 25%, синхронизировали затемнение.
 */
fun iosSlideOutParallax(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = NavigationSpringInt,
        targetOffset = { fullWidth -> -(fullWidth * 0.25f).toInt() } // 25% параллакс
    ) + fadeOut(
        animationSpec = tween(350, easing = AlphaEasing),
        targetAlpha = 0.6f // Эмитируем черную полупрозрачную тень iOS
    )
}

/**
 * BACK: экран возвращается слева (parallax).
 * ФИКС: Стартуем с 25%, синхронно высветляемся.
 */
fun iosPopEnterParallax(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = NavigationSpringInt,
        initialOffset = { fullWidth -> -(fullWidth * 0.25f).toInt() }
    ) + fadeIn(
        animationSpec = tween(350, easing = AlphaEasing),
        initialAlpha = 0.6f
    )
}

/**
 * BACK: текущий экран уходит вправо.
 * ФИКС КРИТИЧЕСКИЙ: Убрали fadeOut! На iOS закрывающийся экран НЕ становится прозрачным.
 * Он просто уезжает, открывая нижний слой.
 */
fun iosPopSlideOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = NavigationSpringInt,
        targetOffset = { fullWidth -> fullWidth }
    )
    // НЕТ + fadeOut(...) — экран плотно уезжает без растворения
}

// ─── macOS / iOS ZOOM TRANSITIONS (scale in/out, no slide jank) ──────────────

/**
 * FORWARD enter: экран "разжимается" из центра (как открытие окна на macOS)
 */
fun macZoomIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
    scaleIn(
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 420f),
        initialScale = 0.88f
    )
}

/**
 * FORWARD exit: текущий экран чуть "уходит вглубь" (iOS card depth)
 */
fun macZoomExitShrink(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing), targetAlpha = 0.45f) +
    scaleOut(
        animationSpec = tween(200, easing = FastOutLinearInEasing),
        targetScale = 0.95f
    )
}

/**
 * BACK enter: фоновый экран "возвращается" из глубины
 */
fun macZoomPopEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing), initialAlpha = 0.45f) +
    scaleIn(
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        initialScale = 0.95f
    )
}

/**
 * BACK exit: текущий экран "сжимается" и исчезает
 */
fun macZoomPopExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing)) +
    scaleOut(
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 420f),
        targetScale = 0.88f
    )
}

// ─── Упрощённые fade-переходы (для модальных/особых экранов) ─────────────────

/**
 * ФИКС: Уменьшили scale. 0.97 смотрится солиднее и не укачивает.
 */
fun smoothFadeIn(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(250, easing = LinearOutSlowInEasing)) +
            scaleIn(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                initialScale = 0.97f
            )
}

fun smoothFadeOut(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) +
            scaleOut(
                animationSpec = tween(200, easing = FastOutLinearInEasing),
                targetScale = 0.98f
            )
}

// ─── STAGGERED CONTENT APPEARANCE ────────────────────────────────────────────
//
// Оборачиваешь элементы экрана в StaggeredItem — они появляются по очереди
// с задержкой. Это создаёт эффект "каскадного" появления как в iOS Settings.

/**
 * Контейнер для staggered-анимации.
 * @param itemCount сколько элементов будет внутри (для расчёта задержек)
 * @param baseDelayMs задержка между элементами
 * @param initialDelayMs задержка перед первым элементом (дать навигации закончить)
 */
@Composable
fun StaggeredColumn(
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
    baseDelayMs: Long = 35L,
    initialDelayMs: Long = 80L,
    content: @Composable StaggeredScope.() -> Unit
) {
    val scope = remember(itemCount) { StaggeredScopeImpl(itemCount, baseDelayMs, initialDelayMs) }
    Column(modifier = modifier) {
        scope.content()
    }
}

interface StaggeredScope {
    /**
     * Оборачивает элемент в анимацию появления.
     * @param index порядковый номер (0, 1, 2, ...)
     */
    @Composable
    fun StaggeredItem(
        index: Int,
        modifier: Modifier,
        content: @Composable () -> Unit
    )
}

private class StaggeredScopeImpl(
    private val itemCount: Int,
    private val baseDelayMs: Long,
    private val initialDelayMs: Long
) : StaggeredScope {

    @Composable
    override fun StaggeredItem(
        index: Int,
        modifier: Modifier,
        content: @Composable () -> Unit
    ) {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(initialDelayMs + (index * baseDelayMs))
            visible = true
        }

        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = ContentSpring,
            label = "stagger_alpha_$index"
        )
        val translationY by animateFloatAsState(
            targetValue = if (visible) 0f else 24f,
            animationSpec = ContentSpring,
            label = "stagger_y_$index"
        )

        Box(
            modifier = modifier
                .graphicsLayer {
                    this.alpha = alpha
                    this.translationY = translationY
                }
        ) {
            content()
        }
    }
}

// ─── ANIMATED VISIBILITY HELPERS ─────────────────────────────────────────────

/**
 * Плавное появление с пружиной (для любых composable)
 * Используй для: карточек, баннеров, FAB, модалок
 */
@Composable
fun SpringAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = ContentSpring) +
                slideInVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 250f),
                    initialOffsetY = { it / 6 }
                ),
        exit = fadeOut(animationSpec = tween(180)) +
                slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { it / 8 }
                ),
        content = content
    )
}

/**
 * Shimmer-скелетон пока данные грузятся.
 * Положи вместо контента — будет мерцающий прямоугольник.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
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

/**
 * Skeleton-версия списка (для экранов с карточками)
 */
@Composable
fun SkeletonList(
    itemCount: Int = 5,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header skeleton
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(20.dp),
            cornerRadius = 8
        )
        Spacer(modifier = Modifier.height(4.dp))

        repeat(itemCount) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            )
        }
    }
}

// ─── SCREEN WRAPPER — delayed content load ───────────────────────────────────

/**
 * Обёртка экрана: показывает skeleton пока анимация перехода идёт,
 * потом плавно показывает реальный контент.
 *
 * @param delayMs сколько ждать перед показом контента (подбирай под скорость перехода)
 * @param skeleton что показывать пока ждём
 * @param content реальный контент экрана
 */
@Composable
fun SmoothScreen(
    delayMs: Long = 100L,
    skeleton: @Composable () -> Unit = { SkeletonList() },
    content: @Composable () -> Unit
) {
    var contentReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMs)
        contentReady = true
    }

    AnimatedContent(
        targetState = contentReady,
        transitionSpec = {
            fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween(150))
        },
        label = "smooth_screen"
    ) { ready ->
        if (ready) {
            content()
        } else {
            skeleton()
        }
    }
}

// ─── SCREEN TRANSITION OVERLAY ───────────────────────────────────────────────
// Затемнение при переходе (как iOS push navigation shadow)

@Composable
fun TransitionScrim(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.15f else 0f,
        animationSpec = tween(300),
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