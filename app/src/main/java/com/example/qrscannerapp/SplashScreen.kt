package com.example.qrscannerapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.sin
import kotlin.random.Random

// ── Цвета ────────────────────────────────────────────────────────────────────
val SplashScreenBackgroundColor    = Color(0xFF040408)
val SplashScreenLogoColor          = Color(0xFF8B5CF6)
val SplashScreenTextColor          = Color(0xFFEAEAF0)
val SplashScreenStudioTextColor    = Color(0xFFFFFBEB)
val SplashScreenStudioGlowColor    = Color(0xFFFFC107)
val BeamColor                      = Color(0xFF6A5AE0)
val BeamColorAlt                   = Color(0xFFEC407A)

// ── Шрифт ─────────────────────────────────────────────────────────────────────
private val splashFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)
val InterFont = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = splashFontProvider)
)

// ── Beam Shape ────────────────────────────────────────────────────────────────
private val BeamShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(size.width * 0.38f, 0f)
            lineTo(size.width * 0.62f, 0f)
            lineTo(size.width * 1.1f,  size.height)
            lineTo(size.width * -0.1f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

// ── Particle data ─────────────────────────────────────────────────────────────
private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val speed: Float,       // разная скорость
    val hue: Float,         // цвет частицы (0=фиолетовый, 1=розовый, 2=белый)
    val wobble: Float       // горизонтальное колебание
)

// ── Particles Canvas ──────────────────────────────────────────────────────────
@Composable
private fun EnhancedParticles(modifier: Modifier, beamAlpha: Float) {
    val particles = remember {
        List(80) {
            Particle(
                x       = Random.nextFloat(),
                y       = Random.nextFloat(),
                size    = Random.nextFloat() * 2.5f + 0.5f,
                alpha   = Random.nextFloat() * 0.6f + 0.15f,
                speed   = Random.nextFloat() * 0.4f + 0.15f,
                hue     = Random.nextInt(3).toFloat(),
                wobble  = Random.nextFloat() * 0.04f - 0.02f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "particle_progress"
    )
    val wobblePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "wobble"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val yOffset  = (p.y + progress * p.speed) % 1.0f
            val xOffset  = p.x + sin(wobblePhase + p.x * 10f).toFloat() * p.wobble
            val color = when (p.hue.toInt()) {
                0 -> Color(0xFF8B5CF6) // фиолетовый
                1 -> Color(0xFFEC407A) // розовый
                else -> Color.White
            }
            drawCircle(
                color  = color,
                radius = p.size,
                center = Offset(xOffset * size.width, yOffset * size.height),
                alpha  = p.alpha * beamAlpha
            )
        }
    }
}

