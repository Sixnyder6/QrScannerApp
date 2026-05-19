package com.example.qrscannerapp.common.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.qrscannerapp.DialogAnimation
import kotlinx.coroutines.delay

val LocalDialogAnimation = compositionLocalOf { DialogAnimation.SCALE_BOUNCY }

// =============================================================================
// AnimatedDialogWrapper
//
// Универсальная обёртка для любого диалога в приложении.
// Читает выбранный стиль анимации из DialogAnimation и применяет его.
//
// Использование:
//   AnimatedDialogWrapper(
//       animation  = currentAnimation,  // из SettingsManager
//       onDismiss  = { showDialog = false }
//   ) {
//       // Любой контент диалога
//       MyDialogContent()
//   }
//
// Поддерживаемые анимации:
//   SCALE_BOUNCY — выпрыгивает из центра с отскоком (BouncySpring)
//   SLIDE_BOTTOM — приезжает снизу как bottom sheet
//   FALL_TOP     — падает сверху и приземляется с bounce
//   FADE         — чистое растворение, без движения
//   FLIP         — переворот по оси X (rotationX 90→0)
//   ZOOM_SOFT    — мягкий zoom без overshoot
// =============================================================================

// Спринги из TransitionEngine — единый источник физики для всего приложения

@Composable
fun AnimatedDialogWrapper(
    animation: DialogAnimation = LocalDialogAnimation.current,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    // Флаг появления — запускается через LaunchedEffect сразу после рендера
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(16); visible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Затемнение фона — всегда fade независимо от анимации
            val scrimAlpha by animateFloatAsState(
                targetValue   = if (visible) 1f else 0f,
                animationSpec = tween(200, easing = IOSEasing),
                label         = "scrim"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha * 0.4f }
                    .background(Color.Black)
                    .clickable(onClick = onDismiss)
            )

            // Контент с выбранной анимацией
            SpyderAnimatedContent(
                animation = animation,
                visible   = visible,
                content   = content
            )
        }
    }
}

@Composable
private fun SpyderAnimatedContent(
    animation: DialogAnimation,
    visible: Boolean,
    content: @Composable () -> Unit
) {
    when (animation) {

        // ── SCALE BOUNCY — выпрыгивает из центра ─────────────────────────
        DialogAnimation.SCALE_BOUNCY -> {
            val progress by animateFloatAsState(
                targetValue   = if (visible) 1f else 0f,
                animationSpec = if (visible) BouncySpring else spring(0.88f, 420f),
                label         = "scale_bouncy"
            )
            Box(modifier = Modifier.graphicsLayer {
                scaleX = 0.85f + progress * 0.15f
                scaleY = 0.85f + progress * 0.15f
                alpha  = progress
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }) { content() }
        }

        // ── SLIDE BOTTOM — приезжает снизу ───────────────────────────────
        DialogAnimation.SLIDE_BOTTOM -> {
            val progress by animateFloatAsState(
                targetValue   = if (visible) 0f else 1f,
                animationSpec = if (visible) FallSpring else spring(0.92f, 480f),
                label         = "slide_bottom"
            )
            val alpha by animateFloatAsState(
                targetValue   = if (visible) 1f else 0f,
                animationSpec = tween(220, easing = IOSEasing),
                label         = "slide_alpha"
            )
            Box(modifier = Modifier.graphicsLayer {
                translationY = progress * 320f
                this.alpha   = alpha
            }) { content() }
        }

        // ── FALL TOP — падает сверху ──────────────────────────────────────
        DialogAnimation.FALL_TOP -> {
            val progress by animateFloatAsState(
                targetValue   = if (visible) 0f else 1f,
                animationSpec = if (visible) FallSpring else spring(0.88f, 400f),
                label         = "fall_top"
            )
            val alpha by animateFloatAsState(
                targetValue   = if (visible) 1f else 0f,
                animationSpec = tween(220, easing = IOSEasing),
                label         = "fall_alpha"
            )
            Box(modifier = Modifier.graphicsLayer {
                translationY = -progress * 280f
                this.alpha   = alpha
            }) { content() }
        }

        // ── FADE — чистое растворение ─────────────────────────────────────
        DialogAnimation.FADE -> {
            val alpha by animateFloatAsState(
                targetValue   = if (visible) 1f else 0f,
                animationSpec = if (visible) ContentSpring else tween(180, easing = IOSEasing),
                label         = "fade"
            )
            Box(modifier = Modifier.graphicsLayer { this.alpha = alpha }) { content() }
        }

        // ── FLIP — переворот по оси X ─────────────────────────────────────
        DialogAnimation.FLIP -> {
            val rotation by animateFloatAsState(
                targetValue   = if (visible) 0f else 90f,
                animationSpec = if (visible) FallSpring else tween(180, easing = IOSEasing),
                label         = "flip_rot"
            )
            val alpha by animateFloatAsState(
                targetValue   = if (visible) 1f else 0f,
                animationSpec = tween(200, easing = IOSEasing),
                label         = "flip_alpha"
            )
            Box(modifier = Modifier.graphicsLayer {
                rotationX      = rotation
                this.alpha     = alpha
                cameraDistance = 12f * density
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }) { content() }
        }

        // ── ZOOM SOFT — мягкий zoom без overshoot ────────────────────────
        DialogAnimation.ZOOM_SOFT -> {
            val progress by animateFloatAsState(
                targetValue   = if (visible) 1f else 0f,
                animationSpec = if (visible) NavSpring else tween(180, easing = IOSEasing),
                label         = "zoom_soft"
            )
            Box(modifier = Modifier.graphicsLayer {
                scaleX = 0.92f + progress * 0.08f
                scaleY = 0.92f + progress * 0.08f
                alpha  = progress
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }) { content() }
        }
    }
}