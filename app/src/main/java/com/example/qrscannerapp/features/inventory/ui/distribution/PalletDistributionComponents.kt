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
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

val ColorFujian = Color(0xFFFF8A65) // Мягкий оранжевый
val ColorByd = Color(0xFF4FC3F7)    // Мягкий голубой

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
    fujianCount: Int,
    bydCount: Int,
    onBufferClick: () -> Unit
) {
    val distributedCount = totalCount - undistributedCount
    val progress = if (totalCount > 0) distributedCount.toFloat() / totalCount else 0f
    val progressPercent = (progress * 100).toInt()

    val fujianPercent = if (totalCount > 0) (fujianCount.toFloat() / totalCount * 100).toInt() else 0
    val bydPercent = if (totalCount > 0) (bydCount.toFloat() / totalCount * 100).toInt() else 0

    // --- АНИМАЦИЯ ЦВЕТОВ КНОПКИ БУФЕРА ---
    // Если есть товары - Янтарный, если пусто - Зеленый (но прозрачный)
    val targetBaseColor = if (undistributedCount > 0) StardustWarning else StardustSuccess

    val animatedBgColor by animateColorAsState(
        targetValue = targetBaseColor.copy(alpha = 0.15f), // 15% прозрачности для фона
        animationSpec = tween(500),
        label = "BufferBg"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBaseColor.copy(alpha = 0.5f), // 50% для рамки
        animationSpec = tween(500),
        label = "BufferBorder"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = targetBaseColor,
        animationSpec = tween(500),
        label = "BufferContent"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Блок "Всего"
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
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(ColorFujian, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fujian: ", color = StardustTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("$fujianCount", color = StardustTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("($fujianPercent%)", color = ColorFujian, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(ColorByd, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("BYD: ", color = StardustTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("$bydCount", color = StardustTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("($bydPercent%)", color = ColorByd, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Блок "В буфере" - СТИЛЬНОЕ СТЕКЛО
                Surface(
                    onClick = onBufferClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = animatedBgColor, // Анимированный фон
                    border = BorderStroke(1.dp, animatedBorderColor) // Анимированная рамка
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, null, tint = animatedContentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("В буфере", color = StardustTextSecondary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        AnimatedCounterText(
                            count = undistributedCount,
                            suffix = " шт.",
                            color = animatedContentColor, // Цвет цифры тоже меняется
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (undistributedCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Pending, null, tint = animatedContentColor, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Нажми для\nпросмотра", color = animatedContentColor, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = animatedContentColor.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Всё чисто", color = animatedContentColor.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
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
}

@Composable
fun BufferDetailsDialog(
    items: List<String>,
    onDismiss: () -> Unit,
    onDeleteItem: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg),
            modifier = Modifier.heightIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Layers, null, tint = StardustWarning, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Буфер обмена", style = MaterialTheme.typography.titleLarge, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Эти АКБ отсканированы, но еще не добавлены в палет. Нажми крестик, чтобы удалить ошибку.", color = StardustTextSecondary, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(16.dp))

                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Буфер пуст", color = StardustTextSecondary.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StardustItemBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item, color = StardustTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                IconButton(
                                    onClick = { onDeleteItem(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = StardustError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)
                ) {
                    Text("Закрыть", color = StardustTextPrimary)
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
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("История операций:", fontWeight = FontWeight.Bold, color = StardustTextPrimary, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
                if (isAdmin) {
                    IconButton(onClick = onClearLogClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Очистить", tint = StardustTextSecondary)
                    }
                }
            }
            if (logEntries.isEmpty()) {
                Text("Нет недавних операций.", color = StardustTextSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(logEntries, key = { it.id }) { entry -> PalletLogEntryItem(entry) }
                }
            }
        }
    }
}

@Composable
fun PalletLogEntryItem(entry: PalletActivityLogEntry) {
    val (icon, color, text) = when (entry.action) {
        "CREATED" -> Triple(Icons.Default.Add, StardustSecondary, "создал палет")
        "DELETED" -> Triple(Icons.Default.Delete, StardustError, "удалил палет")
        "DISTRIBUTED" -> Triple(Icons.Default.Done, StardustSuccess, "добавил АКБ на палет")
        "REMOVED_ITEM" -> Triple(Icons.Default.Clear, StardustError, "удалил АКБ с палета")
        "RESTORED_ITEM" -> Triple(Icons.Default.SettingsBackupRestore, StardustWarning, "восстановил АКБ на палет")
        else -> Triple(Icons.Default.Done, StardustTextSecondary, entry.action)
    }
    val actionText = buildAnnotatedString {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        append("${sdf.format(Date(entry.timestamp))} ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = StardustTextPrimary)) { append("${entry.userName ?: "Система"}") }
        append(" $text ")
        if (entry.palletNumber != null) {
            withStyle(style = SpanStyle(color = StardustPrimary, fontWeight = FontWeight.Bold)) { append("№${entry.palletNumber}") }
        }
        if (entry.itemCount != null && entry.itemCount > 0) {
            append(" (${entry.itemCount} шт.)")
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = actionText, color = StardustTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun PulsatingShareButton(modifier: Modifier = Modifier, enabled: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsating")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 2.5f, animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "scale")
    val alpha by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "alpha")
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        if (enabled) Box(Modifier.size(48.dp).scale(scale).graphicsLayer { this.alpha = alpha }.background(StardustPrimary.copy(alpha = 0.5f), CircleShape))
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp).clip(CircleShape).background(StardustPrimary), colors = IconButtonDefaults.iconButtonColors(contentColor = StardustTextPrimary, disabledContentColor = StardustTextSecondary)) {
            Icon(Icons.Default.Share, "Экспорт")
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