    package com.example.qrscannerapp.common.ui

    import android.graphics.BlurMaskFilter
    import androidx.compose.animation.*
    import androidx.compose.animation.core.*
    import androidx.compose.foundation.*
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.automirrored.filled.ContactSupport
    import androidx.compose.material.icons.filled.Close
    import androidx.compose.material.icons.filled.Lock
    import androidx.compose.material.icons.filled.PlayArrow
    import androidx.compose.material.icons.filled.SystemUpdate
    import androidx.compose.material.icons.outlined.*
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.alpha
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.draw.drawBehind
    import androidx.compose.ui.draw.scale
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.*
    import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
    import androidx.compose.ui.graphics.vector.ImageVector
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.font.FontFamily
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.googlefonts.Font
    import androidx.compose.ui.text.googlefonts.GoogleFont
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import com.example.qrscannerapp.R
    import com.example.qrscannerapp.ShiftRequestStatus
    import com.example.qrscannerapp.StardustError
    import com.example.qrscannerapp.StardustItemBg
    import com.example.qrscannerapp.StardustPrimary
    import com.example.qrscannerapp.StardustSuccess
    import com.example.qrscannerapp.StardustTextPrimary
    import com.example.qrscannerapp.StardustTextSecondary
    import com.example.qrscannerapp.StardustWarning
    import com.example.qrscannerapp.UpdateState
    import kotlinx.coroutines.delay
    import java.text.SimpleDateFormat
    import java.util.*

    // --- ШРИФТ ---
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    val manropeFontName = GoogleFont("Manrope")

    val manrope = FontFamily(
        Font(googleFont = manropeFontName, fontProvider = provider, weight = FontWeight.Normal),
        Font(googleFont = manropeFontName, fontProvider = provider, weight = FontWeight.Medium),
        Font(googleFont = manropeFontName, fontProvider = provider, weight = FontWeight.SemiBold),
        Font(googleFont = manropeFontName, fontProvider = provider, weight = FontWeight.Bold)
    )

    // --- ЛОГИКА ВРЕМЕНИ СУТОК ---
    enum class TimeOfDay { MORNING, DAY, EVENING, NIGHT }

    data class TimeTheme(
        val greeting: String,
        val icon: ImageVector,
        val accentColor: Color,
        val accentGlow: Color
    )

    @Composable
    fun ShiftLockScreen(
        userName: String = "Самир Саидов",
        userRole: String = "Сотрудник",
        isAllowedToWork: Boolean = false,
        shiftRequestStatus: ShiftRequestStatus = ShiftRequestStatus.NONE,
        isLoading: Boolean = false,
        onStartShift: () -> Unit = {},
        onRequestAccess: () -> Unit = {},
        onLogout: () -> Unit = {},
        pingMs: Int = 42,
        isOnline: Boolean = true,
        appVersion: String = "1.0.5",
        // НОВЫЕ ПАРАМЕТРЫ для обновления
        updateState: UpdateState = UpdateState.Idle,
        onStartUpdate: () -> Unit = {},
        onDismissUpdate: () -> Unit = {}
    ) {
        var currentTimeString by remember { mutableStateOf("") }
        var currentHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }

        LaunchedEffect(Unit) {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            while (true) {
                val calendar = Calendar.getInstance()
                currentTimeString = formatter.format(calendar.time)
                currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                delay(1000)
            }
        }

        val timeTheme = remember(currentHour) {
            when (currentHour) {
                in 6..11 -> TimeTheme(greeting = "Доброе утро", icon = Icons.Outlined.WbTwilight, accentColor = Color(0xFFFFA726), accentGlow = Color(0xFFFF6D00))
                in 12..17 -> TimeTheme(greeting = "Добрый день", icon = Icons.Outlined.WbSunny, accentColor = Color(0xFF29B6F6), accentGlow = Color(0xFF0091EA))
                in 18..21 -> TimeTheme(greeting = "Добрый вечер", icon = Icons.Outlined.Nightlight, accentColor = Color(0xFFEC407A), accentGlow = Color(0xFFD81B60))
                else -> TimeTheme(greeting = "Доброй ночи", icon = Icons.Outlined.ModeNight, accentColor = Color(0xFF7E57C2), accentGlow = Color(0xFF5E35B1))
            }
        }

        val animatedAccentColor by animateColorAsState(targetValue = timeTheme.accentColor, tween(1500), label = "accentColor")
        val animatedGlowColor by animateColorAsState(targetValue = timeTheme.accentGlow, tween(1500), label = "glowColor")

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.bg_lockscreen),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GlassLockCard(
                    userName = userName,
                    greeting = timeTheme.greeting,
                    currentTime = currentTimeString,
                    shiftRequestStatus = shiftRequestStatus,
                    isAllowedToWork = isAllowedToWork,
                    isLoading = isLoading,
                    accentColor = animatedAccentColor,
                    glowColor = animatedGlowColor,
                    onStartShift = onStartShift,
                    onRequestAccess = onRequestAccess,
                    onLogout = onLogout
                )
            }

            // НОВОЕ: Баннер обновления — поверх всего, внизу экрана
            AnimatedVisibility(
                visible = updateState is UpdateState.UpdateAvailable || updateState is UpdateState.Downloading,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                UpdateBanner(
                    updateState = updateState,
                    onStartUpdate = onStartUpdate,
                    onDismiss = onDismissUpdate
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = if (updateState is UpdateState.UpdateAvailable ||
                            updateState is UpdateState.Downloading) 100.dp else 16.dp
                    )
            ) {
                SystemStatusBar(pingMs, isOnline, appVersion)
            }
        }
    }

    // ============================================================================================
    // НОВЫЙ КОМПОНЕНТ: Баннер обновления
    // ============================================================================================

    @Composable
    fun UpdateBanner(
        updateState: UpdateState,
        onStartUpdate: () -> Unit,
        onDismiss: () -> Unit
    ) {
        val isDownloading = updateState is UpdateState.Downloading
        val progress = (updateState as? UpdateState.Downloading)?.progress ?: 0
        val versionName = (updateState as? UpdateState.UpdateAvailable)?.info?.latestVersionName ?: ""
        val apkSize = (updateState as? UpdateState.UpdateAvailable)?.info?.apkSize

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.75f)
            ),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        StardustPrimary.copy(alpha = 0.8f),
                        StardustPrimary.copy(alpha = 0.2f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Иконка
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StardustPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = StardustPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Текст
                    Column(modifier = Modifier.weight(1f)) {
                        if (isDownloading) {
                            Text(
                                "Загрузка обновления...",
                                color = StardustTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = manrope
                            )
                            Text(
                                "$progress%",
                                color = StardustPrimary,
                                fontSize = 12.sp,
                                fontFamily = manrope
                            )
                        } else {
                            Text(
                                "Доступно обновление $versionName",
                                color = StardustTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = manrope
                            )
                            Text(
                                apkSize?.let { "Размер: $it" } ?: "Нажмите чтобы установить",
                                color = StardustTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = manrope
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка действия
                    if (isDownloading) {
                        CircularProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.size(32.dp),
                            color = StardustPrimary,
                            strokeWidth = 3.dp,
                            trackColor = StardustItemBg
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Кнопка установить
                            Button(
                                onClick = onStartUpdate,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    "Обновить",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = manrope
                                )
                            }
                            // Кнопка закрыть
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = StardustTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Прогресс-бар при загрузке
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = StardustPrimary,
                        trackColor = StardustItemBg
                    )
                }
            }
        }
    }

    // ============================================================================================
    // Остальные компоненты — без изменений
    // ============================================================================================

    @Composable
    fun GlassLockCard(
        userName: String,
        greeting: String,
        currentTime: String,
        shiftRequestStatus: ShiftRequestStatus,
        isAllowedToWork: Boolean,
        isLoading: Boolean,
        accentColor: Color,
        glowColor: Color,
        onStartShift: () -> Unit,
        onRequestAccess: () -> Unit,
        onLogout: () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "borderPulse")
        val borderAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
            label = "borderAlpha"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.35f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = borderAlpha),
                            Color.White.copy(alpha = 0.1f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 400f)
                    ),
                    RoundedCornerShape(32.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$greeting, $userName",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontFamily = manrope
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = manrope,
                modifier = Modifier.glowEffect(glowColor)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val statusText = when {
                shiftRequestStatus == ShiftRequestStatus.PENDING -> "Ожидание доступа..."
                isAllowedToWork -> "Готов к смене"
                else -> "Доступ закрыт"
            }
            Text(
                text = statusText,
                color = if (shiftRequestStatus == ShiftRequestStatus.PENDING) accentColor
                else Color.White.copy(alpha = 0.5f),
                fontFamily = manrope
            )

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = Triple(isAllowedToWork, shiftRequestStatus, isLoading),
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                }, label = "GlassButtonAnimation"
            ) { (allowed, status, loading) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        allowed -> PremiumStartButton(onClick = onStartShift, accentColor = accentColor)
                        status == ShiftRequestStatus.PENDING -> PremiumPendingState(accentColor = accentColor)
                        else -> PremiumRequestButton(onClick = onRequestAccess, enabled = !loading)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onLogout) {
                Text(
                    text = "Выйти",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontFamily = manrope
                )
            }
        }
    }

    fun Modifier.glowEffect(glowColor: Color) = this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            frameworkPaint.color = glowColor.toArgb()
            frameworkPaint.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
            canvas.nativeCanvas.drawText("00:00", 0f, size.height / 1.5f, frameworkPaint)
        }
    }

    @Composable
    fun PremiumStartButton(onClick: () -> Unit, accentColor: Color) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("НАЧАТЬ СМЕНУ", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = manrope)
        }
    }

    @Composable
    fun PremiumRequestButton(onClick: () -> Unit, enabled: Boolean) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Icon(Icons.AutoMirrored.Filled.ContactSupport, null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Запросить доступ", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = manrope)
        }
    }

    @Composable
    fun PremiumPendingState(accentColor: Color) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Text("Запрос отправлен...", color = accentColor, fontWeight = FontWeight.Bold, fontFamily = manrope)
        }
    }

    @Composable
    fun SystemStatusBar(pingMs: Int, isOnline: Boolean, appVersion: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.alpha(0.7f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOnline) "Связь: ${pingMs}ms" else "Нет подключения",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = manrope
                )
            }
            Text("•", color = Color.White, fontSize = 12.sp, fontFamily = manrope)
            Text(
                text = "Версия $appVersion",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = manrope
            )
        }
    }