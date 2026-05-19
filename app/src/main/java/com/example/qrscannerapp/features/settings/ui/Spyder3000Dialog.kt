package com.example.qrscannerapp.features.settings.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.DialogAnimation
import com.example.qrscannerapp.common.ui.AnimatedDialogWrapper
import com.example.qrscannerapp.R
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import kotlinx.coroutines.delay
import kotlin.math.*

const val SPYDER_VERSION = "1.0.0"
const val SPYDER_BUILD = "SP3-2025-001"

private val SpyderGreen = Color(0xFF00FF41)
private val SpyderGreenDark = Color(0xFF003311)
private val SpyderBg = Color(0xFF000000)
private val SpyderCard = Color(0xFF0A0A0A)
private val SpyderBorder = Color(0xFF1A1A1A)

private val IOSEasing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
private val SP_Content = spring<Float>(dampingRatio = 0.95f, stiffness = 180f)

// =============================================================================
// ТОЧКА ВХОДА
// =============================================================================

@Composable
fun Spyder3000SettingsItem(
    modifier: Modifier = Modifier
) {
    val viewModel: SpyderSettingsViewModel = hiltViewModel()
    val autoUpdate by viewModel.autoUpdateEnabled.collectAsState()
    val currentAnimation by viewModel.currentDialogAnimation.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SpyderMiniLogo(size = 34)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Spyder3000", color = SpyderGreen, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .background(SpyderGreenDark, RoundedCornerShape(4.dp))
                        .border(0.5.dp, SpyderGreen.copy(0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text("v$SPYDER_VERSION", color = SpyderGreen.copy(0.8f),
                        fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text("Графический движок · Нажмите для подробностей",
                color = StardustTextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = autoUpdate,
            onCheckedChange = { viewModel.toggleAutoUpdate() },
            modifier = Modifier.graphicsLayer { scaleX = 0.8f; scaleY = 0.8f },
            colors = SwitchDefaults.colors(
                checkedThumbColor = SpyderBg,
                checkedTrackColor = SpyderGreen,
                uncheckedThumbColor = StardustTextSecondary,
                uncheckedTrackColor = StardustItemBg
            )
        )
    }

    if (showDialog) {
        Spyder3000Dialog(
            viewModel = viewModel,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun Spyder3000Dialog(
    viewModel: SpyderSettingsViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    val autoUpdate by viewModel.autoUpdateEnabled.collectAsState()
    val currentAnimation by viewModel.currentDialogAnimation.collectAsState()
    var previewAnim by remember { mutableStateOf<DialogAnimation?>(null) }

    val spiderBounce = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(30)
        visible = true
        delay(80)
        spiderBounce.animateTo(1.25f, spring(0.45f, 600f))
        spiderBounce.animateTo(1f, spring(0.60f, 400f))
    }

    AnimatedDialogWrapper(
        animation = currentAnimation,
        onDismiss = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(SpyderBg)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(
                        SpyderGreen.copy(0.5f), SpyderGreenDark)),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Column {
                SpyderHeader(onDismiss = onDismiss, spiderScale = spiderBounce.value)
                SpyderTabsAnimated(activeTab, { activeTab = it }, visible)
                HorizontalDivider(color = SpyderBorder, thickness = 0.5.dp)
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 550.dp)) {
                    when (activeTab) {
                        0 -> SpyderAboutTab()
                        1 -> SpyderDemoTab(viewModel)
                        2 -> SpyderSettingsTab(
                            viewModel = viewModel,
                            isAutoUpdateEnabled = autoUpdate,
                            currentAnimation = currentAnimation,
                            onPreviewRequest = { previewAnim = it }
                        )
                        3 -> SpyderPerformanceAnalyzerTab(viewModel)
                    }
                }
            }

            // Оверлей превью анимации
            previewAnim?.let { anim ->
                AnimationPreviewOverlay(
                    animation = anim,
                    onDone = { previewAnim = null }
                )
            }
        }
    }
}

