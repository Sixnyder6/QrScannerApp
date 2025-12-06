// Полная, обновленная версия файла: DashboardScreen.kt
// Реализовано: Добавление, Удаление и РЕДАКТИРОВАНИЕ (карандаш) + НОВАЯ СИСТЕМА РОЛЕЙ

package com.example.qrscannerapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// ИМПОРТ НАШЕГО ENUM
import com.example.qrscannerapp.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToEmployeeProfile: (userId: String) -> Unit,
    onNavigateToAdminRepairLog: () -> Unit,
    onNavigateToTaskCreation: () -> Unit,
    onNavigateToVehicleReport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Состояния для BottomSheets
    val employeeDetailsSheetState = rememberModalBottomSheetState()
    val employeeListSheetState = rememberModalBottomSheetState()
    var showEmployeeListSheet by remember { mutableStateOf(false) }

    val selectedTaskForDetails = remember { mutableStateOf<Task?>(null) }
    val taskDetailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Состояния для диалогов
    var showClearArchiveDialog by remember { mutableStateOf(false) }

    // Состояние диалога Добавления/Редактирования
    var showAddEditDialog by remember { mutableStateOf(false) }
    // Храним сотрудника, которого редактируем (если null - значит создаем нового)
    var employeeToEdit by remember { mutableStateOf<EmployeeInfo?>(null) }

    // Состояния для удаления сотрудника
    var showDeleteEmployeeConfirmDialog by remember { mutableStateOf(false) }
    var employeeToDeleteId by remember { mutableStateOf<String?>(null) }

    // --- ЛОГИКА ПОКАЗА ДИАЛОГОВ ---

    // Диалог Добавления / Редактирования
    if (showAddEditDialog) {
        AddEditEmployeeDialog(
            initialName = employeeToEdit?.name ?: "",
            // Логин при редактировании не показываем старый (безопасность), либо оставляем пустым
            initialUsername = "",
            // Если редактируем - конвертируем строку из БД в Enum, иначе берем дефолтную роль (например Мувер)
            initialRole = if (employeeToEdit != null) UserRole.fromKey(employeeToEdit!!.role) else UserRole.MOVER,
            isEditMode = employeeToEdit != null,
            onDismiss = {
                showAddEditDialog = false
                employeeToEdit = null
            },
            onConfirm = { name, username, pass, role ->
                // role здесь приходит как UserRole, нам нужно достать ключ (.key)
                if (employeeToEdit != null) {
                    // Режим редактирования
                    viewModel.updateEmployee(employeeToEdit!!.id, name, username, pass, role.key)
                } else {
                    // Режим создания
                    viewModel.createEmployee(name, username, pass, role.key)
                }
                showAddEditDialog = false
                employeeToEdit = null
            }
        )
    }

    // Диалог подтверждения удаления
    if (showDeleteEmployeeConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteEmployeeConfirmDialog = false
                employeeToDeleteId = null
            },
            title = { Text("Удалить сотрудника?") },
            text = { Text("Сотрудник будет удален из базы навсегда. Доступ в приложение будет закрыт.") },
            confirmButton = {
                Button(
                    onClick = {
                        employeeToDeleteId?.let { id ->
                            viewModel.deleteEmployee(id)
                        }
                        showDeleteEmployeeConfirmDialog = false
                        employeeToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StardustError)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteEmployeeConfirmDialog = false
                    employeeToDeleteId = null
                }) {
                    Text("Отмена")
                }
            },
            containerColor = StardustModalBg,
            titleContentColor = StardustTextPrimary,
            textContentColor = StardustTextPrimary
        )
    }

    if (uiState.selectedEmployeeDetails != null) {
        EmployeeDetailsSheet(
            details = uiState.selectedEmployeeDetails!!,
            isLoading = uiState.isDetailsLoading,
            sheetState = employeeDetailsSheetState,
            onDismiss = {
                scope.launch {
                    employeeDetailsSheetState.hide()
                    viewModel.clearEmployeeDetails()
                }
            }
        )
    }

    if (showEmployeeListSheet) {
        EmployeeListSheet(
            employees = uiState.allEmployees,
            sheetState = employeeListSheetState,
            onDismiss = { showEmployeeListSheet = false },
            onEmployeeClick = { userId ->
                scope.launch { employeeListSheetState.hide() }.invokeOnCompletion {
                    showEmployeeListSheet = false
                    onNavigateToEmployeeProfile(userId)
                }
            },
            onAddClick = {
                scope.launch { employeeListSheetState.hide() }.invokeOnCompletion {
                    showEmployeeListSheet = false
                    employeeToEdit = null // Очищаем = создание нового
                    showAddEditDialog = true
                }
            },
            onEditClick = { employee ->
                scope.launch { employeeListSheetState.hide() }.invokeOnCompletion {
                    showEmployeeListSheet = false
                    employeeToEdit = employee // Запоминаем = редактирование
                    showAddEditDialog = true
                }
            },
            onDeleteClick = { userId ->
                employeeToDeleteId = userId
                showDeleteEmployeeConfirmDialog = true
            }
        )
    }

    if (selectedTaskForDetails.value != null) {
        DashboardTaskDetailsSheet(
            task = selectedTaskForDetails.value!!,
            sheetState = taskDetailsSheetState,
            onDismiss = {
                scope.launch { taskDetailsSheetState.hide() }.invokeOnCompletion {
                    selectedTaskForDetails.value = null
                }
            }
        )
    }

    if (showClearArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showClearArchiveDialog = false },
            title = { Text("Очистить архив?") },
            text = { Text("Будут удалены все завершенные и отмененные задачи. Это действие необратимо.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearArchivedTasks()
                        showClearArchiveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StardustError)
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearArchiveDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    AppBackground {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToTaskCreation,
                    containerColor = StardustPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать задачу")
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = StardustPrimary)
                    }
                    uiState.error != null -> {
                        Text(uiState.error!!, color = StardustError, modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp), textAlign = TextAlign.Center)
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            item {
                                SectionTitle(title = "Сводка за сегодня")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatCard("Сканы", uiState.scansToday.toString(), Icons.Default.QrCodeScanner, Modifier.weight(1f))
                                    StatCard("Партии", uiState.logEntriesToday.toString(), Icons.Default.BarChart, Modifier.weight(1f))
                                    Box(modifier = Modifier
                                        .weight(1f)
                                        .clickable(onClick = onNavigateToAdminRepairLog)) {
                                        StatCard("Ремонты", uiState.repairsToday.toString(), Icons.Default.Build)
                                    }
                                    Box(modifier = Modifier
                                        .weight(1f)
                                        .clickable { showEmployeeListSheet = true }) {
                                        StatCard(
                                            title = "Сотрудники",
                                            value = uiState.allEmployees.size.toString(),
                                            icon = Icons.Default.Groups
                                        )
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SectionTitle(title = "Терминал Задач")
                                    IconButton(onClick = { showClearArchiveDialog = true }) {
                                        Icon(
                                            Icons.Default.DeleteSweep,
                                            contentDescription = "Очистить архив",
                                            tint = StardustTextSecondary
                                        )
                                    }
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
                                ) {
                                    if (uiState.activeTasks.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Нет активных задач.",
                                                color = StardustTextSecondary,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(uiState.activeTasks, key = { it.id }) { task ->
                                                ActiveTaskItem(
                                                    task = task,
                                                    onClick = { selectedTaskForDetails.value = task }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                SectionTitle(title = "Отчеты")
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onNavigateToVehicleReport),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Summarize,
                                            contentDescription = "Отчеты",
                                            tint = StardustPrimary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Сводка по самокатам",
                                                color = StardustTextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "Загрузка и анализ Excel-отчета",
                                                color = StardustTextSecondary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Перейти",
                                            tint = StardustTextSecondary
                                        )
                                    }
                                }
                            }

                            item { SectionTitle(title = "Активность сотрудников (сегодня)") }

                            if (uiState.employeeActivities.isEmpty()) {
                                item {
                                    Text(
                                        text = "Сегодня еще не было активности.",
                                        color = StardustTextSecondary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                items(uiState.employeeActivities) { activity ->
                                    EmployeeActivityListItem(
                                        activity = activity,
                                        onClick = { onNavigateToEmployeeProfile(activity.id) }
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

// ... Остальные Composable (ActiveTaskItem, DashboardTaskDetailsSheet и т.д.) без изменений ...
// Для краткости я их пропустил, но в твоем файле они должны остаться!
// Если ты копируешь весь файл, убедись, что вспомогательные функции ниже тоже скопированы.
// Я сейчас добавлю их для полноты картины, чтобы ты мог просто сделать Ctrl+A -> Ctrl+V.

@Composable
fun ActiveTaskItem(task: Task, onClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = task.createdAt?.let { sdf.format(it) } ?: "--:--"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(StardustGlassBg.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "[$timeString]",
            color = StardustTextSecondary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = task.title,
            color = StardustTextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTaskDetailsSheet(
    task: Task,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = StardustTextSecondary)
            }

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .padding(top = 24.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = task.title,
                            color = StardustTextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (task.description.isNotBlank()) {
                            Text(
                                text = task.description,
                                color = StardustTextPrimary.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        HorizontalDivider(color = StardustItemBg)

                        DetailRow(label = "Статус") {
                            TaskStatusIndicator(task = task)
                        }

                        DetailRow(label = "Создал", value = task.creatorName)
                        DetailRow(label = "Назначен", value = task.assigneeName)
                        DetailRow(label = "Создана", value = task.createdAt?.let { sdf.format(it) } ?: "-")
                        DetailRow(label = "Обновлена", value = task.updatedAt?.let { sdf.format(it) } ?: "-")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            color = StardustTextSecondary,
            modifier = Modifier.weight(0.4f),
            fontWeight = FontWeight.SemiBold
        )
        Row(modifier = Modifier.weight(0.6f)) {
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
    val inProgressColor = Color(0xFFFFC107) // Yellow

    when (task.status) {
        TaskStatus.NEW -> {
            val infiniteTransition = rememberInfiniteTransition("new_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                label = "scale"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FiberNew,
                    contentDescription = "Новая",
                    tint = StardustPrimary,
                    modifier = Modifier.scale(scale)
                )
                Spacer(Modifier.width(8.dp))
                Text("Новая", color = StardustPrimary, fontWeight = FontWeight.Bold)
            }
        }
        TaskStatus.IN_PROGRESS -> {
            val taskStartedAt = task.startedAt
            if (taskStartedAt == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp)) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = inProgressColor
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Синхронизация...", color = StardustTextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                var durationText by remember { mutableStateOf("00:00:00") }

                val startTime by remember(taskStartedAt) {
                    mutableStateOf(taskStartedAt.time)
                }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "В работе",
                            tint = inProgressColor,
                            modifier = Modifier.rotate(angle)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("В работе:", color = inProgressColor, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        durationText,
                        color = StardustTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = inProgressColor,
                        trackColor = StardustItemBg,
                        strokeCap = StrokeCap.Round
                    )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Завершена",
                    tint = StardustSuccess,
                    modifier = Modifier.scale(scale)
                )
                Spacer(Modifier.width(8.dp))
                Text("Завершена", color = StardustSuccess, fontWeight = FontWeight.Bold)
            }
        }
        TaskStatus.CANCELED -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cancel, contentDescription = "Отменена", tint = StardustError)
                Spacer(Modifier.width(8.dp))
                Text("Отменена", color = StardustError, fontWeight = FontWeight.Bold)
            }
        }
        else -> Text(task.status.name, color = StardustTextSecondary)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            color = StardustTextSecondary,
            modifier = Modifier.weight(0.4f),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = StardustTextPrimary,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun StatusIndicator(isOnline: Boolean) {
    val greenColor = Color(0xFF4CAF50)
    val grayColor = Color.Gray
    if (isOnline) {
        val infiniteTransition = rememberInfiniteTransition(label = "online_indicator_pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(greenColor.copy(alpha = alpha))
        )
    } else {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(grayColor)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListSheet(
    employees: List<EmployeeInfo>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onEmployeeClick: (userId: String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (EmployeeInfo) -> Unit, // Добавлен параметр редактирования
    onDeleteClick: (userId: String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Все сотрудники (${employees.size})",
                    color = StardustTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Добавить сотрудника",
                        tint = StardustPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(employees, key = { it.id }) { employee ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Кликабельная часть (инфо)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onEmployeeClick(employee.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusIndicator(isOnline = employee.status == "online")
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Default.Person, contentDescription = null, tint = StardustTextSecondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(employee.name, color = StardustTextPrimary, fontSize = 16.sp)
                                if (employee.role.isNotBlank()) {
                                    // ИСПОЛЬЗУЕМ ENUM ДЛЯ КРАСИВОГО ОТОБРАЖЕНИЯ РОЛИ
                                    val roleDisplayName = UserRole.fromKey(employee.role).displayName
                                    Text(roleDisplayName, color = StardustTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }

                        // Кнопка Редактирования (Карандаш)
                        IconButton(onClick = { onEditClick(employee) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редактировать",
                                tint = StardustPrimary.copy(alpha = 0.7f)
                            )
                        }

                        // Кнопка Удаления (Корзина)
                        IconButton(onClick = { onDeleteClick(employee.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = StardustError.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailsSheet(
    details: EmployeeDetails,
    isLoading: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Активность: ${details.name}",
                color = StardustTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (isLoading) {
                CircularProgressIndicator(color = StardustPrimary, modifier = Modifier.padding(vertical = 40.dp))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(details.entries) { entry ->
                        DetailLogEntryItem(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailLogEntryItem(entry: ActivityLogEntry) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.History, contentDescription = "Time", tint = StardustTextSecondary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "В ${sdf.format(Date(entry.timestamp))}",
                color = StardustTextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${entry.itemCount} сканов",
                color = StardustTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, color = StardustTextSecondary, modifier = modifier.padding(bottom = 12.dp))
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = StardustPrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(text = title, color = StardustTextSecondary, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
fun EmployeeActivityListItem(activity: EmployeeActivity, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.PersonOutline, contentDescription = "User Icon", modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(StardustItemBg)
                .padding(8.dp), tint = StardustTextSecondary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIndicator(isOnline = activity.status == "online")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = activity.name, color = StardustTextPrimary, fontWeight = FontWeight.SemiBold)
                }
                Text(text = "Партий скопировано: ${activity.logCount}", color = StardustTextSecondary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = activity.totalScans.toString(), color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = " сканов", color = StardustTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEmployeeDialog(
    initialName: String = "",
    initialUsername: String = "",
    initialRole: UserRole = UserRole.MOVER, // Теперь принимаем Enum
    isEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, username: String, password: String, role: UserRole) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }

    // Список ролей берем из ENUM
    val roles = UserRole.getSelectableRoles()
    var selectedRole by remember { mutableStateOf(initialRole) }
    var isRoleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Редактировать сотрудника" else "Новый сотрудник",
                fontWeight = FontWeight.Bold,
                color = StardustTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя и Фамилия") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        focusedLabelColor = StardustPrimary,
                        unfocusedLabelColor = StardustTextSecondary,
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustItemBg
                    )
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = {
                        Text(if (isEditMode) "Новый Логин (оставьте пустым, чтобы не менять)" else "Логин (username)")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        focusedLabelColor = StardustPrimary,
                        unfocusedLabelColor = StardustTextSecondary,
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustItemBg
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(if (isEditMode) "Новый Пароль (оставьте пустым, чтобы не менять)" else "Пароль")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        focusedLabelColor = StardustPrimary,
                        unfocusedLabelColor = StardustTextSecondary,
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustItemBg
                    )
                )

                // Выбор роли
                ExposedDropdownMenuBox(
                    expanded = isRoleExpanded,
                    onExpandedChange = { isRoleExpanded = !isRoleExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRole.displayName, // Показываем красивое имя
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Роль") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StardustTextPrimary,
                            unfocusedTextColor = StardustTextPrimary,
                            focusedLabelColor = StardustPrimary,
                            unfocusedLabelColor = StardustTextSecondary,
                            focusedBorderColor = StardustPrimary,
                            unfocusedBorderColor = StardustItemBg
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isRoleExpanded,
                        onDismissRequest = { isRoleExpanded = false },
                        modifier = Modifier.background(StardustGlassBg)
                    ) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(text = role.displayName, color = StardustTextPrimary) },
                                onClick = {
                                    selectedRole = role
                                    isRoleExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Логика валидации
                    if (isEditMode) {
                        // При редактировании логин и пароль могут быть пустыми
                        if (name.isNotBlank()) {
                            onConfirm(name, username, password, selectedRole)
                        }
                    } else {
                        // При создании все поля обязательны
                        if (name.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                            onConfirm(name, username, password, selectedRole)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
            ) {
                Text(if (isEditMode) "Сохранить" else "Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = StardustTextSecondary)
            }
        },
        containerColor = StardustModalBg,
        textContentColor = StardustTextPrimary,
        titleContentColor = StardustTextPrimary
    )
}