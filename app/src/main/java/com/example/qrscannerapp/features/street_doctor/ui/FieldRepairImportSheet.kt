package com.example.qrscannerapp.features.street_doctor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.common.ui.AnimatedDialogWrapper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private val FrPrimary   = Color(0xFF6C5CE7)
private val FrSuccess   = Color(0xFF10B981)
private val FrDanger    = Color(0xFFEF4444)
private val FrWarning   = Color(0xFFF59E0B)
private val FrCard      = Color(0xFF1C1C22)
private val FrCardDark  = Color(0xFF16161C)
private val FrBorder    = Color(0xFF2A2A35)
private val FrTextMain  = Color(0xFFFFFFFF)
private val FrTextMuted = Color(0xFF8E8E93)
private val FrTextSec   = Color(0xFF7A7A90)

private val dtFormatter = DateTimeFormatter
    .ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru"))
    .withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldRepairImportSheet(
    onDismiss: () -> Unit,
    viewModel: FieldRepairImportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "файл.xlsx"
            viewModel.addToQueue(uri, fileName)
        }
    }

    // Диалог подтверждения при блокирующих предупреждениях
    if (state.showWarningDialog) {
        WarningConfirmDialog(
            warnings  = state.warnings.filter { it.isBlocker },
            onConfirm = { viewModel.confirmWarningAndSend() },
            onDismiss = { viewModel.dismissWarningDialog() }
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            val s = state.stage
            if (s is FieldImportStage.Idle || s is FieldImportStage.Done || s is FieldImportStage.Error) onDismiss()
        },
        sheetState     = sheetState,
        containerColor = FrCard,
        dragHandle = {
            Box(modifier = Modifier.padding(vertical = 12.dp).size(width = 40.dp, height = 4.dp).background(FrBorder, CircleShape))
        }
    ) {
        when (val stage = state.stage) {
            is FieldImportStage.Idle -> {
                IdleQueueContent(
                    state              = state,
                    onSelectTechnician = { tech ->
                        viewModel.selectTechnician(tech)
                        filePicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    },
                    onRemoveEntry      = { viewModel.removeFromQueue(it) },
                    onSendAll          = { viewModel.sendAll() },
                    onCloseSession     = { viewModel.closeActiveSession() }
                )
            }
            is FieldImportStage.Done  -> DoneContent(stage = stage, onDismiss = onDismiss)
            is FieldImportStage.Error -> ErrorContent(
                stage     = stage,
                onRetry   = { viewModel.resetToIdle() },
                onDismiss = { viewModel.resetToIdle(); onDismiss() }
            )
            else -> SendingContent(
                stage    = stage,
                progress = state.progress,
                techName = state.currentSendingName,
                onCancel = { viewModel.cancelImport() }
            )
        }
    }
}

// ── Диалог подтверждения ──────────────────────────────────────────────────────
@Composable
private fun WarningConfirmDialog(
    warnings: List<ImportWarning>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(FrCard)
                .border(1.dp, FrWarning.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Warning, null, tint = FrWarning, modifier = Modifier.size(24.dp))
                Text("Требует внимания", color = FrTextMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }

            warnings.forEach { warning ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FrWarning.copy(alpha = 0.08f))
                        .border(1.dp, FrWarning.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(warning.message, color = FrTextMain, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            Text(
                "Вы уверены что хотите продолжить?",
                color = FrTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FrBorder)
                ) { Text("Отмена", color = FrTextSec) }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FrWarning)
                ) { Text("Всё равно отправить", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
        }
    }
}

