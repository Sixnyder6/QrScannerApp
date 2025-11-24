// Полная, синтезированная версия файла TaskDetailViewModel.kt

package com.example.qrscannerapp.features.tasks.ui.details.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.tasks.data.repository.TaskRepository
import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Класс состояния для экрана деталей задачи (UI State).
 * Является единственным источником истины для этого экрана.
 */
data class TaskDetailUiState(
    val isLoading: Boolean = true,
    val task: Task? = null,
    val error: String? = null,
    val isUpdatingStatus: Boolean = false, // Флаг для блокировки кнопок
    val shouldClose: Boolean = false
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private var currentTaskId: String? = null

    init {
        // Логика остается пустой, как и было
    }

    fun setTaskId(id: String) {
        if (id != currentTaskId) {
            currentTaskId = id
            _uiState.update { TaskDetailUiState(isLoading = true, error = null) }
            loadTaskDetails(id)
        }
    }

    private fun loadTaskDetails(id: String) {
        viewModelScope.launch {
            taskRepository.getTaskById(id)
                .catch { exception ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Ошибка при загрузке: ${exception.message}"
                    )}
                }
                .collect { task ->
                    _uiState.update {
                        if (task != null) {
                            it.copy(isLoading = false, task = task, error = null)
                        } else {
                            it.copy(isLoading = false, error = "Задача с ID ${currentTaskId} не найдена.", task = null)
                        }
                    }
                }
        }
    }

    private fun updateStatus(newStatus: TaskStatus) {
        val idToUpdate = currentTaskId ?: run {
            _uiState.update { it.copy(error = "Невозможно обновить статус: ID задачи отсутствует.") }
            return
        }

        _uiState.update { it.copy(isUpdatingStatus = true, error = null) }

        viewModelScope.launch {
            try {
                taskRepository.updateTaskStatus(idToUpdate, newStatus)

                // === ИЗМЕНЕНИЕ №1 ЗДЕСЬ ===
                _uiState.update { currentState ->
                    currentState.copy(
                        isUpdatingStatus = false,
                        task = currentState.task?.copy(status = newStatus), // Обновляем локальную копию задачи
                        shouldClose = true
                    )
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isUpdatingStatus = false,
                    error = "Не удалось обновить статус: ${e.message}"
                )}
            }
        }
    }

    // === ИЗМЕНЕНИЕ №2 ЗДЕСЬ ===
    /**
     * Сбрасывает флаг закрытия. Вызывается из UI после того, как окно было закрыто.
     */
    fun onDialogDismissed() {
        _uiState.update { it.copy(shouldClose = false) }
    }
    // ===========================

    fun acceptTask() {
        updateStatus(TaskStatus.IN_PROGRESS)
    }

    fun declineTask() {
        updateStatus(TaskStatus.CANCELED)
    }

    fun completeTask() {
        updateStatus(TaskStatus.COMPLETED)
    }
}