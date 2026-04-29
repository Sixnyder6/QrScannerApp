@file:OptIn(ExperimentalLayoutApi::class)

package com.example.qrscannerapp.features.security.ui.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.features.scanner.ui.components.CameraView
import com.example.qrscannerapp.features.security.ui.SecColors
import com.example.qrscannerapp.features.security.ui.ScooterTag
import com.example.qrscannerapp.features.security.ui.scooterTagColor
import com.example.qrscannerapp.features.security.ui.scooterTagIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow

// ============================================================================================
// SECURITY SCANNER SCREEN
// ============================================================================================

@Composable
fun SecurityScannerScreen(
    viewModel: SecurityScannerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPassport: (String) -> Unit
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current

    var isTorchOn            by remember { mutableStateOf(false) }
    var hasCameraPermission  by remember { mutableStateOf(false) }
    var showManualInput      by remember { mutableStateOf(false) }
    var manualInputText      by remember { mutableStateOf("") }

    // ── Запрашиваем КАМЕРУ + ЛОКАЦИЮ одновременно при старте ────────────
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] == true
        // Локация будет доступна TelemetryManager автоматически после выдачи
    }
    LaunchedEffect(Unit) {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Вибратор
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    DisposableEffect(Unit) { onDispose { toneGen.release() } }

    // Обработка событий
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SecurityScannerEvent.PlaySuccessBeep -> {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                is SecurityScannerEvent.PlayErrorBeep -> {
                    toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 300)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
                }
                is SecurityScannerEvent.PlayWarningBeep -> {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                }
                is SecurityScannerEvent.NavigateToPassport -> {
                    onNavigateToPassport(event.scooterId)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Камера
        CameraView(
            isSearchMode   = true,
            hasPermission  = hasCameraPermission,
            scanEventFlow  = emptyFlow(),
            isTorchOn      = isTorchOn,
            onTorchChange  = { isTorchOn = it },
            onCodeScanned  = { code -> viewModel.onCodeScanned(code) },
            onStatusUpdate = { _, _ -> }
        )

        // Верхний оверлей
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick  = onNavigateBack,
                    modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White) }

                Box(
                    modifier = Modifier
                        .background(SecColors.Accent.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("${state.mode.emoji} ${state.mode.label}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                IconButton(
                    onClick  = { isTorchOn = !isTorchOn },
                    modifier = Modifier.size(44.dp).background(
                        if (isTorchOn) SecColors.Warning else Color.Black.copy(alpha = 0.55f), CircleShape
                    )
                ) {
                    Icon(if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff, "Фонарик", tint = if (isTorchOn) Color.Black else Color.White)
                }
            }

            ScannerModeSelector(currentMode = state.mode, onModeChange = { viewModel.setMode(it) })
        }

        // Нижняя панель: ручной ввод
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Keyboard, null, tint = SecColors.TextMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Ручной ввод (QR повреждён)", color = SecColors.TextSecondary, fontSize = 13.sp)
                }
                IconButton(
                    onClick  = { showManualInput = !showManualInput; if (!showManualInput) manualInputText = "" },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(if (showManualInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = SecColors.TextMuted)
                }
            }

            AnimatedVisibility(visible = showManualInput, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                ManualInputPanel(
                    mode         = state.mode,
                    text         = manualInputText,
                    onTextChange = { manualInputText = it },
                    onSubmit     = {
                        if (manualInputText.isNotBlank()) {
                            viewModel.onManualInput(manualInputText)
                            manualInputText = ""
                            showManualInput = false
                        }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Лоадер
        if (state.isProcessing && !state.showNewPassportDialog && !state.showBatteryResultDialog) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SecColors.Accent)
                    Spacer(Modifier.height(12.dp))
                    Text(if (state.mode == ScannerMode.BATTERY) "Ищем АКБ в базе..." else "Получаем GPS и проверяем базу...", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Тост ошибки
        state.error?.let { err ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 160.dp, start = 16.dp, end = 16.dp)
                    .background(SecColors.Danger, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(err, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            LaunchedEffect(err) { delay(3000); viewModel.dismissError() }
        }
    }

    // ── Диалог данных флита (умный поиск) ─────────────────────────────────
    val fleetInfo = state.fleetInfo
    if (state.showFleetInfoDialog && fleetInfo != null) {
        FleetInfoDialog(
            code      = state.scannedCode ?: "",
            info      = fleetInfo,  // ← теперь локальная переменная
            onDismiss = { viewModel.dismissDialog() },
            onCreatePassport = { viewModel.proceedToCreatePassport() }
        )
    }

    if (state.showNewPassportDialog) {
        NewFieldPassportDialog(
            state     = state,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { tags, notes, isLost -> viewModel.createNewPassport(tags, notes, isLost) }
        )
    }

    if (state.showBatteryResultDialog) {
        BatteryLookupResultDialog(state = state, onDismiss = { viewModel.dismissDialog() })
    }
}

// ============================================================================================
// ПЕРЕКЛЮЧАТЕЛЬ РЕЖИМА
// ============================================================================================

@Composable
private fun ScannerModeSelector(currentMode: ScannerMode, onModeChange: (ScannerMode) -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 4.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScannerMode.entries.forEach { mode ->
            val isActive = mode == currentMode
            Surface(
                onClick  = { onModeChange(mode) },
                shape    = RoundedCornerShape(10.dp),
                color    = if (isActive) SecColors.Accent else Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(mode.emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(mode.label, color = if (isActive) Color.White else Color.White.copy(alpha = 0.6f), fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                }
            }
        }
    }
}

// ============================================================================================
// ПАНЕЛЬ РУЧНОГО ВВОДА
// ============================================================================================

@Composable
private fun ManualInputPanel(mode: ScannerMode, text: String, onTextChange: (String) -> Unit, onSubmit: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val keyboard     = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(
            text          = when (mode) { ScannerMode.SCOOTER -> "Введите номер самоката"; ScannerMode.BATTERY -> "Введите код АКБ" },
            color         = SecColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
            modifier      = Modifier.padding(bottom = 6.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                BasicTextField(
                    value           = text,
                    onValueChange   = { onTextChange(it.uppercase().filter { c -> c.isLetterOrDigit() }) },
                    singleLine      = true,
                    textStyle       = TextStyle(color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
                    cursorBrush     = SolidColor(SecColors.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Characters, autoCorrect = false),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboard?.hide(); onSubmit() }),
                    decorationBox   = { inner ->
                        Box {
                            if (text.isEmpty()) Text(
                                when (mode) { ScannerMode.SCOOTER -> "HE600A..."; ScannerMode.BATTERY -> "4BB32501113863..." },
                                color = Color.White.copy(alpha = 0.3f), fontSize = 15.sp, fontFamily = FontFamily.Monospace
                            )
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FilledIconButton(
                onClick  = { focusManager.clearFocus(); keyboard?.hide(); onSubmit() },
                enabled  = text.isNotBlank(),
                shape    = RoundedCornerShape(10.dp),
                colors   = IconButtonDefaults.filledIconButtonColors(containerColor = SecColors.Accent),
                modifier = Modifier.size(48.dp)
            ) { Icon(Icons.Default.Search, null, tint = Color.White) }
        }
    }
}

// ============================================================================================
// ДИАЛОГ РЕЗУЛЬТАТА ПОИСКА АКБ
// ============================================================================================

@Composable
private fun BatteryLookupResultDialog(state: SecurityScannerState, onDismiss: () -> Unit) {
    val result = state.batteryLookupResult
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SecColors.Card,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (state.batteryNotFound) Icons.Default.Warning else Icons.Default.BatteryChargingFull,
                    null,
                    tint     = if (state.batteryNotFound) SecColors.Warning else SecColors.Success,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(if (state.batteryNotFound) "АКБ не найдена" else "АКБ найдена", color = if (state.batteryNotFound) SecColors.Warning else SecColors.Success, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(state.scannedCode ?: "", color = SecColors.TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        text = {
            if (state.batteryNotFound) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Этот АКБ отсутствует в базе данных склада.", color = SecColors.TextSecondary, fontSize = 14.sp)
                    Text("Возможно, он ещё не был отсканирован или принадлежит другому подразделению.", color = SecColors.TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            } else if (result != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BatteryInfoRow(Icons.Outlined.Inventory, "Ячейка", if (result.palletNumber > 0) "№${result.palletNumber}" else "Не привязана")
                    if (!result.cellType.isNullOrBlank()) {
                        val typeLabel = when (result.cellType.uppercase()) {
                            "FUJIAN" -> "WIND 4.0 FUJIAN"; "BYD" -> "WIND 4.0 BYD"
                            "NINEBOT_NEW", "NEW" -> "WIND 5.0 Новый"; "NINEBOT_OLD", "OLD" -> "WIND 5.0 Старый"
                            else -> result.cellType
                        }
                        BatteryInfoRow(Icons.Outlined.Category, "Тип АКБ", typeLabel)
                    }
                    BatteryInfoRow(Icons.Outlined.QrCode2, "По коду", state.itemType)
                    if (!result.creatorName.isNullOrBlank()) BatteryInfoRow(Icons.Outlined.Person, "Принял", result.creatorName)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = if (state.batteryNotFound) SecColors.Warning else SecColors.Success)
            ) { Text("Понятно", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
private fun BatteryInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().background(SecColors.TagBg, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SecColors.TextMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = SecColors.TextMuted, fontSize = 11.sp)
            Text(value, color = SecColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ============================================================================================
// ДИАЛОГ НОВОГО ПАСПОРТА В ПОЛЕ
// ============================================================================================

@Composable
fun NewFieldPassportDialog(
    state: SecurityScannerState,
    onDismiss: () -> Unit,
    onConfirm: (List<ScooterTag>, String, Boolean) -> Unit
) {
    var notes        by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<ScooterTag>() }
    var isLost       by remember { mutableStateOf(true) }

    // Лаунчер для запроса разрешения локации прямо из диалога
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* После выдачи разрешения TelemetryManager получит координаты автоматически */ }

    val hasGps   = state.currentLocation != null
    val gpsColor = if (hasGps) SecColors.Success else SecColors.Warning

    AlertDialog(
        onDismissRequest = { if (!state.isProcessing) onDismiss() },
        containerColor   = SecColors.Card,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text("Новый объект в поле", color = SecColors.TextPrimary, fontWeight = FontWeight.Bold)
                Text(state.itemType, color = SecColors.Accent, fontSize = 13.sp)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier            = Modifier.verticalScroll(rememberScrollState())
            ) {
                // ID объекта
                OutlinedTextField(
                    value         = state.scannedCode ?: "",
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("ID объекта", color = SecColors.TextSecondary) },
                    shape         = RoundedCornerShape(12.dp),
                    colors        = dialogReadonlyFieldColors(SecColors.TextPrimary),
                    modifier      = Modifier.fillMaxWidth()
                )

                // ── GPS блок ────────────────────────────────────────────────
                if (hasGps) {
                    // Координаты получены — показываем зелёное поле
                    Surface(
                        shape  = RoundedCornerShape(12.dp),
                        color  = SecColors.Success.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, SecColors.Success.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = SecColors.Success, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("КООРДИНАТЫ ЗАФИКСИРОВАНЫ", color = SecColors.Success, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                Spacer(Modifier.height(2.dp))
                                Text("Ш: ${"%.6f".format(state.currentLocation!!.latitude)}", color = SecColors.TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                Text("Д: ${"%.6f".format(state.currentLocation.longitude)}", color = SecColors.TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                } else {
                    // GPS нет — показываем кнопку для запроса разрешения
                    Surface(
                        shape    = RoundedCornerShape(12.dp),
                        color    = SecColors.Warning.copy(alpha = 0.06f),
                        border   = BorderStroke(1.dp, SecColors.Warning.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                locationPermLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                    ) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOff, null, tint = SecColors.Warning, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("GPS недоступен", color = SecColors.Warning, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Нажмите чтобы разрешить геолокацию", color = SecColors.TextMuted, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = SecColors.Warning, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(
                        "Координаты будут записаны при следующем открытии диалога",
                        color    = SecColors.TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                // Статус: Утерян / Найден
                Text("Статус", color = SecColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick  = { isLost = true },
                        shape    = RoundedCornerShape(10.dp),
                        color    = if (isLost) SecColors.Accent else SecColors.TagBg,
                        border   = if (!isLost) BorderStroke(1.dp, SecColors.CardBorder) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GppBad, null, tint = if (isLost) Color.White else SecColors.TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Утерян", textAlign = TextAlign.Center, color = if (isLost) Color.White else SecColors.TextSecondary, fontSize = 13.sp, fontWeight = if (isLost) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    Surface(
                        onClick  = { isLost = false },
                        shape    = RoundedCornerShape(10.dp),
                        color    = if (!isLost) SecColors.Success else SecColors.TagBg,
                        border   = if (isLost) BorderStroke(1.dp, SecColors.CardBorder) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, null, tint = if (!isLost) Color.White else SecColors.TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Найден", textAlign = TextAlign.Center, color = if (!isLost) Color.White else SecColors.TextSecondary, fontSize = 13.sp, fontWeight = if (!isLost) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // Теги
                Text("Классификация", color = SecColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScooterTag.entries.forEach { tag ->
                        val selected = tag in selectedTags
                        val color    = scooterTagColor(tag)
                        FilterChip(
                            selected = selected,
                            onClick  = { if (selected) selectedTags.remove(tag) else selectedTags.add(tag) },
                            label    = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(scooterTagIcon(tag), null, modifier = Modifier.size(13.dp), tint = if (selected) color else SecColors.TextMuted)
                                    Spacer(Modifier.width(4.dp))
                                    Text(tag.label, fontSize = 12.sp)
                                }
                            },
                            shape  = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor     = color,
                                containerColor         = SecColors.TagBg,
                                labelColor             = SecColors.TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = selected,
                                selectedBorderColor = color.copy(alpha = 0.5f),
                                borderColor = SecColors.CardBorder
                            )
                        )
                    }
                }

                // Заметки
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Детали (опционально)", color = SecColors.TextSecondary) },
                    maxLines      = 3,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = SecColors.Accent,
                        unfocusedBorderColor    = SecColors.CardBorder,
                        focusedContainerColor   = SecColors.TagBg,
                        unfocusedContainerColor = SecColors.TagBg,
                        cursorColor             = SecColors.Accent,
                        focusedTextColor        = SecColors.TextPrimary,
                        unfocusedTextColor      = SecColors.TextPrimary,
                        focusedLabelColor       = SecColors.Accent,
                        unfocusedLabelColor     = SecColors.TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(selectedTags.toList(), notes, isLost) },
                enabled  = !state.isProcessing,
                colors   = ButtonDefaults.buttonColors(containerColor = SecColors.Accent),
                shape    = RoundedCornerShape(12.dp)
            ) {
                if (state.isProcessing) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Сохранить", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isProcessing) {
                Text("Отмена", color = SecColors.TextSecondary)
            }
        }
    )
}

// ============================================================================================
// ДИАЛОГ ДАННЫХ ФЛИТА (умный поиск)
// Показывается когда паспорта СБ нет, но самокат найден в fleet_vehicles
// ============================================================================================

@Composable
fun FleetInfoDialog(
    code: String,
    info: FleetScooterInfo,
    onDismiss: () -> Unit,
    onCreatePassport: () -> Unit
) {
    val context = LocalContext.current

    // Цвет заряда
    val chargeColor = when {
        info.charge <= 0  -> SecColors.Danger
        info.charge <= 25 -> SecColors.Danger
        info.charge <= 45 -> SecColors.Warning
        info.charge <= 70 -> Color(0xFFFFEB3B)
        else              -> SecColors.Success
    }

    // Статус подозрительности
    val suspectColor = if (info.isSuspicious) SecColors.Danger else SecColors.TextMuted

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SecColors.Card,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Column {
                // Заголовок с номером
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TwoWheeler, null,
                        tint     = SecColors.Accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            code,
                            color         = SecColors.TextPrimary,
                            fontWeight    = FontWeight.ExtraBold,
                            fontSize      = 18.sp,
                            fontFamily    = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Найден в базе флита",
                            color    = SecColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Статус — живой / не живой
                Surface(
                    shape  = RoundedCornerShape(8.dp),
                    color  = (if (info.isAlive) SecColors.Success else SecColors.Danger).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, (if (info.isAlive) SecColors.Success else SecColors.Danger).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (info.isAlive) Icons.Default.Wifi else Icons.Default.WifiOff,
                            null,
                            tint     = if (info.isAlive) SecColors.Success else SecColors.Danger,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (info.isAlive) "Живой — пингует" else "Не живой — нет сигнала",
                            color      = if (info.isAlive) SecColors.Success else SecColors.Danger,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Модель
                if (info.model.isNotBlank()) {
                    FleetInfoRow(Icons.Default.DirectionsBike, "Модель", info.model)
                }

                // Процесс / стадия
                if (info.process.isNotBlank() || info.processStage.isNotBlank()) {
                    val processText = listOf(info.process, info.processStage)
                        .filter { it.isNotBlank() }.joinToString(" → ")
                    FleetInfoRow(Icons.Default.Assignment, "Процесс", processText)
                }

                // Заряд с цветом
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .background(SecColors.TagBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BatteryChargingFull, null, tint = SecColors.TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Заряд", color = SecColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(80.dp))
                    Text(
                        if (info.charge > 0) "${info.charge}%" else "Нет данных",
                        color      = chargeColor,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Heartbeat
                if (info.heartbeatLag.isNotBlank()) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .background(SecColors.TagBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, null, tint = suspectColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Heartbeat", color = SecColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(80.dp))
                        Column {
                            Text(info.heartbeatLag, color = suspectColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                            if (info.isSuspicious) {
                                Text("⚠ Не пингует более 24ч", color = SecColors.Danger, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Последнее обновление
                if (info.statusUpdatedDate.isNotBlank()) {
                    FleetInfoRow(Icons.Outlined.Schedule, "Обновлён", "${info.statusUpdatedDate} ${info.statusUpdatedTime}".trim())
                }

                // Координаты — кликабельные
                if (info.hasCoords) {
                    Surface(
                        shape    = RoundedCornerShape(8.dp),
                        color    = SecColors.TagBg,
                        border   = BorderStroke(1.dp, if (info.isAlive) SecColors.Success.copy(alpha = 0.3f) else SecColors.CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val uri    = Uri.parse("geo:${info.lat},${info.lon}?q=${info.lat},${info.lon}")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(Intent.createChooser(intent, "Открыть в картах"))
                            }
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn, null,
                                tint     = if (info.isAlive) SecColors.Success else SecColors.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${"%.6f".format(info.lat)}, ${"%.6f".format(info.lon)}",
                                    color      = SecColors.TextPrimary,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    if (info.isAlive) "Актуальные · нажмите для навигации"
                                    else "Последние известные · нажмите для навигации",
                                    color    = if (info.isAlive) SecColors.Success.copy(alpha = 0.7f) else SecColors.TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(Icons.Default.OpenInNew, null, tint = SecColors.TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Паспорта СБ нет — предупреждение
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape  = RoundedCornerShape(8.dp),
                    color  = SecColors.Warning.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, SecColors.Warning.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FolderOff, null, tint = SecColors.Warning, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Паспорт СБ не создан. Создать сейчас?",
                            color    = SecColors.Warning,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onCreatePassport,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SecColors.Accent)
            ) {
                Icon(Icons.Default.GppBad, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Создать паспорт", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = SecColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun FleetInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(SecColors.TagBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SecColors.TextMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = SecColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(80.dp))
        Text(value, color = SecColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================================================================
// УТИЛИТЫ
// ============================================================================================

@Composable
private fun dialogReadonlyFieldColors(textColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor        = textColor,
    unfocusedTextColor      = textColor,
    focusedBorderColor      = SecColors.CardBorder,
    unfocusedBorderColor    = SecColors.CardBorder,
    focusedContainerColor   = SecColors.TagBg,
    unfocusedContainerColor = SecColors.TagBg,
    focusedLabelColor       = SecColors.TextSecondary,
    unfocusedLabelColor     = SecColors.TextSecondary
)