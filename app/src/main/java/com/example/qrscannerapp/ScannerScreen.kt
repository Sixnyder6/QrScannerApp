package com.example.qrscannerapp

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
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
// --- ИСПРАВЛЕННЫЕ ИМПОРТЫ АНИМАЦИИ ---
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState // Находится здесь
import androidx.compose.animation.core.animateFloatAsState // Находится здесь
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
// -------------------------------------
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel // <-- ИЗМЕНЕНИЕ: Добавлен импорт Hilt
import com.example.qrscannerapp.core.model.ActiveTab
import com.example.qrscannerapp.core.model.ScanEvent
import com.example.qrscannerapp.core.model.SessionType
import com.example.qrscannerapp.core.model.UiEffect
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseScreen // <-- ИЗМЕНЕНИЕ: Импорт нового экрана
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseViewModel // <-- ИЗМЕНЕНИЕ: Импорт новой ViewModel
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem
import com.example.qrscannerapp.features.scanner.domain.model.ScanSession
import com.example.qrscannerapp.features.scanner.ui.components.ScooterSearchResultDialog
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

// --- ЦВЕТА ---
val StardustGlassBg = Color(0xBF1A1A1D)
val StardustSolidBg = Color(0xFF1A1A1D)
val StardustItemBg = Color(0x14FFFFFF)
val StardustPrimary = Color(0xFF6A5AE0)
val StardustSecondary = Color(0xFF8A7DFF)
val StardustTextPrimary = Color.White
val StardustTextSecondary = Color(0xFFA0A0A5)
val StardustModalBg = Color(0xFF2a2a2e)
val StardustSuccess = Color(0xFF4CAF50)
val StardustError = Color(0xFFF44336)
val StardustWarning = Color(0xFFFFC107)

// Цвета производителей
val ColorFujian = Color(0xFFFF8A65)
val ColorByd = Color(0xFF4FC3F7)


// <-- ИЗМЕНЕНИЕ: ВАЖНО! Не забудьте обновить сам enum class ActiveTab
// в файле com/example/qrscannerapp/core/model/CoreEnums.kt
// enum class ActiveTab { SCOOTERS, WAREHOUSE, BATTERIES }


