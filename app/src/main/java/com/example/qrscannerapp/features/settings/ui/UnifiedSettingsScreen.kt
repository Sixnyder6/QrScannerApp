package com.example.qrscannerapp.features.settings.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.qrscannerapp.common.ui.AnimatedDialogWrapper
import com.example.qrscannerapp.common.ui.ScreenShell
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================================
// MAIN SCREEN
// ============================================================================================

@Composable
fun UnifiedSettingsScreen(authManager: AuthManager) {
    val authState      by authManager.authState.collectAsState()
    val context        = LocalContext.current
    val scope          = rememberCoroutineScope()
    val telemetryManager = remember { TelemetryManager(context) }
    val updateManager: UpdateManager = hiltViewModel()
    val settingsManager  = remember { SettingsManager(context) }

    val isSoundEnabled     by settingsManager.isSoundEnabledFlow.collectAsState(initial = true)
    val isVibrationEnabled by settingsManager.isVibrationEnabledFlow.collectAsState(initial = true)
    val currentTheme       by settingsManager.appThemeFlow.collectAsState(initial = AppTheme.ENGINE)

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotificationPermission = granted
        if (!granted) Toast.makeText(context, "Уведомления необходимы для статуса загрузки.", Toast.LENGTH_LONG).show()
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!hasLocationPermission) Toast.makeText(context, "Геолокация необходима для задач.", Toast.LENGTH_LONG).show()
    }

    ScreenShell {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Заголовок ──────────────────────────────────────────────
            item {
                Text(
                    "Настройки",
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                )
            }

            // ── Тема оформления ─────────────────────────────────────────
            item {
                SettingsWidgetCard(
                    icon       = Icons.Default.Palette,
                    title      = "Тема оформления",
                    subtitle   = currentTheme.backgroundName,
                    accentColor = StardustPrimary,
                    previewContent = {
                        Spacer(Modifier.height(10.dp))
                        ThemePreviewStrip(currentTheme = currentTheme)
                    }
                ) {
                    ThemePickerExpanded(
                        currentTheme    = currentTheme,
                        onThemeSelected = { scope.launch { settingsManager.setAppTheme(it) } }
                    )
                }
            }

            // ── Движок (Spyder3000) — особый виджет ─────────────────────
            item {
                SpyderEngineWidget()
            }

            // ── Эффекты при сканировании ────────────────────────────────
            if (authState.role != UserRole.ELECTRICIAN) {
                item {
                    SettingsWidgetCard(
                        icon        = Icons.Default.Tune,
                        title       = "Эффекты сканирования",
                        subtitle    = buildString {
                            if (isSoundEnabled) append("Звук")
                            if (isSoundEnabled && isVibrationEnabled) append(" · ")
                            if (isVibrationEnabled) append("Вибрация")
                            if (!isSoundEnabled && !isVibrationEnabled) append("Все выключены")
                        },
                        accentColor = Color(0xFFF59E0B)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ExpandedToggleRow(
                                icon     = Icons.Default.VolumeUp,
                                label    = "Звуковой сигнал",
                                checked  = isSoundEnabled,
                                color    = Color(0xFFF59E0B),
                                onChange = { scope.launch { settingsManager.setSoundEnabled(it) } }
                            )
                            HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f))
                            ExpandedToggleRow(
                                icon     = Icons.Default.Vibration,
                                label    = "Вибрация",
                                checked  = isVibrationEnabled,
                                color    = Color(0xFF94A3B8),
                                onChange = { scope.launch { settingsManager.setVibrationEnabled(it) } }
                            )
                        }
                    }
                }
            }

            // ── Разрешения ──────────────────────────────────────────────
            item {
                SettingsWidgetCard(
                    icon        = Icons.Default.Security,
                    title       = "Разрешения",
                    subtitle    = if (hasLocationPermission) "Геолокация разрешена" else "Геолокация не разрешена",
                    accentColor = Color(0xFF4ADE80)
                ) {
                    ExpandedToggleRow(
                        icon    = Icons.Default.LocationOn,
                        label   = "Геолокация",
                        sublabel = if (hasLocationPermission) "Разрешено · используется для задач" else "Не разрешено",
                        checked = hasLocationPermission,
                        color   = Color(0xFF4ADE80),
                        onChange = { enabled ->
                            if (enabled) {
                                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            } else {
                                context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") })
                            }
                        }
                    )
                }
            }

            // ── О приложении ────────────────────────────────────────────
            item {
                val updateState by updateManager.updateState.collectAsState()
                val updateSubtitle = when (val s = updateState) {
                    is UpdateState.UpdateAvailable -> "Доступна v${s.info.latestVersionName}!"
                    is UpdateState.Downloading     -> "Загрузка ${s.progress}%"
                    is UpdateState.UpdateNotAvailable -> "Последняя версия"
                    else -> telemetryManager.getAppVersion()
                }
                SettingsWidgetCard(
                    icon        = Icons.Default.Info,
                    title       = "О приложении",
                    subtitle    = updateSubtitle,
                    accentColor = Color(0xFF38BDF8)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExpandedInfoRow(label = "Версия", value = telemetryManager.getAppVersion(), icon = Icons.Default.Info, color = Color(0xFF38BDF8))
                        HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f))
                        // UpdateChecker встроенный
                        UpdateCheckerRow(
                            updateManager = updateManager,
                            onCheckClick  = {
                                if (hasNotificationPermission) scope.launch { updateManager.checkForUpdates() }
                                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        )
                        HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f))
                        VersionHistoryDemoItem()
                    }
                }
            }

            // ── Управление данными ──────────────────────────────────────
            item {
                var cacheSize   by remember { mutableStateOf("Расчёт...") }
                var historySize by remember { mutableStateOf("Расчёт...") }
                var isClearingCache by remember { mutableStateOf(false) }
                var showClearCacheDialog   by remember { mutableStateOf(false) }
                var showClearHistoryDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    cacheSize   = settingsManager.getTotalCacheSize()
                    historySize = settingsManager.getScanHistorySize()
                }

                SettingsWidgetCard(
                    icon        = Icons.Default.Storage,
                    title       = "Управление данными",
                    subtitle    = "Кэш: $cacheSize",
                    accentColor = Color(0xFFF59E0B)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExpandedActionRow(
                            icon     = Icons.Default.DeleteSweep,
                            label    = "Очистить кэш",
                            sublabel = cacheSize,
                            color    = Color(0xFFF59E0B),
                            onClick  = { showClearCacheDialog = true }
                        )
                        HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f))
                        ExpandedActionRow(
                            icon     = Icons.Default.History,
                            label    = "Очистить историю сканирований",
                            sublabel = historySize,
                            color    = Color(0xFF8B5CF6),
                            onClick  = { showClearHistoryDialog = true }
                        )
                    }
                }

                if (showClearCacheDialog) {
                    SettingsConfirmDialog(
                        title       = "Очистить кэш?",
                        message     = "Это удалит временные файлы. Данные и настройки не пострадают.",
                        confirmText = "Очистить",
                        accentColor = StardustError,
                        onDismiss   = { showClearCacheDialog = false },
                        onConfirm   = {
                            scope.launch {
                                isClearingCache = true
                                val ok = settingsManager.clearAppCache()
                                if (ok) { cacheSize = settingsManager.getTotalCacheSize(); Toast.makeText(context, "Кэш очищен", Toast.LENGTH_SHORT).show() }
                                else Toast.makeText(context, "Ошибка очистки", Toast.LENGTH_SHORT).show()
                                isClearingCache = false
                                showClearCacheDialog = false
                            }
                        }
                    )
                }
                if (showClearHistoryDialog) {
                    SettingsConfirmDialog(
                        title       = "Очистить историю?",
                        message     = "Вся история сканирований будет удалена без возможности восстановления.",
                        confirmText = "Очистить",
                        accentColor = StardustError,
                        onDismiss   = { showClearHistoryDialog = false },
                        onConfirm   = {
                            scope.launch {
                                val ok = settingsManager.clearScanHistory()
                                if (ok) { historySize = settingsManager.getScanHistorySize(); Toast.makeText(context, "История очищена", Toast.LENGTH_SHORT).show() }
                                else Toast.makeText(context, "Нет данных", Toast.LENGTH_SHORT).show()
                                showClearHistoryDialog = false
                            }
                        }
                    )
                }
            }

            // ── Об авторе ───────────────────────────────────────────────
            item {
                SettingsWidgetCard(
                    icon        = Icons.Default.Code,
                    title       = "Об авторе",
                    subtitle    = "Владислав С. · @Cyberdyne_Industries",
                    accentColor = Color(0xFFC084FC)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExpandedInfoRow(label = "Разработчик", value = "Владислав С.", icon = Icons.Default.Code, color = Color(0xFFC084FC))
                        HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f))
                        ExpandedActionRow(
                            icon    = Icons.AutoMirrored.Filled.Send,
                            label   = "Telegram",
                            sublabel = "@Cyberdyne_Industries",
                            color   = Color(0xFF38BDF8),
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Cyberdyne_Industries"))) }
                        )
                        HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f))
                        ExpandedActionRow(
                            icon    = Icons.Default.Email,
                            label   = "Email",
                            sublabel = "pankratovvlad69@gmail.com",
                            color   = Color(0xFFF59E0B),
                            onClick = {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:pankratovvlad69@gmail.com")
                                            putExtra(Intent.EXTRA_SUBJECT, "Обратная связь по QR Scanner")
                                        }, "Отправить письмо..."
                                    )
                                )
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ============================================================================================
// SPYDER ENGINE WIDGET — особый акцент
// ============================================================================================

