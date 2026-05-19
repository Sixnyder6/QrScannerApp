package com.example.qrscannerapp.features.homescreen.ui.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * Глобальный shimmer-движок.
 *
 * Один InfiniteTransition на весь homescreen.
 * Каждый виджет берёт globalPhase и добавляет свой phaseOffset —
 * так все блики сдвинуты относительно друг друга но идут строго синхронно
 * от одного таймера, не создавая N отдельных animateFloat.
 *
 * Использование:
 *   // В HomescreenScreen оборачиваем контент:
 *   ProvideShimmerPhase { /* виджеты */ }
 *
 *   // В WidgetCard / AppIconTile:
 *   val phase = LocalShimmerPhase.current.value
 */

/** Период одного shimmer-цикла в мс. */
private const val SHIMMER_PERIOD_MS = 8000

/**
 * CompositionLocal несёт `State<Float>` (0f..1f) — глобальную фазу shimmer.
 * Default = заглушка 0f (если ProvideShimmerPhase не вызван).
 */
val LocalShimmerPhase = compositionLocalOf<State<Float>> {
    object : State<Float> { override val value = 0f }
}

/**
 * Оборачивает content в контекст с единым shimmer-таймером.
 * Вызывай один раз в HomescreenScreen перед LazyVerticalGrid.
 */
@Composable
fun ProvideShimmerPhase(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "global_shimmer")
    val phase = transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(SHIMMER_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_phase"
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalShimmerPhase provides phase,
        content = content
    )
}