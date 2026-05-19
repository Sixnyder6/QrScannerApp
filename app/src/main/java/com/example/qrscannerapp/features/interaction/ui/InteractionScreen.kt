package com.example.qrscannerapp.features.interaction.ui

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File as JavaFile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.interaction.domain.model.BatteryIssuance
import com.example.qrscannerapp.features.interaction.domain.model.BatteryReception
import com.example.qrscannerapp.features.interaction.domain.model.SbEmployee
import com.example.qrscannerapp.features.scanner.ui.components.CameraView
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val StardustError = Color(0xFFF44336)

private val BgDeep = Color(0xFF0D0D10)
private val CardBg = Color(0xFF1A1A23)
private val AccentCyan = Color(0xFF22D3EE)
private val AccentGreen = Color(0xFF4ADE80)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionScreen(
    viewModel: InteractionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val step by viewModel.step.collectAsState()
    val scannedCodes by viewModel.scannedCodes.collectAsState()
    val receptionBatteryCodes by viewModel.receptionBatteryCodes.collectAsState()
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

    Scaffold(
        containerColor = BgDeep,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (step) {
                            IssuanceStep.SCANNING -> "Сканирование АКБ"
                            IssuanceStep.CONFIRMING -> "Выдача АКБ"
                            IssuanceStep.DETAILS -> "Детали выдачи"
                            IssuanceStep.SCANNING_RECEPTION -> "Сканирование приёмки"
                            IssuanceStep.CONFIRMING_RECEPTION -> "Приёмка"
                            IssuanceStep.DETAILS_RECEPTION -> "Детали приёмки"
                            else -> "Взаимодействие"
                        },
                        color = StardustTextPrimary, fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (step) {
                            IssuanceStep.SCANNING, IssuanceStep.CONFIRMING -> viewModel.cancelScanning()
                            IssuanceStep.DETAILS -> viewModel.closeDetails()
                            IssuanceStep.SCANNING_RECEPTION, IssuanceStep.CONFIRMING_RECEPTION -> viewModel.cancelReception()
                            IssuanceStep.DETAILS_RECEPTION -> viewModel.closeReceptionDetails()
                            else -> onNavigateBack()
                        }
                    }) {
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
            when (currentStep) {
                IssuanceStep.IDLE -> IdleScreen(
                    issuances = activeIssuances,
                    receptions = recentReceptions,
                    padding = padding,
                    onIssueClick = { viewModel.startScanning() },
                    onReceiveClick = { viewModel.startReceptionScanning() },
                    onIssuanceCardClick = { viewModel.openDetails(it) },
                    onReceptionCardClick = { viewModel.openReceptionDetails(it) }
                )
                IssuanceStep.SCANNING -> ScanningScreen(
                    hasPermission = cameraPermission,
                    scannedCodes = scannedCodes,
                    scanEventFlow = viewModel.scanEventFlow,
                    padding = padding,
                    onCodeScanned = viewModel::onCodeScanned,
                    onRemoveCode = viewModel::removeCode,
                    onDone = { viewModel.finishScanning() }
                )
                IssuanceStep.CONFIRMING -> ConfirmingScreen(
                    sbEmployees = sbEmployees,
                    scannedCount = scannedCodes.size,
                    isSaving = isSaving,
                    padding = padding,
                    isReception = false,
                    onConfirm = { employeeId, employeeName, reaniCount, comment, photoUri ->
                        viewModel.confirmIssuance(employeeId, employeeName, reaniCount, comment, photoUri, context)
                    }
                )
                IssuanceStep.DETAILS -> selectedIssuance?.let {
                    DetailsScreen(
                        issuance = it,
                        padding = padding,
                        context = context,
                        onDelete = { viewModel.deleteIssuance(it.id) }
                    )
                }
                IssuanceStep.DETAILS_RECEPTION -> selectedReception?.let {
                    ReceptionDetailsScreen(
                        reception = it,
                        padding = padding,
                        context = context,
                        onDelete = { viewModel.deleteReception(it.id) }
                    )
                }
                IssuanceStep.SCANNING_RECEPTION -> ReceptionScanningScreen(
                    hasPermission = cameraPermission,
                    batteryCodes = receptionBatteryCodes,
                    scooterCodes = receptionScooterCodes,
                    scanEventFlow = viewModel.scanEventFlow,
                    padding = padding,
                    onCodeScanned = viewModel::onReceptionCodeScanned,
                    onRemoveBatteryCode = viewModel::removeReceptionBatteryCode,
                    onRemoveScooterCode = viewModel::removeReceptionScooterCode,
                    onDone = { viewModel.finishReceptionScanning() }
                )
                IssuanceStep.CONFIRMING_RECEPTION -> ConfirmingScreen(
                    sbEmployees = sbEmployees,
                    scannedCount = receptionBatteryCodes.size,
                    scooterCount = receptionScooterCodes.size,
                    isSaving = isSaving,
                    padding = padding,
                    isReception = true,
                    onConfirm = { employeeId, employeeName, reaniCount, comment, photoUri ->
                        viewModel.confirmReception(employeeId, employeeName, reaniCount, comment, photoUri, context)
                    }
                )
            }
        }
    }
}

