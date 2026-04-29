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
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

val ColorFujian = Color(0xFFFF8A65)
val ColorByd = Color(0xFF4FC3F7)
val ColorWind50 = Color(0xFF69F0AE)
val ColorWind50Old = Color(0xFFFFAB40)
val ColorWind40 = Color(0xFF7C8AFF)

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
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean -> hasCameraPermission = granted }

    LaunchedEffect(key1 = true) { launcher.launch(Manifest.permission.CAMERA) }

    val isSearchMode by viewModel.isSearchMode.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val scooterSearchResult by viewModel.scooterSearchResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val scanEventFlow = viewModel.scanEvent
    val warehouseViewModel: WarehouseViewModel = hiltViewModel()
    val activeTab by viewModel.activeTab.collectAsState()

    var isTorchOn by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(StardustSolidBg)) {
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
                isTorchOn = isTorchOn,
                onTorchChange = { torch: Boolean -> isTorchOn = torch },
                onCodeScanned = { code: String ->
                    when (activeTab) {
                        ActiveTab.WAREHOUSE -> warehouseViewModel.onPartScanned(code)
                        else -> viewModel.onCodeScanned(code)
                    }
                },
                onStatusUpdate = { msg: String, isErr: Boolean -> viewModel.updateStatus(msg, isErr) }
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
                    .background(
                        if (isSearchMode) StardustWarning else Color.Black.copy(alpha = 0.3f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Search,
                    "Поиск",
                    tint = if (isSearchMode) Color.Black else Color.White
                )
            }

            IconButton(
                onClick = { isTorchOn = !isTorchOn },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .background(
                        if (isTorchOn) StardustWarning.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.3f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Фонарик",
                    tint = if (isTorchOn) Color.Black else Color.White
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .background(StardustSolidBg)
        ) {
            ScannerOverlayUi(
                viewModel = viewModel,
                warehouseViewModel = warehouseViewModel,
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
            onNavigate = { viewModel.clearScooterSearchResult(); onNavigateToStorage() },
            onOpen3D = { onNavigateToVisualRepair(number) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerOverlayUi(
    viewModel: QrScannerViewModel,
    warehouseViewModel: WarehouseViewModel,
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

    val scooterCount = viewModel.scooterCodes.size
    val batteryCount = viewModel.batteryCodes.size
    val newBatteryCount = viewModel.newBatteryCodes.size
    val statusMessage by viewModel.statusMessage.collectAsState()

    val duplicateCount by viewModel.duplicateCount.collectAsState()
    val scanRate by viewModel.scanRate.collectAsState()
    val batchConfig by viewModel.batchConfig.collectAsState()

    var sessionSeconds by remember { mutableLongStateOf(0L) }
    val hasItems = scooterCount > 0 || batteryCount > 0 || newBatteryCount > 0
    LaunchedEffect(hasItems) {
        if (hasItems) {
            while (true) { delay(1000); sessionSeconds++ }
        } else { sessionSeconds = 0L }
    }

    var showBatchSetupSheet by remember { mutableStateOf(false) }
    val batchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isManualInputExpanded by remember { mutableStateOf(false) }
    val isInputVisible = isManualInputExpanded || isSearchMode

    val currentList: List<ScanItem> = when (activeTab) {
        ActiveTab.SCOOTERS -> viewModel.scooterCodes
        ActiveTab.BATTERIES -> viewModel.batteryCodes
        ActiveTab.NEW_BATTERIES -> viewModel.newBatteryCodes
        else -> emptyList()
    }

    val exportSheetState = rememberModalBottomSheetState()
    var showExportSheet by remember { mutableStateOf(false) }
    var showSaveSessionDialog by remember { mutableStateOf(false) }
    val recentlySavedSession by viewModel.recentlySavedSession.collectAsState()

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    DisposableEffect(Unit) { onDispose { toneGen.release() } }

    val listState = rememberLazyListState()
    var manualInputText by remember { mutableStateOf("") }
    var selectedManufacturer by remember { mutableStateOf("FUJIAN") }
    var selectedWind50Subtype by remember { mutableStateOf("NEW") }

    LaunchedEffect(key1 = currentList.firstOrNull()) {
        if (currentList.isNotEmpty()) listState.animateScrollToItem(0)
    }
    LaunchedEffect(activeTab) { manualInputText = "" }

    LaunchedEffect(Unit) {
        viewModel.scanEffect.collect { effect: UiEffect ->
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

    LaunchedEffect(Unit) {
        viewModel.batchEvent.collect { event: BatchEvent ->
            when (event) {
                is BatchEvent.Completed -> {
                    if (isVibrationEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createWaveform(
                                    longArrayOf(0, 100, 100, 100, 100, 300),
                                    intArrayOf(0, 255, 0, 255, 0, 255), -1
                                )
                            )
                        } else {
                            @Suppress("DEPRECATION") vibrator.vibrate(500)
                        }
                    }
                    if (isSoundEnabled) toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 400)
                }
                is BatchEvent.NearlyDone -> {
                    if (isVibrationEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createWaveform(
                                    longArrayOf(0, 80, 80, 80),
                                    intArrayOf(0, 200, 0, 200), -1
                                )
                            )
                        } else {
                            @Suppress("DEPRECATION") vibrator.vibrate(200)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scanEvent.collect { event: ScanEvent ->
            if (event is ScanEvent.Duplicate && isSoundEnabled) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                delay(150)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        AnimatedVisibility(
            visible = batchConfig.isActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            BatchProgressBar(
                config = batchConfig,
                currentCount = viewModel.getBatchProgress().first,
                onCancelBatch = { viewModel.clearBatchMode() }
            )
        }

        // ============================================================================================
        // ВКЛАДКИ: Самокаты | Склад | WIND 4.0 | WIND 5.0
        // ============================================================================================
        val tabs = listOf(
            "Самокаты${if (scooterCount > 0) " · $scooterCount" else ""}",
            "Склад",
            "WIND 4.0${if (batteryCount > 0) " · $batteryCount" else ""}",
            "WIND 5.0${if (newBatteryCount > 0) " · $newBatteryCount" else ""}"
        )

        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = StardustTextPrimary,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                if (activeTab.ordinal < tabPositions.size) {
                    val indicatorColor = when (activeTab) {
                        ActiveTab.NEW_BATTERIES -> ColorWind50
                        ActiveTab.BATTERIES -> ColorWind40
                        else -> StardustPrimary
                    }
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[activeTab.ordinal])
                            .height(3.dp)
                            .background(
                                indicatorColor,
                                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
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
                    text = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        AnimatedVisibility(
            visible = statusMessage != "Наведите камеру на QR-код" && statusMessage != "РЕЖИМ ПОИСКА",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val isErrorStatus = statusMessage.startsWith("Ошибка") ||
                    statusMessage.contains("уже в списке") || statusMessage.contains("не найден")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isErrorStatus) StardustError.copy(alpha = 0.15f)
                        else StardustSuccess.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusMessage,
                    color = if (isErrorStatus) StardustError else StardustSuccess,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (activeTab != ActiveTab.WAREHOUSE) {
            SessionStatsRow(
                sessionSeconds = sessionSeconds,
                duplicateCount = duplicateCount,
                scanRate = scanRate,
                isBatchActive = batchConfig.isActive,
                onBatchClick = { showBatchSetupSheet = true }
            )
        }

        Column(modifier = Modifier.fillMaxWidth().background(StardustSolidBg)) {
            if (!isSearchMode) {
                val manualInputAvailable = activeTab != ActiveTab.WAREHOUSE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = manualInputAvailable) {
                            isManualInputExpanded = !isManualInputExpanded
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val color = if (manualInputAvailable) StardustTextSecondary
                        else StardustTextSecondary.copy(alpha = 0.3f)
                        Icon(Icons.Outlined.Keyboard, null, tint = color)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ручной ввод", color = color, fontWeight = FontWeight.Medium)
                    }
                    Icon(
                        imageVector = if (isManualInputExpanded) Icons.Outlined.KeyboardArrowUp
                        else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = StardustTextSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isInputVisible && activeTab != ActiveTab.WAREHOUSE,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                ) {
                    // WIND 4.0: чипы FUJIAN / BYD
                    if (activeTab == ActiveTab.BATTERIES && !isSearchMode) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("FUJIAN", "BYD").forEach { brand ->
                                FilterChip(
                                    selected = selectedManufacturer == brand,
                                    onClick = { selectedManufacturer = brand },
                                    label = { Text(brand) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (brand == "BYD") ColorByd else ColorFujian,
                                        selectedLabelColor = Color.Black,
                                        containerColor = StardustItemBg,
                                        labelColor = StardustTextSecondary
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // WIND 5.0: чипы Новые (5BB) / Старые (SF)
                    if (activeTab == ActiveTab.NEW_BATTERIES && !isSearchMode) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("NEW" to "Новые (5BB)", "OLD" to "Старые (SF)").forEach { (type, label) ->
                                FilterChip(
                                    selected = selectedWind50Subtype == type,
                                    onClick = { selectedWind50Subtype = type },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (type == "NEW") ColorWind50 else ColorWind50Old,
                                        selectedLabelColor = Color.Black,
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
                        val prefix = when {
                            isSearchMode -> ""
                            activeTab == ActiveTab.BATTERIES -> when (selectedManufacturer) {
                                "FUJIAN" -> "4BB"
                                "BYD" -> "4BZ"
                                else -> ""
                            }
                            activeTab == ActiveTab.NEW_BATTERIES -> when (selectedWind50Subtype) {
                                "NEW" -> "5BB"
                                "OLD" -> "SF"
                                else -> ""
                            }
                            else -> ""
                        }
                        val activeColor = when {
                            isSearchMode -> StardustWarning
                            activeTab == ActiveTab.NEW_BATTERIES && selectedWind50Subtype == "OLD" -> ColorWind50Old
                            activeTab == ActiveTab.NEW_BATTERIES -> ColorWind50
                            activeTab == ActiveTab.BATTERIES -> ColorWind40
                            else -> StardustPrimary
                        }
                        val containerColor = when {
                            isSearchMode -> StardustWarning.copy(alpha = 0.1f)
                            activeTab == ActiveTab.NEW_BATTERIES && selectedWind50Subtype == "OLD" -> ColorWind50Old.copy(alpha = 0.08f)
                            activeTab == ActiveTab.NEW_BATTERIES -> ColorWind50.copy(alpha = 0.08f)
                            activeTab == ActiveTab.BATTERIES -> ColorWind40.copy(alpha = 0.08f)
                            else -> StardustItemBg
                        }

                        OutlinedTextField(
                            value = manualInputText,
                            onValueChange = { newValue: String ->
                                manualInputText = when {
                                    activeTab == ActiveTab.SCOOTERS ->
                                        newValue.filter { c: Char -> c.isLetterOrDigit() }.uppercase()
                                    activeTab == ActiveTab.NEW_BATTERIES && selectedWind50Subtype == "OLD" ->
                                        newValue.filter { c: Char -> c.isLetterOrDigit() }.uppercase()
                                    else ->
                                        newValue.filter { c: Char -> c.isDigit() || c.isLetter() }.uppercase()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    when {
                                        isSearchMode -> "Поиск по номеру..."
                                        activeTab == ActiveTab.SCOOTERS -> "Номер (напр. HE600A)..."
                                        activeTab == ActiveTab.NEW_BATTERIES && selectedWind50Subtype == "OLD" ->
                                            "Буквы + цифры (AEQ24A5A0175)..."
                                        activeTab == ActiveTab.NEW_BATTERIES -> "11 цифр после 5BB..."
                                        activeTab == ActiveTab.BATTERIES -> "11 цифр после префикса..."
                                        else -> "Введите код..."
                                    }
                                )
                            },
                            prefix = if (prefix.isNotEmpty() && !isSearchMode) {
                                {
                                    Text(
                                        prefix,
                                        color = activeColor.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = when {
                                    activeTab == ActiveTab.SCOOTERS -> KeyboardType.Text
                                    activeTab == ActiveTab.NEW_BATTERIES && selectedWind50Subtype == "OLD" -> KeyboardType.Text
                                    else -> KeyboardType.Number
                                },
                                imeAction = ImeAction.Done,
                                capitalization = when {
                                    activeTab == ActiveTab.SCOOTERS -> KeyboardCapitalization.Characters
                                    activeTab == ActiveTab.NEW_BATTERIES && selectedWind50Subtype == "OLD" -> KeyboardCapitalization.Characters
                                    else -> KeyboardCapitalization.None
                                },
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (manualInputText.isNotBlank()) {
                                    val fullCode = if (prefix.isNotEmpty() && !isSearchMode) {
                                        prefix + manualInputText
                                    } else {
                                        manualInputText
                                    }
                                    viewModel.addManualCode(fullCode)
                                    focusManager.clearFocus()
                                }
                            }),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeColor,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = containerColor,
                                unfocusedContainerColor = containerColor.copy(alpha = 0.5f),
                                cursorColor = activeColor,
                                focusedTextColor = StardustTextPrimary,
                                unfocusedTextColor = StardustTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = {
                                if (manualInputText.isNotBlank()) {
                                    val fullCode = if (prefix.isNotEmpty() && !isSearchMode) {
                                        prefix + manualInputText
                                    } else {
                                        manualInputText
                                    }
                                    viewModel.addManualCode(fullCode)
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
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = StardustItemBg)

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                ActiveTab.SCOOTERS, ActiveTab.BATTERIES, ActiveTab.NEW_BATTERIES -> {
                    if (currentList.isEmpty()) {
                        EmptyState(
                            text = if (isSearchMode) "Введите код для поиска" else "Список пуст"
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(items = currentList, key = { item: ScanItem -> item.id }) { item: ScanItem ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue: SwipeToDismissBoxValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                            if (isVibrationEnabled) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    vibrator.vibrate(
                                                        VibrationEffect.createOneShot(
                                                            30,
                                                            VibrationEffect.DEFAULT_AMPLITUDE
                                                        )
                                                    )
                                                } else {
                                                    @Suppress("DEPRECATION") vibrator.vibrate(30)
                                                }
                                            }
                                            viewModel.removeCode(item)
                                            true
                                        } else false
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                                StardustError
                                            } else {
                                                Color.Transparent
                                            },
                                            label = ""
                                        )
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(color)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
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
                        val fabColor = when (activeTab) {
                            ActiveTab.NEW_BATTERIES -> ColorWind50
                            ActiveTab.BATTERIES -> ColorWind40
                            else -> StardustPrimary
                        }
                        ExtendedFloatingActionButton(
                            text = {
                                Text(
                                    "Действия (${currentList.size})",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            icon = { Icon(Icons.Default.Share, null) },
                            onClick = { showExportSheet = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            containerColor = fabColor,
                            contentColor = Color.Black
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
                ActiveTab.WAREHOUSE -> WarehouseScreen(viewModel = warehouseViewModel)
            }
        }
    }

    if (showBatchSetupSheet) {
        BatchSetupSheet(
            currentConfig = batchConfig,
            sheetState = batchSheetState,
            onDismiss = {
                scope.launch { batchSheetState.hide() }.invokeOnCompletion {
                    showBatchSetupSheet = false
                }
            },
            onConfirm = { config: BatchConfig ->
                viewModel.setBatchConfig(config)
                scope.launch { batchSheetState.hide() }.invokeOnCompletion {
                    showBatchSetupSheet = false
                }
            }
        )
    }

    if (showExportSheet) {
        ExportSheet(
            listToExport = currentList,
            sheetState = exportSheetState,
            activeTab = activeTab,
            onDismiss = { showExportSheet = false },
            onCopyAll = { list: List<ScanItem> ->
                viewModel.logActivity("COPY_ALL")
                val allCodes = list.joinToString("\n") { scanItem: ScanItem -> scanItem.code }
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("All Codes", allCodes))
                Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                scope.launch { exportSheetState.hide() }.invokeOnCompletion {
                    if (!exportSheetState.isVisible) showExportSheet = false
                }
            },
            onShare = { list: List<ScanItem> ->
                val allCodes = list.joinToString("\n") { scanItem: ScanItem -> scanItem.code }
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, allCodes)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
                scope.launch { exportSheetState.hide() }.invokeOnCompletion {
                    if (!exportSheetState.isVisible) showExportSheet = false
                }
            },
            onSaveSession = {
                scope.launch { exportSheetState.hide() }.invokeOnCompletion {
                    if (!exportSheetState.isVisible) {
                        showExportSheet = false
                        showSaveSessionDialog = true
                    }
                }
            },
            onSort = {
                viewModel.sortCurrentList()
                scope.launch { exportSheetState.hide() }.invokeOnCompletion {
                    if (!exportSheetState.isVisible) showExportSheet = false
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
            onSave = { sessionName: String? -> viewModel.saveCurrentSession(sessionName) }
        )
    }

    recentlySavedSession?.let { savedSession ->
        SessionSavedDialog(
            savedSession = savedSession,
            onDismiss = { viewModel.onSessionSaveDialogDismissed() },
            onNavigateToHistory = {
                viewModel.onSessionSaveDialogDismissed()
                onNavigateToHistory()
            }
        )
    }
}