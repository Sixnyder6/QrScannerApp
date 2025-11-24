package com.example.qrscannerapp.features.tasks.ui.creation.viewmodel // <-- ИСПРАВЛЕННЫЙ ПАКЕТ

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.EmployeeInfo
import com.example.qrscannerapp.features.tasks.domain.model.Task // <-- ИСПРАВЛЕНО
import com.example.qrscannerapp.features.tasks.domain.model.TaskPriority // <-- ИСПРАВЛЕНО
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus // <-- ИСПРАВЛЕНО
import com.example.qrscannerapp.features.tasks.domain.model.TaskStep // <-- ИСПРАВЛЕНО
import com.example.qrscannerapp.features.tasks.data.repository.TaskRepository
import com.example.qrscannerapp.data.repository.UserRepository // <-- ОСТАВЛЕН В СТАРОМ ПАКЕТЕ (не перемещали)
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class TaskCreationUiState(
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val steps: List<TaskStep> = emptyList(),
    val availableEmployees: List<EmployeeInfo> = emptyList(),
    val selectedEmployee: EmployeeInfo? = null,
    val isEmployeeListLoading: Boolean = true,
    val isSaving: Boolean = false,
    val titleError: String? = null,
    val employeeError: String? = null
)

sealed class TaskCreationEvent {
    object TaskSavedSuccessfully : TaskCreationEvent()
    data class Error(val message: String) : TaskCreationEvent()
}

@HiltViewModel
class TaskCreationViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskCreationUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<TaskCreationEvent>()
    val events = _eventChannel.receiveAsFlow()

    init {
        loadEmployees()
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            _uiState.update { it.copy(isEmployeeListLoading = true) }
            try {
                // --- ИЗМЕНЕНО: Удалена фильтрация по роли ---
                val employees = userRepository.getAllUsers()
                _uiState.update { it.copy(availableEmployees = employees, isEmployeeListLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEmployeeListLoading = false) }
                _eventChannel.send(TaskCreationEvent.Error("Не удалось загрузить список сотрудников"))
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title, titleError = null) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onPrioritySelected(priority: TaskPriority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun onEmployeeSelected(employee: EmployeeInfo) {
        _uiState.update { it.copy(selectedEmployee = employee, employeeError = null) }
    }

    fun saveTask() {
        val currentState = _uiState.value
        if (!validateInput(currentState)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val creatorId = authManager.authState.value.userId
            val creatorName = authManager.authState.value.userName
            if (creatorId == null || creatorName == null) {
                _eventChannel.send(TaskCreationEvent.Error("Не удалось определить создателя задачи."))
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }

            val newTask = Task(
                title = currentState.title.trim(),
                description = currentState.description.trim(),
                priority = currentState.priority.value,
                status = TaskStatus.NEW,
                steps = currentState.steps,
                assigneeId = currentState.selectedEmployee!!.id,
                assigneeName = currentState.selectedEmployee.name,
                creatorId = creatorId,
                creatorName = creatorName,
                createdAt = Date(),
                updatedAt = Date()
            )

            try {
                taskRepository.createTask(newTask)
                _eventChannel.send(TaskCreationEvent.TaskSavedSuccessfully)
            } catch (e: Exception) {
                _eventChannel.send(TaskCreationEvent.Error("Ошибка сохранения: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun validateInput(state: TaskCreationUiState): Boolean {
        val isTitleValid = state.title.isNotBlank()
        val isEmployeeSelected = state.selectedEmployee != null

        _uiState.update {
            it.copy(
                titleError = if (!isTitleValid) "Название не может быть пустым" else null,
                employeeError = if (!isEmployeeSelected) "Необходимо выбрать исполнителя" else null
            )
        }

        return isTitleValid && isEmployeeSelected
    }
}