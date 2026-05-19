package com.example.qrscannerapp.features.street_doctor.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.example.qrscannerapp.common.ui.AnimatedDialogWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.qrscannerapp.features.street_doctor.domain.model.ScooterFieldStatus

private val SiBg        = Color(0xFF0F0F13)
private val SiCard      = Color(0xFF1C1C22)
private val SiCardDeep  = Color(0xFF141418)
private val SiBorder    = Color(0xFF2A2A35)
private val SiPrimary   = Color(0xFF6C5CE7)
private val SiSuccess   = Color(0xFF22C55E)
private val SiWarning   = Color(0xFFF59E0B)
private val SiDanger    = Color(0xFFEF4444)
private val SiStorage   = Color(0xFF38BDF8)
private val SiNotFound  = Color(0xFF6B7280)
private val SiTextMain  = Color(0xFFFFFFFF)
private val SiTextMuted = Color(0xFF8E8E93)
private val SiTextSec   = Color(0xFF5A5A70)

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

private fun siStatusColor(s: ScooterFieldStatus) = when (s) {
    ScooterFieldStatus.NEW         -> Color(0xFF3B82F6)
    ScooterFieldStatus.IN_PROGRESS -> SiWarning
    ScooterFieldStatus.DONE        -> SiSuccess
    ScooterFieldStatus.TO_STORAGE  -> SiDanger
    ScooterFieldStatus.NOT_FOUND   -> SiNotFound
}

private fun siStatusLabel(s: ScooterFieldStatus) = when (s) {
    ScooterFieldStatus.NEW         -> "Новый"
    ScooterFieldStatus.IN_PROGRESS -> "В работе"
    ScooterFieldStatus.DONE        -> "Готово"
    ScooterFieldStatus.TO_STORAGE  -> "На склад"
    ScooterFieldStatus.NOT_FOUND   -> "Не найден"
}

