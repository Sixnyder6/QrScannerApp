package com.example.qrscannerapp.features.security.ui.fleet

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.features.security.ui.SecColors
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================================
// FLEET IMPORT SHEET
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetImportSheet(
    onDismiss: () -> Unit,
    viewModel: FleetImportViewModel = hiltViewModel()
) {
    val state      by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.importFromUri(it) } }

    ModalBottomSheet(
        onDismissRequest = {
            if (state.stage is ImportStage.Idle ||
                state.stage is ImportStage.Done ||
                state.stage is ImportStage.Error) onDismiss()
        },
        sheetState     = sheetState,
        containerColor = SecColors.Card,
        dragHandle     = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(SecColors.CardBorder, CircleShape)
            )
        }
    ) {
        FleetImportContent(
            state      = state,
            onPickFile = { filePicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
            onReset    = { viewModel.resetToIdle() },
            onDismiss  = onDismiss
        )
    }
}

// ============================================================================================
// КОНТЕНТ
// ============================================================================================

@Composable
fun FleetImportContent(
    state: FleetImportUiState,
    onPickFile: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .background(SecColors.Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.UploadFile, null, tint = SecColors.Accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Импорт флита", color = SecColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Выгрузка из системы Яндекса (.xlsx)", color = SecColors.TextMuted, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Пояснение режимов
        ImportModeExplainer()

        Spacer(Modifier.height(12.dp))

        // Каталог статус
        if (state.catalogCount > 0) {
            CatalogStatusBadge(count = state.catalogCount)
            Spacer(Modifier.height(10.dp))
        }

        // Последний импорт
        state.lastImportedAt?.let { ts ->
            LastImportBadge(ts = ts, count = state.lastImportCount, mode = state.lastImportMode)
            Spacer(Modifier.height(16.dp))
        }

        // Стадия
        when (val stage = state.stage) {
            is ImportStage.Idle -> {
                IdleState(
                    canImport = state.canImport,
                    onPickFile = onPickFile,
                    lastTs = state.lastImportedAt,
                    lastMode = state.lastImportMode
                )
            }
            is ImportStage.Opening,
            is ImportStage.Reading,
            is ImportStage.Parsing,
            is ImportStage.Saving,
            is ImportStage.Catalog,
            is ImportStage.History,
            is ImportStage.Finishing -> {
                ImportingState(stage = stage, progress = state.progress)
            }
            is ImportStage.Done -> {
                DoneState(stage = stage, onDismiss = onDismiss)
            }
            is ImportStage.Error -> {
                ErrorState(
                    stage     = stage,
                    onRetry   = { onReset(); onPickFile() },
                    onDismiss = { onReset(); onDismiss() }
                )
            }
        }
    }
}

// ============================================================================================
// ПОЯСНЕНИЕ РЕЖИМОВ
// ============================================================================================

