// Полная версия файла: features/inventory/ui/distribution/PalletDistributionComponents.kt

package com.example.qrscannerapp.features.inventory.ui.distribution

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.qrscannerapp.DistributionReport
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSecondary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.StardustWarning
import com.example.qrscannerapp.features.inventory.domain.model.CellGroup
import com.example.qrscannerapp.features.inventory.domain.model.CellStatus
import com.example.qrscannerapp.features.inventory.domain.model.CellType
import com.example.qrscannerapp.features.inventory.domain.model.PalletActivityLogEntry
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet
import java.text.SimpleDateFormat
import java.util.*

// --- ЦВЕТА ТИПОВ ЯЧЕЕК ---

val ColorFujian     = Color(0xFFFF8A65)  // WIND 4.0 FUJIAN — оранжевый
val ColorByd        = Color(0xFF4FC3F7)  // WIND 4.0 BYD — голубой
val ColorNinebotNew = Color(0xFF69F0AE)  // WIND 5.0 Новый (5BB) — зелёный
val ColorNinebotOld = Color(0xFFFFAB40)  // WIND 5.0 Старый (SF) — янтарный

/** Возвращает цвет для типа ячейки */
fun colorForCellType(cellType: CellType?): Color = when (cellType) {
    CellType.FUJIAN      -> ColorFujian
    CellType.BYD         -> ColorByd
    CellType.NINEBOT_NEW -> ColorNinebotNew
    CellType.NINEBOT_OLD -> ColorNinebotOld
    null                 -> StardustPrimary
}

// --- ФУНКЦИИ ---

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

@Composable
fun getColorByProgress(progress: Float): Color {
    return when {
        progress < 0.5f -> {
            val r = lerp(StardustError.red, Color.Yellow.red, progress * 2f)
            val g = lerp(StardustError.green, Color.Yellow.green, progress * 2f)
            val b = lerp(StardustError.blue, Color.Yellow.blue, progress * 2f)
            Color(r, g, b)
        }
        else -> {
            val normalizedProgress = (progress - 0.5f) * 2f
            val r = lerp(Color.Yellow.red, StardustSuccess.red, normalizedProgress)
            val g = lerp(Color.Yellow.green, StardustSuccess.green, normalizedProgress)
            val b = lerp(Color.Yellow.blue, StardustSuccess.blue, normalizedProgress)
            Color(r, g, b)
        }
    }
}

@Composable
fun AnimatedCounterText(
    count: Int,
    suffix: String = "",
    prefix: String = "",
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified
) {
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "counter"
    )

    Text(
        text = "$prefix$animatedCount$suffix",
        style = style,
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize
    )
}

// --- КОМПОНЕНТЫ ---

