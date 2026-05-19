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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.qrscannerapp.common.ui.AnimatedDialogWrapper
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.qrscannerapp.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UnifiedSettingsScreen(authManager: AuthManager) {
    val authState by authManager.authState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val telemetryManager = remember { TelemetryManager(context) }
    val updateManager: UpdateManager = hiltViewModel()
    val settingsManager = remember { SettingsManager(context) }

    val isSoundEnabled     by settingsManager.isSoundEnabledFlow.collectAsState(initial = true)
    val isVibrationEnabled by settingsManager.isVibrationEnabledFlow.collectAsState(initial = true)
    val currentTheme by settingsManager.appThemeFlow.collectAsState(initial = AppTheme.ENGINE)
    val dialogAnimation by settingsManager.dialogAnimationFlow
        .collectAsState(initial = DialogAnimation.SCALE_BOUNCY)
    val isSpyderAutoUpdate by settingsManager.spyderAutoUpdateFlow
        .collectAsState(initial = true)

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (!isGranted) Toast.makeText(
                context,
                "Уведомления необходимы для отображения статуса загрузки.",
                Toast.LENGTH_LONG
            ).show()
        }
    )

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (!hasLocationPermission) Toast.makeText(context, "Геолокация необходима для определения ближайших задач.", Toast.LENGTH_LONG).show()
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- Тема оформления ---
        SettingsCategory(title = "Тема оформления")
        ThemePickerCard(
            currentTheme = currentTheme,
            onThemeSelected = { scope.launch { settingsManager.setAppTheme(it) } }
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Эффекты при сканировании ---
        if (authState.role != UserRole.ELECTRICIAN) {
            SettingsCategory(title = "Эффекты при сканировании")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.VolumeUp,
                        iconColor = Color(0xFFF59E0B),
                        title = "Звуковой сигнал",
                        isChecked = isSoundEnabled,
                        onCheckedChange = { scope.launch { settingsManager.setSoundEnabled(it) } }
                    )
                    HorizontalDivider(color = StardustItemBg)
                    SettingsToggleRow(
                        icon = Icons.Default.Vibration,
                        iconColor = Color(0xFF94A3B8),
                        title = "Вибрация",
                        isChecked = isVibrationEnabled,
                        onCheckedChange = { scope.launch { settingsManager.setVibrationEnabled(it) } }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Разрешения ---
        SettingsCategory(title = "Разрешения")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsToggleRow(
                    icon = Icons.Default.LocationOn,
                    iconColor = Color(0xFF4ADE80),
                    title = "Геолокация",
                    subtitle = if (hasLocationPermission) "Разрешено · используется для задач" else "Не разрешено",
                    isChecked = hasLocationPermission,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        } else {
                            context.startActivity(
                                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- О приложении ---
        SettingsCategory(title = "О приложении")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsRow(
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF38BDF8),
                    title = "Версия приложения",
                    value = telemetryManager.getAppVersion()
                )
                HorizontalDivider(color = StardustItemBg)
                UpdateChecker(
                    updateManager = updateManager,
                    onCheckClick = {
                        if (hasNotificationPermission) {
                            scope.launch { updateManager.checkForUpdates() }
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                HorizontalDivider(color = StardustItemBg)
                VersionHistoryDemoItem()
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- Управление кэшем ---
        SettingsCategory(title = "Управление данными")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                var cacheSize by remember { mutableStateOf("Расчёт...") }
                var historySize by remember { mutableStateOf("Расчёт...") }
                var isClearingCache by remember { mutableStateOf(false) }
                var showClearCacheDialog by remember { mutableStateOf(false) }
                var showClearHistoryDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    cacheSize = settingsManager.getTotalCacheSize()
                    historySize = settingsManager.getScanHistorySize()
                }

                SettingsRowWithAction(
                    icon = Icons.Default.DeleteSweep,
                    iconColor = Color(0xFFF59E0B),
                    title = "Очистить кэш",
                    subtitle = cacheSize,
                    onClick = { showClearCacheDialog = true }
                )

                HorizontalDivider(color = StardustItemBg)

                SettingsRowWithAction(
                    icon = Icons.Default.History,
                    iconColor = Color(0xFF8B5CF6),
                    title = "Очистить историю сканирований",
                    subtitle = historySize,
                    onClick = { showClearHistoryDialog = true }
                )

                if (showClearCacheDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearCacheDialog = false },
                        title = { Text("Очистить кэш?", color = StardustTextPrimary) },
                        text = { Text("Это удалит временные файлы и освободит место. Ваши данные и настройки не пострадают.", color = StardustTextSecondary) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isClearingCache = true
                                        val success = settingsManager.clearAppCache()
                                        if (success) {
                                            cacheSize = settingsManager.getTotalCacheSize()
                                            Toast.makeText(context, "Кэш очищен", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Ошибка очистки", Toast.LENGTH_SHORT).show()
                                        }
                                        isClearingCache = false
                                        showClearCacheDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StardustError)
                            ) {
                                if (isClearingCache) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text("Очистить")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearCacheDialog = false }) {
                                Text("Отмена", color = StardustTextSecondary)
                            }
                        },
                        containerColor = StardustModalBg
                    )
                }

                if (showClearHistoryDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearHistoryDialog = false },
                        title = { Text("Очистить историю?", color = StardustTextPrimary) },
                        text = { Text("Вся история сканирований будет удалена без возможности восстановления.", color = StardustError) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val success = settingsManager.clearScanHistory()
                                        if (success) {
                                            historySize = settingsManager.getScanHistorySize()
                                            Toast.makeText(context, "История очищена", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Нет данных для очистки", Toast.LENGTH_SHORT).show()
                                        }
                                        showClearHistoryDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StardustError)
                            ) {
                                Text("Очистить")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearHistoryDialog = false }) {
                                Text("Отмена", color = StardustTextSecondary)
                            }
                        },
                        containerColor = StardustModalBg
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- Spyder3000 ---
        SettingsCategory(title = "Графический движок")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF000000))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Spyder3000SettingsItem()
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- Об авторе ---
        SettingsCategory(title = "Об авторе")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsRow(icon = Icons.Default.Code, iconColor = Color(0xFFC084FC), title = "Разработчик", value = "Владислав С.")
                HorizontalDivider(color = StardustItemBg)
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.Send,
                    iconColor = Color(0xFF38BDF8),
                    title = "Telegram",
                    value = "@Cyberdyne_Industries",
                    isClickable = true
                ) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Cyberdyne_Industries"))
                    )
                }
                HorizontalDivider(color = StardustItemBg)
                SettingsRow(
                    icon = Icons.Default.Email,
                    iconColor = Color(0xFFF59E0B),
                    title = "Email",
                    value = "pankratovvlad69@gmail.com",
                    isClickable = true
                ) {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:pankratovvlad69@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Обратная связь по приложению QR Scanner")
                        }.let { Intent.createChooser(it, "Отправить письмо...") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// =================================================================================
// THEME PICKER
// =================================================================================

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
    AppTheme.ENGINE  to "Неон",
    AppTheme.NEBULA  to "Мгла",
    AppTheme.VORONOI to "Биос",
    AppTheme.WHITE   to "Шёлк",
    AppTheme.YELLOW  to "Злат",
    AppTheme.EMBER   to "Жар",
    AppTheme.AURORA  to "Аврора",
    AppTheme.PLASMA  to "Плазма",
    AppTheme.RIVE    to "Rive",
)

