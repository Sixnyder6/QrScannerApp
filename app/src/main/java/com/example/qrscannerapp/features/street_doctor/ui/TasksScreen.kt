package com.example.qrscannerapp.features.street_doctor.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.qrscannerapp.common.ui.AnimatedDialogWrapper
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
private val SdDivider       = Color(0xFFFFFFFF).copy(alpha = 0.10f)
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

private fun formatTimeAgo(createdAt: Long): String {
    if (createdAt == 0L) return ""
    val diff = System.currentTimeMillis() - createdAt
    val h = diff / 3_600_000
    val m = (diff % 3_600_000) / 60_000
    return when {
        h >= 24 -> "${h / 24}д в очереди"
        h > 0   -> "${h}ч в очереди"
        m > 0   -> "${m}м в очереди"
        else    -> "только что"
    }
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
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("В РЕМОНТЕ", color = SdTextMuted, fontSize = 10.sp, letterSpacing = 0.5.sp)
            Text(formatElapsed(elapsed), color = SdStatusWork, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun FullscreenPhotoDialog(url: String, onDismiss: () -> Unit) {
    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ScooterCard(
    scooter: StreetScooter,
    isExpanded: Boolean,
    isNearest: Boolean = false,
    onToggle: () -> Unit,
    onTakeWork: () -> Unit,
    onOpenPassport: () -> Unit,
    onNotFound: () -> Unit,
) {
    val context = LocalContext.current
    var fullscreenUrl by remember { mutableStateOf<String?>(null) }
    if (fullscreenUrl != null) {
        FullscreenPhotoDialog(url = fullscreenUrl!!, onDismiss = { fullscreenUrl = null })
    }
    val isDone = scooter.status == ScooterFieldStatus.DONE ||
            scooter.status == ScooterFieldStatus.TO_STORAGE ||
            scooter.status == ScooterFieldStatus.NOT_FOUND
    val canNavigate = scooter.lat != 0.0 && scooter.lon != 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isExpanded) SdPrimaryBorder else SdDivider, RoundedCornerShape(16.dp)),
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    // Строка 1: код + модель + стрелка + БЛИЖАЙШИЙ
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            scooter.code,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) SdTextMuted else SdTextMain,
                            letterSpacing = 1.sp
                        )
                        if (scooter.model.isNotBlank()) {
                            Text(
                                scooter.model,
                                fontSize = 11.sp,
                                color = SdPrimary.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = if (isExpanded) SdPrimary else SdTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isNearest) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SdStatusDone.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "БЛИЖАЙШИЙ",
                                    fontSize = 9.sp,
                                    color = SdStatusDone,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    // Строка 2: дистанция + батарея + замок + время в очереди
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                        if (scooter.isUnlocked) {
                            Icon(Icons.Default.LockOpen, null, tint = SdStatusDone, modifier = Modifier.size(13.dp))
                        }
                        val timeAgo = if (!isDone) formatTimeAgo(scooter.createdAt) else ""
                        if (timeAgo.isNotEmpty()) {
                            Text(timeAgo, color = SdTextMuted.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                    // Строка 3: адрес (если есть)
                    if (scooter.address.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.LocationOn, null, tint = SdTextMuted, modifier = Modifier.size(12.dp))
                            Text(
                                scooter.address,
                                color = SdTextMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
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
                            // Описание проблемы
                            if (scooter.problem.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SdStatusWork.copy(alpha = 0.08f))
                                        .border(1.dp, SdStatusWork.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Outlined.Build, null, tint = SdStatusWork, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                                    Text(scooter.problem, color = SdTextMain, fontSize = 13.sp)
                                }
                            }
                            // VIN + пробег
                            if (scooter.vin.isNotBlank() || scooter.mileage > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (scooter.vin.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("VIN", fontSize = 10.sp, color = SdTextMuted, letterSpacing = 0.5.sp)
                                            Text(scooter.vin, fontSize = 12.sp, color = SdTextMain, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    if (scooter.mileage > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Outlined.Speed, null, tint = SdTextMuted, modifier = Modifier.size(12.dp))
                                            Text("${scooter.mileage} км", fontSize = 12.sp, color = SdTextMuted)
                                        }
                                    }
                                }
                            }
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
                            // Маршрут
                            if (canNavigate) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val uri = Uri.parse("google.navigation:q=${scooter.lat},${scooter.lon}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        } catch (_: Exception) {
                                            val uri = Uri.parse("geo:${scooter.lat},${scooter.lon}?q=${scooter.lat},${scooter.lon}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SdStatusNew),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SdStatusNew.copy(alpha = 0.4f))
                                ) {
                                    Icon(Icons.Outlined.Navigation, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Маршрут", fontSize = 13.sp)
                                }
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
                                if (scooter.problem.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SdStatusWork.copy(alpha = 0.08f))
                                            .border(1.dp, SdStatusWork.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Outlined.Build, null, tint = SdStatusWork, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                                        Text(scooter.problem, color = SdTextMain, fontSize = 13.sp)
                                    }
                                }
                                if (canNavigate) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val uri = Uri.parse("google.navigation:q=${scooter.lat},${scooter.lon}")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            } catch (_: Exception) {
                                                val uri = Uri.parse("geo:${scooter.lat},${scooter.lon}?q=${scooter.lat},${scooter.lon}")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SdStatusNew),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SdStatusNew.copy(alpha = 0.4f))
                                    ) {
                                        Icon(Icons.Outlined.Navigation, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Маршрут", fontSize = 13.sp)
                                    }
                                }
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
                            if (scooter.photoUrls.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(scooter.photoUrls) { url ->
                                        AsyncImage(
                                            model = url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { fullscreenUrl = url }
                                        )
                                    }
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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.updateDistances() }
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

    val activeTask = scooters.firstOrNull { it.isMine && it.status == ScooterFieldStatus.IN_PROGRESS }
    val nearestNewId = scooters.filter { it.status == ScooterFieldStatus.NEW }
        .minByOrNull { it.distanceMeters }?.id

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

                    // Прогресс смены
                    if (scooters.isNotEmpty()) {
                        val progress = doneList.size.toFloat() / scooters.size.toFloat()
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Прогресс смены", color = SdTextMuted, fontSize = 11.sp)
                                Text("${doneList.size} из ${scooters.size}", color = SdTextMuted, fontSize = 11.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SdCard)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (progress >= 1f) SdStatusDone else SdPrimary)
                                )
                            }
                        }
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

            // ── Активный ремонт ──
            activeTask?.let { task ->
                item(key = "active_banner") {
                    Box(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)) {
                        ActiveRepairBanner(
                            scooter = task,
                            onResume = { onOpenPassport(task) }
                        )
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
                            isNearest = scooter.id == nearestNewId && scooter.status == ScooterFieldStatus.NEW,
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
private fun ActiveRepairBanner(scooter: StreetScooter, onResume: () -> Unit) {
    val elapsed = rememberElapsed(scooter.workStartTime)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SdPrimary.copy(alpha = 0.10f))
            .border(1.dp, SdPrimaryBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "СЕЙЧАС В РАБОТЕ",
                    fontSize = 10.sp,
                    color = SdPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    scooter.code,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SdTextMain,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("ВРЕМЯ", fontSize = 10.sp, color = SdTextMuted, letterSpacing = 1.sp)
                Text(
                    formatElapsed(elapsed),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SdStatusWork,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        if (scooter.address.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.LocationOn, null, tint = SdTextMuted, modifier = Modifier.size(14.dp))
                Text(
                    scooter.address,
                    color = SdTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Button(
            onClick = onResume,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SdPrimary)
        ) {
            Icon(Icons.Default.Build, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Продолжить ремонт", fontWeight = FontWeight.Bold)
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