@Composable
fun InventorySummaryCard(
    totalCount: Int,
    undistributedCount: Int,
    todayCount: Int = 0,
    fujianCount: Int,
    bydCount: Int,
    ninebotNewCount: Int = 0,
    ninebotOldCount: Int = 0,
    onBufferClick: () -> Unit
) {
    var showTodayStatsDialog by remember { mutableStateOf(false) }

    val distributedCount = totalCount - undistributedCount
    val progress = if (totalCount > 0) distributedCount.toFloat() / totalCount else 0f
    val progressPercent = (progress * 100).toInt()

    val fujianPercent = if (totalCount > 0) (fujianCount.toFloat() / totalCount * 100).toInt() else 0
    val bydPercent = if (totalCount > 0) (bydCount.toFloat() / totalCount * 100).toInt() else 0
    val ninebotNewPercent = if (totalCount > 0) (ninebotNewCount.toFloat() / totalCount * 100).toInt() else 0
    val ninebotOldPercent = if (totalCount > 0) (ninebotOldCount.toFloat() / totalCount * 100).toInt() else 0

    val bufferBaseColor = if (undistributedCount > 0) StardustWarning else StardustSuccess
    val animatedBgColor by animateColorAsState(targetValue = bufferBaseColor.copy(alpha = 0.15f), label = "bufBg")
    val animatedBorderColor by animateColorAsState(targetValue = bufferBaseColor.copy(alpha = 0.5f), label = "bufBorder")
    val animatedContentColor by animateColorAsState(targetValue = bufferBaseColor, label = "bufContent")

    val todayBaseColor = StardustPrimary
    val todayBgColor = todayBaseColor.copy(alpha = 0.15f)
    val todayBorderColor = todayBaseColor.copy(alpha = 0.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Всего на складе", color = StardustTextSecondary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    AnimatedCounterText(
                        count = totalCount,
                        suffix = " шт.",
                        color = StardustTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar — 4 сегмента по типам АКБ
                    Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)) {
                        if (fujianPercent > 0) Box(modifier = Modifier.weight(fujianPercent.toFloat()).fillMaxHeight().background(ColorFujian))
                        if (bydPercent > 0) Box(modifier = Modifier.weight(bydPercent.toFloat()).fillMaxHeight().background(ColorByd))
                        if (ninebotNewPercent > 0) Box(modifier = Modifier.weight(ninebotNewPercent.toFloat()).fillMaxHeight().background(ColorNinebotNew))
                        if (ninebotOldPercent > 0) Box(modifier = Modifier.weight(ninebotOldPercent.toFloat()).fillMaxHeight().background(ColorNinebotOld))
                        // Fallback если всё 0
                        if (fujianPercent == 0 && bydPercent == 0 && ninebotNewPercent == 0 && ninebotOldPercent == 0) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(StardustItemBg))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Чипы — WIND 4.0
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CountChip("Fujian", fujianCount, ColorFujian)
                        CountChip("BYD", bydCount, ColorByd)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Чипы — WIND 5.0
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CountChip("5BB", ninebotNewCount, ColorNinebotNew)
                        CountChip("SF", ninebotOldCount, ColorNinebotOld)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Surface(
                        onClick = { showTodayStatsDialog = true },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = todayBgColor,
                        border = BorderStroke(1.dp, todayBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Сегодня", color = StardustTextSecondary, fontSize = 10.sp)
                                AnimatedCounterText(
                                    count = todayCount,
                                    prefix = "+",
                                    color = StardustPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Outlined.Today, null, tint = StardustPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Surface(
                        onClick = onBufferClick,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = animatedBgColor,
                        border = BorderStroke(1.dp, animatedBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("В буфере", color = StardustTextSecondary, fontSize = 10.sp)
                                AnimatedCounterText(
                                    count = undistributedCount,
                                    suffix = " шт.",
                                    color = animatedContentColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (undistributedCount > 0) Icons.Outlined.Pending else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = animatedContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Упаковано в ячейки", fontSize = 12.sp, color = StardustTextSecondary)
                    AnimatedCounterText(
                        count = progressPercent,
                        suffix = "%",
                        color = StardustPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = StardustPrimary,
                    trackColor = StardustItemBg,
                )
            }
        }
    }

    if (showTodayStatsDialog) {
        TodayStatsDialog(todayCount = todayCount, onDismiss = { showTodayStatsDialog = false })
    }
}

@Composable
private fun CountChip(label: String, count: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = CircleShape,
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("$label: $count", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TodayStatsDialog(todayCount: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Today, null, tint = StardustPrimary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Статистика за сегодня", style = MaterialTheme.typography.titleLarge, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Всего добавлено: +$todayCount шт.", color = StardustSuccess, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Детальная статистика по ячейкам в разработке...", color = StardustTextSecondary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)) {
                    Text("Закрыть", color = StardustTextSecondary)
                }
            }
        }
    }
}