@Composable
fun StardustScreen(
    viewModel: QrScannerViewModel,
    onMenuClick: () -> Unit,
    hapticManager: HapticFeedbackManager,
    view: View,
    onNavigateToPalletDistribution: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToVisualRepair: (String) -> Unit = {}
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    val isSearchMode by viewModel.isSearchMode.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val scooterSearchResult by viewModel.scooterSearchResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val scanEventFlow = viewModel.scanEvent

    // <-- ИЗМЕНЕНИЕ: Получаем ViewModel для склада и текущую активную вкладку здесь
    val warehouseViewModel: WarehouseViewModel = hiltViewModel()
    val activeTab by viewModel.activeTab.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .background(StardustSolidBg)) {

        // 1. ВЕРХНИЙ БЛОК: КАМЕРА (40% высоты)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Color.Black)
        ) {
            CameraView(
                isSearchMode = isSearchMode,
                hasPermission = hasCameraPermission,
                scanEventFlow = scanEventFlow,
                // <-- ИЗМЕНЕНИЕ: Теперь мы решаем, какую ViewModel вызвать, на основе активной вкладки
                onCodeScanned = { code ->
                    when (activeTab) {
                        ActiveTab.WAREHOUSE -> warehouseViewModel.onPartScanned(code)
                        else -> viewModel.onCodeScanned(code)
                    }
                },
                onStatusUpdate = { msg, isErr -> viewModel.updateStatus(msg, isErr) }
            )

            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.Menu, "Меню", tint = Color.White)
            }

            IconButton(
                onClick = { viewModel.toggleSearchMode() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(if(isSearchMode) StardustWarning else Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Search,
                    "Поиск",
                    tint = if(isSearchMode) Color.Black else Color.White
                )
            }

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = StardustWarning)
                }
            }
        }

        // 2. НИЖНИЙ БЛОК: ИНТЕРФЕЙС (60% высоты)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .background(StardustSolidBg)
        ) {
            ScannerOverlayUi(
                viewModel = viewModel,
                warehouseViewModel = warehouseViewModel, // <-- ИЗМЕНЕНИЕ: Передаем ViewModel склада
                hapticManager = hapticManager,
                view = view,
                isSearchMode = isSearchMode,
                onNavigateToPalletDistribution = onNavigateToPalletDistribution,
                onNavigateToStorage = onNavigateToStorage,
                onNavigateToHistory = onNavigateToHistory
            )
        }
    }

    if (searchResult != null) {
        SearchResultDialog(
            result = searchResult!!,
            onDismiss = { viewModel.clearSearchResult() },
            onNavigateToPallet = {
                viewModel.setHighlightedPallet(searchResult!!.palletId)
                viewModel.clearSearchResult()
                onNavigateToPalletDistribution()
            }
        )
    }

    if (scooterSearchResult != null) {
        val number = scooterSearchResult!!.first
        ScooterSearchResultDialog(
            scooterNumber = number,
            locationName = scooterSearchResult!!.second,
            lastUser = "Система",
            onDismiss = { viewModel.clearScooterSearchResult() },
            onNavigate = {
                viewModel.clearScooterSearchResult()
                onNavigateToStorage()
            },
            onOpen3D = {
                onNavigateToVisualRepair(number)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScannerOverlayUi(
    viewModel: QrScannerViewModel,
    warehouseViewModel: WarehouseViewModel, // <-- ИЗМЕНЕНИЕ: Принимаем ViewModel склада
    hapticManager: HapticFeedbackManager,
    view: View,
    isSearchMode: Boolean,
    onNavigateToPalletDistribution: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val hapticFeedback = LocalHapticFeedback.current

    val settingsManager = remember { SettingsManager(context) }
    val isSoundEnabled by settingsManager.isSoundEnabledFlow.collectAsState(initial = true)
    val isVibrationEnabled by settingsManager.isVibrationEnabledFlow.collectAsState(initial = true)

    val activeTab by viewModel.activeTab.collectAsState()
    val newItems by viewModel.newItems.collectAsState()

    var isManualInputExpanded by remember { mutableStateOf(false) }
    val isInputVisible = isManualInputExpanded || isSearchMode

    // <-- ИЗМЕНЕНИЕ: Эта переменная теперь используется только для старых вкладок
    val currentList = when (activeTab) {
        ActiveTab.SCOOTERS -> viewModel.scooterCodes
        ActiveTab.BATTERIES -> viewModel.batteryCodes
        else -> emptyList() // Для WAREHOUSE список будет браться из его ViewModel
    }

    val exportSheetState = rememberModalBottomSheetState()
    var showExportSheet by remember { mutableStateOf(false) }
    var showSaveSessionDialog by remember { mutableStateOf(false) }

    val recentlySavedSession by viewModel.recentlySavedSession.collectAsState()

    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    val listState = rememberLazyListState()

    var manualInputText by remember { mutableStateOf("") }
    var selectedManufacturer by remember { mutableStateOf("FUJIAN") }

    LaunchedEffect(key1 = currentList.firstOrNull()) {
        if (currentList.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scanEffect.collect { effect ->
            when (effect) {
                is UiEffect.ScanSuccess -> {
                    if (isSoundEnabled) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    if (isVibrationEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION") vibrator.vibrate(50)
                        }
                    }
                    manualInputText = ""
                }
                is UiEffect.SessionSaved -> hapticManager.performConfirm(hapticFeedback, scope)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // <-- ИЗМЕНЕНИЕ: Добавлена вкладка "Склад"
        val tabs = listOf("Самокаты", "Склад", "АКБ")
        TabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = StardustTextPrimary,
            indicator = { tabPositions ->
                // <-- ИЗМЕНЕНИЕ: Добавлена проверка, чтобы избежать IndexOutOfBoundsException
                if (activeTab.ordinal < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[activeTab.ordinal])
                            .height(3.dp)
                            .background(StardustPrimary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                }
            },
            divider = { HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f)) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab.ordinal == index,
                    onClick = {
                        hapticManager.performClick(hapticFeedback, scope)
                        viewModel.onTabSelected(ActiveTab.values()[index])
                        manualInputText = ""
                    },
                    text = { Text(title) }
                )
            }
        }

        // 2. ВСТРОЕННАЯ ПАНЕЛЬ ВВОДА (СКРЫВАЕМАЯ)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StardustSolidBg)
        ) {
            if (!isSearchMode) {
                // <-- ИЗМЕНЕНИЕ: Панель ручного ввода скрыта на вкладке Склад (пока что)
                val manualInputAvailable = activeTab != ActiveTab.WAREHOUSE

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = manualInputAvailable) { isManualInputExpanded = !isManualInputExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val color = if (manualInputAvailable) StardustTextSecondary else StardustTextSecondary.copy(alpha = 0.3f)
                        Icon(Icons.Outlined.Keyboard, null, tint = color)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ручной ввод", color = color, fontWeight = FontWeight.Medium)
                    }
                    Icon(
                        imageVector = if(isManualInputExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = StardustTextSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isInputVisible && activeTab != ActiveTab.WAREHOUSE, // <-- ИЗМЕНЕНИЕ: Скрываем для склада
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                ) {
                    if (activeTab == ActiveTab.BATTERIES && !isSearchMode) {
                        Row(modifier = Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("FUJIAN", "BYD").forEach { brand ->
                                FilterChip(
                                    selected = selectedManufacturer == brand,
                                    onClick = { selectedManufacturer = brand },
                                    label = { Text(brand) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StardustPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = StardustItemBg,
                                        labelColor = StardustTextSecondary
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val prefix = if (activeTab == ActiveTab.BATTERIES) {
                            if (selectedManufacturer == "FUJIAN") "4BB" else "4BZ"
                        } else ""

                        val activeColor = if (isSearchMode) StardustWarning else StardustPrimary
                        val containerColor = if (isSearchMode) StardustWarning.copy(alpha = 0.1f) else StardustItemBg

                        OutlinedTextField(
                            value = manualInputText,
                            onValueChange = { manualInputText = it.filter { char -> char.isDigit() } },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    if (isSearchMode) "Поиск по номеру..."
                                    else if (activeTab == ActiveTab.SCOOTERS) "Номер самоката..."
                                    else "Введите цифры..."
                                )
                            },
                            prefix = if (prefix.isNotEmpty() && !isSearchMode) { { Text(prefix, color = StardustTextSecondary, fontWeight = FontWeight.Bold) } } else null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (manualInputText.isNotBlank()) {
                                    val fullCode = if (prefix.isNotEmpty() && !isSearchMode) prefix + manualInputText else manualInputText
                                    viewModel.addManualCode(fullCode) // <-- ИЗМЕНЕНИЕ: В будущем здесь тоже понадобится роутинг
                                    focusManager.clearFocus()
                                }
                            }),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeColor,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = containerColor,
                                unfocusedContainerColor = StardustItemBg,
                                cursorColor = activeColor,
                                focusedTextColor = StardustTextPrimary,
                                unfocusedTextColor = StardustTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = {
                                if (manualInputText.isNotBlank()) {
                                    val fullCode = if (prefix.isNotEmpty() && !isSearchMode) prefix + manualInputText else manualInputText
                                    viewModel.addManualCode(fullCode) // <-- ИЗМЕНЕНИЕ: В будущем здесь тоже понадобится роутинг
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = activeColor),
                            enabled = manualInputText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = if (isSearchMode) Icons.Default.Search else Icons.Default.Add,
                                contentDescription = if (isSearchMode) "Найти" else "Добавить",
                                tint = if (isSearchMode) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = StardustItemBg)

        // 3. СПИСОК СКАНИРОВАНИЯ
        Box(modifier = Modifier.weight(1f)) {
            // <-- ИЗМЕНЕНИЕ: Главный роутер контента
            when (activeTab) {
                ActiveTab.SCOOTERS, ActiveTab.BATTERIES -> {
                    // --- Старая логика для самокатов и АКБ ---
                    if (currentList.isEmpty()) {
                        EmptyState(text = if (isSearchMode) "Введите код для поиска" else "Список пуст")
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(items = currentList, key = { it.id }) { item ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.removeCode(item)
                                            true
                                        } else false
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) StardustError else Color.Transparent, label = ""
                                        )
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(color)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White)
                                        }
                                    },
                                    content = {
                                        ScanListItem(
                                            item = item,
                                            isNew = item.id in newItems,
                                            onItemShown = { viewModel.markAsOld(item) }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (!isSearchMode && currentList.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            text = { Text("Действия (${currentList.size})", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Share, null) },
                            onClick = { showExportSheet = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            containerColor = StardustPrimary,
                            contentColor = Color.White
                        )
                    }

                    if (!isSearchMode && currentList.isNotEmpty()) {
                        SmallFloatingActionButton(
                            onClick = { viewModel.clearList() },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            containerColor = StardustItemBg,
                            contentColor = StardustTextSecondary
                        ) {
                            Icon(Icons.Outlined.Delete, "Очистить")
                        }
                    }
                }
                ActiveTab.WAREHOUSE -> {
                    // --- Новый контент для склада ---
                    WarehouseScreen(viewModel = warehouseViewModel)
                }
            }
        }
    }

    if (showExportSheet) {
        ExportSheet(
            listToExport = currentList,
            sheetState = exportSheetState,
            activeTab = activeTab,
            onDismiss = { showExportSheet = false },
            onCopyAll = { list ->
                viewModel.logActivity("COPY_ALL")
                val allCodes = list.joinToString("\n") { it.code }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("All Codes", allCodes))
                Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) showExportSheet = false }
            },
            onShare = { list ->
                val allCodes = list.joinToString("\n") { it.code }
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, allCodes)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
                scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) showExportSheet = false }
            },
            onSaveSession = {
                scope.launch { exportSheetState.hide() }.invokeOnCompletion {
                    if (!exportSheetState.isVisible) {
                        showExportSheet = false
                        showSaveSessionDialog = true
                    }
                }
            },
            onNavigateToPalletDistribution = {
                scope.launch { exportSheetState.hide() }.invokeOnCompletion {
                    if (!exportSheetState.isVisible) {
                        showExportSheet = false
                        onNavigateToPalletDistribution()
                    }
                }
            },
            onNavigateToStorage = {
                scope.launch { exportSheetState.hide() }.invokeOnCompletion {
                    if (!exportSheetState.isVisible) {
                        showExportSheet = false
                        onNavigateToStorage()
                    }
                }
            }
        )
    }

    if (showSaveSessionDialog) {
        val isSaving by viewModel.isSavingSession.collectAsState()
        SaveSessionDialog(
            isSaving = isSaving,
            onDismissRequest = { if (!isSaving) showSaveSessionDialog = false },
            onSave = { sessionName ->
                viewModel.saveCurrentSession(sessionName)
            }
        )
    }

    recentlySavedSession?.let { savedSession ->
        SessionSavedDialog(
            savedSession = savedSession,
            onDismiss = {
                viewModel.onSessionSaveDialogDismissed()
            },
            onNavigateToHistory = {
                viewModel.onSessionSaveDialogDismissed()
                onNavigateToHistory()
            }
        )
    }
}

