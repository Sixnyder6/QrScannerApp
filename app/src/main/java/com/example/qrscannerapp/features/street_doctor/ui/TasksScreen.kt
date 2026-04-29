package com.example.qrscannerapp.features.street_doctor.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.features.street_doctor.domain.model.ScooterFieldStatus
import com.example.qrscannerapp.features.street_doctor.domain.model.StreetScooter
import kotlinx.coroutines.delay

private val SdBg            = Color(0xFF0F0F13)
private val SdCard          = Color(0xFF1C1C22)
private val SdCardOpen      = Color(0xFF25252C)
private val SdPrimary       = Color(0xFF6C5CE7)
private val SdPrimaryDim    = Color(0xFF6C5CE7).copy(alpha = 0.12f)
private val SdPrimaryBorder = Color(0xFF6C5CE7).copy(alpha = 0.3f)
private val SdTextMain      = Color(0xFFFFFFFF)
private val SdTextMuted     = Color(0xFF8E8E93)
private val SdDivider       = Color(0xFFFFFFFF).copy(alpha = 0.05f)
private val SdStatusNew     = Color(0xFF3B82F6)
private val SdStatusWork    = Color(0xFFF59E0B)
private val SdStatusDone    = Color(0xFF10B981)
private val SdDanger        = Color(0xFFEF4444)

private fun formatDistance(meters: Int): String =
    if (meters >= 1000) "${"%.1f".format(meters / 1000.0)} км" else "$meters м"

private fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun getBatteryColor(pct: Int): Color = when {
    pct <= 10 -> SdDanger
    pct <= 30 -> SdStatusWork
    else      -> SdTextMuted
}

@Composable
private fun rememberElapsed(startTime: Long?): Long {
    var elapsed by remember { mutableStateOf(if (startTime != null) System.currentTimeMillis() - startTime else 0L) }
    LaunchedEffect(startTime) {
        if (startTime == null) return@LaunchedEffect
        while (true) {
            elapsed = System.currentTimeMillis() - startTime
            delay(1000)
        }
    }
    return elapsed
}

