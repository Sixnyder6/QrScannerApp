// File: features/tasks/ui/creation/TaskCreationScreen.kt

package com.example.qrscannerapp.features.tasks.ui.creation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.qrscannerapp.EmployeeInfo
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.tasks.domain.model.TaskPriority
import com.example.qrscannerapp.features.tasks.ui.creation.viewmodel.TaskCreationEvent
import com.example.qrscannerapp.features.tasks.ui.creation.viewmodel.TaskCreationUiState
import com.example.qrscannerapp.features.tasks.ui.creation.viewmodel.TaskCreationViewModel
import kotlinx.coroutines.flow.collectLatest

// =================================================================================
// STARDUST DESIGN CONSTANTS
// =================================================================================
val StardustGlassBg = Color(0xBF1A1A1D)
val StardustItemBg = Color(0x14FFFFFF)
val StardustPrimary = Color(0xFF6A5AE0)
val StardustTextPrimary = Color.White
val StardustTextSecondary = Color(0xFFA0A0A5)
val StardustError = Color(0xFFF44336)
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreationScreen(
    navController: NavController,
    viewModel: TaskCreationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is TaskCreationEvent.TaskSavedSuccessfully -> {
                    navController.popBackStack()
                }
                is TaskCreationEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // V-- НАЧАЛО ИЗМЕНЕНИЙ: ИНТЕГРАЦИЯ APPBACKGROUND --V
    // AppBackground теперь корневой элемент, занимающий весь экран.
    AppBackground {
        // Scaffold находится внутри фона и сделан прозрачным.
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Создать Задачу", color = StardustTextPrimary, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = StardustTextPrimary)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            // Отступы от Scaffold передаются в контент, а не в фон.
            TaskCreationContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onTitleChanged = viewModel::onTitleChanged,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onPrioritySelected = viewModel::onPrioritySelected,
                onEmployeeSelected = viewModel::onEmployeeSelected,
                onSaveClick = viewModel::saveTask
            )
        }
    }
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
}

@Composable
private fun TaskCreationContent(
    modifier: Modifier = Modifier, // <-- Добавлен параметр modifier
    uiState: TaskCreationUiState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
    onEmployeeSelected: (EmployeeInfo) -> Unit,
    onSaveClick: () -> Unit
) {
    LazyColumn(
        // Применяем переданный modifier, который содержит отступы от Scaffold.
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            StardustTextField(
                value = uiState.title,
                onValueChange = onTitleChanged,
                label = "Название задачи",
                isError = uiState.titleError != null,
                supportingText = uiState.titleError
            )
        }

        item {
            StardustTextField(
                value = uiState.description,
                onValueChange = onDescriptionChanged,
                label = "Описание (необязательно)",
                modifier = Modifier.height(120.dp),
                singleLine = false
            )
        }

        item {
            PrioritySelector(
                selectedPriority = uiState.priority,
                onPrioritySelected = onPrioritySelected
            )
        }

        item {
            EmployeeDropdown(
                employees = uiState.availableEmployees,
                selectedEmployee = uiState.selectedEmployee,
                onEmployeeSelected = onEmployeeSelected,
                isLoading = uiState.isEmployeeListLoading,
                error = uiState.employeeError
            )
        }

        item {
            Text("Шаги (в разработке)", style = MaterialTheme.typography.titleMedium, color = StardustTextSecondary)
        }

        item {
            Button(
                onClick = onSaveClick,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
            ) {
                AnimatedContent(targetState = uiState.isSaving, label = "SaveButtonAnimation") { isSaving ->
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Сохранить задачу", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =================================================================================
// СТИЛИЗОВАННЫЙ TEXT FIELD (Stable)
// =================================================================================

@Composable
fun StardustTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        isError = isError,
        supportingText = {
            if (supportingText != null) {
                Text(supportingText, color = if (isError) StardustError else StardustTextSecondary)
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = StardustItemBg, unfocusedContainerColor = StardustItemBg,
            focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary,
            cursorColor = StardustPrimary,
            focusedBorderColor = StardustPrimary, unfocusedBorderColor = StardustItemBg, errorBorderColor = StardustError,
            focusedLabelColor = StardustPrimary, unfocusedLabelColor = StardustTextSecondary, errorLabelColor = StardustError,
        )
    )
}

// =================================================================================
// PRIORITY SELECTOR (Stable - Использует кнопки вместо FilterChip)
// =================================================================================

@Composable
private fun PrioritySelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit
) {
    Column {
        Text("Приоритет", style = MaterialTheme.typography.labelLarge, color = StardustTextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskPriority.entries.forEach { priority ->
                val priorityColor = when (priority) {
                    TaskPriority.HIGH -> StardustError
                    TaskPriority.MEDIUM -> StardustPrimary
                    TaskPriority.LOW -> Color.Gray
                }
                val isSelected = priority == selectedPriority

                Button(
                    onClick = { onPrioritySelected(priority) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) priorityColor.copy(alpha = 0.8f) else StardustItemBg,
                        contentColor = if (isSelected) StardustTextPrimary else StardustTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = when(priority) {
                            TaskPriority.HIGH -> "Высокий"
                            TaskPriority.MEDIUM -> "Средний"
                            TaskPriority.LOW -> "Низкий"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// =================================================================================
// EMPLOYEE DROPDOWN (Stable - Фикс белого фона)
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeDropdown(
    employees: List<EmployeeInfo>,
    selectedEmployee: EmployeeInfo?,
    onEmployeeSelected: (EmployeeInfo) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedEmployee?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Назначить исполнителю") },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = StardustPrimary)
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            isError = error != null,
            supportingText = {
                if (error != null) {
                    Text(error, color = StardustError)
                } else if (selectedEmployee == null && !isLoading && employees.isNotEmpty()) {
                    Text("Необходимо выбрать исполнителя", color = StardustTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StardustItemBg, unfocusedContainerColor = StardustItemBg,
                focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary,
                cursorColor = StardustPrimary,
                focusedBorderColor = StardustPrimary, unfocusedBorderColor = StardustItemBg, errorBorderColor = StardustError,
                focusedLabelColor = StardustPrimary, unfocusedLabelColor = StardustTextSecondary, errorLabelColor = StardustError,
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF2a2a2e))
                .width(IntrinsicSize.Max)
        ) {
            employees.forEach { employee ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEmployeeSelected(employee); expanded = false }
                        .background(Color.Transparent)
                ) {
                    Text(
                        text = employee.name,
                        color = StardustTextPrimary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}