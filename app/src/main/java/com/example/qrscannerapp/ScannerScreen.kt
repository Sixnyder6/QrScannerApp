// File: features/scanner/ui/ScannerScreen.kt

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
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

import com.example.qrscannerapp.core.model.ActiveTab
import com.example.qrscannerapp.core.model.ScanEvent
import com.example.qrscannerapp.core.model.SessionType
import com.example.qrscannerapp.core.model.UiEffect
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem
import com.example.qrscannerapp.features.scanner.domain.model.ScanSession

val StardustGlassBg = Color(0xBF1A1A1D)
val StardustItemBg = Color(0x14FFFFFF)
val StardustPrimary = Color(0xFF6A5AE0)
val StardustSecondary = Color(0xFF8A7DFF)
val StardustTextPrimary = Color.White
val StardustTextSecondary = Color(0xFFA0A0A5)
val StardustModalBg = Color(0xFF2a2a2e)
val StardustSuccess = Color(0xFF4CAF50)
val StardustError = Color(0xFFF44336)
val StardustWarning = Color(0xFFFFC107) // Желтый для поиска

@Composable
fun StardustScreen(
    viewModel: QrScannerViewModel,
    onMenuClick: () -> Unit,
    hapticManager: HapticFeedbackManager,
    view: View,
    onNavigateToPalletDistribution: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    val scanMode by viewModel.scanMode.collectAsState()
    val isSearchMode by viewModel.isSearchMode.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val scanEventFlow = viewModel.scanEvent

    Box(modifier = Modifier.fillMaxSize()) {
        CameraView(
            scanMode = scanMode,
            isSearchMode = isSearchMode,
            hasPermission = hasCameraPermission,
            scanEventFlow = scanEventFlow,
            onCodeScanned = viewModel::onCodeScanned,
            onTextFound = viewModel::onTextFound,
            onStatusUpdate = { msg, isErr -> viewModel.updateStatus(msg, isErr) },
            onToggleScanMode = viewModel::toggleScanMode
        )

        ScannerOverlayUi(
            viewModel = viewModel,
            onMenuClick = onMenuClick,
            hapticManager = hapticManager,
            view = view,
            isSearchMode = isSearchMode,
            onNavigateToPalletDistribution = onNavigateToPalletDistribution,
            onNavigateToStorage = onNavigateToStorage,
            onNavigateToHistory = onNavigateToHistory
        )

        // Диалог результата поиска
        if (searchResult != null) {
            SearchResultDialog(
                result = searchResult!!,
                onDismiss = { viewModel.clearSearchResult() },
                onNavigateToPallet = {
                    // --- ВАЖНОЕ ИЗМЕНЕНИЕ: Передаем ID палета для подсветки ---
                    viewModel.setHighlightedPallet(searchResult!!.palletId)
                    viewModel.clearSearchResult()
                    onNavigateToPalletDistribution()
                }
            )
        }

        // Индикатор загрузки поиска
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScannerOverlayUi(
    viewModel: QrScannerViewModel,
    onMenuClick: () -> Unit,
    hapticManager: HapticFeedbackManager,
    view: View,
    isSearchMode: Boolean,
    onNavigateToPalletDistribution: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    val settingsManager = remember { SettingsManager(context) }
    val isSoundEnabled by settingsManager.isSoundEnabledFlow.collectAsState(initial = true)
    val isVibrationEnabled by settingsManager.isVibrationEnabledFlow.collectAsState(initial = true)

    val activeTab by viewModel.activeTab.collectAsState()
    val newItems by viewModel.newItems.collectAsState()

    val currentList = when (activeTab) {
        ActiveTab.SCOOTERS -> viewModel.scooterCodes
        ActiveTab.BATTERIES -> viewModel.batteryCodes
    }

    var showManualInputDialog by remember { mutableStateOf(false) }
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
    val itemsBeingDeleted = remember { mutableStateListOf<String>() }

    LaunchedEffect(key1 = currentList.firstOrNull()) {
        if (currentList.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scanEffect.collect { effect ->
            when (effect) {
                is UiEffect.ScanSuccess -> {
                    if (isSoundEnabled) {
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    }
                    if (isVibrationEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION") vibrator.vibrate(50)
                        }
                    }
                }
                is UiEffect.SessionSaved -> {
                    hapticManager.performConfirm(hapticFeedback, scope)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Левая кнопка меню
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Меню",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Правая кнопка (Лупа / Поиск)
        IconButton(
            onClick = { viewModel.toggleSearchMode() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .offset(y = 60.dp)
        ) {
            val iconColor by animateColorAsState(
                targetValue = if (isSearchMode) StardustWarning else Color.White,
                label = "SearchIconColor"
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(StardustGlassBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Поиск",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))

            // Если режим поиска, скрываем список сканирования
            AnimatedVisibility(
                visible = !isSearchMode,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .height(250.dp)
                        .fillMaxWidth()
                        .background(StardustGlassBg)
                ) {
                    val tabs = listOf("Самокаты", "АКБ")
                    TabRow(
                        selectedTabIndex = activeTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = StardustTextPrimary,
                        indicator = { tabPositions ->
                            if (activeTab.ordinal < tabPositions.size) {
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[activeTab.ordinal])
                                        .height(3.dp)
                                        .background(
                                            color = StardustPrimary,
                                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                        )
                                )
                            }
                        },
                        divider = {
                            HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = activeTab.ordinal == index,
                                onClick = {
                                    hapticManager.performClick(hapticFeedback, scope)
                                    viewModel.onTabSelected(ActiveTab.values()[index])
                                },
                                text = { Text(title) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Найдено: ",
                                color = StardustTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            AnimatedContent(
                                targetState = currentList.size,
                                transitionSpec = {
                                    slideInVertically(animationSpec = tween(400)) { height -> height } togetherWith
                                            slideOutVertically(animationSpec = tween(400)) { height -> -height }
                                },
                                label = "Animated Counter"
                            ) { targetCount ->
                                Text(
                                    text = "$targetCount",
                                    color = StardustTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row {
                            IconButton(onClick = { viewModel.sortCurrentList() }) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Сортировать список",
                                    tint = StardustTextSecondary
                                )
                            }
                            IconButton(onClick = { viewModel.clearList() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Очистить список",
                                    tint = StardustTextSecondary
                                )
                            }
                        }
                    }
                    if (currentList.isEmpty()) {
                        EmptyState(modifier = Modifier.weight(1f))
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 24.dp)
                        ) {
                            items(items = currentList, key = { it.id }) { item ->
                                val isBeingDeleted = itemsBeingDeleted.contains(item.id)

                                AnimatedVisibility(
                                    visible = !isBeingDeleted,
                                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)),
                                    modifier = Modifier.animateItemPlacement(tween(durationMillis = 300))
                                ) {
                                    ScanListItem(
                                        item = item,
                                        isNew = item.id in newItems,
                                        onDeleteTrigger = {
                                            itemsBeingDeleted.add(item.id)
                                        },
                                        onCopy = { code ->
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("QR Code", code))
                                            Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                                        },
                                        onItemShown = { viewModel.markAsOld(item) }
                                    )
                                }

                                LaunchedEffect(isBeingDeleted) {
                                    if (isBeingDeleted) {
                                        delay(400L)
                                        viewModel.removeCode(item)
                                        itemsBeingDeleted.remove(item.id)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isSearchMode) {
                ActionButtons(
                    onAddClick = { showManualInputDialog = true },
                    onExportClick = {
                        if (currentList.isNotEmpty()) {
                            showExportSheet = true
                        } else {
                            Toast.makeText(context, "Список пуст, нечего экспортировать", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showManualInputDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StardustWarning),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Поиск по номеру (Вручную)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showManualInputDialog) {
            ManualInputDialog(
                activeTab = activeTab,
                isSearchMode = isSearchMode,
                onDismissRequest = { showManualInputDialog = false },
                onAddCode = { code ->
                    viewModel.addManualCode(code)
                    showManualInputDialog = false
                }
            )
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
}

@Composable
fun CameraView(
    scanMode: ScanMode,
    isSearchMode: Boolean,
    hasPermission: Boolean,
    scanEventFlow: Flow<ScanEvent>,
    onCodeScanned: (String) -> Unit,
    onTextFound: (String) -> Unit,
    onStatusUpdate: (String, Boolean) -> Unit,
    onToggleScanMode: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
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
                    cameraProvider.unbindAll()

                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                        .also {
                            val analyzer = when (scanMode) {
                                ScanMode.QR -> QrCodeAnalyzer(
                                    onCodeScanned = { code -> onCodeScanned(code) },
                                    onStatusUpdate = { message -> onStatusUpdate(message, true) }
                                )
                                ScanMode.TEXT -> TextRecognitionAnalyzer(
                                    onTextFound = { text -> onTextFound(text) },
                                    onStatusUpdate = { message -> onStatusUpdate(message, true) }
                                )
                            }
                            it.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
                        camera?.cameraControl?.enableTorch(isTorchOn)
                    } catch (e: Exception) {
                        Log.e("CameraView", "Camera bind error", e)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.Center)
                    .scale(scale)
                    .border(3.dp, borderColor, RoundedCornerShape(24.dp))
            )

            if (isSearchMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-140).dp)
                        .background(StardustWarning.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("ПОИСК", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onToggleScanMode) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Text Recognition",
                        tint = if (scanMode == ScanMode.TEXT) Color.Yellow else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = {
                    isTorchOn = !isTorchOn
                    camera?.cameraControl?.enableTorch(isTorchOn)
                }) {
                    Icon(
                        imageVector = Icons.Filled.FlashOn,
                        contentDescription = "Flashlight",
                        tint = if (isTorchOn) Color.Yellow else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Требуется разрешение на использование камеры", color = StardustTextPrimary)
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
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StardustSuccess,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "АКБ Найден!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold
                )
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
                    InfoRow(label = "Добавлен", value = result.creatorName ?: "-")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNavigateToPallet,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Перейти к палету", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text("Закрыть", color = StardustTextSecondary)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = StardustTextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = StardustTextSecondary, fontSize = 14.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun ManualInputDialog(
    activeTab: ActiveTab,
    isSearchMode: Boolean,
    onDismissRequest: () -> Unit,
    onAddCode: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    var selectedManufacturer by remember { mutableStateOf("FUJIAN") }

    val title = if (isSearchMode) "Поиск АКБ" else when (activeTab) {
        ActiveTab.SCOOTERS -> "Добавить самокат"
        ActiveTab.BATTERIES -> "Добавить АКБ"
    }

    val prefix = if (activeTab == ActiveTab.BATTERIES || isSearchMode) {
        if (selectedManufacturer == "FUJIAN") "4BB323" else "4BZ223"
    } else ""

    Dialog(onDismissRequest = onDismissRequest) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, color = StardustTextPrimary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == ActiveTab.BATTERIES || isSearchMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StardustItemBg, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val manufacturers = listOf("FUJIAN", "BYD")
                        manufacturers.forEach { manuf ->
                            val isSelected = selectedManufacturer == manuf
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) StardustPrimary else Color.Transparent)
                                    .clickable { selectedManufacturer = manuf },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = manuf,
                                    color = if (isSelected) Color.White else StardustTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (prefix.isNotEmpty()) {
                        Text(
                            text = prefix,
                            color = StardustTextSecondary,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { newValue ->
                            if (activeTab == ActiveTab.BATTERIES || isSearchMode) {
                                text = newValue.filter { it.isDigit() }
                            } else {
                                text = newValue.filter { it.isDigit() }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(if (activeTab == ActiveTab.SCOOTERS && !isSearchMode) "Номер..." else "Последние цифры...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = StardustItemBg, unfocusedContainerColor = StardustItemBg,
                            focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary,
                            cursorColor = StardustPrimary,
                            focusedIndicatorColor = StardustPrimary, unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = StardustTextSecondary, unfocusedLabelColor = StardustTextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismissRequest, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)) {
                        Text("Отмена", color = StardustTextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val fullCode = if (prefix.isNotEmpty()) prefix + text else text
                            onAddCode(fullCode)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if(isSearchMode) StardustWarning else StardustPrimary),
                        enabled = text.isNotBlank()
                    ) {
                        Text(if(isSearchMode) "Найти" else "Добавить", fontWeight = FontWeight.Bold, color = if(isSearchMode) Color.Black else Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SaveSessionDialog(
    isSaving: Boolean,
    onDismissRequest: () -> Unit,
    onSave: (name: String?) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Сохранить сессию",
                    color = StardustTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Введите название для сессии (необязательно)",
                    color = StardustTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название сессии...") },
                    singleLine = true,
                    enabled = !isSaving,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = StardustItemBg, unfocusedContainerColor = StardustItemBg,
                        focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary,
                        cursorColor = StardustPrimary,
                        focusedIndicatorColor = StardustPrimary, unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = StardustTextSecondary, unfocusedLabelColor = StardustTextSecondary,
                        disabledTextColor = StardustTextSecondary, disabledLabelColor = StardustTextSecondary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)
                    ) {
                        Text("Отмена", color = StardustTextSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSave(text) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                    ) {
                        AnimatedContent(
                            targetState = isSaving,
                            transitionSpec = { fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200)) },
                            label = "SaveButtonAnimation"
                        ) { saving ->
                            if (saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Сохранить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionSavedDialog(
    savedSession: ScanSession,
    onDismiss: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onNavigateToHistory,
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
            ) {
                Icon(Icons.Outlined.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("В историю")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = StardustTextSecondary)
            }
        },
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = StardustSuccess, modifier = Modifier.size(48.dp)) },
        title = { Text("Сессия сохранена!", color = StardustTextPrimary) },
        text = {
            val sessionName = savedSession.name?.let { "\"$it\"" } ?: "Без названия"
            val sessionType = when (savedSession.type) {
                SessionType.SCOOTERS -> "самокатов"
                SessionType.BATTERIES -> "АКБ"
            }
            val text = buildAnnotatedString {
                append("Сессия ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = StardustPrimary)) {
                    append(sessionName)
                }
                append(" с ${savedSession.items.size} $sessionType успешно сохранена.")
            }
            Text(text, color = StardustTextSecondary)
        },
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Ожидание сканирования...", color = StardustTextSecondary, fontSize = 16.sp)
    }
}

@Composable
fun ScanListItem(
    item: ScanItem,
    isNew: Boolean,
    modifier: Modifier = Modifier,
    onDeleteTrigger: () -> Unit,
    onCopy: (String) -> Unit,
    onItemShown: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isNew) StardustPrimary.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(durationMillis = 500),
        label = "Scan Item Background Color"
    )

    LaunchedEffect(isNew) {
        if (isNew) {
            delay(1500L)
            onItemShown()
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.code,
                color = StardustTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onCopy(item.code) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", tint = StardustTextSecondary)
                }
                IconButton(onClick = onDeleteTrigger, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Удалить", tint = StardustTextSecondary)
                }
            }
        }
        HorizontalDivider(color = StardustItemBg, thickness = 1.dp)
    }
}

@Composable
fun ActionButtons(onAddClick: () -> Unit, onExportClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StardustGlassBg)
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onAddClick,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
        ) {
            Text("+", fontSize = 32.sp, color = StardustTextPrimary)
        }
        Button(
            onClick = onExportClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
        ) {
            Text("Экспорт / Поделиться", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
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

            Button(onClick = onSaveSession, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustSecondary)) {
                Text("Сохранить сессию в историю", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { onCopyAll(listToExport) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                Text("Копировать в буфер обмена", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { onShare(listToExport) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
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

class QrCodeAnalyzer(
    private val onCodeScanned: (String) -> Unit,
    private val onStatusUpdate: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private var isProcessing = false
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close(); return
        }
        isProcessing = true
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.firstNotNullOfOrNull { it.rawValue }?.let { code ->
                            onCodeScanned(code)
                        }
                    }
                }
                .addOnFailureListener { e -> Log.e("QrCodeAnalyzer", "Scan error", e); onStatusUpdate("Ошибка сканера. Попробуйте снова.") }
                .addOnCompleteListener {
                    isProcessing = false
                    imageProxy.close()
                }
        } else {
            isProcessing = false
            imageProxy.close()
        }
    }
}

class TextRecognitionAnalyzer(
    private val onTextFound: (String) -> Unit,
    private val onStatusUpdate: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private var isProcessing = false

    private val BATTERY_ID_REGEX = Regex("^\\d[A-Z]{2}\\d{11}$")
    private val SCOOTER_ID_REGEX = Regex("^00\\d{6}$")

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        isProcessing = true
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val cleanText = line.text.replace(" ", "").replace("-", "").uppercase()

                            if (cleanText.matches(BATTERY_ID_REGEX)) {
                                onTextFound(cleanText)
                                return@addOnSuccessListener
                            }

                            if (cleanText.matches(SCOOTER_ID_REGEX)) {
                                onTextFound(cleanText)
                                return@addOnSuccessListener
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TextAnalyzer", "Text recognition failed", e)
                    onStatusUpdate("Ошибка распознавания текста.")
                }
                .addOnCompleteListener {
                    isProcessing = false
                    imageProxy.close()
                }
        } else {
            isProcessing = false
            imageProxy.close()
        }
    }
}