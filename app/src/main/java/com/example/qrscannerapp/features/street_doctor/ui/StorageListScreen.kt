package com.example.qrscannerapp.features.street_doctor.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

private val StBg        = Color(0xFF0A0A0F)
private val StCard      = Color(0xFF14141C)
private val StBorder    = Color(0xFF252535)
private val StDanger    = Color(0xFFEF4444)
private val StPrimary   = Color(0xFF7C6FE0)
private val StWarning   = Color(0xFFF59E0B)
private val StTextMain  = Color(0xFFF5F5FF)
private val StTextMuted = Color(0xFF6B6B85)
private val StTextSec   = Color(0xFF9999B5)

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

@Composable
fun StorageListScreen(
    scooters: List<StorageScooter>,
    isLoading: Boolean,
    onExportClick: () -> Unit = {} // заглушка — потом наполним
) {
    val context = LocalContext.current

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = StPrimary, modifier = Modifier.size(28.dp))
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StBg),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {

        // ── Шапка ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "НА СКЛАД",
                        fontSize = 10.sp,
                        color = StDanger,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "${scooters.size} самокатов",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StTextMain
                    )
                }
                // Кнопка экспорта (пока заглушка)
                OutlinedButton(
                    onClick = onExportClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StPrimary.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Экспорт", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Пусто ──
        if (scooters.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        null,
                        tint = StTextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Text("Нет самокатов на склад", color = StTextMuted, fontSize = 15.sp)
                    Text("Все в порядке 🎉", color = StTextSec, fontSize = 12.sp)
                }
            }
            return@LazyColumn
        }

        // ── Список ──
        items(scooters, key = { it.id }) { scooter ->
            StorageScooterCard(
                scooter = scooter,
                onMapClick = {
                    val uri = Uri.parse(
                        "geo:${scooter.lat},${scooter.lon}?q=${scooter.lat},${scooter.lon}(${scooter.code})"
                    )
                    context.startActivity(
                        Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Открыть в картах")
                    )
                }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ── Карточка самоката на складе ───────────────────────────────────────────────
@Composable
private fun StorageScooterCard(
    scooter: StorageScooter,
    onMapClick: () -> Unit
) {
    val timeStr = remember(scooter.completedAt) {
        if (scooter.completedAt > 0)
            SimpleDateFormat("HH:mm · dd.MM", Locale.getDefault()).format(Date(scooter.completedAt))
        else "—"
    }
    val initials = scooter.technicianName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StCard)
            .border(1.dp, StDanger.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
    ) {
        // ── Верх: номер + время ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StDanger)
                )
                Column {
                    Text(
                        scooter.code,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = StTextMain,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    if (scooter.model.isNotBlank()) {
                        Text(scooter.model, color = StTextMuted, fontSize = 11.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(StDanger.copy(alpha = 0.12f))
                        .border(0.5.dp, StDanger.copy(alpha = 0.3f), RoundedCornerShape(7.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("На склад", color = StDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(timeStr, color = StTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        HorizontalDivider(color = StBorder, thickness = 0.5.dp)

        // ── Техник + батарея ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(StPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = StPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    scooter.technicianName.split(" ").let { p ->
                        if (p.size >= 2) "${p[0]} ${p[1].firstOrNull() ?: ""}." else p.firstOrNull() ?: ""
                    },
                    color = StTextSec,
                    fontSize = 13.sp
                )
            }
            // Батарея
            if (scooter.batteryPct > 0) {
                val battColor = when {
                    scooter.batteryPct <= 10 -> StDanger
                    scooter.batteryPct <= 30 -> StWarning
                    else -> StTextMuted
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.BatteryStd, null, tint = battColor, modifier = Modifier.size(14.dp))
                    Text("${scooter.batteryPct}%", color = battColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Виды работ ──
        if (scooter.repairTypes.isNotEmpty()) {
            HorizontalDivider(color = StBorder, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                scooter.repairTypes.take(4).forEach { key ->
                    val label = REPAIR_LABELS[key] ?: key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StPrimary.copy(alpha = 0.1f))
                            .border(0.5.dp, StPrimary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(label, color = StPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Заметка ──
        if (scooter.notes.isNotBlank()) {
            HorizontalDivider(color = StBorder, thickness = 0.5.dp)
            Text(
                scooter.notes,
                color = StTextSec,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        HorizontalDivider(color = StBorder, thickness = 0.5.dp)

        // ── Координаты + кнопка карты ──
        if (scooter.lat != 0.0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Координаты", color = StTextMuted, fontSize = 11.sp)
                    Text(
                        "%.4f, %.4f".format(scooter.lat, scooter.lon),
                        color = StTextMain,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                OutlinedButton(
                    onClick = onMapClick,
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StPrimary.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("На карте", fontSize = 12.sp)
                }
            }
        }
    }
}