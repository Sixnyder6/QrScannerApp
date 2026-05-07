package com.example.qrscannerapp.features.tasks.ui.creation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                is TaskCreationEvent.TaskSavedSuccessfully -> navController.popBackStack()
                is TaskCreationEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    AppBackground {
        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF2C2C2E),
                        contentColor = StardustTextPrimary,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
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
}

@Composable
private fun TaskCreationContent(
    modifier: Modifier = Modifier,
    uiState: TaskCreationUiState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
    onEmployeeSelected: (EmployeeInfo) -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Название ──────────────────────────────────────────────────────────────
        CreationSection(label = "Название задачи") {
            CreationTextField(
                value = uiState.title,
                onValueChange = onTitleChanged,
                placeholder = "Например: Провести инвентаризацию",
                leadingIcon = Icons.Default.Title,
                isError = uiState.titleError != null,
                errorText = uiState.titleError
            )
        }

        // ── Описание ──────────────────────────────────────────────────────────────
        CreationSection(label = "Описание") {
            CreationTextField(
                value = uiState.description,
                onValueChange = onDescriptionChanged,
                placeholder = "Дополнительные детали (необязательно)",
                leadingIcon = Icons.Default.Edit,
                singleLine = false,
                minLines = 3
            )
        }

        // ── Приоритет ─────────────────────────────────────────────────────────────
        CreationSection(label = "Приоритет") {
            PrioritySegmentControl(
                selectedPriority = uiState.priority,
                onPrioritySelected = onPrioritySelected
            )
        }

        // ── Исполнитель ───────────────────────────────────────────────────────────
        CreationSection(label = "Исполнитель") {
            EmployeeDropdown(
                employees = uiState.availableEmployees,
                selectedEmployee = uiState.selectedEmployee,
                onEmployeeSelected = onEmployeeSelected,
                isLoading = uiState.isEmployeeListLoading,
                error = uiState.employeeError
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Кнопка сохранения ────────────────────────────────────────────────────
        Button(
            onClick = onSaveClick,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StardustPrimary,
                disabledContainerColor = StardustPrimary.copy(alpha = 0.4f)
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            AnimatedContent(targetState = uiState.isSaving, label = "save_btn") { saving ->
                if (saving) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Text("Сохранение...", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Создать задачу", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// ── Section wrapper ───────────────────────────────────────────────────────────────

@Composable
private fun CreationSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label.uppercase(),
            color = StardustTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        content()
    }
}

// ── Text field ────────────────────────────────────────────────────────────────────

@Composable
private fun CreationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    errorText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = StardustTextSecondary.copy(alpha = 0.6f), fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isError) StardustError else StardustTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        supportingText = errorText?.let { { Text(it, color = StardustError, fontSize = 12.sp) } },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = StardustItemBg,
            unfocusedContainerColor = StardustItemBg,
            focusedTextColor = StardustTextPrimary,
            unfocusedTextColor = StardustTextPrimary,
            cursorColor = StardustPrimary,
            focusedBorderColor = StardustPrimary,
            unfocusedBorderColor = Color.Transparent,
            errorBorderColor = StardustError,
            errorContainerColor = StardustError.copy(alpha = 0.06f),
            focusedLeadingIconColor = StardustPrimary,
            unfocusedLeadingIconColor = StardustTextSecondary
        )
    )
}

// ── Priority segmented control ────────────────────────────────────────────────────

@Composable
private fun PrioritySegmentControl(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StardustItemBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val priorities = listOf(
            TaskPriority.HIGH   to ("Высокий" to StardustError),
            TaskPriority.MEDIUM to ("Средний"  to StardustPrimary),
            TaskPriority.LOW    to ("Низкий"   to StardustTextSecondary)
        )
        priorities.forEach { (priority, meta) ->
            val (label, color) = meta
            val isSelected = selectedPriority == priority
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) color.copy(alpha = 0.18f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) color.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(9.dp)
                    )
                    .clickable { onPrioritySelected(priority) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) color else StardustTextSecondary.copy(alpha = 0.4f))
                    )
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = if (isSelected) color else StardustTextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Employee dropdown ─────────────────────────────────────────────────────────────

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
            placeholder = { Text("Выберите сотрудника", color = StardustTextSecondary.copy(alpha = 0.6f), fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.AssignmentInd, null, tint = if (error != null) StardustError else StardustTextSecondary, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = StardustPrimary)
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            isError = error != null,
            supportingText = error?.let { { Text(it, color = StardustError, fontSize = 12.sp) } },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StardustItemBg,
                unfocusedContainerColor = StardustItemBg,
                focusedTextColor = StardustTextPrimary,
                unfocusedTextColor = StardustTextPrimary,
                cursorColor = StardustPrimary,
                focusedBorderColor = StardustPrimary,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = StardustError,
                errorContainerColor = StardustError.copy(alpha = 0.06f),
                focusedLeadingIconColor = StardustPrimary,
                unfocusedLeadingIconColor = StardustTextSecondary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF2A2A2E))
        ) {
            employees.forEach { employee ->
                DropdownMenuItem(
                    text = { Text(employee.name, color = StardustTextPrimary, fontSize = 14.sp) },
                    onClick = { onEmployeeSelected(employee); expanded = false },
                    modifier = Modifier.background(Color.Transparent)
                )
            }
        }
    }
}