@Composable
private fun SpyderEngineWidget() {
    var expanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "engine_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val cardScale by animateFloatAsState(
        targetValue   = if (expanded) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label         = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .drawBehind {
                // Фоновое свечение движка
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6A5AE0).copy(alpha = glowAlpha * 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.5f),
                        radius = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.2f, size.height * 0.5f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFEC407A).copy(alpha = glowAlpha * 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.3f),
                        radius = size.width * 0.5f
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.85f, size.height * 0.3f)
                )
                // Верхняя линия — цвет движка
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6A5AE0).copy(alpha = glowAlpha),
                            Color(0xFFEC407A).copy(alpha = glowAlpha * 0.6f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, 0f),
                    size    = androidx.compose.ui.geometry.Size(size.width, 1.5.dp.toPx())
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { expanded = true }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Иконка движка с pulse
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6A5AE0).copy(alpha = 0.3f), Color(0xFFEC407A).copy(alpha = 0.15f))
                                )
                            )
                            .drawBehind {
                                drawRoundRect(
                                    brush        = Brush.linearGradient(listOf(Color(0xFF6A5AE0).copy(alpha = glowAlpha * 0.8f), Color(0xFFEC407A).copy(alpha = glowAlpha * 0.5f))),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()),
                                    style        = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column {
                        Text(
                            "Графический движок",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp
                        )
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(5.dp).clip(CircleShape)
                                    .background(Color(0xFF6A5AE0).copy(alpha = glowAlpha + 0.3f))
                            )
                            Text(
                                "Spyder Engine 3000",
                                color    = Color(0xFF9B8FFF),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Icon(
                    Icons.Default.KeyboardArrowRight, null,
                    tint     = Color(0xFF6A5AE0).copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Мини-статус строки движка
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EngineStatusChip("120fps", Color(0xFF4CAF50))
                EngineStatusChip("AGSL Shader", Color(0xFF6A5AE0))
                EngineStatusChip("Spring Physics", Color(0xFFFF7043))
            }
        }
    }

    if (expanded) {
        SettingsModal(
            icon        = Icons.Default.ElectricBolt,
            title       = "Графический движок",
            accentColor = Color(0xFF6A5AE0),
            onDismiss   = { expanded = false }
        ) {
            Spyder3000SettingsItem()
        }
    }
}

