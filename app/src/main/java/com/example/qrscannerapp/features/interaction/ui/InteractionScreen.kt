package com.example.qrscannerapp.features.interaction.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File as JavaFile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.qrscannerapp.core.model.ScanEvent // <-- ИМПОРТ ДОБАВЛЕН ЗДЕСЬ
import com.example.qrscannerapp.features.interaction.domain.model.BatteryIssuance
import com.example.qrscannerapp.features.interaction.domain.model.BatteryReception
import com.example.qrscannerapp.features.interaction.domain.model.SbEmployee
import com.example.qrscannerapp.features.scanner.ui.components.CameraView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── MODERN COLOR PALETTE ───────────────────────────────────────────────────────
private val BgDeep = Color(0xFF09090D)         // Глубокий черный фон
private val CardBg = Color(0xFF14141B)         // Темно-серый для карточек
private val AccentBlue = Color(0xFF2563EB)     // Насыщенный синий (как в Zentra/AI)
private val AccentBlueGlow = Color(0xFF3B82F6) // Светло-синий для свечения
private val AccentCyan = Color(0xFF00E5FF)     // Кибер-циан
private val AccentGreen = Color(0xFF10B981)    // Неоновый зеленый (как в Homex)
private val StardustError = Color(0xFFEF4444)  // Красный (ошибки)
private val StardustTextPrimary = Color(0xFFF8FAFC)
private val StardustTextSecondary = Color(0xFF94A3B8)
private val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.08f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionScreen(
    viewModel: InteractionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val step by viewModel.step.collectAsState()
    val receptionScooterCodes by viewModel.receptionScooterCodes.collectAsState()
    val activeIssuances by viewModel.activeIssuances.collectAsState()
    val recentReceptions by viewModel.recentReceptions.collectAsState()
    val sbEmployees by viewModel.sbEmployees.collectAsState()
    val selectedIssuance by viewModel.selectedIssuance.collectAsState()
    val selectedReception by viewModel.selectedReception.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val context = LocalContext.current

    var cameraPermission by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraPermission = it }
    LaunchedEffect(Unit) { permLauncher.launch(Manifest.permission.CAMERA) }

    // Анимация цвета свечения в зависимости от режима (выдача/приемка)
    val isReceptionMode = step == IssuanceStep.SCANNING_RECEPTION ||
            step == IssuanceStep.CONFIRMING_RECEPTION ||
            step == IssuanceStep.DETAILS_RECEPTION

    val glowColor by animateColorAsState(
        targetValue = if (isReceptionMode) AccentGreen else AccentBlue,
        animationSpec = tween(700), label = "glow"
    )

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        // Spotlight Effect (Градиентное свечение сверху как в NebulaDEX)
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .alpha(0.15f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(screenWidthPx / 2f, -100f),
                        radius = screenWidthPx
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (step) {
                                IssuanceStep.CONFIRMING -> "Выдача АКБ"
                                IssuanceStep.DETAILS -> "Детали выдачи"
                                IssuanceStep.SCANNING_RECEPTION -> "Скан приёмки"
                                IssuanceStep.CONFIRMING_RECEPTION -> "Приёмка АКБ"
                                IssuanceStep.DETAILS_RECEPTION -> "Детали приёмки"
                                else -> "Операции с АКБ"
                            },
                            color = StardustTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                when (step) {
                                    IssuanceStep.CONFIRMING -> viewModel.cancelIssuance()
                                    IssuanceStep.DETAILS -> viewModel.closeDetails()
                                    IssuanceStep.SCANNING_RECEPTION, IssuanceStep.CONFIRMING_RECEPTION -> viewModel.cancelReception()
                                    IssuanceStep.DETAILS_RECEPTION -> viewModel.closeReceptionDetails()
                                    else -> onNavigateBack()
                                }
                            },
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(CardBg.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = StardustTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                },
                label = "step"
            ) { currentStep ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    when (currentStep) {
                        IssuanceStep.IDLE -> IdleScreen(
                            issuances = activeIssuances,
                            receptions = recentReceptions,
                            onIssueClick = { viewModel.startIssuance() },
                            onReceiveClick = { viewModel.startReceptionScanning() },
                            onIssuanceCardClick = { viewModel.openDetails(it) },
                            onReceptionCardClick = { viewModel.openReceptionDetails(it) }
                        )
                        IssuanceStep.CONFIRMING -> ConfirmingScreen(
                            sbEmployees = sbEmployees,
                            isSaving = isSaving,
                            isReception = false,
                            onConfirm = { employeeId, employeeName, batteryCount, reaniCount, comment, photoUri, _, _ ->
                                viewModel.confirmIssuance(employeeId, employeeName, batteryCount, reaniCount, comment, photoUri, context)
                            }
                        )
                        IssuanceStep.DETAILS -> selectedIssuance?.let {
                            DetailsScreen(it, context, onDelete = { viewModel.deleteIssuance(it.id) })
                        }
                        IssuanceStep.DETAILS_RECEPTION -> selectedReception?.let {
                            ReceptionDetailsScreen(it, context, onDelete = { viewModel.deleteReception(it.id) })
                        }
                        IssuanceStep.SCANNING_RECEPTION -> ReceptionScanningScreen(
                            hasPermission = cameraPermission,
                            scooterCodes = receptionScooterCodes,
                            scanEventFlow = viewModel.scanEventFlow,
                            onCodeScanned = viewModel::onReceptionCodeScanned,
                            onRemoveScooterCode = viewModel::removeReceptionScooterCode,
                            onDone = { viewModel.finishReceptionScanning() }
                        )
                        IssuanceStep.CONFIRMING_RECEPTION -> ConfirmingScreen(
                            sbEmployees = sbEmployees,
                            scooterCount = receptionScooterCodes.size,
                            isSaving = isSaving,
                            isReception = true,
                            activeIssuances = activeIssuances,
                            onConfirm = { employeeId, employeeName, batteryCount, reaniCount, comment, photoUri, closedIssuanceId, expectedCount ->
                                viewModel.confirmReception(employeeId, employeeName, batteryCount, reaniCount, comment, photoUri, context, closedIssuanceId, expectedCount)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── IDLE (ГЛАВНЫЙ ЭКРАН) ─────────────────────────────────────────────────────
@Composable
private fun IdleScreen(
    issuances: List<BatteryIssuance>,
    receptions: List<BatteryReception>,
    onIssueClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onIssuanceCardClick: (BatteryIssuance) -> Unit,
    onReceptionCardClick: (BatteryReception) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var sortDescending by remember { mutableStateOf(true) }

    val filteredIssuances = remember(issuances, searchQuery, sortDescending) {
        issuances
            .filter { searchQuery.isBlank() || it.issuedToName.contains(searchQuery, ignoreCase = true) }
            .let { if (sortDescending) it else it.reversed() }
    }
    val filteredReceptions = remember(receptions, searchQuery, sortDescending) {
        receptions
            .filter { searchQuery.isBlank() || it.receivedFromName.contains(searchQuery, ignoreCase = true) }
            .let { if (sortDescending) it else it.reversed() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Modern Segmented Control
        ModernSegmentedControl(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            title1 = "Выдачи ${if(issuances.isNotEmpty()) "(${issuances.size})" else ""}",
            title2 = "Приёмки ${if(receptions.isNotEmpty()) "(${receptions.size})" else ""}"
        )

        // Search & Sort
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = TextStyle(color = StardustTextPrimary, fontSize = 15.sp),
                singleLine = true,
                cursorBrush = SolidColor(AccentBlue),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Outlined.Search, null, tint = StardustTextSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) Text("Поиск по сотруднику...", color = StardustTextSecondary, fontSize = 15.sp)
                            innerTextField()
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable { sortDescending = !sortDescending },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (sortDescending) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // List
        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                if (filteredIssuances.isEmpty()) EmptyState(Icons.Outlined.BatteryChargingFull, "Нет активных выдач")
                else LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredIssuances, key = { it.id }) { issuance ->
                        IssuanceCard(issuance) { onIssuanceCardClick(issuance) }
                    }
                }
            } else {
                if (filteredReceptions.isEmpty()) EmptyState(Icons.Outlined.MoveToInbox, "Нет записей о приёмке")
                else LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredReceptions, key = { it.id }) { reception ->
                        ReceptionCard(reception) { onReceptionCardClick(reception) }
                    }
                }
            }

            // Glass Floating Buttons Bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassFAB(
                    text = "Выдать АКБ",
                    icon = Icons.Filled.AddCircle,
                    color = AccentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onIssueClick
                )
                GlassFAB(
                    text = "Принять",
                    icon = Icons.Filled.MoveToInbox,
                    color = AccentGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onReceiveClick
                )
            }
        }
    }
}