// ─── IDLE ─────────────────────────────────────────────────────────────────────

@Composable
private fun IdleScreen(
    issuances: List<BatteryIssuance>,
    receptions: List<BatteryReception>,
    padding: PaddingValues,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = StardustPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = StardustPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Выдачи${if (issuances.isNotEmpty()) " (${issuances.size})" else ""}",
                            color = if (selectedTab == 0) StardustPrimary else StardustTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Приёмки${if (receptions.isNotEmpty()) " (${receptions.size})" else ""}",
                            color = if (selectedTab == 1) StardustPrimary else StardustTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            // Search + sort bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск по сотруднику…", color = StardustTextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = StardustTextSecondary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustItemBg,
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg
                    )
                )
                IconButton(
                    onClick = { sortDescending = !sortDescending },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBg)
                ) {
                    Icon(
                        if (sortDescending) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                        contentDescription = if (sortDescending) "Сначала новые" else "Сначала старые",
                        tint = StardustPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    if (filteredIssuances.isEmpty()) {
                        EmptyState(Icons.Outlined.BatteryChargingFull,
                            if (searchQuery.isNotEmpty()) "Ничего не найдено" else "Нет активных выдач")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredIssuances, key = { it.id }) { issuance ->
                                IssuanceCard(issuance = issuance, onClick = { onIssuanceCardClick(issuance) })
                            }
                        }
                    }
                } else {
                    if (filteredReceptions.isEmpty()) {
                        EmptyState(Icons.Outlined.MoveToInbox,
                            if (searchQuery.isNotEmpty()) "Ничего не найдено" else "Нет записей о приёмке")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredReceptions, key = { it.id }) { reception ->
                                ReceptionCard(reception = reception, onClick = { onReceptionCardClick(reception) })
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = onIssueClick,
                containerColor = StardustPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.AddCircle, null) },
                text = { Text("Выдать АКБ", fontWeight = FontWeight.Bold) }
            )
            ExtendedFloatingActionButton(
                onClick = onReceiveClick,
                containerColor = AccentGreen,
                contentColor = Color.Black,
                icon = { Icon(Icons.Filled.MoveToInbox, null) },
                text = { Text("Принять", fontWeight = FontWeight.Bold) }
            )
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = StardustTextSecondary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(text, color = StardustTextSecondary, fontSize = 16.sp)
    }
}

@Composable
private fun IssuanceCard(issuance: BatteryIssuance, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(StardustPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, null, tint = StardustPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(issuance.issuedToName, color = StardustTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgeChip(icon = Icons.Outlined.BatteryFull, label = "${issuance.batteryCodes.size} АКБ", color = AccentCyan)
                    if (issuance.reanimatorCount > 0)
                        BadgeChip(icon = Icons.Outlined.ElectricBolt, label = "${issuance.reanimatorCount} реан.", color = StardustSuccess)
                }
                Spacer(Modifier.height(4.dp))
                Text(fmt.format(Date(issuance.timestamp)), color = StardustTextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = StardustTextSecondary)
        }
    }
}

@Composable
private fun ReceptionCard(reception: BatteryReception, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MoveToInbox, null, tint = AccentGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "От: ${reception.receivedFromName}",
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (reception.batteryCodes.isNotEmpty())
                        BadgeChip(icon = Icons.Outlined.BatteryFull, label = "${reception.batteryCodes.size} АКБ", color = AccentCyan)
                    if (reception.scooterCodes.isNotEmpty())
                        BadgeChip(icon = Icons.Filled.ElectricScooter, label = "${reception.scooterCodes.size} самок.", color = AccentGreen)
                    if (reception.reanimatorCount > 0)
                        BadgeChip(icon = Icons.Outlined.ElectricBolt, label = "${reception.reanimatorCount} реан.", color = StardustSuccess)
                }
                Spacer(Modifier.height(4.dp))
                Text(fmt.format(Date(reception.timestamp)), color = StardustTextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = StardustTextSecondary)
        }
    }
}