@Composable
private fun EngineStatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// ============================================================================================
// SETTINGS WIDGET CARD — универсальная карточка-виджет
// ============================================================================================

@Composable
private fun SettingsWidgetCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    previewContent: @Composable () -> Unit = {},
    expandedContent: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val cardScale by animateFloatAsState(
        targetValue   = if (expanded) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label         = "card_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clip(RoundedCornerShape(20.dp))
            .background(StardustGlassBg)
            .drawBehind {
                drawRect(
                    color   = accentColor.copy(alpha = 0.7f),
                    topLeft = Offset(0f, size.height * 0.15f),
                    size    = androidx.compose.ui.geometry.Size(3.5.dp.toPx(), size.height * 0.7f)
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { expanded = true }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, null, tint = accentColor, modifier = Modifier.size(19.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(subtitle, color = StardustTextSecondary, fontSize = 11.sp, maxLines = 1)
                    }
                }
                Icon(Icons.Default.KeyboardArrowRight, null, tint = accentColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
            previewContent()
        }
    }

    if (expanded) {
        SettingsModal(icon = icon, title = title, accentColor = accentColor, onDismiss = { expanded = false }) {
            expandedContent()
        }
    }
}

// ============================================================================================
// SETTINGS MODAL — iOS-стиль
// ============================================================================================