@Composable
private fun StatusBadge(status: ScooterFieldStatus) {
    val (bg, dot, text, label) = when (status) {
        ScooterFieldStatus.NEW         -> listOf(SdStatusNew.copy(0.1f),  SdStatusNew,  SdStatusNew,  "Новый")
        ScooterFieldStatus.IN_PROGRESS -> listOf(SdStatusWork.copy(0.1f), SdStatusWork, SdStatusWork, "В работе")
        ScooterFieldStatus.DONE        -> listOf(SdStatusDone.copy(0.1f), SdStatusDone, SdStatusDone, "Готово")
        ScooterFieldStatus.NOT_FOUND   -> listOf(SdTextMuted.copy(0.1f),  SdTextMuted,  SdTextMuted,  "Не найден")
        ScooterFieldStatus.TO_STORAGE  -> listOf(SdDanger.copy(0.1f),     SdDanger,     SdDanger,     "На склад")
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg as Color)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dot as Color))
        Text(label as String, color = text as Color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WorkerBox(scooter: StreetScooter) {
    val elapsed = rememberElapsed(scooter.workStartTime)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(SdPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(scooter.workerInitials ?: "??", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Column {
                Text(scooter.workerName ?: "", color = SdTextMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(scooter.workerRole ?: "", color = SdTextMuted, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("В РЕМОНТЕ", color = SdTextMuted, fontSize = 10.sp, letterSpacing = 0.5.sp)
            Text(formatElapsed(elapsed), color = SdStatusWork, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ScooterCard(
    scooter: StreetScooter,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onTakeWork: () -> Unit,      // берёт в работу → открывает паспорт
    onOpenPassport: () -> Unit,  // открыть паспорт для завершения (IN_PROGRESS)
    onNotFound: () -> Unit,
) {
    val isDone = scooter.status == ScooterFieldStatus.DONE ||
            scooter.status == ScooterFieldStatus.TO_STORAGE ||
            scooter.status == ScooterFieldStatus.NOT_FOUND

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isExpanded) SdPrimaryBorder else Color.Transparent, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isExpanded) SdCardOpen else SdCard)
    ) {
        Column {
            // ── Заголовок карточки ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            scooter.code,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) SdTextMuted else SdTextMain,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = if (isExpanded) SdPrimary else SdTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.NearMe, null, tint = SdTextMuted, modifier = Modifier.size(13.dp))
                            Text(formatDistance(scooter.distanceMeters), color = SdTextMuted, fontSize = 13.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.BatteryStd,
                                null,
                                tint = getBatteryColor(scooter.batteryPct),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                "${scooter.batteryPct}%",
                                color = getBatteryColor(scooter.batteryPct),
                                fontSize = 13.sp,
                                fontWeight = if (scooter.batteryPct <= 10) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                StatusBadge(scooter.status)
            }

            // ── Раскрытая часть ──
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit  = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (scooter.status) {

                        // ── Новый: взять в работу (→ паспорт) + не найден ──
                        ScooterFieldStatus.NEW -> {
                            Button(
                                onClick = onTakeWork,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SdPrimary)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Взять в работу", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onNotFound,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SdDanger),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, SdDanger.copy(alpha = 0.3f)
                                )
                            ) {
                                Icon(Icons.Outlined.LocationOff, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Не найден", fontSize = 13.sp)
                            }
                        }

                        // ── В работе: открыть паспорт для завершения ──
                        ScooterFieldStatus.IN_PROGRESS -> {
                            if (scooter.isMine) {
                                Button(
                                    onClick = onOpenPassport,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SdStatusDone)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Завершить ремонт", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                WorkerBox(scooter)
                            }
                        }

                        // ── Завершён / на склад / не найден ──
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    null,
                                    tint = when (scooter.status) {
                                        ScooterFieldStatus.TO_STORAGE -> SdDanger
                                        ScooterFieldStatus.NOT_FOUND  -> SdTextMuted
                                        else                          -> SdStatusDone
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    when (scooter.status) {
                                        ScooterFieldStatus.TO_STORAGE -> "Отправлен на склад"
                                        ScooterFieldStatus.NOT_FOUND  -> "Не найден"
                                        else                          -> "Ремонт завершён"
                                    },
                                    color = when (scooter.status) {
                                        ScooterFieldStatus.TO_STORAGE -> SdDanger
                                        ScooterFieldStatus.NOT_FOUND  -> SdTextMuted
                                        else                          -> SdStatusDone
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TasksScreen(
    viewModel: StreetDoctorViewModel = hiltViewModel(),
    currentUserName: String = "Техник",
    currentUserInitials: String = "ТХ",
    onOpenPassport: (StreetScooter) -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedId by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf("active") }
    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearToast()
        }
    }

    val scooters = uiState.scooters
    val activeList = scooters.filter {
        it.status == ScooterFieldStatus.NEW || it.status == ScooterFieldStatus.IN_PROGRESS
    }
    val doneList = scooters.filter {
        it.status == ScooterFieldStatus.DONE ||
                it.status == ScooterFieldStatus.TO_STORAGE ||
                it.status == ScooterFieldStatus.NOT_FOUND
    }
    var listData = if (activeTab == "active") activeList else doneList
    if (searchQuery.isNotBlank()) {
        listData = listData.filter { it.code.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Scaffold(
        containerColor = SdBg,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Шапка ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "STREET DOCTOR",
                            fontSize = 10.sp,
                            color = SdPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Задания",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SdTextMain,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Дашборд
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashCard("ВСЕГО", scooters.size.toString(), null, Modifier.weight(1f))
                        DashCard(
                            "ГОТОВО / ОСТ",
                            "${doneList.size} / ${activeList.size}",
                            SdPrimary,
                            Modifier.weight(1f)
                        )
                        DashCard(
                            "В РАБОТЕ",
                            scooters.count { it.status == ScooterFieldStatus.IN_PROGRESS }.toString(),
                            if (scooters.any { it.status == ScooterFieldStatus.IN_PROGRESS }) SdStatusWork else null,
                            Modifier.weight(1f)
                        )
                    }

                    // Поиск
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SdCard)
                            .border(1.dp, SdDivider, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Search, null, tint = SdTextMuted, modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = SdTextMain, fontSize = 15.sp
                            ),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text("Поиск по номеру...", color = SdTextMuted, fontSize = 15.sp)
                                }
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Close, null, tint = SdTextMuted)
                            }
                        }
                    }
                }
            }

            // ── Табы ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabBtn(
                        label = "Активные · ${activeList.size}",
                        selected = activeTab == "active",
                        modifier = Modifier.weight(1f)
                    ) { activeTab = "active" }
                    TabBtn(
                        label = "Завершены · ${doneList.size}",
                        selected = activeTab == "done",
                        modifier = Modifier.weight(1f)
                    ) { activeTab = "done" }
                }
            }

            // ── Список ──
            if (listData.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = SdTextMuted, modifier = Modifier.size(44.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "Ничего не найдено"
                            else if (activeTab == "active") "Все задания выполнены 🎉"
                            else "Ещё ничего не завершено",
                            color = SdTextMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(listData, key = { it.id }) { scooter ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        ScooterCard(
                            scooter = scooter,
                            isExpanded = expandedId == scooter.id,
                            onToggle = {
                                expandedId = if (expandedId == scooter.id) null else scooter.id
                            },
                            // Взять в работу → пишем статус И сразу открываем паспорт
                            onTakeWork = {
                                viewModel.takeScooter(scooter.id, currentUserName, currentUserInitials)
                                expandedId = null
                                onOpenPassport(scooter)
                            },
                            // В работе → "Завершить ремонт" открывает паспорт для заполнения
                            onOpenPassport = {
                                expandedId = null
                                onOpenPassport(scooter)
                            },
                            onNotFound = {
                                viewModel.markNotFound(scooter.id)
                                expandedId = null
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun DashCard(label: String, value: String, accentColor: Color?, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (accentColor != null) accentColor.copy(alpha = 0.12f) else SdCard)
            .border(
                1.dp,
                if (accentColor != null) accentColor.copy(alpha = 0.3f) else SdDivider,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor ?: SdTextMain)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = SdTextMuted, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun TabBtn(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SdPrimary else SdCard)
            .border(1.dp, if (selected) Color.Transparent else SdDivider, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else SdTextMuted
        )
    }
}