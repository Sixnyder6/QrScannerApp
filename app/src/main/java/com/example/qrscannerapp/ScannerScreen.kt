package com.example.qrscannerapp

import androidx.compose.ui.graphics.graphicsLayer
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.core.model.ActiveTab
import com.example.qrscannerapp.core.model.ScanEvent
import com.example.qrscannerapp.core.model.UiEffect
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseScreen
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseViewModel
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem
import com.example.qrscannerapp.features.scanner.ui.components.BatchProgressBar
import com.example.qrscannerapp.features.scanner.ui.components.BatchSetupSheet
import com.example.qrscannerapp.features.scanner.ui.components.CameraView
import com.example.qrscannerapp.features.scanner.ui.components.EmptyState
import com.example.qrscannerapp.features.scanner.ui.components.ExportSheet
import com.example.qrscannerapp.features.scanner.ui.components.SaveSessionDialog
import com.example.qrscannerapp.features.scanner.ui.components.ScanListItem
import com.example.qrscannerapp.features.scanner.ui.components.ScooterSearchResultDialog
import com.example.qrscannerapp.features.scanner.ui.components.SearchResultDialog
import com.example.qrscannerapp.features.scanner.ui.components.SessionSavedDialog
import com.example.qrscannerapp.features.scanner.ui.components.SessionStatsRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- ЦВЕТА ---
val StardustGlassBg     = Color(0xBF1A1A1D)
val StardustSolidBg     = Color(0xFF1A1A1D)
val StardustItemBg      = Color(0x14FFFFFF)
val StardustPrimary     = Color(0xFF6A5AE0)
val StardustSecondary   = Color(0xFF8A7DFF)
val StardustTextPrimary = Color.White
val StardustTextSecondary = Color(0xFFA0A0A5)
val StardustModalBg     = Color(0xFF2a2a2e)
val StardustSuccess     = Color(0xFF4CAF50)
val StardustError       = Color(0xFFF44336)
val StardustWarning     = Color(0xFFFFC107)

val ColorFujian   = Color(0xFFFF8A65)
val ColorByd      = Color(0xFF4FC3F7)
val ColorWind50   = Color(0xFF69F0AE)
val ColorWind50Old = Color(0xFFFFAB40)
val ColorWind40   = Color(0xFF7C8AFF)

// ============================================================================================
// ВЬЮФАЙНДЕР
// ============================================================================================

@Composable
private fun ScanViewfinder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "vf")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan_line"
    )
    Box(
        modifier = modifier.drawBehind {
            val cl = 36.dp.toPx()
            val sw = 3.dp.toPx()
            val r  = 10.dp.toPx()
            val w  = size.width
            val h  = size.height
            val stroke = Stroke(width = sw, cap = StrokeCap.Round)
            listOf(
                Path().apply { moveTo(0f, cl); lineTo(0f, r); arcTo(Rect(0f, 0f, r*2, r*2), 180f, 90f, false); lineTo(cl, 0f) },
                Path().apply { moveTo(w-cl, 0f); lineTo(w-r, 0f); arcTo(Rect(w-r*2, 0f, w, r*2), 270f, 90f, false); lineTo(w, cl) },
                Path().apply { moveTo(0f, h-cl); lineTo(0f, h-r); arcTo(Rect(0f, h-r*2, r*2, h), 180f, -90f, false); lineTo(cl, h) },
                Path().apply { moveTo(w-cl, h); lineTo(w-r, h); arcTo(Rect(w-r*2, h-r*2, w, h), 90f, -90f, false); lineTo(w, h-cl) }
            ).forEach { drawPath(it, StardustPrimary, style = stroke) }
            val y = h * progress
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, StardustSecondary.copy(alpha = 0.7f), Color.Transparent),
                    startX = 0f, endX = w
                ),
                start = Offset(0f, y), end = Offset(w, y),
                strokeWidth = 2.dp.toPx()
            )
        }
    )
}

// ============================================================================================
// ГЛАВНЫЙ ЭКРАН
// ============================================================================================

