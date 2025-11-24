// Полная версия файла MyTasksViewModel.kt

package com.example.qrscannerapp.features.tasks.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.AuthState
import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.data.repository.TaskRepository
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


private const val TAG = "MyTasksViewModel"

// === ИЗМЕНЕНИЕ №1: Добавляем счетчик активных задач ===
data class MyTasksUiState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val error: String? = null,
    val activeTaskCount: Int = 0
)

@HiltViewModel
class MyTasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyTasksUiState())
    val uiState: StateFlow<MyTasksUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized. Waiting for user authentication...")

        val userIdFlow = authManager.authState
            .map { authState: AuthState -> authState.userId }
            .distinctUntilChanged()
            .filterNotNull()

        userIdFlow.onEach { userId: String ->
            Log.d(TAG, "User detected with ID: $userId. Initializing data flows.")
            syncTasks(userId)
            observeLocalTasks(userId)
        }.launchIn(viewModelScope)
    }

    private fun syncTasks(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Task synchronization attempt started for user: $userId")
                taskRepository.syncTasks(userId)
            } catch (e: Exception) {
                Log.e(TAG, "FATAL Error during task synchronization", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка синхронизации данных: ${e.message}"
                    )
                }
            }
        }
    }

    private fun observeLocalTasks(userId: String) {
        taskRepository.getTasks(userId)
            .onEach { tasks ->
                // === ИЗМЕНЕНИЕ №2: Считаем активные задачи при каждом обновлении списка ===
                val activeCount = tasks.count { it.status == TaskStatus.NEW || it.status == TaskStatus.IN_PROGRESS }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tasks = tasks,
                        activeTaskCount = activeCount
                    )
                }
                Log.d(TAG, "Local tasks updated. Found ${tasks.size} tasks, $activeCount are active.")
            }
            .launchIn(viewModelScope)
    }

    fun updateTaskStatusLocally(taskId: String, newStatus: TaskStatus) {
        val updatedTasks = _uiState.value.tasks.map { task ->
            if (task.id == taskId) {
                task.copy(status = newStatus)
            } else {
                task
            }
        }
        // === ИЗМЕНЕНИЕ №3: Пересчитываем счетчик после локального обновления ===
        val activeCount = updatedTasks.count { it.status == TaskStatus.NEW || it.status == TaskStatus.IN_PROGRESS }
        _uiState.update {
            it.copy(
                tasks = updatedTasks,
                activeTaskCount = activeCount
            )
        }
        Log.d(TAG, "Task $taskId status locally updated to $newStatus. Active count: $activeCount")
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting to delete task with ID: $taskId")
                taskRepository.deleteTask(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting task $taskId", e)
                _uiState.update { it.copy(error = "Не удалось удалить задачу: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared.")
    }
}