@Composable
private fun AnimationPreviewOverlay(
    animation: DialogAnimation,
    onDone: () -> Unit
) {
    val density = LocalDensity.current
    var cardVisible by remember { mutableStateOf(false) }
    var scrimVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scrimVisible = true
        delay(40)
        cardVisible = true
        delay(1200)
        cardVisible = false
        delay(350)
        scrimVisible = false
        delay(200)
        onDone()
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (scrimVisible) 0.6f else 0f,
        animationSpec = tween(220, easing = IOSEasing),
        label = "prev_scrim"
    )

    val springSpec: AnimationSpec<Float> = when (animation) {
        DialogAnimation.SCALE_BOUNCY -> spring(0.52f, 520f)
        DialogAnimation.SLIDE_BOTTOM -> spring(0.60f, 380f)
        DialogAnimation.FALL_TOP     -> spring(0.60f, 380f)
        DialogAnimation.FADE         -> tween(280, easing = IOSEasing)
        DialogAnimation.FLIP         -> spring(0.75f, 380f)
        DialogAnimation.ZOOM_SOFT    -> spring(0.90f, 420f)
    }
    val exitSpec: AnimationSpec<Float> = tween(220, easing = IOSEasing)

    val progress by animateFloatAsState(
        targetValue = if (cardVisible) 1f else 0f,
        animationSpec = if (cardVisible) springSpec else exitSpec,
        label = "prev_card"
    )
    val alphaProgress by animateFloatAsState(
        targetValue = if (cardVisible) 1f else 0f,
        animationSpec = tween(220, easing = IOSEasing),
        label = "prev_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .graphicsLayer {
                    this.alpha = alphaProgress
                    when (animation) {
                        DialogAnimation.SCALE_BOUNCY -> {
                            scaleX = 0.82f + progress * 0.18f
                            scaleY = 0.82f + progress * 0.18f
                        }
                        DialogAnimation.SLIDE_BOTTOM -> {
                            translationY = (1f - progress) * 200f
                        }
                        DialogAnimation.FALL_TOP -> {
                            translationY = -(1f - progress) * 200f
                        }
                        DialogAnimation.FADE -> { /* только alpha */ }
                        DialogAnimation.FLIP -> {
                            rotationX = (1f - progress) * 90f
                            cameraDistance = 12f * density.density
                        }
                        DialogAnimation.ZOOM_SOFT -> {
                            scaleX = 0.90f + progress * 0.10f
                            scaleY = 0.90f + progress * 0.10f
                        }
                    }
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111))
                .border(1.dp, SpyderGreen.copy(0.6f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(animation.emoji, fontSize = 22.sp)
                Text(
                    animation.label,
                    color = SpyderGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    animation.description,
                    color = SpyderGreen.copy(0.5f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// =============================================================================
// МИНИ ЛОГОТИП
// =============================================================================

@Composable
fun SpyderMiniLogo(size: Int = 34) {
    val infinite = rememberInfiniteTransition(label = "mini_logo")
    val pulse by infinite.animateFloat(
        0.6f, 1.0f,
        infiniteRepeatable(tween(1800, easing = IOSEasing), RepeatMode.Reverse),
        label = "mini_pulse"
    )
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.26f).dp))
            .background(SpyderBg)
            .border(0.8.dp, SpyderGreen.copy(pulse * 0.6f),
                RoundedCornerShape((size * 0.26f).dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_spider_logo),
            contentDescription = null,
            tint = SpyderGreen.copy(alpha = pulse),
            modifier = Modifier.size((size * 0.70f).dp)
        )
    }
}

// =============================================================================
// ШАПКА
// =============================================================================

