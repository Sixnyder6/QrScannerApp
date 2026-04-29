package com.example.qrscannerapp.features.interaction.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.features.scanner.ui.components.CameraView
import com.example.qrscannerapp.SettingsManager
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.StardustWarning
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.core.model.ScanEvent
import com.example.qrscannerapp.features.interaction.domain.model.InteractionSession
import com.example.qrscannerapp.features.interaction.domain.model.OperationType
import com.example.qrscannerapp.features.inventory.ui.distribution.AnimatedCounterText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Цвет для удаления/ошибок
val StardustError = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionScreen(
    viewModel: InteractionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val currentOperation by viewModel.currentOperation.collectAsState()
    val scannedCodes by viewModel.scannedCodes.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val history by viewModel.history.collectAsState()

    // ВОТ ОН - ПЕРЕКЛЮЧАТЕЛЬ ВКЛАДОК
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentOperation != null) currentOperation!!.displayName else "Взаимодействие",
                        color = StardustTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentOperation != null) viewModel.cancelSession() else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = StardustTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentOperation,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut())
                },
                label = "OperationScreenTransition"
            ) { operation ->
                if (operation == null) {
                    // === ГЛАВНЫЙ ЭКРАН С ВКЛАДКАМИ ===
                    Column(modifier = Modifier.fillMaxSize()) {

                        // ВКЛАДКИ: СМЕНЫ | ИСТОРИЯ
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = StardustTextPrimary,
                            indicator = { tabPositions ->
                                if (selectedTab < tabPositions.size) {
                                    Box(
                                        modifier = Modifier
                                            .tabIndicatorOffset(tabPositions[selectedTab])
                                            .height(3.dp)
                                            .background(StardustPrimary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    )
                                }
                            },
                            divider = { HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f)) }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Смены", fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Outlined.WorkOutline, null) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("История", fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Outlined.History, null) }
                            )
                        }

                        // КОНТЕНТ В ЗАВИСИМОСТИ ОТ ВЫБРАННОЙ ВКЛАДКИ
                        when(selectedTab) {
                            0 -> {
                                // Плитки (Мойка, Шлифовка...)
                                OperationSelectionGrid(
                                    onOperationClick = { viewModel.selectOperation(it) }
                                )
                            }
                            1 -> {
                                // Список истории
                                HistoryList(history = history)
                            }
                        }
                    }
                } else {
                    // === ЭКРАН СКАНЕРА (АКТИВНАЯ СЕССИЯ) ===
                    ActiveSessionScreen(
                        operation = operation,
                        scannedCount = scannedCodes.size,
                        scannedCodes = scannedCodes,
                        isSaving = isSaving,
                        viewModel = viewModel,
                        onRemoveCode = { viewModel.removeCode(it) },
                        onFinish = { viewModel.finishAndSaveSession() }
                    )
                }
            }
        }
    }
}

// === КОМПОНЕНТЫ ===

@Composable
fun OperationSelectionGrid(onOperationClick: (OperationType) -> Unit) {
    val operations = listOf(
        Triple(OperationType.WASHING, Icons.Outlined.WaterDrop, Color(0xFF4FC3F7)),
        Triple(OperationType.SANDING, Icons.Outlined.Carpenter, Color(0xFFFFB74D)),
        Triple(OperationType.DECALING, Icons.Outlined.Style, Color(0xFFBA68C8)),
        Triple(OperationType.BATTERY_SWAP, Icons.Outlined.BatteryChargingFull, Color(0xFF81C784))
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(operations) { (type, icon, color) ->
            OperationCard(
                title = type.displayName,
                icon = icon,
                color = color,
                onClick = { onOperationClick(type) }
            )
        }
    }
}

@Composable
fun OperationCard(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StardustItemBg),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(32.dp), tint = color)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = StardustTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun HistoryList(history: List<InteractionSession>) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.HistoryToggleOff, null, tint = StardustTextSecondary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("История пуста", color = StardustTextSecondary)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history, key = { it.id }) { session ->
                HistoryItem(session)
            }
        }
    }
}