@Composable
private fun SettingsModal(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val sheetScale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label         = "sheet_scale"
    )
    val sheetAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(250),
        label         = "sheet_alpha"
    )

    fun handleDismiss() { visible = false; onDismiss() }

    LaunchedEffect(visible) {
        if (!visible) { kotlinx.coroutines.delay(300); onDismiss() }
    }

    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * sheetAlpha))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .graphicsLayer { scaleX = sheetScale; scaleY = sheetScale; alpha = sheetAlpha }
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1C1830), Color(0xFF12102A)), Offset(0f, 0f), Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                    .drawBehind {
                        drawRect(color = accentColor.copy(alpha = 0.5f), topLeft = Offset(size.width * 0.12f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.76f, 1.5.dp.toPx()))
                    }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            ) {
                Column {
                    // Хэндл
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(StardustTextSecondary.copy(alpha = 0.22f)))
                    }
                    // Заголовок
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accentColor.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(title, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape).background(StardustTextSecondary.copy(alpha = 0.1f))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(14.dp)) }
                    }
                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 22.dp))
                    // Контент
                    Box(modifier = Modifier.padding(22.dp)) { content() }
                }
            }
        }
    }
}

// ============================================================================================
// EXPANDED ROW COMPONENTS — для контента внутри модала
// ============================================================================================

@Composable
private fun ExpandedToggleRow(
    icon: ImageVector,
    label: String,
    sublabel: String? = null,
    checked: Boolean,
    color: Color,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(!checked) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(color.copy(alpha = if (checked) 0.15f else 0.08f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (checked) color else StardustTextSecondary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = StardustTextPrimary, fontSize = 15.sp)
            if (sublabel != null) Text(sublabel, color = StardustTextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = color,
                uncheckedThumbColor = StardustTextSecondary,
                uncheckedTrackColor = StardustItemBg
            )
        )
    }
}