@Composable
private fun ThemePickerCard(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val colors = themePreviewColors[currentTheme] ?: listOf(Color.Black, Color.DarkGray, Color.Gray)

    val infiniteTransition = rememberInfiniteTransition(label = "compact_prev")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "compact_off"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        modifier = Modifier.clickable { showDialog = true }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(color = colors[0])
                            val cx = size.width * (0.3f + gradientOffset * 0.4f)
                            val cy = size.height * (0.5f - gradientOffset * 0.2f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        colors[2].copy(alpha = 0.7f),
                                        colors[1].copy(alpha = 0.3f),
                                        Color.Transparent
                                    ),
                                    center = Offset(cx, cy),
                                    radius = size.width * 0.9f
                                ),
                                radius = size.width * 0.9f,
                                center = Offset(cx, cy)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(currentTheme.backgroundEmoji, fontSize = 22.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Фон приложения", color = StardustTextSecondary, fontSize = 12.sp)
                Text(
                    currentTheme.backgroundName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = StardustTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showDialog) {
        ThemePickerDialog(
            currentTheme = currentTheme,
            onThemeSelected = { onThemeSelected(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun ThemePickerDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13131F))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Фон приложения",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = StardustTextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTheme.entries.chunked(3).forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowThemes.forEach { theme ->
                                ThemePreviewCard(
                                    theme = theme,
                                    isSelected = theme == currentTheme,
                                    onClick = { onThemeSelected(theme) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowThemes.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = themePreviewColors[theme] ?: listOf(Color.Black, Color.DarkGray, Color.Gray)

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) StardustPrimary else Color(0xFF2A2A3A),
        animationSpec = tween(300),
        label = "border"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "preview_${theme.backgroundKey}")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(3000 + theme.ordinal * 700, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(12.dp))
                .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(color = colors[0])
                        val cx = size.width * (0.3f + gradientOffset * 0.4f)
                        val cy = size.height * (0.5f - gradientOffset * 0.2f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    colors[2].copy(alpha = 0.7f),
                                    colors[1].copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = size.width * 0.7f
                            ),
                            radius = size.width * 0.7f,
                            center = Offset(cx, cy)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(StardustPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            } else {
                Text(text = theme.backgroundEmoji, fontSize = 20.sp)
            }
        }

        Text(
            text = themeShortNames[theme] ?: theme.backgroundKey,
            color = if (isSelected) StardustPrimary else StardustTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

// =================================================================================
// UPDATE CHECKER
// =================================================================================

@Composable
private fun UpdateChecker(updateManager: UpdateManager, onCheckClick: () -> Unit) {
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
                info = currentState.info,
                updateState = updateState,
                onDismiss = {
                    showUpdateDialog = false
                    if (updateState !is UpdateState.Downloading) updateManager.resetState()
                },
                onConfirm = { updateManager.startUpdate(currentState.info) }
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading && updateState !is UpdateState.ReadyToInstall)
                    onCheckClick()
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.SystemUpdate, contentDescription = "Обновления", tint = StardustTextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Проверить обновления", color = StardustTextPrimary, fontSize = 16.sp)
            when (val state = updateState) {
                is UpdateState.Checking        -> Text("Идет проверка...",            color = StardustTextSecondary, fontSize = 12.sp)
                is UpdateState.UpdateAvailable -> Text("Доступна версия ${state.info.latestVersionName}!", color = StardustSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                is UpdateState.UpdateNotAvailable -> Text("У вас последняя версия",   color = StardustTextSecondary, fontSize = 12.sp)
                is UpdateState.Error           -> Text(state.message,                 color = StardustError,         fontSize = 12.sp)
                is UpdateState.Downloading     -> Text("Загрузка: ${state.progress}%",color = StardustPrimary,       fontSize = 12.sp)
                is UpdateState.ReadyToInstall  -> Text("Готово к установке",          color = StardustSuccess,       fontSize = 12.sp)
                is UpdateState.Idle            -> Text("Нажмите, чтобы проверить",    color = StardustTextSecondary, fontSize = 12.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StardustTextSecondary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpdateAvailableDialog(
    info: UpdateInfo,
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }

    if (fullScreenImageUri != null) {
        FullScreenImageViewerDialog(
            imageUrl = fullScreenImageUri!!,
            onDismiss = { fullScreenImageUri = null }
        )
    }

    AlertDialog(
        onDismissRequest = { if (updateState !is UpdateState.Downloading) onDismiss() },
        confirmButton = {
            if (updateState !is UpdateState.Downloading) {
                val buttonText = "Скачать" + (info.apkSize?.let { " ($it)" } ?: "")
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                ) { Text(buttonText) }
            }
        },
        dismissButton = {
            if (updateState !is UpdateState.Downloading) {
                TextButton(onClick = onDismiss) { Text("Позже", color = StardustTextSecondary) }
            }
        },
        title = { Text("Доступна новая версия ${info.latestVersionName}!", color = StardustTextPrimary) },
        text = {
            AnimatedContent(
                targetState = updateState is UpdateState.Downloading,
                label = "update_content_switcher"
            ) { isDownloading ->
                if (isDownloading) {
                    val progress = (updateState as? UpdateState.Downloading)?.progress ?: 0
                    DownloadProgressIndicator(progress = progress)
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
        if (!info.imageUrls.isNullOrEmpty()) {
            ImageSlider(imageUrls = info.imageUrls, onImageClick = onImageClick)
            Spacer(modifier = Modifier.height(24.dp))
        }
        Column(modifier = Modifier.animateContentSize()) {
            if (!itemsToShow.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsToShow.forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            val (tagText, tagBaseColor) = when (item.type.lowercase()) {
                                "new"  -> "New"  to Color(0xFF4CAF50)
                                "fix"  -> "Fix"  to Color(0xFFFFC107)
                                "beta" -> "Beta" to Color(0xFFE91E63)
                                else   -> "Info" to Color.Gray
                            }
                            UpdateTag(text = tagText, baseColor = tagBaseColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = item.text, color = StardustTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
                if ((info.releaseItems?.size ?: 0) > 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isExpanded) "Свернуть" else "Подробнее...",
                        color = StardustPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { isExpanded = !isExpanded }
                    )
                }
            } else if (info.releaseNotes.isNotBlank()) {
                Text(text = info.releaseNotes, color = StardustTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun DownloadProgressIndicator(progress: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Идет загрузка...", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = StardustPrimary,
            trackColor = StardustItemBg
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("${progress}%", color = StardustTextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun FullScreenImageViewerDialog(imageUrl: String, onDismiss: () -> Unit) {
    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full screen image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun UpdateTag(text: String, baseColor: Color) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .border(width = 1.dp, color = baseColor, shape = shape)
            .background(color = baseColor.copy(alpha = 0.25f), shape = shape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = baseColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 12.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageSlider(imageUrls: List<String>, onImageClick: (String) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    LaunchedEffect(pagerState.pageCount) {
        while (true) {
            delay(4000)
            if (pagerState.pageCount > 0) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
        ) { page ->
            AsyncImage(
                model = imageUrls[page],
                contentDescription = "Изображение обновления ${page + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clickable { onImageClick(imageUrls[page]) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(pagerState.pageCount) { iteration ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == iteration) StardustPrimary
                            else StardustTextSecondary.copy(alpha = 0.5f)
                        )
                )
            }
        }
    }
}

@Composable
private fun SettingsCategory(title: String) {
    Text(
        text = title.uppercase(),
        color = StardustTextSecondary,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    iconColor: Color = StardustTextSecondary,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = (if (isClickable) Modifier.clickable(onClick = onClick) else Modifier)
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, color = StardustTextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (isClickable) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconColor: Color = StardustTextSecondary,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconColor.copy(alpha = if (isChecked) 0.15f else 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isChecked) iconColor else StardustTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = StardustTextPrimary, fontSize = 15.sp)
            if (subtitle != null) {
                Text(text = subtitle, color = StardustTextSecondary, fontSize = 12.sp)
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = StardustPrimary,
                uncheckedThumbColor = StardustTextSecondary,
                uncheckedTrackColor = StardustItemBg
            )
        )
    }
}

@Composable
private fun SettingsRowWithAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = StardustTextSecondary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = StardustTextPrimary, fontSize = 15.sp)
            Text(text = subtitle, color = StardustTextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp))
    }
}

// =================================================================================
// ИСТОРИЯ ВЕРСИЙ - DEMO ЗАГЛУШКА
// =================================================================================

@Composable
private fun VersionHistoryDemoItem() {
    var showHistoryDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showHistoryDialog = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF8B5CF6).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "История версий",
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "История версий", color = StardustTextPrimary, fontSize = 15.sp)
            Text(text = "Что нового в предыдущих обновлениях", color = StardustTextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp))
    }

    if (showHistoryDialog) {
        VersionHistoryDemoDialog(onDismiss = { showHistoryDialog = false })
    }
}

@Composable
private fun VersionHistoryDemoDialog(onDismiss: () -> Unit) {
    val demoVersions = listOf(
        DemoVersionInfo(
            versionName = "2.1.0",
            versionCode = 210,
            releaseDate = "15 марта 2025",
            isCurrent = true,
            releaseItems = listOf(
                DemoReleaseItem("new", "Новая система обновлений с фоновой загрузкой"),
                DemoReleaseItem("new", "История версий в настройках"),
                DemoReleaseItem("improve", "Улучшена производительность камеры"),
                DemoReleaseItem("fix", "Исправлен вылет при сканировании некорректных QR-кодов")
            )
        ),
        DemoVersionInfo(
            versionName = "2.0.0",
            versionCode = 200,
            releaseDate = "1 февраля 2025",
            isCurrent = false,
            releaseItems = listOf(
                DemoReleaseItem("new", "Редизайн главного экрана"),
                DemoReleaseItem("new", "Добавлена тёмная тема"),
                DemoReleaseItem("improve", "Ускорено распознавание QR-кодов")
            )
        ),
        DemoVersionInfo(
            versionName = "1.5.0",
            versionCode = 150,
            releaseDate = "10 декабря 2024",
            isCurrent = false,
            releaseItems = listOf(
                DemoReleaseItem("new", "Поддержка новых форматов штрих-кодов"),
                DemoReleaseItem("improve", "Оптимизация работы с памятью"),
                DemoReleaseItem("beta", "Экспериментальный режим batch-сканирования")
            )
        ),
        DemoVersionInfo(
            versionName = "1.0.0",
            versionCode = 100,
            releaseDate = "1 октября 2024",
            isCurrent = false,
            releaseItems = listOf(
                DemoReleaseItem("new", "Первый релиз приложения"),
                DemoReleaseItem("new", "Базовое сканирование QR-кодов")
            )
        )
    )

    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📦 История версий",
                        color = StardustTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = StardustTextSecondary)
                    }
                }

                HorizontalDivider(color = StardustItemBg)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(demoVersions) { version ->
                        VersionCard(version = version)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Text(
                    text = "⚠️ Это демо-данные. Полноценная история версий будет добавлена в следующем обновлении.",
                    color = StardustTextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun VersionCard(version: DemoVersionInfo) {
    var isExpanded by remember { mutableStateOf(false) }
    val itemsToShow = if (isExpanded) version.releaseItems else version.releaseItems.take(3)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (version.isCurrent) StardustPrimary.copy(alpha = 0.1f) else StardustGlassBg
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = version.versionName,
                        color = if (version.isCurrent) StardustPrimary else StardustTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (version.isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StardustPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Текущая",
                                color = StardustPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = version.releaseDate,
                    color = StardustTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Version code: ${version.versionCode}",
                color = StardustTextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsToShow.forEach { item ->
                    ReleaseItemRow(item = item)
                }
            }

            if (version.releaseItems.size > 3) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isExpanded) "Свернуть ▲" else "Показать ещё ▼",
                    color = StardustPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }
        }
    }
}

@Composable
private fun ReleaseItemRow(item: DemoReleaseItem) {
    val (tagText, tagColor) = when (item.type.lowercase()) {
        "new" -> "Новое" to Color(0xFF4CAF50)
        "improve" -> "Улучшено" to Color(0xFF2196F3)
        "fix" -> "Исправлено" to Color(0xFFFF9800)
        "beta" -> "Beta" to Color(0xFFE91E63)
        else -> "Info" to Color.Gray
    }

    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = tagColor.copy(alpha = 0.15f),
            modifier = Modifier.width(70.dp)
        ) {
            Text(
                text = tagText,
                color = tagColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = item.text,
            color = StardustTextPrimary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

data class DemoVersionInfo(
    val versionName: String,
    val versionCode: Int,
    val releaseDate: String,
    val isCurrent: Boolean,
    val releaseItems: List<DemoReleaseItem>
)

data class DemoReleaseItem(
    val type: String,
    val text: String
)