@Composable
fun StardustScreen(
    viewModel: QrScannerViewModel,
    onMenuClick: () -> Unit,
    hapticManager: HapticFeedbackManager,
    view: View,
    onNavigateToPalletDistribution: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToVisualRepair: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasCameraPermission = granted }
    LaunchedEffect(true) { launcher.launch(Manifest.permission.CAMERA) }

    val isSearchMode         by viewModel.isSearchMode.collectAsState()
    val isNumberMode         by viewModel.isNumberMode.collectAsState()
    val numberItems          by viewModel.numberItems.collectAsState()
    val expandedStickerCode  by viewModel.expandedStickerCode.collectAsState()
    val searchResult         by viewModel.searchResult.collectAsState()
    val scooterSearchResult  by viewModel.scooterSearchResult.collectAsState()
    val isSearching          by viewModel.isSearching.collectAsState()
    val scanEventFlow        = viewModel.scanEvent
    val warehouseViewModel: WarehouseViewModel = hiltViewModel()
    val activeTab            by viewModel.activeTab.collectAsState()
    var isTorchOn            by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Камера (верхние 35%) ──────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().weight(0.35f)) {
            CameraView(
                isSearchMode  = isSearchMode,
                hasPermission = hasCameraPermission,
                scanEventFlow = scanEventFlow,
                isTorchOn     = isTorchOn,
                onTorchChange = { isTorchOn = it },
                onCodeScanned = { code ->
                    when (activeTab) {
                        ActiveTab.WAREHOUSE -> warehouseViewModel.onPartScanned(code)
                        else -> viewModel.onCodeScanned(code)
                    }
                },
                onStatusUpdate = { msg, isErr -> viewModel.updateStatus(msg, isErr) }
            )
            ScanViewfinder(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.65f).aspectRatio(1f))
            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StardustWarning)
                }
            }
        }

        // ── Нижняя панель ────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().weight(0.65f).background(Color(0xFF0D0D10))) {

            Column(modifier = Modifier.fillMaxSize()) {
                // ── Панель управления камерой ─────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Поиск
                    CameraControlButton(
                        active   = isSearchMode,
                        activeColor = StardustWarning,
                        onClick  = { viewModel.toggleSearchMode() }
                    ) {
                        Icon(Icons.Default.Search, "Поиск", tint = if (isSearchMode) Color.Black else StardustTextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    // Номерной режим
                    CameraControlButton(
                        active   = isNumberMode,
                        activeColor = StardustPrimary,
                        onClick  = { viewModel.toggleNumberMode() }
                    ) {
                        Text("#", color = if (isNumberMode) Color.White else StardustTextSecondary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                    // Фонарик
                    CameraControlButton(
                        active   = isTorchOn,
                        activeColor = StardustWarning,
                        onClick  = { isTorchOn = !isTorchOn }
                    ) {
                        Icon(if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff, "Фонарик", tint = if (isTorchOn) Color.Black else StardustTextPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                ScannerOverlayUi(
                    modifier = Modifier.weight(1f),
                    viewModel = viewModel,
                    warehouseViewModel = warehouseViewModel,
                    hapticManager = hapticManager,
                    view = view,
                    isSearchMode = isSearchMode,
                    isNumberMode = isNumberMode,
                    numberItems = numberItems,
                    expandedStickerCode = expandedStickerCode,
                    onNavigateToPalletDistribution = onNavigateToPalletDistribution,
                    onNavigateToStorage = onNavigateToStorage,
                    onNavigateToHistory = onNavigateToHistory
                )
            }
        }
    }

    if (searchResult != null) {
        SearchResultDialog(
            result = searchResult!!,
            onDismiss = { viewModel.clearSearchResult() },
            onNavigateToPallet = { viewModel.setHighlightedPallet(searchResult!!.palletId); viewModel.clearSearchResult(); onNavigateToPalletDistribution() }
        )
    }
    if (scooterSearchResult != null) {
        val number = scooterSearchResult!!.first
        ScooterSearchResultDialog(
            scooterNumber = number,
            locationName  = scooterSearchResult!!.second,
            lastUser      = "Система",
            onDismiss     = { viewModel.clearScooterSearchResult() },
            onNavigate    = { viewModel.clearScooterSearchResult(); onNavigateToStorage() },
            onOpen3D      = { onNavigateToVisualRepair(number) }
        )
    }
}

// ============================================================================================
// КНОПКА УПРАВЛЕНИЯ КАМЕРОЙ — iOS стиль
// ============================================================================================

@Composable
private fun CameraControlButton(
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (active) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label         = "btn_scale"
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) activeColor
                else StardustGlassBg
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ============================================================================================
// OVERLAY UI
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerOverlayUi(
    modifier: Modifier = Modifier,
    viewModel: QrScannerViewModel,
    warehouseViewModel: WarehouseViewModel,
    hapticManager: HapticFeedbackManager,
    view: View,
    isSearchMode: Boolean,
    isNumberMode: Boolean = false,
    numberItems: List<com.example.qrscannerapp.features.scanner.domain.model.StickerItem> = emptyList(),
    expandedStickerCode: String? = null,
    onNavigateToPalletDistribution: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val focusManager  = LocalFocusManager.current
    val hapticFeedback = LocalHapticFeedback.current

    val settingsManager    = remember { SettingsManager(context) }
    val isSoundEnabled     by settingsManager.isSoundEnabledFlow.collectAsState(initial = true)
    val isVibrationEnabled by settingsManager.isVibrationEnabledFlow.collectAsState(initial = true)

    val activeTab      by viewModel.activeTab.collectAsState()
    val newItems       by viewModel.newItems.collectAsState()
    val scooterCount   = viewModel.scooterCodes.size
    val batteryCount   = viewModel.batteryCodes.size
    val newBatteryCount = viewModel.newBatteryCodes.size
    val statusMessage  by viewModel.statusMessage.collectAsState()
    val duplicateCount by viewModel.duplicateCount.collectAsState()
    val scanRate       by viewModel.scanRate.collectAsState()
    val batchConfig    by viewModel.batchConfig.collectAsState()

    var sessionSeconds by remember { mutableLongStateOf(0L) }
    val hasItems = scooterCount > 0 || batteryCount > 0 || newBatteryCount > 0
    LaunchedEffect(hasItems) {
        if (hasItems) { while (true) { delay(1000); sessionSeconds++ } } else sessionSeconds = 0L
    }

    var showBatchSetupSheet by remember { mutableStateOf(false) }
    val batchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isManualInputExpanded by remember { mutableStateOf(false) }
    val isInputVisible = isManualInputExpanded || isSearchMode

    val currentList: List<ScanItem> = when (activeTab) {
        ActiveTab.SCOOTERS      -> viewModel.scooterCodes
        ActiveTab.BATTERIES     -> viewModel.batteryCodes
        ActiveTab.NEW_BATTERIES -> viewModel.newBatteryCodes
        else -> emptyList()
    }

    val exportSheetState = rememberModalBottomSheetState()
    var showExportSheet       by remember { mutableStateOf(false) }
    var showSaveSessionDialog by remember { mutableStateOf(false) }
    val recentlySavedSession  by viewModel.recentlySavedSession.collectAsState()

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    DisposableEffect(Unit) { onDispose { toneGen.release() } }

    val listState = rememberLazyListState()
    var manualInputText by remember { mutableStateOf("") }
    var selectedManufacturer by remember { mutableStateOf("FUJIAN") }
    var selectedWind50Subtype by remember { mutableStateOf("NEW") }

    LaunchedEffect(currentList.firstOrNull()) { if (currentList.isNotEmpty()) listState.animateScrollToItem(0) }
    LaunchedEffect(activeTab) { manualInputText = "" }

    LaunchedEffect(Unit) {
        viewModel.scanEffect.collect { effect ->
            when (effect) {
                is UiEffect.ScanSuccess -> {
                    if (isSoundEnabled) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    if (isVibrationEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        else @Suppress("DEPRECATION") vibrator.vibrate(50)
                    }
                    manualInputText = ""
                }
                is UiEffect.SessionSaved -> hapticManager.performConfirm(hapticFeedback, scope)
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.batchEvent.collect { event ->
            when (event) {
                is BatchEvent.Completed -> {
                    if (isVibrationEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0,100,100,100,100,300), intArrayOf(0,255,0,255,0,255), -1))
                        else @Suppress("DEPRECATION") vibrator.vibrate(500)
                    }
                    if (isSoundEnabled) toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 400)
                }
                is BatchEvent.NearlyDone -> {
                    if (isVibrationEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0,80,80,80), intArrayOf(0,200,0,200), -1))
                        else @Suppress("DEPRECATION") vibrator.vibrate(200)
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.scanEvent.collect { event ->
            if (event is ScanEvent.Duplicate && isSoundEnabled) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100); delay(150); toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        if (activeTab != ActiveTab.WAREHOUSE) {
            SessionStatsRow(sessionSeconds = sessionSeconds, duplicateCount = duplicateCount, scanRate = scanRate, isBatchActive = batchConfig.isActive, onBatchClick = { showBatchSetupSheet = true })
        }

        AnimatedVisibility(visible = batchConfig.isActive, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            BatchProgressBar(config = batchConfig, currentCount = viewModel.getBatchProgress().first, onCancelBatch = { viewModel.clearBatchMode() })
        }

        // ── Табы ─────────────────────────────────────────────────────
        val tabs = listOf(
            "Самокаты${if (scooterCount > 0) " · $scooterCount" else ""}",
            "Склад",
            "WIND 4.0${if (batteryCount > 0) " · $batteryCount" else ""}",
            "WIND 5.0${if (newBatteryCount > 0) " · $newBatteryCount" else ""}"
        )
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor   = Color.Transparent,
            contentColor     = StardustTextPrimary,
            edgePadding      = 0.dp,
            indicator        = { tabPositions ->
                if (activeTab.ordinal < tabPositions.size) {
                    val indicatorColor = when (activeTab) {
                        ActiveTab.NEW_BATTERIES -> ColorWind50
                        ActiveTab.BATTERIES     -> ColorWind40
                        else                    -> StardustPrimary
                    }
                    Box(modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]).height(3.dp).background(indicatorColor, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                }
            },
            divider = { HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f)) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab.ordinal == index,
                    onClick  = { hapticManager.performClick(hapticFeedback, scope); viewModel.onTabSelected(ActiveTab.values()[index]); manualInputText = "" },
                    text     = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        // ── Статус-строка ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = statusMessage != "Наведите камеру на QR-код" && statusMessage != "РЕЖИМ ПОИСКА",
            enter = fadeIn() + expandVertically(),
            exit  = fadeOut() + shrinkVertically()
        ) {
            val isError = statusMessage.startsWith("Ошибка") || statusMessage.contains("уже в списке") || statusMessage.contains("не найден")
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(if (isError) StardustError.copy(alpha = 0.18f) else StardustSuccess.copy(alpha = 0.18f))
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isError) StardustError else StardustSuccess))
                    Text(statusMessage, color = if (isError) StardustError else StardustSuccess, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // ── Ручной ввод ───────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!isSearchMode) {
                val manualAvailable = activeTab != ActiveTab.WAREHOUSE
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StardustGlassBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            enabled           = manualAvailable
                        ) { isManualInputExpanded = !isManualInputExpanded }
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        val color = if (manualAvailable) StardustTextPrimary else StardustTextSecondary.copy(alpha = 0.3f)
                        Icon(Icons.Outlined.Keyboard, null, tint = color, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Ручной ввод", color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Icon(if (isManualInputExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = StardustTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            AnimatedVisibility(
                visible = isInputVisible && activeTab != ActiveTab.WAREHOUSE,
                enter = expandVertically() + fadeIn(),
                exit  = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
                    // Чипы типа батареи
                    if (activeTab == ActiveTab.BATTERIES && !isSearchMode) {
                        Row(modifier = Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("FUJIAN", "BYD").forEach { brand ->
                                BatteryTypeChip(
                                    label    = brand,
                                    selected = selectedManufacturer == brand,
                                    color    = if (brand == "BYD") ColorByd else ColorFujian,
                                    onClick  = { selectedManufacturer = brand }
                                )
                            }
                        }
                    }
                    if (activeTab == ActiveTab.NEW_BATTERIES && !isSearchMode) {
                        Row(modifier = Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("NEW" to "Новые (5BB)", "OLD" to "Старые (SF)").forEach { (type, label) ->
                                BatteryTypeChip(
                                    label    = label,
                                    selected = selectedWind50Subtype == type,
                                    color    = if (type == "NEW") ColorWind50 else ColorWind50Old,
                                    onClick  = { selectedWind50Subtype = type }
                                )
                            }
                        }
                    }

                    // Поле ввода
                    val prefix = when {
                        isSearchMode -> ""
                        activeTab == ActiveTab.BATTERIES -> if (selectedManufacturer == "BYD") "4BZ" else "4BB"
                        activeTab == ActiveTab.NEW_BATTERIES -> if (selectedWind50Subtype == "OLD") "SF" else "5BB"
                        else -> ""
                    }
                    val activeColor = when {
                        isSearchMode -> StardustWarning
                        activeTab == ActiveTab.NEW_BATTERIES && selectedWind50Subtype == "OLD" -> ColorWind50Old
                        activeTab == ActiveTab.NEW_BATTERIES -> ColorWind50
                        activeTab == ActiveTab.BATTERIES -> ColorWind40
                        else -> StardustPrimary
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Кастомное поле ввода
                        Box(
                            modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(12.dp))
                                .background(StardustGlassBg)
                                .drawBehind {
                                    drawRoundRect(
                                        color        = activeColor.copy(alpha = 0.5f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                                        style        = Stroke(1.5.dp.toPx())
                                    )
                                }
                        ) {
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (prefix.isNotEmpty() && !isSearchMode) {
                                    Text(prefix, color = activeColor.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Box(modifier = Modifier.width(1.dp).height(18.dp).background(activeColor.copy(alpha = 0.3f)))
                                    Spacer(Modifier.width(8.dp))
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value         = manualInputText,
                                    onValueChange = { newVal ->
                                        manualInputText = when {
                                            activeTab == ActiveTab.SCOOTERS -> newVal.filter { it.isLetterOrDigit() }.uppercase()
                                            activeTab == ActiveTab.NEW_BATTERIES && selectedWind50Subtype == "OLD" -> newVal.filter { it.isLetterOrDigit() }.uppercase()
                                            else -> newVal.filter { it.isDigit() || it.isLetter() }.uppercase()
                                        }
                                    },
                                    modifier      = Modifier.weight(1f),
                                    singleLine    = true,
                                    textStyle     = androidx.compose.ui.text.TextStyle(color = StardustTextPrimary, fontSize = 15.sp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType   = if (activeTab == ActiveTab.SCOOTERS || (activeTab == ActiveTab.NEW_BATTERIES && selectedWind50Subtype == "OLD")) KeyboardType.Text else KeyboardType.Number,
                                        imeAction      = ImeAction.Done,
                                        capitalization = if (activeTab == ActiveTab.SCOOTERS || (activeTab == ActiveTab.NEW_BATTERIES && selectedWind50Subtype == "OLD")) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                                        autoCorrect    = false
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (manualInputText.isNotBlank()) {
                                            viewModel.addManualCode(if (prefix.isNotEmpty() && !isSearchMode) prefix + manualInputText else manualInputText)
                                            focusManager.clearFocus()
                                        }
                                    }),
                                    cursorBrush = Brush.verticalGradient(listOf(activeColor, activeColor)),
                                    decorationBox = { inner ->
                                        Box {
                                            if (manualInputText.isEmpty()) Text(
                                                if (isSearchMode) "Поиск по номеру..."
                                                else when (activeTab) {
                                                    ActiveTab.SCOOTERS -> "Номер (напр. HE600A)..."
                                                    ActiveTab.NEW_BATTERIES -> if (selectedWind50Subtype == "OLD") "Буквы + цифры..." else "11 цифр..."
                                                    else -> "11 цифр после префикса..."
                                                },
                                                color = StardustTextSecondary.copy(alpha = 0.4f), fontSize = 15.sp
                                            )
                                            inner()
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        // Кнопка добавить
                        val canAdd = manualInputText.isNotBlank()
                        val btnAlpha by animateFloatAsState(if (canAdd) 1f else 0.4f, tween(200), label = "add_btn")
                        Box(
                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(activeColor.copy(alpha = btnAlpha), activeColor.copy(alpha = btnAlpha * 0.7f))))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                    enabled           = canAdd
                                ) {
                                    viewModel.addManualCode(if (prefix.isNotEmpty() && !isSearchMode) prefix + manualInputText else manualInputText)
                                    focusManager.clearFocus()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (isSearchMode) Icons.Default.Search else Icons.Default.Add, null, tint = Color.Black.copy(alpha = btnAlpha), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = StardustItemBg.copy(alpha = 0.4f))

        // ── Список / Склад ────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).animateContentSize()) {
            if (isNumberMode) {
                com.example.qrscannerapp.features.scanner.ui.components.NumberModePanel(
                    items             = numberItems,
                    expandedCode      = expandedStickerCode,
                    onExpandToggle    = { code -> viewModel.expandSticker(code) },
                    onDirectionToggle = { code, dir -> viewModel.toggleDirection(code, dir) },
                    onRemoveItem      = { code -> viewModel.removeNumberItem(code) },
                    onSelectAllDirections = { code -> viewModel.selectAllDirections(code) },
                    onSendToStorage   = { onNavigateToStorage() }
                )
            } else when (activeTab) {
                ActiveTab.SCOOTERS, ActiveTab.BATTERIES, ActiveTab.NEW_BATTERIES -> {
                    if (currentList.isEmpty()) {
                        EmptyState(text = if (isSearchMode) "Введите код для поиска" else "Список пуст")
                    } else {
                        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(items = currentList, key = { it.id }) { item ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                            if (isVibrationEnabled) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                                                else @Suppress("DEPRECATION") vibrator.vibrate(30)
                                            }
                                            viewModel.removeCode(item); true
                                        } else false
                                    }
                                )
                                SwipeToDismissBox(
                                    state             = dismissState,
                                    backgroundContent = {
                                        val color by animateColorAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) StardustError else Color.Transparent, label = "")
                                        Box(Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                                            Icon(Icons.Outlined.Delete, null, tint = Color.White)
                                        }
                                    },
                                    content = { ScanListItem(item = item, isNew = item.id in newItems, onItemShown = { viewModel.markAsOld(item) }) }
                                )
                            }
                        }
                    }

                    if (!isSearchMode && currentList.isNotEmpty()) {
                        val fabColor = when (activeTab) {
                            ActiveTab.NEW_BATTERIES -> ColorWind50
                            ActiveTab.BATTERIES     -> ColorWind40
                            else                    -> StardustPrimary
                        }
                        // FAB действия
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(fabColor, fabColor.copy(alpha = 0.7f))))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showExportSheet = true }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Share, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Text("Действия (${currentList.size})", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        // Кнопка очистить
                        Box(
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).size(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(StardustGlassBg)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.clearList() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Delete, null, tint = StardustError.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
                ActiveTab.WAREHOUSE -> WarehouseScreen(viewModel = warehouseViewModel)
            }
        }
    }

    // Шторки и диалоги
    if (showBatchSetupSheet) {
        BatchSetupSheet(
            currentConfig = batchConfig,
            sheetState    = batchSheetState,
            onDismiss     = { scope.launch { batchSheetState.hide() }.invokeOnCompletion { showBatchSetupSheet = false } },
            onConfirm     = { config -> viewModel.setBatchConfig(config); scope.launch { batchSheetState.hide() }.invokeOnCompletion { showBatchSetupSheet = false } }
        )
    }
    if (showExportSheet) {
        ExportSheet(
            listToExport  = currentList,
            sheetState    = exportSheetState,
            activeTab     = activeTab,
            onDismiss     = { showExportSheet = false },
            onCopyAll     = { list ->
                viewModel.logActivity("COPY_ALL")
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("All Codes", list.joinToString("\n") { it.code }))
                Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) showExportSheet = false }
            },
            onShare       = { list ->
                context.startActivity(Intent.createChooser(Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, list.joinToString("\n") { it.code }); type = "text/plain" }, null))
                scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) showExportSheet = false }
            },
            onSaveSession = { scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) { showExportSheet = false; showSaveSessionDialog = true } } },
            onSort        = { viewModel.sortCurrentList(); scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) showExportSheet = false } },
            onNavigateToPalletDistribution = { scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) { showExportSheet = false; onNavigateToPalletDistribution() } } },
            onNavigateToStorage = { scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) { showExportSheet = false; onNavigateToStorage() } } }
        )
    }
    if (showSaveSessionDialog) {
        val isSaving by viewModel.isSavingSession.collectAsState()
        SaveSessionDialog(isSaving = isSaving, onDismissRequest = { if (!isSaving) showSaveSessionDialog = false }, onSave = { sessionName -> viewModel.saveCurrentSession(sessionName) })
    }
    recentlySavedSession?.let { savedSession ->
        SessionSavedDialog(savedSession = savedSession, onDismiss = { viewModel.onSessionSaveDialogDismissed() }, onNavigateToHistory = { viewModel.onSessionSaveDialogDismissed(); onNavigateToHistory() })
    }
}

// ============================================================================================
// ЧИП ТИПА БАТАРЕИ
// ============================================================================================

@Composable
private fun BatteryTypeChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1f else 0.96f, spring(0.65f, 600f), label = "chip")
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.2f) else StardustGlassBg)
            .drawBehind {
                if (selected) drawRoundRect(color = color.copy(alpha = 0.6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) color else StardustTextSecondary, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}