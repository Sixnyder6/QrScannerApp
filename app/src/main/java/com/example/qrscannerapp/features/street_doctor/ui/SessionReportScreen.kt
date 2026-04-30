package com.example.qrscannerapp.features.street_doctor.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// ── Цвета ─────────────────────────────────────────────────────────────────────
private val RpBg        = Color(0xFF0A0A0F)
private val RpCard      = Color(0xFF14141C)
private val RpCardAlt   = Color(0xFF1A1A24)
private val RpBorder    = Color(0xFF252535)
private val RpPrimary   = Color(0xFF7C6FE0)
private val RpSuccess   = Color(0xFF22C55E)
private val RpWarning   = Color(0xFFF59E0B)
private val RpDanger    = Color(0xFFEF4444)
private val RpStorage   = Color(0xFF38BDF8)
private val RpNotFound  = Color(0xFF6B7280)
private val RpTextMain  = Color(0xFFF5F5FF)
private val RpTextMuted = Color(0xFF6B6B85)
private val RpTextSec   = Color(0xFF9999B5)

private val REPAIR_LABELS = mapOf(
    "brakes"  to "Тормоза",
    "wheel"   to "Колесо",
    "battery" to "АКБ",
    "lock"    to "Замок",
    "display" to "Дисплей",
    "frame"   to "Рама",
    "cable"   to "Кабель",
    "other"   to "Другое"
)

// ── Модели ────────────────────────────────────────────────────────────────────
data class ReportScooter(
    val id: String = "",
    val code: String = "",
    val status: String = "",
    val technicianName: String = "",
    val technicianId: String = "",
    val repairTypes: List<String> = emptyList(),
    val batteryPct: Int = 0,
    val batteryReal: Int = 0,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val notes: String = "",
    val completedAt: Long = 0L,
    val model: String = ""
)