@Composable
private fun BadgeChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── SCANNING (ISSUANCE) ──────────────────────────────────────────────────────

@Composable
private fun ScanningScreen(
    hasPermission: Boolean,
    scannedCodes: List<String>,
    scanEventFlow: kotlinx.coroutines.flow.Flow<com.example.qrscannerapp.core.model.ScanEvent>,
    padding: PaddingValues,
    onCodeScanned: (String) -> Unit,
    onRemoveCode: (String) -> Unit,
    onDone: () -> Unit
) {
    var isTorchOn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            CameraView(
                isSearchMode = false,
                hasPermission = hasPermission,
                scanEventFlow = scanEventFlow,
                isTorchOn = isTorchOn,
                onTorchChange = { isTorchOn = it },
                onCodeScanned = onCodeScanned,
                onStatusUpdate = { _, _ -> }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Отсканировано: ${scannedCodes.size} АКБ",
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                if (scannedCodes.isNotEmpty())
                    Text("Нажми чтобы удалить", color = StardustTextSecondary, fontSize = 11.sp)
            }

            if (scannedCodes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(scannedCodes) { code ->
                        ScannedCodeRow(
                            icon = Icons.Outlined.BatteryFull,
                            iconColor = AccentCyan,
                            code = code,
                            onClick = { onRemoveCode(code) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onDone,
                enabled = scannedCodes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
            ) {
                Icon(Icons.Filled.CheckCircle, null)
                Spacer(Modifier.width(8.dp))
                Text("Готово — ${scannedCodes.size} АКБ", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── SCANNING (RECEPTION) ─────────────────────────────────────────────────────

@Composable
private fun ReceptionScanningScreen(
    hasPermission: Boolean,
    batteryCodes: List<String>,
    scooterCodes: List<String>,
    scanEventFlow: kotlinx.coroutines.flow.Flow<com.example.qrscannerapp.core.model.ScanEvent>,
    padding: PaddingValues,
    onCodeScanned: (String) -> Unit,
    onRemoveBatteryCode: (String) -> Unit,
    onRemoveScooterCode: (String) -> Unit,
    onDone: () -> Unit
) {
    var isTorchOn by remember { mutableStateOf(false) }
    val totalCount = batteryCodes.size + scooterCodes.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            CameraView(
                isSearchMode = false,
                hasPermission = hasPermission,
                scanEventFlow = scanEventFlow,
                isTorchOn = isTorchOn,
                onTorchChange = { isTorchOn = it },
                onCodeScanned = onCodeScanned,
                onStatusUpdate = { _, _ -> }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (batteryCodes.isNotEmpty())
                        BadgeChip(Icons.Outlined.BatteryFull, "${batteryCodes.size} АКБ", AccentCyan)
                    if (scooterCodes.isNotEmpty())
                        BadgeChip(Icons.Filled.ElectricScooter, "${scooterCodes.size} самок.", AccentGreen)
                    if (totalCount == 0)
                        Text("Сканируй коды АКБ и самокатов", color = StardustTextSecondary, fontSize = 13.sp)
                }
                if (totalCount > 0)
                    Text("Нажми чтобы удалить", color = StardustTextSecondary, fontSize = 11.sp)
            }

            if (batteryCodes.isNotEmpty() || scooterCodes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(batteryCodes, key = { "b_$it" }) { code ->
                        ScannedCodeRow(Icons.Outlined.BatteryFull, AccentCyan, code) { onRemoveBatteryCode(code) }
                    }
                    items(scooterCodes, key = { "s_$it" }) { code ->
                        ScannedCodeRow(Icons.Filled.ElectricScooter, AccentGreen, code) { onRemoveScooterCode(code) }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onDone,
                enabled = totalCount > 0,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Готово — $totalCount шт.", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
private fun ScannedCodeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    code: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StardustItemBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(code, color = StardustTextPrimary, fontSize = 14.sp)
        }
        Icon(Icons.Filled.Close, null, tint = StardustError, modifier = Modifier.size(16.dp))
    }
}

// ─── CONFIRMING ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmingScreen(
    sbEmployees: List<SbEmployee>,
    scannedCount: Int,
    scooterCount: Int = 0,
    isSaving: Boolean,
    padding: PaddingValues,
    isReception: Boolean,
    onConfirm: (String, String, Int, String, Uri?) -> Unit
) {
    val context = LocalContext.current
    var selectedEmployee by remember { mutableStateOf<SbEmployee?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var reanimatorCount by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { photoUri = it }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) photoUri = cameraUri
    }

    fun launchCamera() {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "akb_photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isReception) AccentGreen.copy(alpha = 0.1f) else AccentCyan.copy(alpha = 0.1f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isReception) {
                Icon(Icons.Outlined.MoveToInbox, null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                val parts = buildList {
                    if (scannedCount > 0) add("$scannedCount АКБ")
                    if (scooterCount > 0) add("$scooterCount самок.")
                }
                Text(parts.joinToString(" + ") + " к приёмке", color = AccentGreen, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Outlined.BatteryFull, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("$scannedCount АКБ готовы к выдаче", color = AccentCyan, fontWeight = FontWeight.SemiBold)
            }
        }

        // Employee dropdown
        SectionLabel(if (isReception) "От кого принято (СБ)" else "Получатель (СБ)")
        ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }) {
            OutlinedTextField(
                value = selectedEmployee?.displayName ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Выбрать сотрудника…", color = StardustTextSecondary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StardustPrimary,
                    unfocusedBorderColor = StardustItemBg,
                    focusedTextColor = StardustTextPrimary,
                    unfocusedTextColor = StardustTextPrimary,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                )
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.background(CardBg)
            ) {
                if (sbEmployees.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Сотрудники СБ не найдены", color = StardustTextSecondary) },
                        onClick = { dropdownExpanded = false }
                    )
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

        // Reanimator counter
        SectionLabel("Кол-во реаниматоров")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(CardBg)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = { if (reanimatorCount > 0) reanimatorCount-- },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.Remove, null, tint = if (reanimatorCount > 0) StardustPrimary else StardustTextSecondary)
            }
            Text(
                "$reanimatorCount",
                color = StardustTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { reanimatorCount++ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.Add, null, tint = StardustPrimary)
            }
        }

        // Comment
        SectionLabel("Комментарий (опционально)")
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            placeholder = { Text("Например: у самоката HA222A нет АКБ", color = StardustTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StardustPrimary,
                unfocusedBorderColor = StardustItemBg,
                focusedTextColor = StardustTextPrimary,
                unfocusedTextColor = StardustTextPrimary,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg
            )
        )

        // Photo
        SectionLabel("Фото (опционально)")
        if (photoUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { photoUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoButton(
                    icon = Icons.Outlined.CameraAlt,
                    label = "Камера",
                    modifier = Modifier.weight(1f),
                    onClick = { launchCamera() }
                )
                PhotoButton(
                    icon = Icons.Outlined.Photo,
                    label = "Галерея",
                    modifier = Modifier.weight(1f),
                    onClick = { galleryLauncher.launch("image/*") }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val emp = selectedEmployee
                if (emp == null) {
                    Toast.makeText(context, "Выберите сотрудника", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                onConfirm(emp.id, emp.displayName, reanimatorCount, comment.trim(), photoUri)
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isReception) AccentGreen else StardustPrimary
            )
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else {
                Icon(if (isReception) Icons.Filled.MoveToInbox else Icons.Filled.Send, null,
                    tint = if (isReception) Color.Black else Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isReception) "Принять" else "Выдать",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isReception) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
private fun PhotoButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, StardustItemBg),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBg)
    ) {
        Icon(icon, null, tint = StardustPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = StardustTextPrimary)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

// ─── DETAILS ──────────────────────────────────────────────────────────────────

@Composable
private fun DetailsScreen(issuance: BatteryIssuance, padding: PaddingValues, context: Context, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { writePdfToUri(issuance, it, context) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = CardBg,
            title = { Text("Удалить запись?", color = StardustTextPrimary) },
            text = { Text("Запись о выдаче будет удалена из Firestore. Это действие нельзя отменить.", color = StardustTextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Удалить", color = StardustError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена", color = StardustTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (issuance.photoUrl != null) {
            val photoModel = if (issuance.photoUrl.startsWith("/")) JavaFile(issuance.photoUrl) else issuance.photoUrl
            AsyncImage(
                model = photoModel,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(Icons.Outlined.Person, "Кому выдано", "СБ: ${issuance.issuedToName}")
                HorizontalDivider(color = StardustItemBg)
                DetailRow(Icons.Outlined.AdminPanelSettings, "Кем выдано",
                    if (issuance.issuedByRole.isNotEmpty()) "${issuance.issuedByRole}: ${issuance.issuedByName}"
                    else issuance.issuedByName)
                HorizontalDivider(color = StardustItemBg)
                DetailRow(Icons.Outlined.Schedule, "Дата / время", fmt.format(Date(issuance.timestamp)))
                HorizontalDivider(color = StardustItemBg)
                DetailRow(Icons.Outlined.BatteryFull, "АКБ", "${issuance.batteryCodes.size} шт.")
                if (issuance.reanimatorCount > 0) {
                    HorizontalDivider(color = StardustItemBg)
                    DetailRow(Icons.Outlined.ElectricBolt, "Реаниматоры", "${issuance.reanimatorCount} шт.")
                }
            }
        }

        if (issuance.comment.isNotEmpty()) {
            Text("Комментарий", color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = StardustPrimary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(issuance.comment, color = StardustTextPrimary, fontSize = 14.sp)
                }
            }
        }

        if (issuance.batteryCodes.isNotEmpty()) {
            Text("Список АКБ", color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    issuance.batteryCodes.forEachIndexed { i, code ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(StardustItemBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${i + 1}.", color = StardustTextSecondary, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                            Icon(Icons.Outlined.BatteryFull, null, tint = AccentCyan, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(code, color = StardustTextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Button(
            onClick = { pdfLauncher.launch("akb_выдача_${issuance.id.take(8)}.pdf") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D3A)),
            border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Outlined.PictureAsPdf, null, tint = StardustPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Экспорт в PDF", color = StardustTextPrimary, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StardustError.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
        ) {
            Icon(Icons.Outlined.DeleteOutline, null, tint = StardustError, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Удалить запись", color = StardustError, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── RECEPTION DETAILS ────────────────────────────────────────────────────────

@Composable
private fun ReceptionDetailsScreen(reception: BatteryReception, padding: PaddingValues, context: Context, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { writeReceptionPdfToUri(reception, it, context) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = CardBg,
            title = { Text("Удалить запись?", color = StardustTextPrimary) },
            text = { Text("Запись о приёмке будет удалена. Это действие нельзя отменить.", color = StardustTextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Удалить", color = StardustError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена", color = StardustTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (reception.photoUrl != null) {
            val photoModel = if (reception.photoUrl.startsWith("/")) JavaFile(reception.photoUrl) else reception.photoUrl
            AsyncImage(
                model = photoModel,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(Icons.Outlined.Person, "От кого принято", "СБ: ${reception.receivedFromName}")
                HorizontalDivider(color = StardustItemBg)
                DetailRow(Icons.Outlined.AdminPanelSettings, "Кем принято", reception.receivedByName)
                HorizontalDivider(color = StardustItemBg)
                DetailRow(Icons.Outlined.Schedule, "Дата / время", fmt.format(Date(reception.timestamp)))
                if (reception.batteryCodes.isNotEmpty()) {
                    HorizontalDivider(color = StardustItemBg)
                    DetailRow(Icons.Outlined.BatteryFull, "АКБ принято", "${reception.batteryCodes.size} шт.")
                }
                if (reception.scooterCodes.isNotEmpty()) {
                    HorizontalDivider(color = StardustItemBg)
                    DetailRow(Icons.Filled.ElectricScooter, "Самокатов принято", "${reception.scooterCodes.size} шт.")
                }
                if (reception.reanimatorCount > 0) {
                    HorizontalDivider(color = StardustItemBg)
                    DetailRow(Icons.Outlined.ElectricBolt, "Реаниматоры", "${reception.reanimatorCount} шт.")
                }
            }
        }

        if (reception.comment.isNotEmpty()) {
            Text("Комментарий", color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = StardustPrimary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(reception.comment, color = StardustTextPrimary, fontSize = 14.sp)
                }
            }
        }

        if (reception.batteryCodes.isNotEmpty()) {
            Text("Список АКБ", color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reception.batteryCodes.forEachIndexed { i, code ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(StardustItemBg).padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${i + 1}.", color = StardustTextSecondary, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                            Icon(Icons.Outlined.BatteryFull, null, tint = AccentCyan, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(code, color = StardustTextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        if (reception.scooterCodes.isNotEmpty()) {
            Text("Список самокатов", color = StardustTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reception.scooterCodes.forEachIndexed { i, code ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(StardustItemBg).padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${i + 1}.", color = StardustTextSecondary, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                            Icon(Icons.Filled.ElectricScooter, null, tint = AccentGreen, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(code, color = StardustTextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Button(
            onClick = { pdfLauncher.launch("акт_приёмки_${reception.id.take(8)}.pdf") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D3A)),
            border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Outlined.PictureAsPdf, null, tint = AccentGreen)
            Spacer(Modifier.width(8.dp))
            Text("Экспорт в PDF", color = StardustTextPrimary, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StardustError.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
        ) {
            Icon(Icons.Outlined.DeleteOutline, null, tint = StardustError, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Удалить запись", color = StardustError, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = StardustPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = StardustTextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = StardustTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── PDF EXPORT ───────────────────────────────────────────────────────────────

private fun writeReceptionPdfToUri(reception: BatteryReception, outputUri: Uri, context: Context) {
    val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val doc = PdfDocument()
    val pageWidth = 595; val pageHeight = 842
    val page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
    val canvas: Canvas = page.canvas

    val paintTitle = Paint().apply { color = android.graphics.Color.BLACK; textSize = 18f; isFakeBoldText = true }
    val paintSubtitle = Paint().apply { color = android.graphics.Color.rgb(60, 60, 60); textSize = 13f; isFakeBoldText = true }
    val paintLabel = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 12f }
    val paintValue = Paint().apply { color = android.graphics.Color.BLACK; textSize = 13f }
    val paintLine = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f }
    val paintHeader = Paint().apply { color = android.graphics.Color.rgb(74, 222, 128); textSize = 14f; isFakeBoldText = true }

    var y = 60f; val left = 40f; val right = (pageWidth - 40).toFloat()

    canvas.drawText("Акт приёмки АКБ", left, y, paintTitle); y += 22f
    canvas.drawText("${reception.receivedByName} принял от СБ: ${reception.receivedFromName}", left, y, paintSubtitle); y += 24f
    canvas.drawLine(left, y, right, y, paintLine); y += 18f

    fun row(label: String, value: String) {
        canvas.drawText(label, left, y, paintLabel); canvas.drawText(value, left + 170f, y, paintValue); y += 22f
    }

    row("Принял:", reception.receivedByName)
    row("От кого:", "СБ: ${reception.receivedFromName}")
    row("Дата / время:", fmt.format(Date(reception.timestamp)))
    if (reception.batteryCodes.isNotEmpty()) row("АКБ принято:", "${reception.batteryCodes.size} шт.")
    if (reception.scooterCodes.isNotEmpty()) row("Самокатов принято:", "${reception.scooterCodes.size} шт.")
    if (reception.reanimatorCount > 0) row("Реаниматоры:", "${reception.reanimatorCount} шт.")
    if (reception.comment.isNotEmpty()) row("Комментарий:", reception.comment)

    if (reception.batteryCodes.isNotEmpty()) {
        y += 10f; canvas.drawLine(left, y, right, y, paintLine); y += 18f
        canvas.drawText("Список АКБ:", left, y, paintHeader); y += 20f
        reception.batteryCodes.forEachIndexed { i, code ->
            if (y < pageHeight - 80f) { canvas.drawText("${i + 1}.  $code", left + 10f, y, paintValue); y += 18f }
        }
    }

    if (reception.scooterCodes.isNotEmpty()) {
        y += 10f; canvas.drawLine(left, y, right, y, paintLine); y += 18f
        canvas.drawText("Список самокатов:", left, y, paintHeader); y += 20f
        reception.scooterCodes.forEachIndexed { i, code ->
            if (y < pageHeight - 80f) { canvas.drawText("${i + 1}.  $code", left + 10f, y, paintValue); y += 18f }
        }
    }

    if (reception.photoUrl != null && reception.photoUrl.startsWith("/")) {
        val bitmap = try { BitmapFactory.decodeFile(reception.photoUrl) } catch (_: Exception) { null }
        if (bitmap != null) {
            y += 14f; canvas.drawLine(left, y, right, y, paintLine); y += 18f
            canvas.drawText("Фото:", left, y, paintHeader); y += 12f
            val scale = minOf((right - left) / bitmap.width, 200f / bitmap.height)
            val bmpW = bitmap.width * scale; val bmpH = bitmap.height * scale
            if (y + bmpH < pageHeight - 40f) {
                canvas.drawBitmap(bitmap, null, RectF(left, y, left + bmpW, y + bmpH), null); y += bmpH + 10f
            }
            bitmap.recycle()
        }
    }

    doc.finishPage(page)
    try {
        context.contentResolver.openOutputStream(outputUri)?.use { doc.writeTo(it) }
        Toast.makeText(context, "PDF сохранён", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        doc.close()
    }
}

private fun writePdfToUri(issuance: BatteryIssuance, outputUri: Uri, context: Context) {
    val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val doc = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842

    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    val paintTitle = Paint().apply { color = android.graphics.Color.BLACK; textSize = 18f; isFakeBoldText = true }
    val paintSubtitle = Paint().apply { color = android.graphics.Color.rgb(60, 60, 60); textSize = 13f; isFakeBoldText = true }
    val paintLabel = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 12f }
    val paintValue = Paint().apply { color = android.graphics.Color.BLACK; textSize = 13f }
    val paintLine = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f }
    val paintHeader = Paint().apply { color = android.graphics.Color.rgb(106, 90, 224); textSize = 14f; isFakeBoldText = true }

    var y = 60f
    val left = 40f
    val right = (pageWidth - 40).toFloat()

    // Заголовок
    canvas.drawText("Акт выдачи АКБ", left, y, paintTitle); y += 22f

    // Краткая сводка "Кто кому"
    val issuedByLabel = if (issuance.issuedByRole.isNotEmpty()) "${issuance.issuedByRole}: ${issuance.issuedByName}" else issuance.issuedByName
    canvas.drawText("$issuedByLabel  →  СБ: ${issuance.issuedToName}", left, y, paintSubtitle); y += 24f

    canvas.drawLine(left, y, right, y, paintLine); y += 18f

    fun row(label: String, value: String) {
        canvas.drawText(label, left, y, paintLabel)
        canvas.drawText(value, left + 170f, y, paintValue)
        y += 22f
    }

    row("Кем выдано:", issuedByLabel)
    row("Кому выдано:", "СБ: ${issuance.issuedToName}")
    row("Дата / время:", fmt.format(Date(issuance.timestamp)))
    row("Кол-во АКБ:", "${issuance.batteryCodes.size} шт.")
    if (issuance.reanimatorCount > 0) row("Реаниматоры:", "${issuance.reanimatorCount} шт.")
    if (issuance.comment.isNotEmpty()) row("Комментарий:", issuance.comment)

    // Список АКБ
    y += 10f
    canvas.drawLine(left, y, right, y, paintLine); y += 18f
    canvas.drawText("Список АКБ:", left, y, paintHeader); y += 20f
    issuance.batteryCodes.forEachIndexed { i, code ->
        if (y < pageHeight - 80f) {
            canvas.drawText("${i + 1}.  $code", left + 10f, y, paintValue)
            y += 18f
        }
    }

    // Фото
    if (issuance.photoUrl != null && issuance.photoUrl.startsWith("/")) {
        val bitmap = try { BitmapFactory.decodeFile(issuance.photoUrl) } catch (_: Exception) { null }
        if (bitmap != null) {
            y += 14f
            canvas.drawLine(left, y, right, y, paintLine); y += 18f
            canvas.drawText("Фото:", left, y, paintHeader); y += 12f
            val maxW = right - left
            val maxH = 200f
            val scale = minOf(maxW / bitmap.width, maxH / bitmap.height)
            val bmpW = bitmap.width * scale
            val bmpH = bitmap.height * scale
            if (y + bmpH < pageHeight - 40f) {
                canvas.drawBitmap(bitmap, null, RectF(left, y, left + bmpW, y + bmpH), null)
                y += bmpH + 10f
            }
            bitmap.recycle()
        }
    }

    doc.finishPage(page)

    try {
        context.contentResolver.openOutputStream(outputUri)?.use { doc.writeTo(it) }
        Toast.makeText(context, "PDF сохранён", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        doc.close()
    }
}