@Composable
private fun ImportModeExplainer() {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = SecColors.TagBg,
        border = BorderStroke(1.dp, SecColors.CardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "КАК РАБОТАЕТ ИМПОРТ",
                color = SecColors.TextMuted, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp
            )
            // Полный каталог
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SecColors.Accent.copy(alpha = 0.15f)
                ) {
                    Text(
                        "500+",
                        color = SecColors.Accent, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Полный каталог", color = SecColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Обновляет базу всех самокатов + оперативные данные. Раз в 6 часов.", color = SecColors.TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
            HorizontalDivider(color = SecColors.Divider)
            // Оперативный
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SecColors.Success.copy(alpha = 0.15f)
                ) {
                    Text(
                        "<500",
                        color = SecColors.Success, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Оперативный (СБ/Скаут)", color = SecColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Обновляет только самокаты из файла. Остальные не трогает. Раз в 15 минут.", color = SecColors.TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

// ============================================================================================
// СТАТУС КАТАЛОГА
// ============================================================================================

@Composable
private fun CatalogStatusBadge(count: Int) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(SecColors.Success.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
            .border(1.dp, SecColors.Success.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Storage, null, tint = SecColors.Success, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Каталог самокатов", color = SecColors.TextMuted, fontSize = 11.sp)
            Text("$count самокатов в базе", color = SecColors.Success, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Default.CheckCircle, null, tint = SecColors.Success, modifier = Modifier.size(16.dp))
    }
}

// ============================================================================================
// IDLE
// ============================================================================================

@Composable
private fun IdleState(
    canImport: Boolean,
    onPickFile: () -> Unit,
    lastTs: Long?,
    lastMode: ImportMode
) {
    val cooldownHours = if (lastTs != null) calcCooldown(lastTs, lastMode) else 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(
                2.dp,
                if (canImport) SecColors.Accent.copy(alpha = 0.4f) else SecColors.CardBorder,
                RoundedCornerShape(16.dp)
            )
            .background(
                if (canImport) SecColors.Accent.copy(alpha = 0.05f) else SecColors.TagBg,
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = canImport, onClick = onPickFile),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.FileOpen, null,
                tint     = if (canImport) SecColors.Accent else SecColors.TextMuted,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (canImport) "Нажмите чтобы выбрать файл" else "Подождите перед следующим импортом",
                color      = if (canImport) SecColors.TextSecondary else SecColors.TextMuted,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (!canImport && cooldownHours > 0) "Следующий через $cooldownHours мин"
                else "Файл: выгрузка 5.0 / .xlsx",
                color = SecColors.TextMuted, fontSize = 12.sp
            )
        }
    }

    if (canImport) {
        Spacer(Modifier.height(16.dp))
        Button(
            onClick        = onPickFile,
            modifier       = Modifier.fillMaxWidth(),
            shape          = RoundedCornerShape(14.dp),
            colors         = ButtonDefaults.buttonColors(containerColor = SecColors.Accent),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Выбрать файл флита", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ============================================================================================
// ИМПОРТ ИДЁТ
// ============================================================================================

@Composable
private fun ImportingState(stage: ImportStage, progress: Float) {
    val animProg by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(400, easing = EaseOutCubic),
        label         = "progress"
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = SecColors.CardBorder, strokeWidth = 10.dp, trackColor = Color.Transparent)
            CircularProgressIndicator(progress = { animProg }, modifier = Modifier.fillMaxSize(), color = SecColors.Accent, strokeWidth = 10.dp, trackColor = Color.Transparent)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(animProg * 100).toInt()}%", color = SecColors.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                Text("готово", color = SecColors.TextMuted, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(SecColors.CardBorder, RoundedCornerShape(3.dp))) {
            Box(modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight().background(Brush.horizontalGradient(listOf(SecColors.Accent.copy(alpha = 0.6f), SecColors.Accent)), RoundedCornerShape(3.dp)))
            val shimmerOffset by rememberInfiniteTransition(label = "shimmer").animateFloat(
                initialValue = -1f, targetValue = 2f,
                animationSpec = infiniteRepeatable(tween(1200, easing = LinearOutSlowInEasing), RepeatMode.Restart),
                label = "shimmer_x"
            )
            Box(modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight().background(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.4f), Color.Transparent), startX = shimmerOffset * 400, endX = shimmerOffset * 400 + 200),
                RoundedCornerShape(3.dp)
            ))
        }

        Spacer(Modifier.height(20.dp))

        AnimatedContent(
            targetState  = stage.message,
            transitionSpec = { (fadeIn(tween(200)) + slideInVertically { it / 2 }).togetherWith(fadeOut(tween(150)) + slideOutVertically { -it / 2 }) },
            label        = "stage_msg"
        ) { msg ->
            Text(msg, color = SecColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(8.dp))
        Text("Не закрывайте приложение", color = SecColors.TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        PulsingDots()
    }
}

// ============================================================================================
// ГОТОВО
// ============================================================================================