@Composable
private fun ExpandedActionRow(
    icon: ImageVector,
    label: String,
    sublabel: String? = null,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = StardustTextPrimary, fontSize = 15.sp)
            if (sublabel != null) Text(sublabel, color = StardustTextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = StardustTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ExpandedInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = StardustTextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================================================================
// THEME PICKER
// ============================================================================================

@Composable
private fun ThemePreviewStrip(currentTheme: AppTheme) {
    val colors = themePreviewColors[currentTheme] ?: listOf(Color.Black, Color.DarkGray, Color.Gray)
    val infiniteTransition = rememberInfiniteTransition(label = "strip")
    val offset by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse), label = "off")

    Box(
        modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(10.dp))
            .drawWithCache {
                onDrawBehind {
                    drawRect(color = colors[0])
                    val cx = size.width * (0.3f + offset * 0.4f)
                    drawCircle(brush = Brush.radialGradient(colors = listOf(colors[2].copy(alpha = 0.7f), colors[1].copy(alpha = 0.3f), Color.Transparent), center = Offset(cx, size.height * 0.5f), radius = size.width * 0.8f), radius = size.width * 0.8f, center = Offset(cx, size.height * 0.5f))
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(currentTheme.backgroundEmoji, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(currentTheme.backgroundName, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ThemePickerExpanded(currentTheme: AppTheme, onThemeSelected: (AppTheme) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppTheme.entries.chunked(3).forEach { rowThemes ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowThemes.forEach { theme ->
                    ThemePreviewCard(theme = theme, isSelected = theme == currentTheme, onClick = { onThemeSelected(theme) }, modifier = Modifier.weight(1f))
                }
                repeat(3 - rowThemes.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private val themePreviewColors = mapOf(
    AppTheme.ENGINE  to listOf(Color(0xFF0D0020), Color(0xFF4A00E0), Color(0xFF8E2DE2)),
    AppTheme.NEBULA  to listOf(Color(0xFF000217), Color(0xFF002E72), Color(0xFF0085E5)),
    AppTheme.VORONOI to listOf(Color(0xFF010A04), Color(0xFF0D5C2E), Color(0xFF33E87A)),
    AppTheme.WHITE   to listOf(Color(0xFF141418), Color(0xFFD0D0DC), Color(0xFFF5F5FF)),
    AppTheme.YELLOW  to listOf(Color(0xFF0F0800), Color(0xFF3D2000), Color(0xFFCC8800)),
    AppTheme.EMBER   to listOf(Color(0xFF1F0203), Color(0xFF990A10), Color(0xFFF2844D)),
    AppTheme.AURORA  to listOf(Color(0xFF010D05), Color(0xFF004060), Color(0xFF7700CC)),
    AppTheme.PLASMA  to listOf(Color(0xFF0F0030), Color(0xFFBB009E), Color(0xFF00E0FF)),
    AppTheme.RIVE    to listOf(Color(0xFF0A0015), Color(0xFF3D1A7A), Color(0xFF9B59F5)),
)

private val themeShortNames = mapOf(
    AppTheme.ENGINE  to "Неон",   AppTheme.NEBULA  to "Мгла",
    AppTheme.VORONOI to "Биос",   AppTheme.WHITE   to "Шёлк",
    AppTheme.YELLOW  to "Злат",   AppTheme.EMBER   to "Жар",
    AppTheme.AURORA  to "Аврора", AppTheme.PLASMA  to "Плазма",
    AppTheme.RIVE    to "Rive",
)

@Composable
private fun ThemePreviewCard(theme: AppTheme, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = themePreviewColors[theme] ?: listOf(Color.Black, Color.DarkGray, Color.Gray)
    val borderColor by animateColorAsState(if (isSelected) StardustPrimary else Color(0xFF2A2A3A), tween(300), label = "border")
    val scale by animateFloatAsState(if (isSelected) 1.04f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")
    val infiniteTransition = rememberInfiniteTransition(label = "prev_${theme.backgroundKey}")
    val gradientOffset by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(3000 + theme.ordinal * 700, easing = LinearEasing), RepeatMode.Reverse), label = "go")

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(64.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(12.dp)).border(2.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(color = colors[0])
                        val cx = size.width * (0.3f + gradientOffset * 0.4f)
                        val cy = size.height * (0.5f - gradientOffset * 0.2f)
                        drawCircle(brush = Brush.radialGradient(listOf(colors[2].copy(alpha = 0.7f), colors[1].copy(alpha = 0.3f), Color.Transparent), center = Offset(cx, cy), radius = size.width * 0.7f), radius = size.width * 0.7f, center = Offset(cx, cy))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(StardustPrimary), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
            else Text(theme.backgroundEmoji, fontSize = 20.sp)
        }
        Text(themeShortNames[theme] ?: theme.backgroundKey, color = if (isSelected) StardustPrimary else StardustTextSecondary, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center)
    }
}

// ============================================================================================
// UPDATE CHECKER ROW — встроенная версия для модала
// ============================================================================================

@Composable
private fun UpdateCheckerRow(updateManager: UpdateManager, onCheckClick: () -> Unit) {
    val updateState by updateManager.updateState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(updateState) {
        if (updateState is UpdateState.UpdateAvailable) showUpdateDialog = true
        if (updateState is UpdateState.ReadyToInstall) {
            updateManager.installApk((updateState as UpdateState.ReadyToInstall).uri)
            updateManager.resetState()
        }
    }

    if (showUpdateDialog) {
        val currentState = updateState
        if (currentState is UpdateState.UpdateAvailable) {
            UpdateAvailableDialog(
                info        = currentState.info,
                updateState = updateState,
                onDismiss   = { showUpdateDialog = false; if (updateState !is UpdateState.Downloading) updateManager.resetState() },
                onConfirm   = { updateManager.startUpdate(currentState.info) }
            )
        }
    }

    Row(
        modifier          = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            if (updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading && updateState !is UpdateState.ReadyToInstall) onCheckClick()
        }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF38BDF8).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SystemUpdate, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Проверить обновления", color = StardustTextPrimary, fontSize = 15.sp)
            Text(
                when (val s = updateState) {
                    is UpdateState.Checking           -> "Идёт проверка..."
                    is UpdateState.UpdateAvailable    -> "Доступна v${s.info.latestVersionName}!"
                    is UpdateState.UpdateNotAvailable -> "У вас последняя версия"
                    is UpdateState.Error              -> s.message
                    is UpdateState.Downloading        -> "Загрузка: ${s.progress}%"
                    is UpdateState.ReadyToInstall     -> "Готово к установке"
                    else                              -> "Нажмите для проверки"
                },
                color = when (updateState) {
                    is UpdateState.UpdateAvailable -> StardustSuccess
                    is UpdateState.Error           -> StardustError
                    is UpdateState.Downloading     -> StardustPrimary
                    is UpdateState.ReadyToInstall  -> StardustSuccess
                    else                           -> StardustTextSecondary
                },
                fontSize = 11.sp,
                fontWeight = if (updateState is UpdateState.UpdateAvailable) FontWeight.Bold else FontWeight.Normal
            )
        }
        Icon(Icons.Default.ChevronRight, null, tint = StardustTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
}

// ============================================================================================
// SETTINGS CONFIRM DIALOG
// ============================================================================================

@Composable
private fun SettingsConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    accentColor: Color = StardustError,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(if (visible) 1f else 0.90f, spring(0.75f, 400f), label = "s")
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(220), label = "a")
    fun handleDismiss() { visible = false; onDismiss() }

    Dialog(onDismissRequest = { handleDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f * alpha))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(0.86f).wrapContentHeight()
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1C1830), Color(0xFF12102A)), Offset(0f, 0f), Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                    .drawBehind { drawRect(color = accentColor.copy(alpha = 0.45f), topLeft = Offset(size.width * 0.15f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.7f, 1.5.dp.toPx())) }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            ) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Warning, null, tint = accentColor, modifier = Modifier.size(24.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
                        Text(message, color = StardustTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(StardustGlassBg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() }, contentAlignment = Alignment.Center) {
                            Text("Отмена", color = StardustTextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                        Box(modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(accentColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onConfirm() }, contentAlignment = Alignment.Center) {
                            Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// UPDATE DIALOG — без изменений в логике
// ============================================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpdateAvailableDialog(info: UpdateInfo, updateState: UpdateState, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    if (fullScreenImageUri != null) FullScreenImageViewerDialog(imageUrl = fullScreenImageUri!!, onDismiss = { fullScreenImageUri = null })

    AlertDialog(
        onDismissRequest = { if (updateState !is UpdateState.Downloading) onDismiss() },
        confirmButton = {
            if (updateState !is UpdateState.Downloading) {
                Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) { Text("Скачать" + (info.apkSize?.let { " ($it)" } ?: "")) }
            }
        },
        dismissButton = { if (updateState !is UpdateState.Downloading) TextButton(onClick = onDismiss) { Text("Позже", color = StardustTextSecondary) } },
        title = { Text("Доступна версия ${info.latestVersionName}!", color = StardustTextPrimary) },
        text = {
            AnimatedContent(targetState = updateState is UpdateState.Downloading, label = "update_content") { isDownloading ->
                if (isDownloading) {
                    val progress = (updateState as? UpdateState.Downloading)?.progress ?: 0
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Идёт загрузка...", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = StardustPrimary, trackColor = StardustItemBg)
                        Spacer(Modifier.height(8.dp))
                        Text("$progress%", color = StardustTextSecondary, fontSize = 12.sp)
                    }
                } else {
                    UpdateInfoContent(info = info, onImageClick = { fullScreenImageUri = it })
                }
            }
        },
        containerColor = StardustModalBg
    )
}

@Composable
private fun UpdateInfoContent(info: UpdateInfo, onImageClick: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val itemsToShow = if (isExpanded) info.releaseItems else info.releaseItems?.take(2)
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (!info.imageUrls.isNullOrEmpty()) { ImageSlider(imageUrls = info.imageUrls, onImageClick = onImageClick); Spacer(Modifier.height(24.dp)) }
        Column(modifier = Modifier.animateContentSize()) {
            if (!itemsToShow.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsToShow.forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            val (tagText, tagBaseColor) = when (item.type.lowercase()) { "new" -> "New" to Color(0xFF4CAF50); "fix" -> "Fix" to Color(0xFFFFC107); "beta" -> "Beta" to Color(0xFFE91E63); else -> "Info" to Color.Gray }
                            UpdateTag(text = tagText, baseColor = tagBaseColor)
                            Spacer(Modifier.width(12.dp))
                            Text(text = item.text, color = StardustTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
                if ((info.releaseItems?.size ?: 0) > 2) {
                    Spacer(Modifier.height(12.dp))
                    Text(if (isExpanded) "Свернуть" else "Подробнее...", color = StardustPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isExpanded = !isExpanded })
                }
            } else if (info.releaseNotes.isNotBlank()) {
                Text(info.releaseNotes, color = StardustTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun FullScreenImageViewerDialog(imageUrl: String, onDismiss: () -> Unit) {
    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(16.dp)))
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close, null, tint = Color.White) }
        }
    }
}