@Composable
fun PalletActivityLogView(
    logEntries: List<PalletActivityLogEntry>,
    isAdmin: Boolean,
    onClearLogClick: () -> Unit
) {
    val latestEntry = logEntries.firstOrNull()
    var showFullLogDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { showFullLogDialog = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.History, null, tint = StardustTextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))

            if (latestEntry != null) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(latestEntry.timestamp))
                val desc = getActionDescriptionShort(latestEntry)
                Text(
                    text = "$time • ${latestEntry.userName}: $desc",
                    color = StardustTextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text("История операций пуста", color = StardustTextSecondary, style = MaterialTheme.typography.bodyMedium)
            }

            Icon(Icons.Default.ChevronRight, null, tint = StardustTextSecondary.copy(alpha = 0.5f))
        }
    }

    if (showFullLogDialog) {
        val groupedLogs = remember(logEntries) {
            logEntries.groupBy {
                SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(it.timestamp))
            }
        }

        AlertDialog(
            onDismissRequest = { showFullLogDialog = false },
            containerColor = StardustModalBg,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("История операций", color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                    if (isAdmin && logEntries.isNotEmpty()) {
                        IconButton(onClick = { showFullLogDialog = false; onClearLogClick() }) {
                            Icon(Icons.Outlined.DeleteOutline, null, tint = StardustError)
                        }
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    groupedLogs.forEach { (date, entries) ->
                        item {
                            Text(
                                text = date,
                                color = StardustPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                        items(entries) { entry -> PalletLogEntryItem(entry) }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFullLogDialog = false }) { Text("Закрыть", color = StardustPrimary) }
            }
        )
    }
}

private fun getActionDescriptionShort(entry: PalletActivityLogEntry): String {
    return when (entry.action) {
        "CREATED" -> "Создал ячейку №${entry.palletNumber}"
        "DISTRIBUTED" -> "Добавил ${entry.itemCount} шт. → №${entry.palletNumber}"
        "REMOVED_ITEM" -> "Удалил АКБ (№${entry.palletNumber})"
        "RESTORED_ITEM" -> "Вернул АКБ (№${entry.palletNumber})"
        "DELETED" -> "Удалил ячейку №${entry.palletNumber}"
        else -> entry.action
    }
}

@Composable
fun BufferDetailsDialog(items: List<String>, onDismiss: () -> Unit, onDeleteItem: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg), modifier = Modifier.heightIn(max = 500.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Layers, null, tint = StardustWarning, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Буфер обмена", style = MaterialTheme.typography.titleLarge, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("Буфер пуст", color = StardustTextSecondary.copy(alpha = 0.5f)) }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { item ->
                            Row(modifier = Modifier.fillMaxWidth().background(StardustItemBg, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(item, color = StardustTextPrimary, fontSize = 14.sp)
                                IconButton(onClick = { onDeleteItem(item) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = StardustError, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)) { Text("Закрыть", color = StardustTextPrimary) }
            }
        }
    }
}

@Composable
fun PalletLogEntryItem(entry: PalletActivityLogEntry) {
    val (icon, color, text) = when (entry.action) {
        "CREATED" -> Triple(Icons.Default.Add, StardustSecondary, "создал ячейку")
        "DELETED" -> Triple(Icons.Default.Delete, StardustError, "удалил ячейку")
        "DISTRIBUTED" -> Triple(Icons.Default.Done, StardustSuccess, "добавил АКБ")
        "REMOVED_ITEM" -> Triple(Icons.Default.Clear, StardustError, "удалил АКБ")
        "RESTORED_ITEM" -> Triple(Icons.Default.SettingsBackupRestore, StardustWarning, "восстановил АКБ")
        else -> Triple(Icons.Default.Info, StardustTextSecondary, entry.action)
    }
    val actionText = buildAnnotatedString {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
        withStyle(style = SpanStyle(color = StardustTextSecondary, fontSize = 11.sp)) { append("$time ") }
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = StardustTextPrimary)) { append("${entry.userName ?: "System"}") }
        append(" $text ")
        if (entry.palletNumber != null) {
            withStyle(style = SpanStyle(color = StardustPrimary, fontWeight = FontWeight.Bold)) { append("№${entry.palletNumber}") }
        }
        if (entry.itemCount != null && entry.itemCount > 0) {
            append(" (${entry.itemCount} шт.)")
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = actionText, color = StardustTextSecondary, fontSize = 12.sp)
    }
    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f), thickness = 0.5.dp)
}

