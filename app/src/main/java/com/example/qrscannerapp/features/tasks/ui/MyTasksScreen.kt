package com.example.qrscannerapp.features.tasks.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.domain.model.TaskPriority
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import com.example.qrscannerapp.features.tasks.ui.details.viewmodel.TaskDetailViewModel
import com.example.qrscannerapp.features.tasks.ui.viewmodel.MyTasksUiState
import com.example.qrscannerapp.features.tasks.ui.viewmodel.MyTasksViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// =================================================================================
// STARDUST DESIGN CONSTANTS
// =================================================================================
val StardustGlassBg = Color(0xBF1A1A1D)
val StardustItemBg = Color(0x14FFFFFF)
val StardustPrimary = Color(0xFF6A5AE0)
val StardustTextPrimary = Color.White
val StardustTextSecondary = Color(0xFFA0A0A5)
val StardustError = Color(0xFFF44336)
val StardustSuccess = Color(0xFF4CAF50)
val StardustModalBg = Color(0xFF2C2C2E)
// =================================================================================


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen(
    onMenuClick: () -> Unit,
    onTaskClick: (taskId: String) -> Unit,
    viewModel: MyTasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTaskId = remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val taskToDeleteId = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.error) {
        val msg = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = StardustModalBg,
                        contentColor = StardustTextPrimary,
                        actionColor = StardustError,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        ) { paddingValues ->
            MyTasksContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onTaskClick = { taskId -> selectedTaskId.value = taskId },
                onDeleteClick = { taskId -> taskToDeleteId.value = taskId }
            )
        }
    }

    if (selectedTaskId.value != null) {
        TaskDetailModal(
            taskId = selectedTaskId.value!!,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    selectedTaskId.value = null
                }
            },
            onTaskUpdate = { updatedTaskId, newStatus ->
                viewModel.updateTaskStatusLocally(updatedTaskId, newStatus)
            }
        )
    }

    if (taskToDeleteId.value != null) {
        val task = uiState.tasks.find { it.id == taskToDeleteId.value }
        if (task != null) {
            AlertDialog(
                onDismissRequest = { taskToDeleteId.value = null },
                title = { Text("Удалить задачу?", color = StardustTextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("«${task.title}» будет удалена без возможности восстановления.", color = StardustTextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTask(task.id)
                            taskToDeleteId.value = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StardustError)
                    ) { Text("Удалить", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { taskToDeleteId.value = null }) {
                        Text("Отмена", color = StardustTextSecondary)
                    }
                },
                containerColor = StardustModalBg,
                titleContentColor = StardustTextPrimary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MyTasksContent(
    uiState: MyTasksUiState,
    modifier: Modifier = Modifier,
    onTaskClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StardustPrimary)
            }
        }
        uiState.tasks.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Нет назначенных задач",
                    style = MaterialTheme.typography.bodyLarge,
                    color = StardustTextSecondary
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.tasks, key = { it.id }) { task ->
                    Box(modifier = Modifier.animateItemPlacement()) {
                        TaskListItem(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onDeleteClick = { onDeleteClick(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskListItem(
    task: Task,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val priority = remember(task.priority) { TaskPriority.fromInt(task.priority) }
    val priorityColor = when (priority) {
        TaskPriority.HIGH -> StardustError
        TaskPriority.MEDIUM -> StardustPrimary
        TaskPriority.LOW -> StardustTextSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(priorityColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = StardustTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (task.status == TaskStatus.COMPLETED || task.status == TaskStatus.CANCELED) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить задачу",
                                tint = StardustError.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        color = StardustTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusChip(status = task.status)
            }
        }
    }
}

@Composable
fun StatusChip(status: TaskStatus) {
    val (text, color, icon) = when (status) {
        TaskStatus.NEW -> Triple("Новая", StardustPrimary, Icons.Default.FiberNew)
        TaskStatus.IN_PROGRESS -> Triple("В работе", Color(0xFF00C2FF), Icons.Default.Sync)
        TaskStatus.COMPLETED -> Triple("Выполнена", StardustSuccess, Icons.Default.CheckCircle)
        TaskStatus.CANCELED -> Triple("Отменена", StardustTextSecondary.copy(alpha = 0.7f), Icons.Default.Cancel)
        TaskStatus.UNKNOWN -> Triple("Неизвестно", StardustTextSecondary, Icons.AutoMirrored.Filled.HelpOutline)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailModal(
    taskId: String,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onTaskUpdate: (taskId: String, newStatus: TaskStatus) -> Unit
) {
    val viewModel = hiltViewModel<TaskDetailViewModel>(key = taskId)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(key1 = taskId) {
        viewModel.setTaskId(taskId)
    }

    LaunchedEffect(key1 = uiState.shouldClose) {
        if (uiState.shouldClose) {
            uiState.task?.let { updatedTask ->
                onTaskUpdate(updatedTask.id, updatedTask.status)
            }
            onDismiss()
            viewModel.onDialogDismissed()
        }
    }

    val isBusy = uiState.isUpdatingStatus

    ModalBottomSheet(
        onDismissRequest = { if (!isBusy) onDismiss() },
        sheetState = sheetState,
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        val taskData = uiState.task

        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(40.dp),
                        color = StardustPrimary
                    )
                }
                uiState.error != null -> {
                    Text(
                        uiState.error!!,
                        color = StardustError,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(20.dp)
                    )
                }
                taskData != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Заголовок: чипы статуса/приоритета + название + описание
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusChip(status = taskData.status)
                                TaskPriorityBadge(priorityValue = taskData.priority)
                            }
                            Text(
                                text = taskData.title,
                                color = StardustTextPrimary,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (taskData.description.isNotBlank()) {
                                Text(
                                    text = taskData.description,
                                    color = StardustTextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // Секция: статус с анимацией
                        SectionBlock(label = "СТАТУС") {
                            TaskStatusIndicator(task = taskData)
                        }

                        // Секция: детали задачи
                        SectionBlock(label = "ДЕТАЛИ") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                DetailRow("Создал", taskData.creatorName)
                                HorizontalDivider(color = Color(0xFF3A3A3E))
                                DetailRow("Создана", formatTaskDate(taskData.createdAt))
                            }
                        }

                        TaskActionButtons(
                            currentStatus = taskData.status,
                            isBusy = isBusy,
                            onAccept = {
                                if (taskData.status == TaskStatus.NEW) {
                                    viewModel.acceptTask()
                                } else if (taskData.status == TaskStatus.IN_PROGRESS) {
                                    viewModel.completeTask()
                                }
                            },
                            onDecline = { viewModel.declineTask() }
                        )
                    }
                }
                else -> {
                    Box(modifier = Modifier.height(200.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionBlock(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = StardustTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StardustItemBg)
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun TaskPriorityBadge(priorityValue: Int) {
    val priority = TaskPriority.fromInt(priorityValue)
    val (text, color) = when (priority) {
        TaskPriority.HIGH -> "Высокий" to StardustError
        TaskPriority.MEDIUM -> "Средний" to StardustPrimary
        TaskPriority.LOW -> "Низкий" to StardustTextSecondary
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            color = StardustTextSecondary,
            modifier = Modifier.fillMaxWidth(0.4f),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = StardustTextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            color = StardustTextSecondary,
            modifier = Modifier.fillMaxWidth(0.4f),
            fontWeight = FontWeight.SemiBold
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

private fun formatDuration(millis: Long): String {
    val hours = (millis / (1000 * 60 * 60))
    val minutes = (millis / (1000 * 60)) % 60
    val seconds = (millis / 1000) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
fun TaskStatusIndicator(task: Task) {
    val inProgressColor = Color(0xFFFFC107)

    when (task.status) {
        TaskStatus.NEW -> {
            val infiniteTransition = rememberInfiniteTransition("new_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                label = "scale"
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.FiberNew,
                    contentDescription = "Новая",
                    tint = StardustPrimary,
                    modifier = Modifier.scale(scale)
                )
                Text("Новая", color = StardustPrimary, fontWeight = FontWeight.Bold)
            }
        }
        TaskStatus.IN_PROGRESS -> {
            val taskStartedAt = task.startedAt
            if (taskStartedAt == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(24.dp)) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = inProgressColor)
                    }
                    Text("Синхронизация...", color = StardustTextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                var durationText by remember { mutableStateOf("00:00:00") }
                val startTime by remember(taskStartedAt) { mutableStateOf(taskStartedAt.time) }
                LaunchedEffect(startTime) {
                    while (true) {
                        val elapsedTime = System.currentTimeMillis() - startTime
                        durationText = formatDuration(elapsedTime)
                        delay(1000)
                    }
                }
                val infiniteTransition = rememberInfiniteTransition("in_progress_rotate")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
                    label = "angle"
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "В работе", tint = inProgressColor, modifier = Modifier.rotate(angle))
                        Text("В работе:", color = inProgressColor, fontWeight = FontWeight.Bold)
                    }
                    Text(durationText, color = StardustTextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = inProgressColor, trackColor = StardustItemBg, strokeCap = StrokeCap.Round)
                }
            }
        }
        TaskStatus.COMPLETED -> {
            var scale by remember { mutableStateOf(0.5f) }
            LaunchedEffect(Unit) {
                animate(0.5f, 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { value, _ ->
                    scale = value
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Завершена", tint = StardustSuccess, modifier = Modifier.scale(scale))
                Text("Завершена", color = StardustSuccess, fontWeight = FontWeight.Bold)
            }
        }
        TaskStatus.CANCELED -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Cancel, contentDescription = "Отменена", tint = StardustError)
                Text("Отменена", color = StardustError, fontWeight = FontWeight.Bold)
            }
        }
        else -> Text(task.status.name, color = StardustTextSecondary)
    }
}

@Composable
private fun TaskActionButtons(
    currentStatus: TaskStatus,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (currentStatus == TaskStatus.NEW) {
            Button(
                onClick = onAccept,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustSuccess),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Принять в работу", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onDecline,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StardustError),
                border = BorderStroke(1.dp, StardustError),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Отклонить", fontWeight = FontWeight.Bold)
            }
        } else if (currentStatus == TaskStatus.IN_PROGRESS) {
            Button(
                onClick = onAccept,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Завершить задачу", fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
private fun formatTaskDate(date: Date?): String {
    if (date == null) return "Неизвестно"
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy в HH:mm", Locale.getDefault()) }
    return sdf.format(date)
}


// =================================================================================
// PREVIEWS
// =================================================================================

@Preview(showBackground = true)
@Composable
private fun TaskListItemPreview() {
    val sampleTask = Task(
        id = "1",
        title = "Провести инвентаризацию Зоны-А",
        description = "Необходимо просканировать все АКБ на стеллажах B-4, B-5 и сделать фото.",
        status = TaskStatus.IN_PROGRESS,
        priority = TaskPriority.HIGH.value
    )
    MaterialTheme {
        AppBackground {
            Box(modifier = Modifier.padding(16.dp)) {
                TaskListItem(task = sampleTask, onClick = {}, onDeleteClick = {})
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyTasksContentPreview() {
    val tasks = listOf(
        Task(id = "1", title = "Задача 1 (Низкий)", description = "Описание 1", status = TaskStatus.NEW, priority = TaskPriority.LOW.value),
        Task(id = "2", title = "Задача 2 (Средний)", description = "Описание 2", status = TaskStatus.IN_PROGRESS, priority = TaskPriority.MEDIUM.value),
        Task(id = "3", title = "Задача 3 (Высокий)", description = "Описание 3", status = TaskStatus.COMPLETED, priority = TaskPriority.HIGH.value)
    )
    val uiState = MyTasksUiState(isLoading = false, tasks = tasks)
    MaterialTheme {
        AppBackground {
            MyTasksContent(uiState = uiState, onTaskClick = {}, onDeleteClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyTasksContentEmptyPreview() {
    val uiState = MyTasksUiState(isLoading = false, tasks = emptyList())
    MaterialTheme {
        AppBackground {
            MyTasksContent(uiState = uiState, onTaskClick = {}, onDeleteClick = {})
        }
    }
}