@Composable
private fun ModernSegmentedControl(selectedTab: Int, onTabSelected: (Int) -> Unit, title1: String, title2: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(CardBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(26.dp))
            .padding(4.dp)
    ) {
        listOf(title1 to AccentBlue, title2 to AccentGreen).forEachIndexed { index, (title, color) ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) color else StardustTextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun GlassFAB(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(color.copy(alpha = 0.1f), color.copy(alpha = 0.25f)))
            )
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun IssuanceCard(issuance: BatteryIssuance, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val statusColor = if (issuance.isActive) AccentBlue else StardustTextSecondary

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    if (issuance.isActive) {
        LaunchedEffect(issuance.id) {
            while (true) {
                delay(60_000L)
                now = System.currentTimeMillis()
            }
        }
    }
    val elapsedMs = now - issuance.timestamp
    val timerColor = when {
        elapsedMs < 4 * 3600_000L  -> AccentGreen
        elapsedMs < 8 * 3600_000L  -> Color(0xFFFBBF24) // Yellow
        else                        -> StardustError
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = if (issuance.isActive) 0.8f else 0.4f)),
        border = BorderStroke(1.dp, if (issuance.isActive) AccentBlue.copy(alpha = 0.3f) else GlassBorder)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, null, tint = statusColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        issuance.issuedToName,
                        color = StardustTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (issuance.isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Активна", color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgeChip(Icons.Outlined.BatteryFull, "${issuance.batteryCount} АКБ", AccentCyan)
                    if (issuance.reanimatorCount > 0) BadgeChip(Icons.Outlined.ElectricBolt, "${issuance.reanimatorCount} реан.", AccentBlue)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (issuance.isActive) {
                        Icon(Icons.Outlined.Timer, null, tint = timerColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(formatElapsed(elapsedMs), color = timerColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("  •  ", color = StardustTextSecondary, fontSize = 12.sp)
                    }
                    Text(fmt.format(Date(issuance.timestamp)), color = StardustTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ReceptionCard(reception: BatteryReception, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.15f))
                    .border(1.dp, AccentGreen.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MoveToInbox, null, tint = AccentGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "От: ${reception.receivedFromName}",
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    if (reception.batteryCount > 0) BadgeChip(Icons.Outlined.BatteryFull, "${reception.batteryCount} АКБ", AccentCyan)
                    if (reception.scooterCodes.isNotEmpty()) BadgeChip(Icons.Filled.ElectricScooter, "${reception.scooterCodes.size} самок.", AccentGreen)
                    if (reception.reanimatorCount > 0) BadgeChip(Icons.Outlined.ElectricBolt, "${reception.reanimatorCount} реан.", AccentBlue)
                }
                Spacer(Modifier.height(10.dp))
                Text(fmt.format(Date(reception.timestamp)), color = StardustTextSecondary, fontSize = 12.sp)

                if (reception.expectedBatteryCount > 0 && reception.expectedBatteryCount != reception.batteryCount) {
                    val diff = reception.batteryCount - reception.expectedBatteryCount
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StardustError.copy(alpha = 0.15f))
                            .border(1.dp, StardustError.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = StardustError, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (diff < 0) "Недостача: $diff АКБ" else "Излишек: +$diff АКБ", color = StardustError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(CardBg)
                .border(1.dp, GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = StardustTextSecondary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(text, color = StardustTextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── CONFIRMING SCREEN (ФОРМЫ И ВВОД) ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmingScreen(
    sbEmployees: List<SbEmployee>,
    scooterCount: Int = 0,
    isSaving: Boolean,
    isReception: Boolean,
    activeIssuances: List<BatteryIssuance> = emptyList(),
    onConfirm: (String, String, Int, Int, String, Uri?, String?, Int) -> Unit
) {
    val context = LocalContext.current
    var selectedEmployee by remember { mutableStateOf<SbEmployee?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedIssuanceToClose by remember { mutableStateOf<BatteryIssuance?>(null) }
    var issuanceDropdownExpanded by remember { mutableStateOf(false) }
    var batteryCount by remember { mutableIntStateOf(0) }
    var reanimatorCount by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val accent = if (isReception) AccentGreen else AccentBlue

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { photoUri = it } }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) photoUri = cameraUri }

    fun launchCamera() {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "akb_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Neon Header Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isReception) Icons.Outlined.MoveToInbox else Icons.Outlined.BatteryFull, null, tint = accent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(if (isReception) "Оформление приёмки" else "Оформление выдачи", color = StardustTextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(if (isReception) "АКБ: $batteryCount • Самок: $scooterCount" else "Выбрано: $batteryCount АКБ", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Employee Dropdown
        InputSection("Сотрудник СБ") {
            ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }) {
                ModernTextField(
                    value = selectedEmployee?.displayName ?: "",
                    hint = "Выбрать сотрудника...",
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(CardBg)
                ) {
                    if (sbEmployees.isEmpty()) {
                        DropdownMenuItem(text = { Text("Нет сотрудников", color = StardustTextSecondary) }, onClick = { dropdownExpanded = false })
                    } else {
                        sbEmployees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text(emp.displayName, color = StardustTextPrimary) },
                                onClick = { selectedEmployee = emp; dropdownExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        // Battery Counter (Homex Style Widget)
        InputSection("Количество АКБ") {
            BatteryCounterWidget(value = batteryCount, onValueChange = { batteryCount = it }, accentColor = accent)
        }

        InputSection("Реаниматоры") {
            ModernSegmentedControl(
                selectedTab = reanimatorCount,
                onTabSelected = { reanimatorCount = it },
                title1 = "0", title2 = "1"
            )
            // If need 3 options (0,1,2):
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0,1,2).forEach { opt ->
                    val isSel = reanimatorCount == opt
                    Box(modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp))
                        .background(if(isSel) accent.copy(0.2f) else CardBg)
                        .border(1.dp, if(isSel) accent.copy(0.5f) else GlassBorder, RoundedCornerShape(14.dp))
                        .clickable { reanimatorCount = opt }, contentAlignment = Alignment.Center
                    ){ Text("$opt", color= if(isSel) accent else StardustTextSecondary, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                }
            }
        }

        if (isReception && activeIssuances.any { it.isActive }) {
            InputSection("Связать с выдачей (опционально)") {
                ExposedDropdownMenuBox(expanded = issuanceDropdownExpanded, onExpandedChange = { issuanceDropdownExpanded = it }) {
                    ModernTextField(
                        value = selectedIssuanceToClose?.let { "СБ ${it.issuedToName} — ${it.batteryCount} АКБ" } ?: "",
                        hint = "Не привязывать",
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = issuanceDropdownExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = issuanceDropdownExpanded, onDismissRequest = { issuanceDropdownExpanded = false }, modifier = Modifier.background(CardBg)) {
                        DropdownMenuItem(text = { Text("Не привязывать", color = StardustTextSecondary) }, onClick = { selectedIssuanceToClose = null; issuanceDropdownExpanded = false })
                        activeIssuances.filter { it.isActive }.forEach { iss ->
                            DropdownMenuItem(
                                text = { Column { Text("СБ ${iss.issuedToName}", color = StardustTextPrimary, fontWeight = FontWeight.SemiBold); Text("${iss.batteryCount} АКБ", color = StardustTextSecondary, fontSize = 12.sp) } },
                                onClick = { selectedIssuanceToClose = iss; issuanceDropdownExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        InputSection("Комментарий") {
            BasicTextField(
                value = comment, onValueChange = { comment = it },
                textStyle = TextStyle(color = StardustTextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(accent),
                modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)).background(CardBg).border(1.dp, GlassBorder, RoundedCornerShape(16.dp)).padding(16.dp),
                decorationBox = { inner -> if (comment.isEmpty()) Text("Добавить подробности...", color = StardustTextSecondary) else inner() }
            )
        }

        InputSection("Фото подтверждение") {
            if (photoUri != null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, accent.copy(0.3f), RoundedCornerShape(16.dp))) {
                    AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    IconButton(onClick = { photoUri = null }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(36.dp).clip(CircleShape).background(Color.Black.copy(0.6f))) {
                        Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { launchCamera() }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBg), border = BorderStroke(1.dp, GlassBorder)) {
                        Icon(Icons.Outlined.CameraAlt, null, tint = accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Камера", color = StardustTextPrimary)
                    }
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBg), border = BorderStroke(1.dp, GlassBorder)) {
                        Icon(Icons.Outlined.Photo, null, tint = accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Галерея", color = StardustTextPrimary)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                val emp = selectedEmployee
                if (emp == null) { Toast.makeText(context, "Выберите сотрудника", Toast.LENGTH_SHORT).show(); return@Button }
                onConfirm(emp.id, emp.displayName, batteryCount, reanimatorCount, comment.trim(), photoUri, selectedIssuanceToClose?.id, selectedIssuanceToClose?.batteryCount ?: 0)
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(accent.copy(0.8f), accent))), contentAlignment = Alignment.Center) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isReception) Icons.Filled.MoveToInbox else Icons.Filled.Send, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isReception) "ПОДТВЕРДИТЬ ПРИЁМКУ" else "ПОДТВЕРДИТЬ ВЫДАЧУ", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ─── СКАНИРОВАНИЕ (РЕЖИМ ПРИЕМКИ) ─────────────────────────────────────────────
@Composable
private fun ReceptionScanningScreen(
    hasPermission: Boolean, scooterCodes: List<String>, scanEventFlow: kotlinx.coroutines.flow.Flow<ScanEvent>,
    onCodeScanned: (String) -> Unit, onRemoveScooterCode: (String) -> Unit, onDone: () -> Unit
) {
    var isTorchOn by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))) {
            CameraView(
                isSearchMode = false, hasPermission = hasPermission, scanEventFlow = scanEventFlow,
                isTorchOn = isTorchOn, onTorchChange = { isTorchOn = it }, onCodeScanned = onCodeScanned, onStatusUpdate = { _, _ -> }
            )
        }
        Column(modifier = Modifier.fillMaxWidth().background(BgDeep).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (scooterCodes.isEmpty()) "Отсканируйте QR самокатов" else "Самок. отсканировано: ${scooterCodes.size}", color = StardustTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (scooterCodes.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scooterCodes, key = { it }) { code ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardBg).border(1.dp, GlassBorder, RoundedCornerShape(12.dp)).clickable { onRemoveScooterCode(code) }.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ElectricScooter, null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(code, color = StardustTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Filled.Close, null, tint = StardustError, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDone, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text(if (scooterCodes.isEmpty()) "ПРОДОЛЖИТЬ БЕЗ САМОКАТОВ" else "ГОТОВО", fontWeight = FontWeight.ExtraBold, color = Color.Black, fontSize = 16.sp)
            }
        }
    }
}

// ─── ДЕТАЛИ ЗАПИСЕЙ ───────────────────────────────────────────────────────────
@Composable
fun DetailsScreen(issuance: BatteryIssuance, context: Context, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    DetailsLayout(
        title = "Детали выдачи", accent = AccentBlue, photoUrl = issuance.photoUrl,
        onDelete = onDelete, onCopy = { copyText(context, buildIssuanceText(issuance, fmt)) }
    ) {
        DetailRow(Icons.Outlined.Person, "Кому", "СБ: ${issuance.issuedToName}")
        DetailRow(Icons.Outlined.AdminPanelSettings, "Кем", issuance.issuedByName)
        DetailRow(Icons.Outlined.Schedule, "Дата", fmt.format(Date(issuance.timestamp)))
        DetailRow(Icons.Outlined.BatteryFull, "АКБ", "${issuance.batteryCount} шт.", AccentCyan)
        if (issuance.reanimatorCount > 0) DetailRow(Icons.Outlined.ElectricBolt, "Реаниматоры", "${issuance.reanimatorCount} шт.", AccentBlue)
        if (issuance.comment.isNotEmpty()) CommentBlock(issuance.comment)
    }
}

@Composable
fun ReceptionDetailsScreen(reception: BatteryReception, context: Context, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    DetailsLayout(
        title = "Детали приёмки", accent = AccentGreen, photoUrl = reception.photoUrl,
        onDelete = onDelete, onCopy = { copyText(context, buildReceptionText(reception, fmt)) }
    ) {
        if (reception.expectedBatteryCount > 0) {
            val diff = reception.batteryCount - reception.expectedBatteryCount
            val isShortage = diff < 0
            val c = if (isShortage) StardustError else AccentGreen
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.copy(0.15f)).border(1.dp, c.copy(0.4f), RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isShortage) Icons.Filled.Warning else Icons.Filled.CheckCircle, null, tint = c, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(if (isShortage) "НЕДОСТАЧА: $diff АКБ" else "ИЗЛИШЕК: +$diff АКБ", color = c, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("Выдано: ${reception.expectedBatteryCount} / Принято: ${reception.batteryCount}", color = c.copy(0.8f), fontSize = 13.sp)
                }
            }
        }
        DetailRow(Icons.Outlined.Person, "От кого", "СБ: ${reception.receivedFromName}")
        DetailRow(Icons.Outlined.AdminPanelSettings, "Кем принято", reception.receivedByName)
        DetailRow(Icons.Outlined.Schedule, "Дата", fmt.format(Date(reception.timestamp)))
        if (reception.batteryCount > 0) DetailRow(Icons.Outlined.BatteryFull, "АКБ", "${reception.batteryCount} шт.", AccentCyan)
        if (reception.scooterCodes.isNotEmpty()) DetailRow(Icons.Filled.ElectricScooter, "Самокаты", "${reception.scooterCodes.size} шт.", AccentGreen)
        if (reception.reanimatorCount > 0) DetailRow(Icons.Outlined.ElectricBolt, "Реаниматоры", "${reception.reanimatorCount} шт.", AccentBlue)
        if (reception.comment.isNotEmpty()) CommentBlock(reception.comment)

        if (reception.scooterCodes.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Список самокатов", color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            reception.scooterCodes.forEachIndexed { i, code ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardBg).border(1.dp, GlassBorder, RoundedCornerShape(12.dp)).clickable { copyText(context, code) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}.", color = StardustTextSecondary, fontSize = 14.sp, modifier = Modifier.width(28.dp))
                    Text(code, color = StardustTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ContentCopy, null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailsLayout(title: String, accent: Color, photoUrl: String?, onDelete: () -> Unit, onCopy: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, containerColor = CardBg,
            title = { Text("Удалить запись?", color = StardustTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Это действие необратимо.", color = StardustTextSecondary) },
            confirmButton = { TextButton(onClick = { showDialog = false; onDelete() }) { Text("Удалить", color = StardustError, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена", color = StardustTextSecondary) } }
        )
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (photoUrl != null) {
            val photoModel = if (photoUrl.startsWith("/")) JavaFile(photoUrl) else photoUrl
            AsyncImage(model = photoModel, contentDescription = null, modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
        }
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, GlassBorder)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBg), border = BorderStroke(1.dp, accent.copy(0.5f))) {
                Icon(Icons.Outlined.ContentCopy, null, tint = accent); Spacer(Modifier.width(8.dp)); Text("Копировать", color = StardustTextPrimary)
            }
            OutlinedButton(onClick = { showDialog = true }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = StardustError.copy(0.1f)), border = BorderStroke(1.dp, StardustError.copy(0.5f))) {
                Icon(Icons.Outlined.DeleteOutline, null, tint = StardustError); Spacer(Modifier.width(8.dp)); Text("Удалить", color = StardustError)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ─── UTILS & SMALL COMPONENTS ─────────────────────────────────────────────────
@Composable
private fun InputSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        content()
    }
}

@Composable
private fun ModernTextField(value: String, hint: String, readOnly: Boolean = false, trailingIcon: @Composable (() -> Unit)? = null, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = {}, readOnly = readOnly,
        placeholder = { Text(hint, color = StardustTextSecondary) },
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue, unfocusedBorderColor = GlassBorder,
            focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary,
            focusedContainerColor = CardBg, unfocusedContainerColor = CardBg
        )
    )
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, valueColor: Color = StardustTextPrimary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(CardBg).border(1.dp, GlassBorder, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = StardustTextSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = StardustTextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun CommentBlock(text: String) {
    Column {
        Text("Комментарий", color = StardustTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BgDeep).border(1.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(16.dp)) {
            Text(text, color = StardustTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun BatteryCounterWidget(value: Int, onValueChange: (Int) -> Unit, accentColor: Color) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBg).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onValueChange((value - 1).coerceAtLeast(0)) }, modifier = Modifier.size(56.dp).clip(CircleShape).background(BgDeep).border(1.dp, GlassBorder, CircleShape)) {
                Icon(Icons.Default.Remove, null, tint = StardustTextPrimary, modifier = Modifier.size(28.dp))
            }
            Text("$value", color = accentColor, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
            IconButton(onClick = { onValueChange(value + 1) }, modifier = Modifier.size(56.dp).clip(CircleShape).background(accentColor.copy(0.15f)).border(1.dp, accentColor.copy(0.4f), CircleShape)) {
                Icon(Icons.Default.Add, null, tint = accentColor, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 50).forEach { amt ->
                Box(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(BgDeep).border(1.dp, GlassBorder, RoundedCornerShape(12.dp)).clickable { onValueChange(value + amt) }, contentAlignment = Alignment.Center) {
                    Text("+$amt", color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun formatElapsed(ms: Long): String {
    val totalMin = ms / 60_000
    val hours = totalMin / 60
    val mins = totalMin % 60
    return when {
        hours >= 24 -> "${hours / 24}д ${hours % 24}ч"
        hours > 0 -> "${hours}ч ${mins}м"
        else -> "${mins}м"
    }
}

private fun copyText(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clipData = android.content.ClipData.newPlainText("text", text)
    clipboardManager.setPrimaryClip(clipData)
    Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
}

private fun buildIssuanceText(issuance: BatteryIssuance, fmt: SimpleDateFormat): String {
    val sb = StringBuilder()
    sb.appendLine("📦 Выдача АКБ")
    sb.appendLine(fmt.format(Date(issuance.timestamp)))
    sb.appendLine("Выдал: ${issuance.issuedByName}")
    sb.appendLine("Кому СБ: ${issuance.issuedToName}")
    sb.appendLine("АКБ: ${issuance.batteryCount} шт.")
    if (issuance.reanimatorCount > 0) sb.appendLine("Реаниматоры: ${issuance.reanimatorCount} шт.")
    if (issuance.comment.isNotEmpty()) sb.appendLine("💬 ${issuance.comment}")
    return sb.toString().trimEnd()
}

private fun buildReceptionText(reception: BatteryReception, fmt: SimpleDateFormat): String {
    val sb = StringBuilder()
    sb.appendLine("📥 Приёмка")
    sb.appendLine(fmt.format(Date(reception.timestamp)))
    sb.appendLine("Принял: ${reception.receivedByName}")
    sb.appendLine("От СБ: ${reception.receivedFromName}")
    if (reception.batteryCount > 0) sb.appendLine("АКБ: ${reception.batteryCount} шт.")
    if (reception.scooterCodes.isNotEmpty()) sb.appendLine("Самокаты: ${reception.scooterCodes.size} шт.")
    if (reception.reanimatorCount > 0) sb.appendLine("Реаниматоры: ${reception.reanimatorCount} шт.")
    if (reception.comment.isNotEmpty()) sb.appendLine("💬 ${reception.comment}")
    if (reception.scooterCodes.isNotEmpty()) {
        sb.appendLine()
        reception.scooterCodes.forEach { code -> sb.appendLine("`$code`") }
    }
    return sb.toString().trimEnd()
}