data class ScooterSheetDetails(
    val id: String = "",
    val code: String = "",
    val status: ScooterFieldStatus = ScooterFieldStatus.NEW,
    val technicianName: String = "",
    val batteryFromExcel: Int = 0,
    val batteryReal: Int? = null,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val repairTypes: List<String> = emptyList(),
    val notes: String = "",
    val photoUrls: List<String> = emptyList(),
    val model: String = "",
    // прогресс техника за смену
    val techDone: Int = 0,
    val techToStorage: Int = 0,
    val techNotFound: Int = 0,
    val techTotal: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScooterInfoSheet(
    details: ScooterSheetDetails?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var fullscreenUrl by remember { mutableStateOf<String?>(null) }

    if (fullscreenUrl != null) {
        AnimatedDialogWrapper(onDismiss = { fullscreenUrl = null }) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black).clickable { fullscreenUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fullscreenUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SiBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(SiBorder, RoundedCornerShape(2.dp))
            )
        }
    ) {
        if (isLoading || details == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SiPrimary, modifier = Modifier.size(28.dp))
            }
            return@ModalBottomSheet
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── 1. Номер + статус ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        details.code,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = SiTextMain,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    if (details.model.isNotBlank()) {
                        Text(details.model, color = SiTextMuted, fontSize = 12.sp)
                    }
                }
                val statusColor = siStatusColor(details.status)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                    Text(
                        siStatusLabel(details.status),
                        color = statusColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── 2. Карточка техника с прогрессом ──
            if (details.technicianName.isNotBlank()) {
                val initials = details.technicianName
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                val progressF = if (details.techTotal > 0)
                    (details.techDone + details.techToStorage + details.techNotFound).toFloat() / details.techTotal
                else 0f
                val closedCount = details.techDone + details.techToStorage + details.techNotFound

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SiCard)
                        .border(0.5.dp, SiBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Имя + инициалы
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SiPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, color = SiPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text(
                                details.technicianName,
                                color = SiTextMain,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text("Техник · сегодня в поле", color = SiTextMuted, fontSize = 11.sp)
                        }
                    }

                    // Счётчики
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TechStatChip("${details.techDone}", "Готово", SiSuccess, Modifier.weight(1f))
                        TechStatChip("${details.techToStorage}", "На склад", SiDanger, Modifier.weight(1f))
                        TechStatChip("${details.techNotFound}", "Не найден", SiNotFound, Modifier.weight(1f))
                        TechStatChip("${details.techTotal}", "Всего", SiTextMuted, Modifier.weight(1f))
                    }

                    // Прогресс-бар
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Прогресс смены", color = SiTextMuted, fontSize = 11.sp)
                            Text(
                                "$closedCount / ${details.techTotal}",
                                color = SiSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Составной прогресс-бар
                        if (details.techTotal > 0) {
                            val t = details.techTotal.toFloat()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            ) {
                                if (details.techDone > 0)
                                    Box(Modifier.weight(details.techDone / t).fillMaxHeight().background(SiSuccess))
                                if (details.techToStorage > 0)
                                    Box(Modifier.weight(details.techToStorage / t).fillMaxHeight().background(SiDanger))
                                if (details.techNotFound > 0)
                                    Box(Modifier.weight(details.techNotFound / t).fillMaxHeight().background(SiNotFound))
                                val remaining = details.techTotal - closedCount
                                if (remaining > 0)
                                    Box(Modifier.weight(remaining / t).fillMaxHeight().background(SiBorder))
                            }
                        }
                    }

                    // Батарея компактно
                    val battColor = when {
                        details.batteryFromExcel <= 10 -> SiDanger
                        details.batteryFromExcel <= 30 -> SiWarning
                        else -> SiTextMuted
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SiCardDeep)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.BatteryStd,
                            null,
                            tint = battColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "АКБ: ${details.batteryFromExcel}% из задания",
                            color = battColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (details.batteryReal != null) {
                            Text("·", color = SiTextSec, fontSize = 12.sp)
                            val realColor = when {
                                details.batteryReal <= 10 -> SiDanger
                                details.batteryReal <= 30 -> SiWarning
                                else -> SiSuccess
                            }
                            Text(
                                "${details.batteryReal}% факт",
                                color = realColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── 3. Координаты + карта ──
            if (details.lat != 0.0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SiCard)
                        .border(0.5.dp, SiBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Координаты", color = SiTextMuted, fontSize = 13.sp)
                        Text(
                            "%.4f, %.4f".format(details.lat, details.lon),
                            color = SiTextMain,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    HorizontalDivider(color = SiBorder, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val uri = Uri.parse(
                                    "geo:${details.lat},${details.lon}?q=${details.lat},${details.lon}(${details.code})"
                                )
                                context.startActivity(
                                    Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Открыть в картах")
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SiPrimary))
                        Text(
                            "Открыть на карте",
                            color = SiPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, null, tint = SiPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // ── 4. Виды работ ──
            if (details.repairTypes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "ВИДЫ РАБОТ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SiTextMuted,
                        letterSpacing = 1.5.sp
                    )
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        details.repairTypes.forEach { key ->
                            val label = REPAIR_LABELS[key] ?: key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SiPrimary.copy(alpha = 0.12f))
                                    .border(0.5.dp, SiPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(label, color = SiPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── 5. Заметка ──
            if (details.notes.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SiCard)
                        .border(0.5.dp, SiBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "ЗАМЕТКА",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SiTextMuted,
                        letterSpacing = 1.5.sp
                    )
                    Text(details.notes, color = SiTextMain, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }

            // ── 6. Фото ──
            if (details.photoUrls.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "ФОТО",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SiTextMuted,
                        letterSpacing = 1.5.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(details.photoUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { fullscreenUrl = url }
                            )
                        }
                    }
                }
            }

            // ── Пусто ──
            if (details.repairTypes.isEmpty() && details.notes.isBlank()
                && details.photoUrls.isEmpty()
                && details.status == ScooterFieldStatus.NEW
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SiCard)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Самокат ещё не обработан", color = SiTextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Чип статистики техника ────────────────────────────────────────────────────
@Composable
private fun TechStatChip(value: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .border(0.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = SiTextMuted, fontSize = 9.sp, letterSpacing = 0.sp)
    }
}