@Composable
fun PulsatingShareButton(modifier: Modifier = Modifier, enabled: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsating")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.1f, animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "scale")
    val alpha by infiniteTransition.animateFloat(initialValue = 0.5f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "alpha")

    Box(modifier = modifier.size(56.dp), contentAlignment = Alignment.Center) {
        if (enabled) Box(Modifier.size(56.dp).scale(scale).graphicsLayer { this.alpha = alpha }.background(StardustPrimary, CircleShape))
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(56.dp).clip(CircleShape).background(StardustPrimary),
            colors = IconButtonDefaults.iconButtonColors(contentColor = StardustTextPrimary, disabledContentColor = StardustTextSecondary)
        ) {
            Icon(Icons.Default.Share, "Экспорт", modifier = Modifier.size(24.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PalletTile(
    pallet: StoragePallet,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditManufacturerClick: () -> Unit
) {
    val progress = pallet.fillProgress
    val progressColor = getColorByProgress(progress)
    val borderWidth by animateDpAsState(targetValue = if (isHighlighted) 4.dp else 0.dp, animationSpec = tween(500), label = "border")
    val borderColor by animateColorAsState(targetValue = if (isHighlighted) StardustWarning else Color.Transparent, animationSpec = tween(500), label = "borderCol")

    Card(
        modifier = Modifier.aspectRatio(1f).border(borderWidth, borderColor, RoundedCornerShape(16.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pallet.resolvedDisplayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StardustTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEditManufacturerClick, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Factory, null, tint = StardustSecondary) }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = StardustError) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                CellTypeChip(pallet)
                Spacer(modifier = Modifier.weight(1f, fill = false))
                Text("${pallet.items.size} / ${pallet.capacity}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                // Показать статус если не ACTIVE
                val status = pallet.resolvedStatus
                if (status != CellStatus.ACTIVE) {
                    Box(
                        modifier = Modifier
                            .background(StardustPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${status.emoji} ${status.displayName}",
                            fontSize = 10.sp,
                            color = StardustPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text("Нажмите для добавления\nУдерживайте для деталей", fontSize = 10.sp, textAlign = TextAlign.Center, color = StardustTextSecondary)
                }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).align(Alignment.BottomCenter), color = progressColor, trackColor = StardustItemBg)
        }
    }
}

@Composable
fun NewPalletTile(onClick: () -> Unit) {
    Card(modifier = Modifier.aspectRatio(1f).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustItemBg)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(Icons.Default.Add, "Создать", modifier = Modifier.size(48.dp), tint = StardustTextSecondary)
        }
    }
}

/**
 * Чип типа ячейки — показывает цветной лейбл с коротким названием типа.
 */
@Composable
fun CellTypeChip(pallet: StoragePallet) {
    val cellType = pallet.resolvedCellType
    val color = colorForCellType(cellType)
    val label = cellType?.shortLabel ?: pallet.manufacturer ?: return

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Диалог выбора типа ячейки.
 * Показывает 4 типа АКБ + опцию "Универсальная" (без типа).
 */
@Composable
fun CellTypeSelectionDialog(
    pallet: StoragePallet,
    onDismiss: () -> Unit,
    onCellTypeSelected: (StoragePallet, CellType?) -> Unit
) {
    val currentType = pallet.resolvedCellType

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Тип ячейки №${pallet.palletNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StardustTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ячейка будет принимать только АКБ выбранного типа",
                    style = MaterialTheme.typography.bodySmall,
                    color = StardustTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Группа WIND 4.0
                Text(
                    "WIND 4.0",
                    color = StardustTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                CellType.entries.filter { it.group == CellGroup.WIND_40 }.forEach { type ->
                    CellTypeOption(
                        type = type,
                        isSelected = currentType == type,
                        onClick = { onCellTypeSelected(pallet, type) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Группа WIND 5.0
                Text(
                    "WIND 5.0 / Ninebot",
                    color = StardustTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                CellType.entries.filter { it.group == CellGroup.WIND_50 }.forEach { type ->
                    CellTypeOption(
                        type = type,
                        isSelected = currentType == type,
                        onClick = { onCellTypeSelected(pallet, type) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(4.dp))

                // Опция "Универсальная"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCellTypeSelected(pallet, null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentType == null,
                        onClick = { onCellTypeSelected(pallet, null) },
                        colors = RadioButtonDefaults.colors(selectedColor = StardustTextSecondary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Универсальная (любые АКБ)", color = StardustTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun CellTypeOption(
    type: CellType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = colorForCellType(type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = color)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(type.displayName, color = StardustTextPrimary, fontWeight = FontWeight.Medium)
            Text(
                "Префикс: ${type.prefix}",
                color = StardustTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onShareExcel: () -> Unit,
    onSaveExcel: () -> Unit,
    onSharePdf: () -> Unit,
    onSavePdf: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Экспорт склада", color = StardustTextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))

                Text("Excel (Полный список ID)", color = StardustTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveExcel, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) { Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Файл", fontSize = 12.sp) }
                    Button(onClick = onShareExcel, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustSecondary)) { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Поделиться", fontSize = 12.sp) }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text("PDF (Сводный реестр)", color = StardustTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSavePdf, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) { Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Файл", fontSize = 12.sp) }
                    Button(onClick = onSharePdf, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustSecondary)) { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Поделиться", fontSize = 12.sp) }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)) { Text("Отмена", color = StardustTextSecondary) }
            }
        }
    }
}

@Composable
fun PalletDeleteDialog(pallet: StoragePallet, onDismiss: () -> Unit, onConfirmDelete: (StoragePallet) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить ячейку №${pallet.palletNumber}?") },
        text = { Text("Вы уверены? Это действие удалит ячейку из системы и сбросит статус ${pallet.items.size} привязанных АКБ. АКБ снова станут доступны для приемки.") },
        confirmButton = { Button(onClick = { onConfirmDelete(pallet) }, colors = ButtonDefaults.buttonColors(containerColor = StardustError)) { Text("Удалить", color = StardustTextPrimary) } },
        dismissButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)) { Text("Отмена", color = StardustTextSecondary) } },
        containerColor = StardustModalBg, titleContentColor = StardustTextPrimary, textContentColor = StardustTextSecondary
    )
}

@Composable
fun PalletItemListItem(batteryId: String, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(batteryId, color = StardustTextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Clear, null, tint = StardustError) }
    }
}

// --- ДИАЛОГ ОТЧЕТА ---
@Composable
fun DistributionReportDialog(
    report: DistributionReport,
    onDismiss: () -> Unit
) {
    val hasErrors = report.errorCount > 0
    val dialogColor = if (hasErrors) StardustWarning else StardustSuccess

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg),
            border = if (hasErrors) BorderStroke(2.dp, StardustError) else null,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                if (hasErrors) {
                    Text("ВНИМАНИЕ", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Попытка добавить АКБ другого типа",
                        color = StardustError,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = StardustSuccess,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Успешно!", style = MaterialTheme.typography.titleLarge, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Добавлено", color = StardustTextSecondary, fontSize = 12.sp)
                        Text("+${report.successCount}", color = StardustSuccess, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    if (report.errorCount > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Исключено", color = StardustTextSecondary, fontSize = 12.sp)
                            Text("${report.errorCount}", color = StardustError, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (report.duplicateCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Дубликатов: ${report.duplicateCount}", color = StardustTextSecondary, fontSize = 12.sp)
                }

                if (hasErrors) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Уберите эти АКБ из списка:",
                        color = StardustTextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .background(StardustItemBg, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(report.excludedItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StardustModalBg, RoundedCornerShape(8.dp))
                                    .border(1.dp, StardustError.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(StardustError, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("№${item.indexInList}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(item.code, color = StardustTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(item.reason, color = StardustError, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if(hasErrors) StardustWarning else StardustPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if(hasErrors) "Я понял" else "Продолжить", color = if(hasErrors) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}