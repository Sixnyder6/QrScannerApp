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
// import androidx.compose.material.icons.outlined.WarningAmber // Убрали иконку
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
import com.example.qrscannerapp.features.inventory.domain.model.PalletActivityLogEntry
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet
import java.text.SimpleDateFormat
import java.util.*

// --- КОНСТАНТЫ И ФУНКЦИИ ---

const val MAX_ITEMS_PER_PALLET = 500

val ColorFujian = Color(0xFFFF8A65)
val ColorByd = Color(0xFF4FC3F7)

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
    onBufferClick: () -> Unit
) {
    var showTodayStatsDialog by remember { mutableStateOf(false) }

    val distributedCount = totalCount - undistributedCount
    val progress = if (totalCount > 0) distributedCount.toFloat() / totalCount else 0f
    val progressPercent = (progress * 100).toInt()

    val fujianPercent = if (totalCount > 0) (fujianCount.toFloat() / totalCount * 100).toInt() else 0
    val bydPercent = if (totalCount > 0) (bydCount.toFloat() / totalCount * 100).toInt() else 0

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

                    Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)) {
                        Box(modifier = Modifier.weight(fujianPercent.coerceAtLeast(1).toFloat()).fillMaxHeight().background(ColorFujian))
                        Box(modifier = Modifier.weight(bydPercent.coerceAtLeast(1).toFloat()).fillMaxHeight().background(ColorByd))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = ColorFujian.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(ColorFujian, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fujian: $fujianCount", color = ColorFujian, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = ColorByd.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(ColorByd, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("BYD: $bydCount", color = ColorByd, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
                    Text("Упаковано в палеты", fontSize = 12.sp, color = StardustTextSecondary)
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
                Text("Детальная статистика по палетам в разработке...", color = StardustTextSecondary, textAlign = TextAlign.Center)
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
        "CREATED" -> "Создал палет №${entry.palletNumber}"
        "DISTRIBUTED" -> "Добавил ${entry.itemCount} шт. → №${entry.palletNumber}"
        "REMOVED_ITEM" -> "Удалил АКБ (№${entry.palletNumber})"
        "RESTORED_ITEM" -> "Вернул АКБ (№${entry.palletNumber})"
        "DELETED" -> "Удалил палет №${entry.palletNumber}"
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
        "CREATED" -> Triple(Icons.Default.Add, StardustSecondary, "создал палет")
        "DELETED" -> Triple(Icons.Default.Delete, StardustError, "удалил палет")
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
    val progress = if (MAX_ITEMS_PER_PALLET > 0) pallet.items.size.toFloat() / MAX_ITEMS_PER_PALLET else 0f
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
                    Text("Палет №${pallet.palletNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StardustTextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEditManufacturerClick, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Factory, null, tint = StardustSecondary) }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = StardustError) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                pallet.manufacturer?.let { Chip(it) }
                Spacer(modifier = Modifier.weight(1f, fill = false))
                Text("${pallet.items.size} / $MAX_ITEMS_PER_PALLET", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                Text("Нажмите для добавления\nУдерживайте для деталей", fontSize = 10.sp, textAlign = TextAlign.Center, color = StardustTextSecondary)
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

@Composable
fun Chip(text: String) {
    val bgColor = when(text) {
        "FUJIAN" -> ColorFujian.copy(alpha = 0.2f)
        "BYD" -> ColorByd.copy(alpha = 0.2f)
        else -> StardustPrimary.copy(alpha = 0.3f)
    }
    val contentColor = when(text) {
        "FUJIAN" -> ColorFujian
        "BYD" -> ColorByd
        else -> StardustPrimary
    }

    Box(modifier = Modifier.background(bgColor, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = contentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
        title = { Text("Удалить палет №${pallet.palletNumber}?") },
        text = { Text("Вы уверены? Это действие удалит палет из системы и сбросит статус ${pallet.items.size} привязанных АКБ. АКБ снова станут доступны для приемки.") },
        confirmButton = { Button(onClick = { onConfirmDelete(pallet) }, colors = ButtonDefaults.buttonColors(containerColor = StardustError)) { Text("Удалить", color = StardustTextPrimary) } },
        dismissButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)) { Text("Отмена", color = StardustTextSecondary) } },
        containerColor = StardustModalBg, titleContentColor = StardustTextPrimary, textContentColor = StardustTextSecondary
    )
}

@Composable
fun ManufacturerSelectionDialog(pallet: StoragePallet, onDismiss: () -> Unit, onManufacturerSelected: (StoragePallet, String) -> Unit) {
    val manufacturers = listOf("FUJIAN", "BYD", "Нет")
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Производитель для Палета №${pallet.palletNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                manufacturers.forEach { manufacturer ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onManufacturerSelected(pallet, manufacturer) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = (pallet.manufacturer == manufacturer) || (manufacturer == "Нет" && pallet.manufacturer == null), onClick = { onManufacturerSelected(pallet, manufacturer) }, colors = RadioButtonDefaults.colors(selectedColor = StardustPrimary))
                        Spacer(Modifier.width(8.dp))
                        Text(manufacturer, color = StardustTextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun PalletItemListItem(batteryId: String, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(batteryId, color = StardustTextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Clear, null, tint = StardustError) }
    }
}

// --- НОВЫЙ ДИАЛОГ ОТЧЕТА ---
@Composable
fun DistributionReportDialog(
    report: DistributionReport,
    onDismiss: () -> Unit
) {
    val hasErrors = report.errorCount > 0
    val dialogColor = if (hasErrors) StardustWarning else StardustSuccess
    // Заголовок только текстом
    // Убираем иконку и оставляем только текст "ВНИМАНИЕ" (капсом) для ошибок

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg),
            border = if (hasErrors) BorderStroke(2.dp, StardustError) else null, // Красная рамка при ошибке
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                if (hasErrors) {
                    // СТРОГИЙ ДИЗАЙН ПРИ ОШИБКЕ
                    Text("ВНИМАНИЕ", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Попытка добавить АКБ другого производителя",
                        color = StardustError,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // ОБЫЧНЫЙ ДИЗАЙН ПРИ УСПЕХЕ
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

                // Крупные цифры
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

                // Список ошибок
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
                                Text(item.code, color = StardustTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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