@Composable
fun UpdateTag(text: String, baseColor: Color) {
    val shape = RoundedCornerShape(6.dp)
    Box(modifier = Modifier.border(1.dp, baseColor, shape).background(baseColor.copy(alpha = 0.25f), shape).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, color = baseColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 12.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageSlider(imageUrls: List<String>, onImageClick: (String) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    LaunchedEffect(pagerState.pageCount) { while (true) { delay(4000); if (pagerState.pageCount > 0) pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount) } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))) { page ->
            AsyncImage(model = imageUrls[page], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clickable { onImageClick(imageUrls[page]) })
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(pagerState.pageCount) { i ->
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (pagerState.currentPage == i) StardustPrimary else StardustTextSecondary.copy(alpha = 0.5f)))
            }
        }
    }
}

// ============================================================================================
// VERSION HISTORY
// ============================================================================================

@Composable
private fun VersionHistoryDemoItem() {
    var showHistoryDialog by remember { mutableStateOf(false) }
    Row(
        modifier          = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showHistoryDialog = true }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF8B5CF6).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.History, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("История версий", color = StardustTextPrimary, fontSize = 15.sp)
            Text("Что нового в предыдущих обновлениях", color = StardustTextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = StardustTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
    if (showHistoryDialog) VersionHistoryDemoDialog(onDismiss = { showHistoryDialog = false })
}