@Composable
private fun DoneState(stage: ImportStage.Done, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))

        Box(
            modifier         = Modifier
                .size(100.dp)
                .background(Brush.radialGradient(listOf(SecColors.Success.copy(alpha = 0.2f), Color.Transparent)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = SecColors.Success, modifier = Modifier.size(64.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("Импорт завершён!", color = SecColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

        // Режим импорта
        Spacer(Modifier.height(8.dp))
        Surface(
            shape  = RoundedCornerShape(8.dp),
            color  = if (stage.mode == ImportMode.FULL_CATALOG) SecColors.Accent.copy(alpha = 0.12f)
            else SecColors.Success.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, if (stage.mode == ImportMode.FULL_CATALOG) SecColors.Accent.copy(alpha = 0.3f)
            else SecColors.Success.copy(alpha = 0.3f))
        ) {
            Text(
                if (stage.mode == ImportMode.FULL_CATALOG) "Полный каталог обновлён" else "Оперативное обновление",
                color      = if (stage.mode == ImportMode.FULL_CATALOG) SecColors.Accent else SecColors.Success,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DoneStatCard(Icons.Default.TwoWheeler, stage.count.toString(), "самокатов", SecColors.Accent, Modifier.weight(1f))
            DoneStatCard(Icons.Default.Timer, "${stage.sec}с", "времени", SecColors.Success, Modifier.weight(1f))
            if (stage.historyCount > 0) {
                DoneStatCard(Icons.Default.History, stage.historyCount.toString(), "в историю", Color(0xFF9C7BFF), Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick        = onDismiss,
            modifier       = Modifier.fillMaxWidth(),
            shape          = RoundedCornerShape(14.dp),
            colors         = ButtonDefaults.buttonColors(containerColor = SecColors.Success),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text("Отлично!", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }
    }
}

@Composable
private fun DoneStatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, color: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f), border = BorderStroke(1.dp, color.copy(alpha = 0.25f)), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = SecColors.TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

// ============================================================================================
// ОШИБКА
// ============================================================================================

@Composable
private fun ErrorState(stage: ImportStage.Error, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Icon(Icons.Outlined.ErrorOutline, null, tint = SecColors.Danger, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("Ошибка импорта", color = SecColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stage.reason, color = SecColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, SecColors.CardBorder)) {
                Text("Закрыть", color = SecColors.TextSecondary)
            }
            Button(onClick = onRetry, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SecColors.Accent)) {
                Text("Повторить", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================================================
// УТИЛИТЫ
// ============================================================================================

@Composable
private fun LastImportBadge(ts: Long, count: Int, mode: ImportMode) {
    val sdf     = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru")) }
    val dateStr = remember(ts) { sdf.format(Date(ts)) }
    val modeColor = if (mode == ImportMode.FULL_CATALOG) SecColors.Accent else SecColors.Success
    val modeLabel = if (mode == ImportMode.FULL_CATALOG) "Полный" else "Оперативный"

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(SecColors.TagBg, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.History, null, tint = SecColors.TextMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Последний импорт · $dateStr", color = SecColors.TextMuted, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = modeColor.copy(alpha = 0.12f)) {
                    Text(modeLabel, color = modeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("$count самокатов", color = SecColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PulsingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..2).forEach { i ->
            val alpha by transition.animateFloat(
                initialValue  = 0.2f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(600, delayMillis = i * 200, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)),
                    RepeatMode.Reverse
                ),
                label = "dot_$i"
            )
            Box(modifier = Modifier.size(8.dp).background(SecColors.Accent.copy(alpha = alpha), CircleShape))
        }
    }
}

// Возвращает оставшееся время в минутах
private fun calcCooldown(lastTs: Long, mode: ImportMode): Int {
    val remain = cooldownMs(mode) - (System.currentTimeMillis() - lastTs)
    if (remain <= 0) return 0
    return (remain / 60_000L + 1).toInt()
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)