data class SessionReportUiState(
    val isLoading: Boolean = true,
    val sessionId: String = "",
    val sessionDate: String = "",
    val totalCount: Int = 0,
    val scooters: List<ReportScooter> = emptyList(),
    val error: String? = null
)

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun SessionReportScreen(
    session: FieldSession,
    onBack: () -> Unit,
    viewModel: SessionReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(session.id) {
        viewModel.loadReport(session.id)
    }

    // Группировки
    val done      = state.scooters.filter { it.status == "done" }
    val toStorage = state.scooters.filter { it.status == "to_storage" }
    val notFound  = state.scooters.filter { it.status == "not_found" }
    val inProg    = state.scooters.filter { it.status == "in_progress" }
    val newList   = state.scooters.filter { it.status == "new" }

    // По техникам
    val byTech = state.scooters.groupBy { it.technicianId }

    // Какая секция раскрыта
    var expandedSection by remember { mutableStateOf<String?>(null) }
    var expandedTech by remember { mutableStateOf<String?>(null) }

    val dateStr = remember(session.createdAt) {
        if (session.createdAt > 0)
            java.text.SimpleDateFormat("dd.MM.yyyy · HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(session.createdAt))
        else "—"
    }

    Box(modifier = Modifier.fillMaxSize().background(RpBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Шапка ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(RpPrimary.copy(0.15f), Color.Transparent)))
                        .padding(horizontal = 16.dp)
                        .padding(top = 52.dp, bottom = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RpCard)
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = RpTextMain, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ОТЧЁТ ПО СЕССИИ", fontSize = 10.sp, color = RpPrimary, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                            Text(dateStr, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = RpTextMain)
                        }
                    }
                }
            }

            // ── Загрузка ──
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RpPrimary, modifier = Modifier.size(28.dp))
                    }
                }
                return@LazyColumn
            }

            // ── Главная диаграмма ──
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(RpPrimary.copy(0.15f), RpPrimary.copy(0.04f))))
                        .border(1.dp, RpPrimary.copy(0.2f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Итого самокатов", color = RpTextMuted, fontSize = 11.sp)
                            Text(state.scooters.size.toString(), color = RpTextMain, fontSize = 40.sp, fontWeight = FontWeight.Black, lineHeight = 44.sp)
                        }
                        // Круговой индикатор
                        val total = state.scooters.size
                        val doneF = if (total > 0) (done.size + toStorage.size).toFloat() / total else 0f
                        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = RpBorder, strokeWidth = 7.dp, trackColor = Color.Transparent)
                            CircularProgressIndicator(progress = { doneF }, modifier = Modifier.fillMaxSize(), color = RpSuccess, strokeWidth = 7.dp, trackColor = Color.Transparent)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${(doneF * 100).toInt()}%", color = RpTextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("закрыто", color = RpTextMuted, fontSize = 8.sp)
                            }
                        }
                    }

                    // Составной прогресс-бар
                    val total = state.scooters.size
                    if (total > 0) {
                        val t = total.toFloat()
                        Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                            if (done.isNotEmpty())      Box(Modifier.weight(done.size / t).fillMaxHeight().background(RpSuccess))
                            if (toStorage.isNotEmpty()) Box(Modifier.weight(toStorage.size / t).fillMaxHeight().background(RpStorage))
                            if (notFound.isNotEmpty())  Box(Modifier.weight(notFound.size / t).fillMaxHeight().background(RpDanger))
                            if (inProg.isNotEmpty())    Box(Modifier.weight(inProg.size / t).fillMaxHeight().background(RpWarning))
                            if (newList.isNotEmpty())   Box(Modifier.weight(newList.size / t).fillMaxHeight().background(RpBorder))
                        }
                    }

                    // Легенда
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (done.isNotEmpty())      ReportChip("✓ ${done.size}", RpSuccess)
                        if (toStorage.isNotEmpty()) ReportChip("▲ ${toStorage.size}", RpStorage)
                        if (notFound.isNotEmpty())  ReportChip("? ${notFound.size}", RpDanger)
                        if (inProg.isNotEmpty())    ReportChip("⟳ ${inProg.size}", RpWarning)
                        if (newList.isNotEmpty())   ReportChip("○ ${newList.size}", RpTextMuted)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── По технику ──
            item {
                Text(
                    "ПО ТЕХНИКУ",
                    color = RpTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                )
            }

            byTech.forEach { (techId, techScooters) ->
                val techName = techScooters.firstOrNull()?.technicianName ?: "Неизвестный"
                val techDone = techScooters.count { it.status == "done" }
                val techStorage = techScooters.count { it.status == "to_storage" }
                val techNotFound = techScooters.count { it.status == "not_found" }
                val techInProg = techScooters.count { it.status == "in_progress" }
                val techNew = techScooters.count { it.status == "new" }
                val techTotal = techScooters.size
                val techDoneF = if (techTotal > 0) (techDone + techStorage).toFloat() / techTotal else 0f
                val initials = techName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                val isExpanded = expandedTech == techId

                item(key = "tech_$techId") {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isExpanded) RpCardAlt else RpCard)
                            .border(1.dp, if (isExpanded) RpPrimary.copy(0.3f) else RpBorder, RoundedCornerShape(14.dp))
                    ) {
                        // Строка техника
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedTech = if (isExpanded) null else techId }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(RpPrimary.copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initials, color = RpPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(techName, color = RpTextMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("$techTotal самокатов", color = RpTextMuted, fontSize = 11.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (techDone > 0)     ReportChip("✓$techDone", RpSuccess)
                                if (techStorage > 0)  ReportChip("▲$techStorage", RpStorage)
                                if (techNotFound > 0) ReportChip("?$techNotFound", RpDanger)
                                if (techInProg > 0)   ReportChip("⟳$techInProg", RpWarning)
                            }
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = RpTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Прогресс-бар техника
                        if (techTotal > 0) {
                            val t = techTotal.toFloat()
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                            ) {
                                if (techDone > 0)     Box(Modifier.weight(techDone / t).fillMaxHeight().background(RpSuccess))
                                if (techStorage > 0)  Box(Modifier.weight(techStorage / t).fillMaxHeight().background(RpStorage))
                                if (techNotFound > 0) Box(Modifier.weight(techNotFound / t).fillMaxHeight().background(RpDanger))
                                if (techInProg > 0)   Box(Modifier.weight(techInProg / t).fillMaxHeight().background(RpWarning))
                                if (techNew > 0)      Box(Modifier.weight(techNew / t).fillMaxHeight().background(RpBorder))
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        // Детали техника
                        AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                techScooters.sortedWith(compareBy {
                                    when (it.status) {
                                        "done"        -> 0
                                        "to_storage"  -> 1
                                        "not_found"   -> 2
                                        "in_progress" -> 3
                                        else          -> 4
                                    }
                                }).forEach { scooter ->
                                    TechScooterRow(
                                        scooter = scooter,
                                        onMapClick = {
                                            if (scooter.lat != 0.0) {
                                                val uri = Uri.parse("geo:${scooter.lat},${scooter.lon}?q=${scooter.lat},${scooter.lon}(${scooter.code})")
                                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Открыть в картах"))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Секции по статусам ──
            item { Spacer(Modifier.height(4.dp)) }

            item {
                Text(
                    "ПО СТАТУСУ",
                    color = RpTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                )
            }

            // Готово
            if (done.isNotEmpty()) {
                item(key = "sec_done") {
                    StatusSection(
                        label     = "Ремонт завершён",
                        count     = done.size,
                        color     = RpSuccess,
                        isExpanded = expandedSection == "done",
                        onToggle  = { expandedSection = if (expandedSection == "done") null else "done" }
                    ) {
                        done.forEach { s ->
                            StatusScooterRow(scooter = s, color = RpSuccess, onMapClick = {
                                val uri = Uri.parse("geo:${s.lat},${s.lon}?q=${s.lat},${s.lon}(${s.code})")
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Открыть в картах"))
                            })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // На склад
            if (toStorage.isNotEmpty()) {
                item(key = "sec_storage") {
                    StatusSection(
                        label     = "На склад",
                        count     = toStorage.size,
                        color     = RpStorage,
                        isExpanded = expandedSection == "storage",
                        onToggle  = { expandedSection = if (expandedSection == "storage") null else "storage" }
                    ) {
                        toStorage.forEach { s ->
                            StatusScooterRow(scooter = s, color = RpStorage, showCoords = true, onMapClick = {
                                val uri = Uri.parse("geo:${s.lat},${s.lon}?q=${s.lat},${s.lon}(${s.code})")
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Открыть в картах"))
                            })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Не найден
            if (notFound.isNotEmpty()) {
                item(key = "sec_notfound") {
                    StatusSection(
                        label     = "Не найден",
                        count     = notFound.size,
                        color     = RpDanger,
                        isExpanded = expandedSection == "notfound",
                        onToggle  = { expandedSection = if (expandedSection == "notfound") null else "notfound" }
                    ) {
                        notFound.forEach { s ->
                            StatusScooterRow(scooter = s, color = RpDanger, onMapClick = {
                                val uri = Uri.parse("geo:${s.lat},${s.lon}?q=${s.lat},${s.lon}(${s.code})")
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Открыть в картах"))
                            })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // В работе (не закрыты)
            if (inProg.isNotEmpty()) {
                item(key = "sec_inprog") {
                    StatusSection(
                        label     = "Не завершено (в работе)",
                        count     = inProg.size,
                        color     = RpWarning,
                        isExpanded = expandedSection == "inprog",
                        onToggle  = { expandedSection = if (expandedSection == "inprog") null else "inprog" }
                    ) {
                        inProg.forEach { s ->
                            StatusScooterRow(scooter = s, color = RpWarning, onMapClick = {})
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Не обработаны
            if (newList.isNotEmpty()) {
                item(key = "sec_new") {
                    StatusSection(
                        label     = "Не обработано",
                        count     = newList.size,
                        color     = RpTextMuted,
                        isExpanded = expandedSection == "new",
                        onToggle  = { expandedSection = if (expandedSection == "new") null else "new" }
                    ) {
                        newList.forEach { s ->
                            StatusScooterRow(scooter = s, color = RpTextMuted, onMapClick = {})
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── Секция по статусу ─────────────────────────────────────────────────────────
@Composable
private fun StatusSection(
    label: String,
    count: Int,
    color: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RpCard)
            .border(1.dp, if (isExpanded) color.copy(0.35f) else RpBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(label, color = RpTextMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(count.toString(), color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = RpTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content()
            }
        }
    }
}

// ── Строка самоката в секции статуса ─────────────────────────────────────────
@Composable
private fun StatusScooterRow(
    scooter: ReportScooter,
    color: Color,
    showCoords: Boolean = false,
    onMapClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RpBg.copy(0.6f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            scooter.code,
            color = RpTextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(70.dp)
        )
        // Виды работ
        if (scooter.repairTypes.isNotEmpty()) {
            Text(
                scooter.repairTypes.mapNotNull { REPAIR_LABELS[it] }.joinToString(", "),
                color = RpTextMuted,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        // Батарея
        if (scooter.batteryPct > 0 || scooter.batteryReal > 0) {
            val batt = scooter.batteryReal.takeIf { it > 0 } ?: scooter.batteryPct
            val battColor = when {
                batt <= 10 -> RpDanger
                batt <= 30 -> RpWarning
                else -> RpTextMuted
            }
            Text("$batt%", color = battColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        // Кнопка карты
        if (scooter.lat != 0.0) {
            Icon(
                Icons.Outlined.Map,
                null,
                tint = RpPrimary,
                modifier = Modifier.size(16.dp).clickable { onMapClick() }
            )
        }
    }
}

// ── Строка самоката в детализации техника ─────────────────────────────────────
@Composable
private fun TechScooterRow(scooter: ReportScooter, onMapClick: () -> Unit) {
    val (statusColor, statusLabel) = when (scooter.status) {
        "done"        -> Pair(RpSuccess,  "Готово")
        "to_storage"  -> Pair(RpStorage,  "На склад")
        "not_found"   -> Pair(RpDanger,   "Не найден")
        "in_progress" -> Pair(RpWarning,  "В работе")
        else          -> Pair(RpTextMuted, "Новый")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RpBg.copy(0.6f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
        Text(scooter.code, color = RpTextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(70.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            if (scooter.repairTypes.isNotEmpty()) {
                Text(
                    scooter.repairTypes.mapNotNull { REPAIR_LABELS[it] }.joinToString(", "),
                    color = RpTextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (scooter.lat != 0.0) {
            Icon(Icons.Outlined.Map, null, tint = RpPrimary, modifier = Modifier.size(16.dp).clickable { onMapClick() })
        }
    }
}

// ── Чип ──────────────────────────────────────────────────────────────────────
@Composable
private fun ReportChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}