@Composable
private fun VersionHistoryDemoDialog(onDismiss: () -> Unit) {
    val demoVersions = listOf(
        DemoVersionInfo("2.1.0", 210, "15 марта 2025", true, listOf(DemoReleaseItem("new","Новая система обновлений"), DemoReleaseItem("new","История версий"), DemoReleaseItem("improve","Производительность камеры"), DemoReleaseItem("fix","Вылет при сканировании"))),
        DemoVersionInfo("2.0.0", 200, "1 февраля 2025", false, listOf(DemoReleaseItem("new","Редизайн главного экрана"), DemoReleaseItem("new","Тёмная тема"), DemoReleaseItem("improve","Распознавание QR-кодов"))),
        DemoVersionInfo("1.5.0", 150, "10 декабря 2024", false, listOf(DemoReleaseItem("new","Новые форматы штрих-кодов"), DemoReleaseItem("improve","Оптимизация памяти"), DemoReleaseItem("beta","Batch-сканирование"))),
        DemoVersionInfo("1.0.0", 100, "1 октября 2024", false, listOf(DemoReleaseItem("new","Первый релиз"), DemoReleaseItem("new","Базовое сканирование QR")))
    )
    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 600.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📦 История версий", color = StardustTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = StardustTextSecondary) }
                }
                HorizontalDivider(color = StardustItemBg)
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                    items(demoVersions) { version -> VersionCard(version = version); Spacer(Modifier.height(12.dp)) }
                }
                Text("⚠️ Демо-данные. Полная история появится в следующем обновлении.", color = StardustTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun VersionCard(version: DemoVersionInfo) {
    var isExpanded by remember { mutableStateOf(false) }
    val itemsToShow = if (isExpanded) version.releaseItems else version.releaseItems.take(3)
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (version.isCurrent) StardustPrimary.copy(alpha = 0.1f) else StardustGlassBg), modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(version.versionName, color = if (version.isCurrent) StardustPrimary else StardustTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (version.isCurrent) { Spacer(Modifier.width(8.dp)); Surface(shape = RoundedCornerShape(4.dp), color = StardustPrimary.copy(alpha = 0.2f)) { Text("Текущая", color = StardustPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }
                }
                Text(version.releaseDate, color = StardustTextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text("Version code: ${version.versionCode}", color = StardustTextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { itemsToShow.forEach { ReleaseItemRow(it) } }
            if (version.releaseItems.size > 3) { Spacer(Modifier.height(8.dp)); Text(if (isExpanded) "Свернуть ▲" else "Показать ещё ▼", color = StardustPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { isExpanded = !isExpanded }) }
        }
    }
}

@Composable
private fun ReleaseItemRow(item: DemoReleaseItem) {
    val (tagText, tagColor) = when (item.type.lowercase()) { "new" -> "Новое" to Color(0xFF4CAF50); "improve" -> "Улучшено" to Color(0xFF2196F3); "fix" -> "Исправлено" to Color(0xFFFF9800); "beta" -> "Beta" to Color(0xFFE91E63); else -> "Info" to Color.Gray }
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(4.dp), color = tagColor.copy(alpha = 0.15f), modifier = Modifier.width(70.dp)) { Text(tagText, color = tagColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
        Spacer(Modifier.width(10.dp))
        Text(item.text, color = StardustTextPrimary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

data class DemoVersionInfo(val versionName: String, val versionCode: Int, val releaseDate: String, val isCurrent: Boolean, val releaseItems: List<DemoReleaseItem>)
data class DemoReleaseItem(val type: String, val text: String)