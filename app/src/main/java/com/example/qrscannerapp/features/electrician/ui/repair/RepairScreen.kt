// File: features/electrician/ui/repair/RepairScreen.kt

package com.example.qrscannerapp.features.electrician.ui.repair

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.HapticFeedbackManager
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.electrician.domain.model.BatteryRepairLog
import com.example.qrscannerapp.features.electrician.domain.model.ElectricianUiState
import com.example.qrscannerapp.features.electrician.domain.model.Manufacturer
import com.example.qrscannerapp.features.electrician.domain.model.RepairType
import com.example.qrscannerapp.features.electrician.ui.viewmodel.ElectricianViewModel
import com.example.qrscannerapp.features.electrician.ui.viewmodel.ElectricianViewModelFactory
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun RepairScreen(
    authManager: AuthManager,
    hapticManager: HapticFeedbackManager
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val viewModel: ElectricianViewModel = viewModel(
        factory = ElectricianViewModelFactory(authManager, hapticManager, hapticFeedback, scope)
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            Toast.makeText(context, "Отчет успешно отправлен в базу!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    if (uiState.isRepairMode) {
        RepairSelectionUI(
            scannedId = uiState.scannedBatteryId!!,
            selectedRepairs = uiState.selectedRepairs,
            selectedManufacturer = uiState.selectedManufacturer,
            customRepairText = uiState.customRepairText,
            isSaving = uiState.isSaving,
            onRepairToggle = viewModel::onRepairTypeToggle,
            onManufacturerSelected = viewModel::onManufacturerSelected,
            onCustomRepairTextChanged = viewModel::onCustomRepairTextChanged,
            onSubmit = viewModel::submitRepairLog,
            onCancel = viewModel::cancelScan
        )
    } else {
        CameraWithDiagnosticUI(
            uiState = uiState,
            onCodeScanned = { code -> viewModel.onQrCodeScanned(code) },
            onStartRepair = viewModel::onStartRepair,
            onCancelScan = viewModel::cancelScan,
            onShowManualInputDialog = viewModel::onShowManualInputDialog,
            onDismissManualInputDialog = viewModel::onDismissManualInputDialog,
            onManualInputTextChanged = viewModel::onManualInputTextChanged,
            onConfirmManualInput = { viewModel.onConfirmManualInput() },
            onToggleFlashlight = viewModel::toggleFlashlight
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraWithDiagnosticUI(
    uiState: ElectricianUiState,
    onCodeScanned: (String) -> Unit,
    onStartRepair: () -> Unit,
    onCancelScan: () -> Unit,
    onShowManualInputDialog: () -> Unit,
    onDismissManualInputDialog: () -> Unit,
    onManualInputTextChanged: (String) -> Unit,
    onConfirmManualInput: () -> Unit,
    onToggleFlashlight: () -> Unit
) {
    var showHistorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    Box(modifier = Modifier.fillMaxSize()) {
        ElectricianCameraView(
            onCodeScanned = onCodeScanned,
            isScanningActive = uiState.scannedBatteryId == null,
            isFlashlightOn = uiState.isFlashlightOn
        )

        IconButton(
            onClick = onToggleFlashlight,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = if (uiState.isFlashlightOn) Icons.Default.FlashOff else Icons.Default.FlashOn,
                contentDescription = "Переключить фонарик",
                tint = Color.White
            )
        }

        AnimatedVisibility(
            visible = uiState.scannedBatteryId == null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Button(
                onClick = onShowManualInputDialog,
                colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg.copy(alpha = 0.8f), contentColor = StardustTextPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Ввести ID вручную")
            }
        }

        DiagnosticCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            uiState = uiState,
            onStartRepair = onStartRepair,
            onViewHistory = { showHistorySheet = true },
            onCancel = onCancelScan
        )
    }

    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }, sheetState = sheetState, containerColor = StardustModalBg) {
            BatteryHistorySheetContent(history = uiState.batteryHistory ?: emptyList())
        }
    }

    if (uiState.showManualInputDialog) {
        ManualInputDialog(
            text = uiState.manualInputText,
            onTextChange = onManualInputTextChanged,
            onConfirm = onConfirmManualInput,
            onDismiss = onDismissManualInputDialog
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualInputDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StardustItemBg,
        title = { Text("Ручной ввод ID аккумулятора (14 символов)", color = StardustTextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("ID аккумулятора") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Ascii
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedTextColor = StardustTextPrimary,
                    unfocusedTextColor = StardustTextPrimary,
                    containerColor = Color.Transparent,
                    cursorColor = StardustPrimary,
                    focusedBorderColor = StardustPrimary,
                    unfocusedBorderColor = StardustTextSecondary,
                    focusedLabelColor = StardustPrimary,
                    unfocusedLabelColor = StardustTextSecondary
                ),
                supportingText = {
                    Text(
                        text = "${text.length} / 14",
                        color = if (text.length == 14) StardustPrimary else StardustTextSecondary
                    )
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = text.length == 14,
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
            ) {
                Text("Подтвердить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = StardustTextSecondary)
            }
        }
    )
}
@Composable
private fun ElectricianCameraView(
    onCodeScanned: (String) -> Unit,
    isScanningActive: Boolean,
    isFlashlightOn: Boolean
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(), onResult = { granted -> hasPermission = granted })
    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            val lifecycleOwner = LocalLifecycleOwner.current
            val scanningActiveState = rememberUpdatedState(isScanningActive)

            val camera = remember { mutableStateOf<Camera?>(null) }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                            it.setAnalyzer(Executors.newSingleThreadExecutor(), ElectricianQrCodeAnalyzer(
                                isScanningActiveProvider = { scanningActiveState.value },
                                onCodeScanned = { code -> ContextCompat.getMainExecutor(ctx).execute { onCodeScanned(code) } }
                            ))
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            camera.value = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
                        } catch (e: Exception) {
                            Log.e("CameraView", "Camera bind error", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            LaunchedEffect(camera.value, isFlashlightOn) {
                camera.value?.let {
                    if (it.cameraInfo.hasFlashUnit()) {
                        it.cameraControl.enableTorch(isFlashlightOn)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.Center)
                    .border(3.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            )

        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Требуется разрешение на использование камеры", color = StardustTextPrimary)
            }
        }
    }
}
@Composable
private fun DiagnosticCard(
    modifier: Modifier = Modifier,
    uiState: ElectricianUiState,
    onStartRepair: () -> Unit,
    onViewHistory: () -> Unit,
    onCancel: () -> Unit
) {
    AnimatedVisibility(
        visible = uiState.scannedBatteryId != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StardustItemBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "АКБ: ${uiState.scannedBatteryId}",
                        color = StardustTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена", tint = StardustTextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                when {
                    uiState.isCheckingHistory -> {
                        CircularProgressIndicator(color = StardustPrimary)
                        Text(
                            text = "Проверка в базе...",
                            color = StardustTextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    uiState.batteryHistory != null -> {
                        if (uiState.batteryHistory.isEmpty()) {
                            Text("Новый АКБ, в базе не найден", color = StardustTextPrimary, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onStartRepair, colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                                Text("Начать ремонт")
                            }
                        } else {
                            Text(
                                "АКБ найден (ремонтов: ${uiState.batteryHistory.size})",
                                color = StardustTextPrimary,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedButton(
                                    onClick = onViewHistory,
                                    border = BorderStroke(1.dp, StardustTextSecondary)
                                ) {
                                    Text("Посмотреть историю", color = StardustTextSecondary)
                                }
                                Button(
                                    onClick = onStartRepair,
                                    colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                                ) {
                                    Text("Начать ремонт")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun BatteryHistorySheetContent(history: List<BatteryRepairLog>) {
    val sortedHistory = history.sortedByDescending { it.timestamp }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    Column {
        Text(
            text = "История ремонтов",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = StardustTextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            items(sortedHistory) { log ->
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = dateFormatter.format(Date(log.timestamp)),
                        color = StardustTextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Исполнитель: ${log.electricianName}",
                        color = StardustTextPrimary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Работы: ${log.repairs.joinToString(", ")}",
                        color = StardustTextPrimary,
                        fontSize = 16.sp
                    )
                    HorizontalDivider(
                        color = StardustGlassBg,
                        thickness = 1.dp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepairSelectionUI(
    scannedId: String,
    selectedRepairs: Set<RepairType>,
    selectedManufacturer: Manufacturer,
    customRepairText: String,
    isSaving: Boolean,
    onRepairToggle: (RepairType) -> Unit,
    onManufacturerSelected: (Manufacturer) -> Unit,
    onCustomRepairTextChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    // V-- АРХИТЕКТУРНОЕ ИЗМЕНЕНИЕ: AppBackground здесь не нужен, так как он предоставляется
    // родительским экраном ElectricianMainScreen. Этот компонент должен быть прозрачным. --V
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Default.Close, contentDescription = "Отмена", tint = StardustTextSecondary)
                }
            }
            Text(text = "АКБ отсканирован", color = StardustTextSecondary, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = scannedId, color = StardustTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Выберите производителя:", color = StardustTextSecondary, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                Manufacturer.entries.forEach { manufacturer ->
                    val isSelected = selectedManufacturer == manufacturer
                    OutlinedButton(
                        onClick = { onManufacturerSelected(manufacturer) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) StardustPrimary.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (isSelected) StardustPrimary else StardustTextSecondary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) StardustPrimary else StardustItemBg
                        )
                    ) {
                        Text(manufacturer.displayName, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Выберите выполненные работы:", color = StardustTextSecondary, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(0.9f), horizontalAlignment = Alignment.Start) {
                RepairType.entries.forEach { repairType ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Checkbox(
                            checked = selectedRepairs.contains(repairType),
                            onCheckedChange = { onRepairToggle(repairType) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = StardustPrimary,
                                uncheckedColor = StardustTextSecondary,
                                checkmarkColor = StardustModalBg
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = repairType.displayName, color = StardustTextPrimary, fontSize = 18.sp)
                    }
                }
                AnimatedVisibility(
                    visible = selectedRepairs.contains(RepairType.OTHER),
                    enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(300))
                ) {
                    OutlinedTextField(
                        value = customRepairText,
                        onValueChange = onCustomRepairTextChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text("Опишите неисправность") },
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedTextColor = StardustTextPrimary,
                            unfocusedTextColor = StardustTextPrimary,
                            containerColor = Color.Transparent,
                            cursorColor = StardustPrimary,
                            focusedBorderColor = StardustPrimary,
                            unfocusedBorderColor = StardustTextSecondary,
                            focusedLabelColor = StardustPrimary,
                            unfocusedLabelColor = StardustTextSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        val isOtherRepairValid = !selectedRepairs.contains(RepairType.OTHER) || customRepairText.isNotBlank()
        if (isSaving) {
            CircularProgressIndicator(color = StardustPrimary, modifier = Modifier.padding(bottom = 32.dp))
        } else {
            SwipeToCompleteSlider(
                modifier = Modifier.padding(bottom = 32.dp),
                onSwiped = onSubmit,
                enabled = selectedRepairs.isNotEmpty() && isOtherRepairValid
            )
        }
    }
}
@Composable
private fun SwipeToCompleteSlider(
    modifier: Modifier = Modifier,
    onSwiped: () -> Unit,
    enabled: Boolean
) {
    val sliderWidth = 250.dp
    val thumbSize = 50.dp
    val maxDragPx = with(LocalDensity.current) { (sliderWidth - thumbSize).toPx() }
    var dragOffsetX by remember { mutableStateOf(0f) }
    val animatedDragOffsetX by animateFloatAsState(targetValue = dragOffsetX, label = "dragAnimation")
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor by animateColorAsState(targetValue = if (enabled) StardustPrimary else StardustItemBg, animationSpec = tween(300), label = "background_color_anim")
    val thumbColor by animateColorAsState(targetValue = if (enabled) Color.White else StardustTextSecondary, animationSpec = tween(300), label = "thumb_color_anim")
    Box(
        modifier = modifier
            .width(sliderWidth)
            .height(thumbSize)
            .clip(CircleShape)
            .background(backgroundColor)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                coroutineScope {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffsetX > maxDragPx * 0.8f) {
                                onSwiped()
                            }
                            launch { dragOffsetX = 0f }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = (dragOffsetX + dragAmount).coerceIn(0f, maxDragPx)
                            dragOffsetX = newOffset
                        }
                    )
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Завершить",
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(1f - (animatedDragOffsetX / maxDragPx))
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedDragOffsetX.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Swipe", tint = backgroundColor)
        }
    }
}
private class ElectricianQrCodeAnalyzer(
    private val isScanningActiveProvider: () -> Boolean,
    private val onCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanningActiveProvider()) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && isScanningActiveProvider()) {
                        barcodes.firstNotNullOfOrNull { it.rawValue }?.let { code ->
                            onCodeScanned(code)
                        }
                    }
                }
                .addOnFailureListener { e -> Log.e("QrCodeAnalyzer", "Scan error", e) }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }
}