// ── Главный экран с очередью ──────────────────────────────────────────────────
@Composable
private fun IdleQueueContent(
    state: FieldRepairImportUiState,
    onSelectTechnician: (TechnicianInfo) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onSendAll: () -> Unit,
    onCloseSession: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Заголовок
        item {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(FrPrimary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AssignmentInd, null, tint = FrPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Полевой ремонт", color = FrTextMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Назначьте файлы техникам и отправьте", color = FrTextMuted, fontSize = 12.sp)
                }
            }
        }

        // ── Активная сессия ──
        if (state.isCheckingSession) {
            item {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(FrCardDark).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = FrPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Проверяем активные сессии...", color = FrTextMuted, fontSize = 12.sp)
                }
            }
        } else if (state.activeSession != null) {
            item {
                ActiveSessionBanner(
                    session       = state.activeSession,
                    onCloseSession = onCloseSession
                )
            }
        }

        // ── Предупреждения ──
        val nonSessionWarnings = state.warnings.filter { it.type != WarningType.ACTIVE_SESSION }
        if (nonSessionWarnings.isNotEmpty()) {
            items(nonSessionWarnings) { warning ->
                WarningBanner(warning = warning)
            }
        }

        // История
        state.lastImportedAt?.let { ts ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(FrBorder.copy(alpha = 0.4f)).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.History, null, tint = FrTextMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Последний выезд · ${dtFormatter.format(Instant.ofEpochMilli(ts))}", color = FrTextMuted, fontSize = 11.sp)
                        Text("${state.lastImportCount} заданий", color = FrTextSec, fontSize = 12.sp)
                    }
                }
            }
        }

        // Инструкция
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FrPrimary.copy(alpha = 0.07f)).border(1.dp, FrPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("КАК НАЗНАЧИТЬ", color = FrTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("1. Нажми на имя техника", color = FrTextSec, fontSize = 12.sp)
                Text("2. Выбери его файл .xlsx (с фильтром «Полевой ремонт»)", color = FrTextSec, fontSize = 12.sp)
                Text("3. Повтори для других техников", color = FrTextSec, fontSize = 12.sp)
                Text("4. Нажми «Отправить всё»", color = FrTextSec, fontSize = 12.sp)
            }
        }

        // Техники
        item {
            Text("ТЕХНИКИ", color = FrTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        if (state.isLoadingTechnicians) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FrPrimary, modifier = Modifier.size(24.dp))
                }
            }
        } else if (state.technicians.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FrCardDark).padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("Нет техников в системе", color = FrTextMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(state.technicians) { tech ->
                val inQueue   = state.queue.find { it.technician.id == tech.id }
                val isInQueue = inQueue != null

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isInQueue) FrSuccess.copy(alpha = 0.08f) else FrCardDark)
                        .border(1.dp, if (isInQueue) FrSuccess.copy(alpha = 0.3f) else FrBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isInQueue) { onSelectTechnician(tech) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(if (isInQueue) FrSuccess.copy(alpha = 0.2f) else FrPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isInQueue) {
                            Icon(Icons.Default.Check, null, tint = FrSuccess, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                tech.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                                color = FrPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(tech.name, color = FrTextMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (isInQueue && inQueue != null) {
                            val hasDupes = state.warnings.any { it.type == WarningType.DUPLICATE_SCOOTERS }
                            Text(
                                "${inQueue.scooterCount} самокатов · ${inQueue.fileName.take(20)}",
                                color = if (hasDupes) FrWarning else FrSuccess,
                                fontSize = 11.sp
                            )
                            if (inQueue.scooterCount > 500) {
                                Text("⚠️ Подозрительно много", color = FrWarning, fontSize = 10.sp)
                            }
                        } else {
                            Text("Нажмите чтобы выбрать файл", color = FrTextMuted, fontSize = 11.sp)
                        }
                    }

                    if (isInQueue && inQueue != null) {
                        IconButton(onClick = { onRemoveEntry(inQueue.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, tint = FrTextMuted, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Icon(Icons.Default.FileUpload, null, tint = FrPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Сводка и кнопка отправить
        if (state.queue.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))

                // Сводка
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FrCardDark).border(1.dp, FrBorder, RoundedCornerShape(12.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ИТОГО К ОТПРАВКЕ", color = FrTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    state.queue.forEach { entry ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.technician.name, color = FrTextMain, fontSize = 13.sp)
                            Text("${entry.scooterCount} самокатов", color = FrPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = FrBorder, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Всего", color = FrTextMuted, fontSize = 13.sp)
                        Text("${state.queue.sumOf { it.scooterCount }} самокатов", color = FrSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Если есть активная сессия — предупреждаем что заменим
                    if (state.activeSession != null) {
                        HorizontalDivider(color = FrBorder, thickness = 0.5.dp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.SwapHoriz, null, tint = FrWarning, modifier = Modifier.size(14.dp))
                            Text("Текущий выезд будет завершён и заменён", color = FrWarning, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                val hasBlockers = state.warnings.any { it.isBlocker }

                Button(
                    onClick = onSendAll,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasBlockers) FrWarning else FrSuccess
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(if (hasBlockers) Icons.Default.Warning else Icons.Default.Send, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (hasBlockers) "Отправить с предупреждениями"
                        else "Отправить всё · ${state.queue.size} техника(ов)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (hasBlockers) Color.Black else Color.White
                    )
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FrCardDark).border(1.dp, FrBorder, RoundedCornerShape(12.dp)).padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Inbox, null, tint = FrTextMuted, modifier = Modifier.size(32.dp))
                        Text("Очередь пуста", color = FrTextMuted, fontSize = 13.sp)
                        Text("Выберите техника и файл для него", color = FrTextSec, fontSize = 11.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Баннер активной сессии ────────────────────────────────────────────────────
@Composable
private fun ActiveSessionBanner(
    session: ActiveSessionInfo,
    onCloseSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FrWarning.copy(alpha = 0.08f))
            .border(1.dp, FrWarning.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Warning, null, tint = FrWarning, modifier = Modifier.size(18.dp))
            Text("Активный выезд", color = FrWarning, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text(
            "Дата: ${session.date} · ${session.totalCount} самокатов · выполнено ${session.doneCount}",
            color = FrTextMain, fontSize = 12.sp
        )

        if (session.technicNames.isNotEmpty()) {
            Text(
                "Техники: ${session.technicNames.joinToString(", ")}",
                color = FrTextMuted, fontSize = 11.sp
            )
        }

        val progress = if (session.totalCount > 0) session.doneCount.toFloat() / session.totalCount else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = FrSuccess,
            trackColor = FrBorder
        )

        Text(
            "${(progress * 100).toInt()}% выполнено",
            color = FrTextMuted, fontSize = 10.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCloseSession,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FrDanger.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FrDanger)
            ) {
                Icon(Icons.Default.Stop, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Завершить сессию", fontSize = 12.sp)
            }
        }
    }
}

// ── Баннер предупреждения ─────────────────────────────────────────────────────
@Composable
private fun WarningBanner(warning: ImportWarning) {
    val color = if (warning.isBlocker) FrDanger else FrWarning
    val icon: ImageVector = when (warning.type) {
        WarningType.DUPLICATE_SCOOTERS -> Icons.Default.ContentCopy
        WarningType.TOO_MANY_ROWS      -> Icons.Default.ErrorOutline
        WarningType.PARTIAL_UPLOAD     -> Icons.Default.CloudOff
        else                           -> Icons.Default.Warning
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Text(warning.message, color = FrTextMain, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
    }
}

// ── Отправка ──────────────────────────────────────────────────────────────────
@Composable
private fun SendingContent(stage: FieldImportStage, progress: Float, techName: String, onCancel: () -> Unit) {
    val animProg by animateFloatAsState(targetValue = progress, animationSpec = tween(400), label = "progress")

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = FrBorder, strokeWidth = 8.dp, trackColor = Color.Transparent)
            CircularProgressIndicator(progress = { animProg }, modifier = Modifier.fillMaxSize(), color = FrPrimary, strokeWidth = 8.dp, trackColor = Color.Transparent)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(progress * 100).toInt()}%", color = FrTextMain, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("готово", color = FrTextMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        if (techName.isNotEmpty()) {
            Text("Загружаем для", color = FrTextMuted, fontSize = 12.sp)
            Text(techName, color = FrPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        Text(stage.message, color = FrTextSec, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Не закрывайте приложение", color = FrTextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        val transition = rememberInfiniteTransition(label = "dots")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..2).forEach { i ->
                val alpha by transition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 200), RepeatMode.Reverse), label = "dot_$i")
                Box(modifier = Modifier.size(8.dp).background(FrPrimary.copy(alpha = alpha), CircleShape))
            }
        }
    }
}

// ── Готово ────────────────────────────────────────────────────────────────────
@Composable
private fun DoneContent(stage: FieldImportStage.Done, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.size(90.dp).background(Brush.radialGradient(listOf(FrSuccess.copy(alpha = 0.2f), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CheckCircle, null, tint = FrSuccess, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Задания отправлены!", color = FrTextMain, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("Каждый техник видит только свои самокаты", color = FrTextSec, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(FrPrimary.copy(alpha = 0.1f)).border(1.dp, FrPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.TwoWheeler, null, tint = FrPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(stage.count.toString(), color = FrPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("заданий", color = FrTextMuted, fontSize = 10.sp)
            }
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(FrSuccess.copy(alpha = 0.1f)).border(1.dp, FrSuccess.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Timer, null, tint = FrSuccess, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text("${stage.sec}с", color = FrSuccess, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("времени", color = FrTextMuted, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = FrSuccess), contentPadding = PaddingValues(vertical = 14.dp)) {
            Text("Отлично!", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }
    }
}

// ── Ошибка ────────────────────────────────────────────────────────────────────
@Composable
private fun ErrorContent(stage: FieldImportStage.Error, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Icon(Icons.Outlined.ErrorOutline, null, tint = FrDanger, modifier = Modifier.size(60.dp))
        Spacer(Modifier.height(12.dp))
        Text("Ошибка", color = FrTextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stage.reason, color = FrTextSec, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, FrBorder)) {
                Text("Закрыть", color = FrTextSec)
            }
            Button(onClick = onRetry, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = FrPrimary)) {
                Text("Попробовать снова", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}