// ============================================================================================
// SPLASH SCREEN
// ============================================================================================

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit,
    viewModel: SplashScreenViewModel = hiltViewModel()
) {
    val isLoading     by viewModel.isLoading.collectAsState()
    val loadingStatus by viewModel.loadingStatus.collectAsState()

    // ── Стейты анимаций ────────────────────────────────────────────────
    var startAnimation by remember { mutableStateOf(false) }
    var showContent    by remember { mutableStateOf(false) }

    // Луч — plавное появление
    val beamScaleY by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0f,
        animationSpec = tween(1800, easing = FastOutSlowInEasing),
        label         = "beam_scale"
    )
    val beamAlpha by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0f,
        animationSpec = tween(1200, easing = LinearEasing),
        label         = "beam_alpha"
    )

    // Пульс светового пятна вверху
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val lampGlow by infiniteTransition.animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "lamp_glow"
    )
    val lampWidth by infiniteTransition.animateFloat(
        initialValue  = 200f, targetValue = 280f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "lamp_width"
    )

    // Логотип — spring bounce
    val logoScale by animateFloatAsState(
        targetValue   = if (showContent) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 280f),
        label         = "logo_scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue   = if (showContent) 1f else 0f,
        animationSpec = tween(600),
        label         = "logo_alpha"
    )

    // Свечение вокруг логотипа — пульс
    val logoGlowRadius by infiniteTransition.animateFloat(
        initialValue  = 60f, targetValue = 90f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "logo_glow"
    )
    val logoGlowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.12f, targetValue = 0.28f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "logo_glow_alpha"
    )

    // Reveal логотипа — снизу вверх убираем маску
    val logoReveal by animateFloatAsState(
        targetValue   = if (showContent) 0f else 1f,
        animationSpec = tween(900, delayMillis = 100, easing = FastOutSlowInEasing),
        label         = "logo_reveal"
    )

    // Текст — посимвольный эффект через alpha + смещение
    val textAlpha by animateFloatAsState(
        targetValue   = if (showContent) 1f else 0f,
        animationSpec = tween(800, delayMillis = 400),
        label         = "text_alpha"
    )
    val textOffsetY by animateFloatAsState(
        targetValue   = if (showContent) 0f else 20f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 220f),
        label         = "text_offset"
    )
    val textLetterSpacing by animateFloatAsState(
        targetValue   = if (showContent) 4f else 12f,
        animationSpec = tween(800, delayMillis = 400, easing = FastOutSlowInEasing),
        label         = "letter_spacing"
    )

    // Подзаголовок
    val subtitleAlpha by animateFloatAsState(
        targetValue   = if (showContent) 1f else 0f,
        animationSpec = tween(700, delayMillis = 700),
        label         = "subtitle_alpha"
    )
    val subtitleOffsetY by animateFloatAsState(
        targetValue   = if (showContent) 0f else 10f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label         = "subtitle_offset"
    )

    // Статус загрузки
    val statusAlpha by animateFloatAsState(
        targetValue   = if (showContent) 1f else 0f,
        animationSpec = tween(600, delayMillis = 900),
        label         = "status_alpha"
    )

    // Студия
    val studioAlpha by animateFloatAsState(
        targetValue   = if (showContent) 1f else 0f,
        animationSpec = tween(800, delayMillis = 1100),
        label         = "studio_alpha"
    )

    // ── Эффект разделительной линии под текстом ────────────────────────
    val lineWidth by animateFloatAsState(
        targetValue   = if (showContent) 1f else 0f,
        animationSpec = tween(600, delayMillis = 600, easing = FastOutSlowInEasing),
        label         = "line_width"
    )

    // ── Запуск анимаций ────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        startAnimation = true
        kotlinx.coroutines.delay(800)
        showContent = true
    }

    LaunchedEffect(isLoading) {
        if (!isLoading) onAnimationFinished()
    }

    // ── UI ─────────────────────────────────────────────────────────────
    Box(
        modifier         = Modifier.fillMaxSize().background(SplashScreenBackgroundColor),
        contentAlignment = Alignment.Center
    ) {

        // ── 1. Луч света — трапеция сверху ──────────────────────────
        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Основной луч — двухцветный градиент
            Box(
                modifier = Modifier
                    .width(900.dp)
                    .fillMaxHeight(0.65f)
                    .graphicsLayer {
                        scaleY           = beamScaleY
                        transformOrigin  = TransformOrigin(0.5f, 0f)
                        clip             = true
                        shape            = BeamShape
                        this.alpha       = beamAlpha * 0.5f
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BeamColor.copy(alpha = 0.25f),
                                BeamColorAlt.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Яркое ядро луча — узкое и интенсивное
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(0.50f)
                    .graphicsLayer {
                        scaleY          = beamScaleY
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        clip            = true
                        shape           = BeamShape
                        this.alpha      = beamAlpha * 0.7f
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                BeamColor.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Частицы в луче
            Box(
                modifier = Modifier
                    .width(900.dp)
                    .fillMaxHeight(0.65f)
                    .graphicsLayer {
                        scaleY          = beamScaleY
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        clip            = true
                        shape           = BeamShape
                    }
            ) {
                EnhancedParticles(modifier = Modifier.fillMaxSize(), beamAlpha = beamAlpha)
            }

            // ── Световое пятно-лампа сверху ─────────────────────────
            val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

            // Внешнее мягкое свечение
            Box(
                modifier = Modifier
                    .padding(top = screenHeightDp * 0.13f)
                    .width(lampWidth.dp * 2)
                    .height(24.dp)
                    .alpha(beamAlpha * lampGlow * 0.4f)
                    .blur(16.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, BeamColor.copy(alpha = 0.8f), BeamColorAlt.copy(alpha = 0.5f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            // Основная линия-лампа
            Box(
                modifier = Modifier
                    .padding(top = screenHeightDp * 0.135f)
                    .width(lampWidth.dp)
                    .height(3.dp)
                    .alpha(beamAlpha * lampGlow)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BeamColor,
                                Color.White,
                                BeamColorAlt,
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        // ── 2. Свечение вокруг логотипа ──────────────────────────────
        Canvas(modifier = Modifier.size(200.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BeamColor.copy(alpha = logoGlowAlpha),
                        BeamColorAlt.copy(alpha = logoGlowAlpha * 0.4f),
                        Color.Transparent
                    ),
                    radius = logoGlowRadius
                ),
                radius = logoGlowRadius,
                alpha  = logoAlpha
            )
        }

        // ── 3. Логотип + текст ────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Логотип
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter      = painterResource(id = R.drawable.ic_qr_logo),
                    contentDescription = "Logo",
                    modifier     = Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            scaleX               = logoScale
                            scaleY               = logoScale
                            alpha                = logoAlpha
                            compositingStrategy  = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            // Маска reveal снизу вверх
                            if (logoReveal > 0f) {
                                drawRect(
                                    brush     = Brush.verticalGradient(
                                        colors  = listOf(Color.Transparent, Color.Black),
                                        startY  = size.height * (1f - logoReveal),
                                        endY    = size.height
                                    ),
                                    blendMode = BlendMode.DstOut
                                )
                            }
                        },
                    colorFilter  = ColorFilter.tint(SplashScreenLogoColor)
                )
            }

            Spacer(Modifier.height(28.dp))

            // Название приложения
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha        = textAlpha
                    translationY = textOffsetY
                }
            ) {
                Text(
                    text          = "STARDUST",
                    fontFamily    = InterFont,
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = 32.sp,
                    color         = SplashScreenTextColor,
                    letterSpacing = textLetterSpacing.sp,
                    style         = TextStyle(
                        shadow = Shadow(
                            color      = SplashScreenLogoColor.copy(alpha = 0.6f),
                            blurRadius = 20f
                        )
                    )
                )
            }

            Spacer(Modifier.height(6.dp))

            // Разделитель под названием
            Box(
                modifier = Modifier
                    .width((160f * lineWidth).dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, BeamColor.copy(alpha = 0.8f), BeamColorAlt.copy(alpha = 0.5f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(1.dp)
                    )
            )

            Spacer(Modifier.height(8.dp))

            // Подзаголовок
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha        = subtitleAlpha
                    translationY = subtitleOffsetY
                }
            ) {
                Text(
                    text          = "FIELD OPERATIONS",
                    fontFamily    = InterFont,
                    fontWeight    = FontWeight.Medium,
                    fontSize      = 11.sp,
                    color         = BeamColor.copy(alpha = 0.85f),
                    letterSpacing = 3.5.sp
                )
            }
        }

        // ── 4. Статус загрузки ────────────────────────────────────────
        Column(
            modifier             = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            horizontalAlignment  = Alignment.CenterHorizontally,
            verticalArrangement  = Arrangement.spacedBy(8.dp)
        ) {
            // Индикатор загрузки — три точки
            LoadingDots(alpha = statusAlpha)

            Text(
                text       = loadingStatus,
                modifier   = Modifier.alpha(statusAlpha),
                fontFamily = InterFont,
                fontSize   = 12.sp,
                color      = SplashScreenTextColor.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )
        }

        // ── 5. Студия ─────────────────────────────────────────────────
        Text(
            text          = "A LUCIUS STUDIO PROJECT",
            modifier      = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .alpha(studioAlpha),
            fontFamily    = InterFont,
            fontWeight    = FontWeight.Medium,
            fontSize      = 10.sp,
            color         = SplashScreenStudioTextColor.copy(alpha = 0.7f),
            letterSpacing = 2.5.sp,
            textAlign     = TextAlign.Center,
            style         = TextStyle(
                shadow = Shadow(
                    color      = SplashScreenStudioGlowColor.copy(alpha = 0.5f),
                    blurRadius = 10f
                )
            )
        )
    }
}

// ============================================================================================
// LOADING DOTS — три пульсирующие точки
// ============================================================================================

@Composable
private fun LoadingDots(alpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dot1Scale by infiniteTransition.animateFloat(
        initialValue  = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "d1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue  = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "d2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue  = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "d3"
    )

    Row(
        modifier              = Modifier.alpha(alpha),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        listOf(dot1Scale, dot2Scale, dot3Scale).forEach { scale ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(BeamColor.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
            )
        }
    }
}