@Composable
fun CameraView(
    isSearchMode: Boolean,
    hasPermission: Boolean,
    scanEventFlow: Flow<ScanEvent>,
    onCodeScanned: (String) -> Unit,
    onStatusUpdate: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    var borderColorTarget by remember { mutableStateOf(Color.White.copy(alpha = 0.8f)) }
    var scaleTarget by remember { mutableStateOf(1f) }

    val targetBorderColor = if (isSearchMode) StardustWarning else borderColorTarget

    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 400),
        label = "Border Color Animation"
    )
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = tween(durationMillis = 200),
        label = "Border Scale Animation"
    )

    LaunchedEffect(Unit) {
        scanEventFlow.collect { event ->
            launch {
                val (color, newScale) = when (event) {
                    ScanEvent.Success -> StardustSuccess to 1.05f
                    ScanEvent.Duplicate -> StardustError to 1.05f
                }
                borderColorTarget = color
                scaleTarget = newScale
                delay(250)
                borderColorTarget = Color.White.copy(alpha = 0.8f)
                scaleTarget = 1f
            }
        }
    }

    LaunchedEffect(isTorchOn, camera) {
        try {
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                camera?.cameraControl?.enableTorch(isTorchOn)
            }
        } catch (e: Exception) {
            Log.e("CameraView", "Torch error", e)
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView.setOnTouchListener { _, event ->
                        val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        camera?.cameraControl?.startFocusAndMetering(action)
                        true
                    }
                    previewView
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProvider = cameraProviderFuture.get()
                    try {
                        cameraProvider.unbindAll()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                val analyzer = QrCodeAnalyzer(
                                    onCodeScanned = { code -> onCodeScanned(code) },
                                    onStatusUpdate = { message -> onStatusUpdate(message, true) }
                                )
                                it.setAnalyzer(cameraExecutor, analyzer)
                            }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)

                        if (camera!!.cameraInfo.hasFlashUnit()) {
                            camera!!.cameraControl.enableTorch(isTorchOn)
                        }

                    } catch (e: Exception) {
                        Log.e("CameraView", "Camera bind error", e)
                    }
                }
            )

            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)))

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.Center)
                    .scale(scale)
                    .border(3.dp, borderColor, RoundedCornerShape(24.dp))
                    .background(Color.Transparent)
            )

            if (isSearchMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .background(StardustWarning.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("РЕЖИМ ПОИСКА", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(
                onClick = { isTorchOn = !isTorchOn },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    contentDescription = "Flashlight",
                    tint = if (isTorchOn) StardustWarning else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет доступа к камере", color = StardustTextPrimary)
            }
        }
    }
}