@Composable
private fun SpyderHeader(onDismiss: () -> Unit, spiderScale: Float = 1f) {
    val infinite = rememberInfiniteTransition(label = "hdr")
    val glowAlpha by infinite.animateFloat(
        0.3f, 0.8f,
        infiniteRepeatable(tween(2000, easing = IOSEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val rotation by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(SpyderBg)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(SpyderGreen.copy(glowAlpha * 0.25f), SpyderGreen.copy(0.05f), Color.Transparent),
                        Offset(size.width / 2f, size.height * 0.55f), size.width * 0.45f
                    ),
                    size.width * 0.45f,
                    Offset(size.width / 2f, size.height * 0.55f)
                )
            }
    ) {
        Box(modifier = Modifier.size(72.dp).align(Alignment.Center).offset(y = (-8).dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                rotate(rotation) {
                    drawCircle(
                        SpyderGreen.copy(0.25f),
                        size.minDimension / 2f - 2.dp.toPx(),
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = spiderScale
                        scaleY = spiderScale
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(SpyderBg)
                    .border(1.dp, SpyderGreen.copy(glowAlpha), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_spider_logo),
                    contentDescription = "Spyder3000",
                    tint = SpyderGreen,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SPYDER3000", color = SpyderGreen, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("Graphics Engine · $SPYDER_BUILD",
                color = SpyderGreen.copy(0.5f), fontSize = 9.sp, letterSpacing = 1.sp)
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = SpyderGreen.copy(0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// =============================================================================
// ТАБЫ
// =============================================================================

@Composable
private fun SpyderTabsAnimated(activeTab: Int, onTabSelected: (Int) -> Unit, visible: Boolean) {
    val tabs = listOf("О движке", "Демо", "Настройки", "Анализатор")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { idx, label ->
            val isActive = idx == activeTab
            var tabVisible by remember { mutableStateOf(false) }
            LaunchedEffect(visible) {
                if (visible) {
                    delay(idx * 40L)
                    tabVisible = true
                } else {
                    tabVisible = false
                }
            }
            val tabProg by animateFloatAsState(
                targetValue = if (tabVisible) 1f else 0f,
                animationSpec = spring(0.75f, 500f),
                label = "tab_$idx"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .graphicsLayer {
                        alpha = tabProg
                        translationY = (1f - tabProg) * 12f
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) SpyderGreenDark else Color.Transparent)
                    .border(0.8.dp, if (isActive) SpyderGreen.copy(0.6f) else SpyderBorder, RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(idx) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isActive) SpyderGreen else StardustTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// =============================================================================
// ТАБ 1: О ДВИЖКЕ
// =============================================================================

@Composable
private fun SpyderAboutTab() {
    val specs = listOf(
        "Версия" to "Spyder3000 v$SPYDER_VERSION",
        "Spring-система" to "5 пружин · 120fps",
        "Шейдеры" to "AGSL · Android 13+",
        "Частота" to "ProMotion 120Hz",
        "Easing" to "iOS CubicBezier",
        "Движок" to "Compose Canvas"
    )
    val capabilities = listOf(
        "Физические spring-анимации",
        "AGSL шейдеры (nebula, plasma, aurora, silk, ember)",
        "Parallax, tilt, swipe-back с rubber band",
        "Confetti, ripple, success burst",
        "Animated border sweep по периметру",
        "Scanner beam с EaseInOut физикой",
        "Staggered content с каскадом",
        "Morph button, shake, floating entrance"
    )
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(20); mounted = true }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StaggerCard(0, mounted) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpyderCard)
                    .border(0.5.dp, SpyderBorder, RoundedCornerShape(12.dp))
            ) {
                specs.forEachIndexed { idx, (k, v) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp, 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(k, color = StardustTextSecondary, fontSize = 12.sp)
                        Text(v, color = SpyderGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    if (idx < specs.lastIndex) {
                        HorizontalDivider(color = SpyderBorder, thickness = 0.5.dp)
                    }
                }
            }
        }
        StaggerCard(1, mounted) {
            Text("ВОЗМОЖНОСТИ", color = SpyderGreen.copy(0.5f), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        StaggerCard(2, mounted) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpyderCard)
                    .border(0.5.dp, SpyderBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                capabilities.forEach { text ->
                    Text("✦ $text", color = StardustTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

// =============================================================================
// ТАБ 2: ДЕМО
// =============================================================================

@Composable
private fun SpyderDemoTab(viewModel: SpyderSettingsViewModel) {
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(20); mounted = true }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StaggerCard(0, mounted) {
            Text("LIVE DEMO", color = SpyderGreen.copy(0.5f), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        StaggerCard(1, mounted) {
            SpyderDemoCard("Spring System") {
                SpringBallsDemo(viewModel)
            }
        }
        StaggerCard(2, mounted) {
            SpyderDemoCard("Shader Engine") {
                ShaderWaveDemo()
            }
        }
        StaggerCard(3, mounted) {
            SpyderDemoCard("Border Sweep") {
                BorderSweepDemo()
            }
        }
        StaggerCard(4, mounted) {
            SpyderDemoCard("Performance") {
                PerformanceDemo(viewModel)
            }
        }
    }
}

@Composable
private fun SpyderDemoCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpyderCard)
            .border(0.5.dp, SpyderBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = SpyderGreen.copy(0.7f), fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        content()
    }
}

@Composable
private fun SpringBallsDemo(viewModel: SpyderSettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.performanceProfile
    val springDamping = profile?.springDamping ?: 0.52f
    val springStiffness = profile?.springStiffness ?: 520f

    val springs = listOf(
        "Press" to Triple(springDamping * 1.35f, springStiffness * 1.44f, SpyderGreen),
        "Content" to Triple(0.95f, 180f, Color(0xFF00BFFF)),
        "Bouncy" to Triple(springDamping * 0.86f, springStiffness, Color(0xFFFF6B35))
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        springs.forEach { (label, spec) ->
            val (d, s, color) = spec
            var triggered by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (triggered) 1.4f else 1f,
                animationSpec = spring(d, s),
                label = "ball_$label",
                finishedListener = {
                    triggered = false
                    viewModel.spyderEngine.incrementAnimationCounter()
                }
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { triggered = true }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .background(color.copy(0.15f))
                        .border(1.dp, color.copy(0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                }
                Text(label, color = StardustTextSecondary, fontSize = 9.sp)
                Text("d=${String.format("%.2f", d)}", color = SpyderGreen.copy(0.4f), fontSize = 8.sp)
            }
        }
    }
    Text(
        "Нажмите на шарик",
        color = StardustTextSecondary.copy(0.5f),
        fontSize = 9.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ShaderWaveDemo() {
    val inf = rememberInfiniteTransition(label = "wave")
    val phase by inf.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        val w = size.width
        val h = size.height
        val p1 = Path()
        val p2 = Path()
        for (i in 0..120) {
            val x = w * i / 120f
            val y1 = h * 0.5f + h * 0.25f * sin(phase + x / w * 4 * PI.toFloat())
            val y2 = h * 0.5f + h * 0.18f * sin(phase * 1.3f + x / w * 6 * PI.toFloat())
            if (i == 0) {
                p1.moveTo(x, y1)
                p2.moveTo(x, y2)
            } else {
                p1.lineTo(x, y1)
                p2.lineTo(x, y2)
            }
        }
        drawPath(p1, SpyderGreen.copy(0.7f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(p2, Color(0xFF00BFFF).copy(0.4f), style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun BorderSweepDemo() {
    val inf = rememberInfiniteTransition(label = "sweep")
    val phase by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SpyderBg)
            .drawWithContent {
                drawContent()
                val w = size.width
                val h = size.height
                val per = 2f * (w + h)
                val hot = phase * per
                val hw = per * 0.25f

                fun alphaAt(ps: Float, p: Float): Float {
                    val gp = ps + p
                    val d = minOf(abs(gp - hot), abs(gp - hot + per), abs(gp - hot - per))
                    return if (d > hw / 2f) 0.08f else (1f - d / (hw / 2f)).coerceIn(0f, 1f) * 0.95f
                }

                val sw = 1.5f
                val hf = sw / 2f
                drawRoundRect(
                    SpyderBorder,
                    style = Stroke(0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )

                fun drawSide(x0: Float, y0: Float, x1: Float, y1: Float, ps: Float, l: Float) {
                    val a0 = alphaAt(ps, 0f)
                    val a1 = alphaAt(ps, l)
                    if (maxOf(a0, a1) <= 0.09f) return
                    drawLine(
                        Brush.linearGradient(
                            listOf(SpyderGreen.copy(a0), SpyderGreen.copy(maxOf(a0, a1)), SpyderGreen.copy(a1)),
                            Offset(x0, y0), Offset(x1, y1)
                        ),
                        Offset(x0, y0), Offset(x1, y1), sw + 2f * maxOf(a0, a1)
                    )
                }

                drawSide(hf, hf, w - hf, hf, 0f, w)
                drawSide(w - hf, hf, w - hf, h - hf, w, h)
                drawSide(w - hf, h - hf, hf, h - hf, w + h, w)
                drawSide(hf, h - hf, hf, hf, 2f * w + h, h)
            },
        contentAlignment = Alignment.Center
    ) {
        Text("Animated Border Sweep", color = SpyderGreen.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PerformanceDemo(viewModel: SpyderSettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.performanceProfile
    val targetFps = profile?.targetFps ?: 60
    val currentFps = uiState.currentFps
    val fpsColor = when {
        currentFps >= targetFps - 5 -> SpyderGreen
        currentFps >= targetFps - 15 -> Color.Yellow
        else -> Color.Red
    }

    val metrics = listOf(
        "Target FPS" to "$targetFps",
        "Current FPS" to String.format("%.1f", currentFps),
        "Frame Time" to String.format("%.1f", uiState.currentFrameTime) + " ms",
        "Spring calc" to "< 1ms",
        "Shader pass" to "GPU",
        "Canvas ops" to "Hardware"
    )

    val inf = rememberInfiniteTransition(label = "perf")
    val sl by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "slider"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            metrics.forEach { (k, v) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(k, color = StardustTextSecondary, fontSize = 11.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (k == "Current FPS") fpsColor else SpyderGreen)
                        )
                        Text(
                            v,
                            color = if (k == "Current FPS") fpsColor else SpyderGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .offset(y = (sl * 80).dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, SpyderGreen.copy(0.3f), SpyderGreen.copy(0.6f), SpyderGreen.copy(0.3f), Color.Transparent)
                    )
                )
        )
    }
}

// =============================================================================
// ТАБ 3: НАСТРОЙКИ
// =============================================================================

@Composable
private fun SpyderSettingsTab(
    viewModel: SpyderSettingsViewModel,
    isAutoUpdateEnabled: Boolean,
    currentAnimation: DialogAnimation,
    onPreviewRequest: (DialogAnimation) -> Unit = {}
) {
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(20); mounted = true }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StaggerCard(0, mounted) {
            Text("ПАРАМЕТРЫ", color = SpyderGreen.copy(0.5f), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        StaggerCard(1, mounted) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpyderCard)
                    .border(0.5.dp, SpyderBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleAutoUpdate() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(SpyderGreenDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_spider_logo),
                            contentDescription = null,
                            tint = SpyderGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Авто-обновление", color = StardustTextPrimary, fontSize = 14.sp)
                        Text(
                            if (isAutoUpdateEnabled) "Без подтверждения" else "С подтверждением",
                            color = StardustTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isAutoUpdateEnabled,
                        onCheckedChange = { viewModel.toggleAutoUpdate() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SpyderBg,
                            checkedTrackColor = SpyderGreen,
                            uncheckedThumbColor = StardustTextSecondary,
                            uncheckedTrackColor = StardustItemBg
                        )
                    )
                }
            }
        }

        StaggerCard(2, mounted) {
            Text("АНИМАЦИЯ ОКОН", color = SpyderGreen.copy(0.5f), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        StaggerCard(3, mounted) {
            AnimationPickerGrid(
                currentAnimation = currentAnimation,
                onAnimationSelected = { anim ->
                    viewModel.setDialogAnimation(anim)
                    onPreviewRequest(anim)
                }
            )
        }

        StaggerCard(4, mounted) {
            Text("ПРОФИЛЬ ПРОИЗВОДИТЕЛЬНОСТИ", color = SpyderGreen.copy(0.5f), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        StaggerCard(5, mounted) {
            PerformanceProfileGrid(viewModel)
        }

        StaggerCard(6, mounted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpyderGreenDark.copy(0.5f))
                    .border(0.5.dp, SpyderGreen.copy(0.2f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text("Spyder3000 v$SPYDER_VERSION · $SPYDER_BUILD",
                    color = SpyderGreen.copy(0.65f), fontSize = 11.sp)
            }
        }
    }
}

// =============================================================================
// ПРОФИЛИ ПРОИЗВОДИТЕЛЬНОСТИ
// =============================================================================

@Composable
private fun PerformanceProfileGrid(viewModel: SpyderSettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentProfile = uiState.performanceProfile

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpyderCard)
            .border(0.5.dp, SpyderBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        viewModel.spyderEngine.availableProfiles.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { profile ->
                    ProfileCard(
                        profile = profile,
                        isSelected = currentProfile?.name == profile.name,
                        onClick = { viewModel.setPerformanceProfile(profile) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: SpyderPerformanceProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) SpyderGreenDark else Color(0xFF111111))
            .border(0.5.dp, if (isSelected) SpyderGreen else SpyderBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = profile.name,
            color = if (isSelected) SpyderGreen else StardustTextPrimary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            text = "${profile.targetFps} FPS",
            color = SpyderGreen.copy(0.6f),
            fontSize = 9.sp
        )
        Text(
            text = profile.description,
            color = StardustTextSecondary.copy(0.5f),
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// =============================================================================
// ТАБ 4: АНАЛИЗАТОР ПРОИЗВОДИТЕЛЬНОСТИ
// =============================================================================

@Composable
private fun SpyderPerformanceAnalyzerTab(viewModel: SpyderSettingsViewModel) {
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(20); mounted = true }
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.performanceProfile

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StaggerCard(0, mounted) {
            Text("АНАЛИЗАТОР ПРОИЗВОДИТЕЛЬНОСТИ", color = SpyderGreen.copy(0.5f), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }

        StaggerCard(1, mounted) {
            LiveFpsAnalyzer(viewModel, profile)
        }

        StaggerCard(2, mounted) {
            ThermalAnalyzerCard(uiState)
        }

        StaggerCard(3, mounted) {
            AnimationStatisticsCard(viewModel, uiState)
        }

        StaggerCard(4, mounted) {
            RecentAnimationsCard(uiState.recentAnimations)
        }

        StaggerCard(5, mounted) {
            Button(
                onClick = { viewModel.resetStatistics() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpyderGreenDark,
                    contentColor = SpyderGreen
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("СБРОСИТЬ СТАТИСТИКУ", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ThermalAnalyzerCard(uiState: SpyderUiState) {
    val temp = uiState.batteryTemp
    val thermalStatus = uiState.thermalStatus

    val tempColor = when {
        temp < 35f -> SpyderGreen
        temp < 45f -> Color(0xFFFFFF00)
        temp < 55f -> Color(0xFFFF8C00)
        else       -> Color(0xFFFF2222)
    }
    val tempLabel = when {
        temp < 35f -> "Холодно"
        temp < 45f -> "Норма"
        temp < 55f -> "Тепло"
        else       -> "Горячо ⚠"
    }
    val thermalLabel = when (thermalStatus) {
        0 -> "Норма"
        1 -> "Умеренный"
        2 -> "Средний"
        3 -> "Серьёзный"
        4 -> "Критический"
        5 -> "Аварийный"
        else -> "—"
    }

    val tempHistory = remember { mutableStateListOf<Float>() }
    LaunchedEffect(temp) {
        if (temp > 0f) {
            tempHistory.add(temp)
            if (tempHistory.size > 30) tempHistory.removeAt(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpyderCard)
            .border(0.5.dp, SpyderBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌡 ТЕМПЕРАТУРА", color = SpyderGreen.copy(0.5f),
                fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(thermalLabel, color = tempColor, fontSize = 10.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (temp > 0f) String.format("%.1f°C", temp) else "—",
                color = tempColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(tempLabel, color = tempColor.copy(0.7f), fontSize = 12.sp)
        }

        val tempProgress = (temp / 80f).coerceIn(0f, 1f)
        val animatedTempProgress by animateFloatAsState(
            targetValue = tempProgress,
            animationSpec = tween(700),
            label = "temp_bar"
        )
        LinearProgressIndicator(
            progress = { animatedTempProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = tempColor,
            trackColor = SpyderBorder
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(SpyderBg, RoundedCornerShape(6.dp))
        ) {
            if (tempHistory.size < 2) return@Canvas
            val step = size.width / 30f
            val path = Path()
            tempHistory.forEachIndexed { idx, t ->
                val x = idx * step
                val y = size.height - (t / 80f * size.height).coerceIn(0f, size.height)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, tempColor.copy(0.7f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

            val warnY = size.height - (45f / 80f * size.height)
            drawLine(
                color = Color(0xFFFF8C00).copy(0.3f),
                start = Offset(0f, warnY),
                end   = Offset(size.width, warnY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )
        }

        if (temp > 55f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF2222).copy(0.1f))
                    .border(0.5.dp, Color(0xFFFF2222).copy(0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    "⚠ Режим охлаждения — шейдеры снижены автоматически",
                    color = Color(0xFFFF2222).copy(0.8f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun LiveFpsAnalyzer(
    viewModel: SpyderSettingsViewModel,
    profile: SpyderPerformanceProfile?
) {
    val uiState by viewModel.uiState.collectAsState()
    val targetFps = profile?.targetFps ?: 60
    val currentFps = uiState.currentFps
    val frameTime = uiState.currentFrameTime
    val targetFrameTime = 1000f / targetFps

    val performanceStatus = when {
        currentFps >= targetFps - 2 -> "Отлично"
        currentFps >= targetFps - 10 -> "Хорошо"
        currentFps >= targetFps - 20 -> "Средне"
        else -> "Низкая производительность"
    }

    val statusColor = when {
        currentFps >= targetFps - 2 -> SpyderGreen
        currentFps >= targetFps - 10 -> Color(0xFF00FF88)
        currentFps >= targetFps - 20 -> Color.Yellow
        else -> Color.Red
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpyderCard)
            .border(0.5.dp, SpyderBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Живой FPS", color = SpyderGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(performanceStatus, color = statusColor, fontSize = 11.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (currentFps > 0f) String.format("%.1f", currentFps) else "—",
                color = statusColor,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(" fps", color = StardustTextSecondary, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        val fpsProgress = (currentFps / targetFps).coerceIn(0f, 1f)
        val animatedFpsProgress by animateFloatAsState(
            targetValue = fpsProgress,
            animationSpec = tween(500),
            label = "fps_bar"
        )
        LinearProgressIndicator(
            progress = { animatedFpsProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = statusColor,
            trackColor = SpyderBorder
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricItem("Цель", "$targetFps fps", SpyderGreen.copy(0.6f))
            MetricItem("Frame Time", String.format("%.1f", frameTime) + " ms",
                if (frameTime <= targetFrameTime * 1.1f) SpyderGreen.copy(0.6f) else Color.Yellow)
            MetricItem("Дропнуто", "${uiState.droppedFrames}", StardustTextSecondary)
        }

        FrameTimeHistoryGraph(frameTime = frameTime)
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = StardustTextSecondary, fontSize = 10.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FrameTimeHistoryGraph(frameTime: Float) {
    val history = remember { mutableStateListOf<Float>() }
    val frameTimeState = rememberUpdatedState(frameTime)

    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            val current = frameTimeState.value
            if (current > 0f) {
                history.add(current)
                if (history.size > 30) history.removeAt(0)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(SpyderBg, RoundedCornerShape(6.dp))
    ) {
        if (history.isEmpty()) return@Canvas

        val step = size.width / 30f
        val maxFrameTime = 50f
        val path = Path()

        history.forEachIndexed { index, value ->
            val x = index * step
            val y = size.height - (value / maxFrameTime * size.height).coerceIn(0f, size.height)

            if (index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = SpyderGreen.copy(0.7f),
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
        )

        val targetY = size.height - (16.6f / maxFrameTime * size.height)
        drawLine(
            color = SpyderGreen.copy(0.3f),
            start = Offset(0f, targetY),
            end = Offset(size.width, targetY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
        )
    }
}

@Composable
private fun AnimationStatisticsCard(viewModel: SpyderSettingsViewModel, uiState: SpyderUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpyderCard)
            .border(0.5.dp, SpyderBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("СТАТИСТИКА АНИМАЦИЙ", color = SpyderGreen.copy(0.5f), fontSize = 9.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCircle(
                value = uiState.totalAnimations.toString(),
                label = "Всего анимаций",
                color = SpyderGreen
            )
            StatCircle(
                value = String.format("%.0f", uiState.currentFps),
                label = "Текущий FPS",
                color = if (uiState.currentFps >= 55) SpyderGreen else Color.Yellow
            )
            StatCircle(
                value = uiState.recentAnimations.size.toString(),
                label = "Записано в лог",
                color = StardustTextSecondary
            )
        }
    }
}

@Composable
private fun StatCircle(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(0.1f))
                .border(1.dp, color.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = StardustTextSecondary, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RecentAnimationsCard(animations: List<SpyderAnimationLog>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpyderCard)
            .border(0.5.dp, SpyderBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("ПОСЛЕДНИЕ АНИМАЦИИ", color = SpyderGreen.copy(0.5f), fontSize = 9.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

        if (animations.isEmpty()) {
            Text("Нет записей", color = StardustTextSecondary.copy(0.5f), fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 16.dp))
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                animations.take(8).forEach { anim ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = anim.animationType.take(25),
                            color = StardustTextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = anim.fpsAtMoment.toInt().toString() + " fps",
                            color = if (anim.fpsAtMoment >= 55) SpyderGreen else Color.Yellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// GRID ВЫБОРА АНИМАЦИИ
// =============================================================================

@Composable
private fun AnimationPickerGrid(
    currentAnimation: DialogAnimation,
    onAnimationSelected: (DialogAnimation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpyderCard)
            .border(0.5.dp, SpyderBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DialogAnimation.entries.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { anim ->
                    AnimationCard(
                        animation = anim,
                        isSelected = anim == currentAnimation,
                        onClick = { onAnimationSelected(anim) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AnimationCard(
    animation: DialogAnimation,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previewVisible by remember { mutableStateOf(true) }
    val density = LocalDensity.current

    LaunchedEffect(isSelected) {
        if (isSelected) {
            previewVisible = false
            delay(80)
            previewVisible = true
        }
    }

    val previewProgress by animateFloatAsState(
        targetValue = if (previewVisible) 1f else 0f,
        animationSpec = when (animation) {
            DialogAnimation.SCALE_BOUNCY -> spring(0.52f, 520f)
            DialogAnimation.SLIDE_BOTTOM -> spring(0.60f, 380f)
            DialogAnimation.FALL_TOP -> spring(0.60f, 380f)
            DialogAnimation.FADE -> tween(280, easing = IOSEasing)
            DialogAnimation.FLIP -> spring(0.75f, 380f)
            DialogAnimation.ZOOM_SOFT -> spring(0.90f, 420f)
        },
        label = "preview_${animation.name}"
    )

    Column(
        modifier = modifier.clickable {
            onClick()
            previewVisible = false
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) SpyderGreenDark else Color(0xFF111111))
                .border(
                    if (isSelected) 1.dp else 0.5.dp,
                    if (isSelected) SpyderGreen.copy(0.7f) else SpyderBorder,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp, 20.dp)
                    .graphicsLayer {
                        when (animation) {
                            DialogAnimation.SCALE_BOUNCY -> {
                                scaleX = 0.7f + previewProgress * 0.3f
                                scaleY = 0.7f + previewProgress * 0.3f
                                alpha = previewProgress
                            }
                            DialogAnimation.SLIDE_BOTTOM -> {
                                translationY = (1f - previewProgress) * 16f
                                alpha = previewProgress
                            }
                            DialogAnimation.FALL_TOP -> {
                                translationY = -(1f - previewProgress) * 16f
                                alpha = previewProgress
                            }
                            DialogAnimation.FADE -> {
                                alpha = previewProgress
                            }
                            DialogAnimation.FLIP -> {
                                rotationX = (1f - previewProgress) * 60f
                                alpha = previewProgress
                                cameraDistance = 12f * density.density
                            }
                            DialogAnimation.ZOOM_SOFT -> {
                                scaleX = 0.82f + previewProgress * 0.18f
                                scaleY = 0.82f + previewProgress * 0.18f
                                alpha = previewProgress
                            }
                        }
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) SpyderGreen.copy(0.5f) else Color.White.copy(0.15f))
            )

            Text(
                text = animation.emoji,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
            )
        }

        Text(
            text = animation.label,
            color = if (isSelected) SpyderGreen else StardustTextSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
        Text(
            text = animation.description,
            color = StardustTextSecondary.copy(0.6f),
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp
        )
    }
}

// =============================================================================
// STAGGER HELPER
// =============================================================================

@Composable
private fun StaggerCard(index: Int, visible: Boolean, content: @Composable () -> Unit) {
    var itemVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(index * 60L)
            itemVisible = true
        } else {
            itemVisible = false
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (itemVisible) 1f else 0f,
        animationSpec = SP_Content,
        label = "stagger_$index"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 16f
            }
    ) {
        content()
    }
}