@Composable
fun HistoryItem(session: InteractionSession) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("ru")) }
    val operationColor = when(session.operationType) {
        OperationType.WASHING -> Color(0xFF4FC3F7)
        OperationType.SANDING -> Color(0xFFFFB74D)
        OperationType.DECALING -> Color(0xFFBA68C8)
        OperationType.BATTERY_SWAP -> Color(0xFF81C784)
    }

    val operationIcon = when(session.operationType) {
        OperationType.WASHING -> Icons.Outlined.WaterDrop
        OperationType.SANDING -> Icons.Outlined.Carpenter
        OperationType.DECALING -> Icons.Outlined.Style
        OperationType.BATTERY_SWAP -> Icons.Outlined.BatteryChargingFull
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustItemBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(operationColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(operationIcon, null, tint = operationColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.operationType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StardustTextPrimary
                )
                Text(
                    text = "${session.creatorName} • ${dateFormatter.format(Date(session.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StardustTextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.scooterCount} шт.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = StardustPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (session.isSynced) {
                    Icon(Icons.Default.CloudDone, null, tint = StardustSuccess, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.CloudOff, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    operation: OperationType,
    scannedCount: Int,
    scannedCodes: List<String>,
    isSaving: Boolean,
    viewModel: InteractionViewModel,
    onRemoveCode: (String) -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var manualInputText by remember { mutableStateOf("") }

    val settingsManager = remember { SettingsManager(context) }
    val isSoundEnabled by settingsManager.isSoundEnabledFlow.collectAsState(initial = true)
    val isVibrationEnabled by settingsManager.isVibrationEnabledFlow.collectAsState(initial = true)

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    LaunchedEffect(Unit) {
        viewModel.scanEventFlow.collect { event ->
            if (event == ScanEvent.Success) {
                if (isSoundEnabled) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                if (isVibrationEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION") vibrator.vibrate(50)
                    }
                }
            } else if (event == ScanEvent.Duplicate) {
                if (isSoundEnabled) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                if (isVibrationEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
                    } else {
                        @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
        ) {
            CameraView(
                isSearchMode = false,
                hasPermission = true,
                scanEventFlow = viewModel.scanEventFlow,
                isTorchOn = false,           // ← добавить
                onTorchChange = { },         // ← добавить (в InteractionScreen фонарик не нужен)
                onCodeScanned = { code ->
                    viewModel.onCodeScanned(code)
                },
                onStatusUpdate = { _, _ -> }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "РЕЖИМ: ${operation.displayName.uppercase()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // ВРЕМЕННАЯ КНОПКА
            Button(
                onClick = { viewModel.onCodeScanned("00${(10..99).random()}") },
                colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg.copy(alpha = 0.8f)),
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            ) {
                Text("Эмулировать", color = StardustPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualInputText,
                onValueChange = { manualInputText = it.filter { char -> char.isDigit() } },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ручной ввод номера...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (manualInputText.isNotBlank()) {
                        viewModel.onCodeScanned(manualInputText)
                        manualInputText = ""
                        focusManager.clearFocus()
                    }
                }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StardustPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = StardustItemBg,
                    unfocusedContainerColor = StardustItemBg,
                    cursorColor = StardustPrimary,
                    focusedTextColor = StardustTextPrimary,
                    unfocusedTextColor = StardustTextPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = {
                    if (manualInputText.isNotBlank()) {
                        viewModel.onCodeScanned(manualInputText)
                        manualInputText = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = StardustPrimary),
                enabled = manualInputText.isNotBlank()
            ) {
                Icon(Icons.Default.Add, "Добавить", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Отсканировано", color = StardustTextSecondary, fontSize = 14.sp)
        AnimatedCounterText(
            count = scannedCount,
            prefix = "",
            color = StardustPrimary,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scannedCodes, key = { it }) { code ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StardustItemBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Самокат: $code", color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onRemoveCode(code) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Close, "Удалить", tint = StardustError)
                    }
                }
            }
        }

        Button(
            onClick = onFinish,
            enabled = scannedCount > 0 && !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StardustPrimary,
                contentColor = Color.Black
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.DoneAll, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Завершить и сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}