// ИСПРАВЛЕННЫЙ ЭЛЕМЕНТ СПИСКА: Бейдж перенесен влево
@Composable
fun ScanListItem(
    item: ScanItem,
    isNew: Boolean,
    modifier: Modifier = Modifier,
    onItemShown: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isNew) StardustPrimary.copy(alpha = 0.3f) else StardustSolidBg,
        animationSpec = tween(durationMillis = 500),
        label = "BgAnim"
    )

    LaunchedEffect(isNew) {
        if (isNew) {
            delay(1500L)
            onItemShown()
        }
    }

    val manufacturer = when {
        item.code.startsWith("4BB") -> "FUJIAN"
        item.code.startsWith("4BZ") -> "BYD"
        else -> null
    }

    Column(modifier = modifier
        .fillMaxWidth()
        .background(backgroundColor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
            // Убрали Arrangement.SpaceBetween, чтобы все элементы шли подряд слева направо
        ) {
            Icon(Icons.Default.QrCode2, null, tint = StardustSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.code,
                color = StardustTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            // Бейдж теперь здесь, сразу после текста
            if (manufacturer != null) {
                Spacer(modifier = Modifier.width(12.dp)) // Отступ от номера
                val badgeColor = if (manufacturer == "FUJIAN") ColorFujian else ColorByd
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = manufacturer,
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        HorizontalDivider(color = StardustItemBg, thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
fun EmptyState(text: String = "Ожидание сканирования...", modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.QrCodeScanner, null, tint = StardustItemBg, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text, color = StardustTextSecondary, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    listToExport: List<ScanItem>,
    sheetState: SheetState,
    activeTab: ActiveTab,
    onDismiss: () -> Unit,
    onCopyAll: (List<ScanItem>) -> Unit,
    onShare: (List<ScanItem>) -> Unit,
    onSaveSession: () -> Unit,
    onNavigateToPalletDistribution: () -> Unit,
    onNavigateToStorage: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Экспорт данных", color = StardustTextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            if (activeTab == ActiveTab.BATTERIES) {
                Button(
                    onClick = onNavigateToPalletDistribution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustSuccess.copy(alpha = 0.3f), contentColor = StardustSuccess)
                ) {
                    Text("Приемка на склад", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (activeTab == ActiveTab.SCOOTERS) {
                Button(
                    onClick = onNavigateToStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustSuccess.copy(alpha = 0.3f), contentColor = StardustSuccess)
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = "Хранение")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отправить на хранение", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = onSaveSession, modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustSecondary)) {
                Text("Сохранить сессию в историю", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { onCopyAll(listToExport) }, modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                Text("Копировать в буфер обмена", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { onShare(listToExport) }, modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                Text("Поделиться (как текст)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)
            ) {
                Text("Отмена", color = StardustTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SearchResultDialog(
    result: BatterySearchResult,
    onDismiss: () -> Unit,
    onNavigateToPallet: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = StardustSuccess, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("АКБ Найден!", style = MaterialTheme.typography.headlineSmall, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StardustItemBg, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(label = "Серийный номер", value = result.batteryCode)
                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.2f))
                    InfoRow(label = "Производитель", value = result.manufacturer)
                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.2f))
                    InfoRow(label = "Палет №", value = result.palletNumber.toString(), valueColor = StardustPrimary)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToPallet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Перейти к палету", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss) { Text("Закрыть", color = StardustTextSecondary) }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = StardustTextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = StardustTextSecondary, fontSize = 14.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun SaveSessionDialog(isSaving: Boolean, onDismissRequest: () -> Unit, onSave: (name: String?) -> Unit) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismissRequest) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Сохранить сессию", color = StardustTextPrimary, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название...") }, singleLine = true, enabled = !isSaving,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = StardustItemBg, unfocusedContainerColor = StardustItemBg,
                        focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary,
                        cursorColor = StardustPrimary, focusedIndicatorColor = StardustPrimary, unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismissRequest, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)) {
                        Text("Отмена", color = StardustTextSecondary)
                    }
                    Button(onClick = { onSave(text) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun SessionSavedDialog(savedSession: ScanSession, onDismiss: () -> Unit, onNavigateToHistory: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onNavigateToHistory, colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) { Text("В историю") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = StardustTextSecondary) } },
        title = { Text("Сохранено!", color = StardustTextPrimary) },
        text = { Text("Сессия успешно сохранена.", color = StardustTextSecondary) },
        containerColor = StardustModalBg
    )
}

class QrCodeAnalyzer(
    private val onCodeScanned: (String) -> Unit,
    private val onStatusUpdate: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private var isProcessing = false
    private var lastAnalyzedTimestamp = 0L
    private val THROTTLE_DURATION = 300L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (isProcessing || (currentTimestamp - lastAnalyzedTimestamp < THROTTLE_DURATION)) {
            imageProxy.close(); return
        }
        isProcessing = true
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            BarcodeScanning.getClient(options).process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstNotNullOfOrNull { it.rawValue }?.let {
                        onCodeScanned(it)
                        lastAnalyzedTimestamp = System.currentTimeMillis()
                    }
                }
                .addOnFailureListener { Log.e("QrCodeAnalyzer", "Error", it) }
                .addOnCompleteListener { isProcessing = false; imageProxy.close() }
        } else { isProcessing = false; imageProxy.close() }
    }
}