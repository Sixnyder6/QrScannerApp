// Полная версия файла DashboardViewModel.kt

package com.example.qrscannerapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.tasks.data.remote.TaskRemoteDataSource
import com.example.qrscannerapp.features.tasks.data.repository.TaskRepository
import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

data class EmployeeActivity(
    val id: String,
    val name: String,
    val totalScans: Int,
    val logCount: Int,
    val status: String = "offline"
)

data class ActivityLogEntry(
    val timestamp: Long,
    val itemCount: Int
)

data class EmployeeDetails(
    val name: String,
    val entries: List<ActivityLogEntry>
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val scansToday: Int = 0,
    val logEntriesToday: Int = 0,
    val repairsToday: Int = 0,
    val activeElectricians: List<EmployeeActivity> = emptyList(),
    val employeeActivities: List<EmployeeActivity> = emptyList(),
    val allEmployees: List<EmployeeInfo> = emptyList(),
    val selectedEmployeeDetails: EmployeeDetails? = null,
    val isDetailsLoading: Boolean = false,
    val activeTasks: List<Task> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskRemoteDataSource: TaskRemoteDataSource
) : ViewModel() {
    private val db = Firebase.firestore
    private val TAG = "DashboardViewModel"
    private var employeesListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        subscribeToEmployeeUpdates()
        subscribeToActiveTasks()
    }

    // === Создание сотрудника ===
    fun createEmployee(name: String, username: String, pass: String, role: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Creating new employee: $username")
                val newUser = hashMapOf(
                    "username" to username,
                    "password" to pass,
                    "role" to role,
                    "displayName" to name,
                    "isAdmin" to (role == "admin"),
                    "status" to "offline",
                    "registrationTimestamp" to System.currentTimeMillis(),
                    "appVersion" to "1.0.0",
                    "lastBatteryLevel" to 100,
                    "deviceInfo" to "Created by Admin",
                    "batteryHealth" to "Unknown",
                    "isCharging" to false,
                    "isPowerSaveMode" to false,
                    "networkState" to "None",
                    "freeRam" to "N/A",
                    "freeStorage" to "N/A",
                    "deviceUptime" to "N/A"
                )

                db.collection("internal_users")
                    .add(newUser)
                    .await()

                Log.d(TAG, "Employee created successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error creating employee", e)
                _uiState.update { it.copy(error = "Не удалось создать сотрудника: ${e.message}") }
            }
        }
    }

    // === НОВАЯ ФУНКЦИЯ: Обновление сотрудника ===
    fun updateEmployee(id: String, name: String, username: String, password: String, role: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating employee: $id")
                val updates = hashMapOf<String, Any>(
                    "displayName" to name,
                    "role" to role,
                    "isAdmin" to (role == "admin")
                )
                // Обновляем логин/пароль только если они введены (не пустые)
                if (username.isNotBlank()) updates["username"] = username
                if (password.isNotBlank()) updates["password"] = password

                db.collection("internal_users").document(id).update(updates).await()
                Log.d(TAG, "Employee updated successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error updating employee", e)
                _uiState.update { it.copy(error = "Не удалось обновить данные: ${e.message}") }
            }
        }
    }

    // === Удаление сотрудника ===
    fun deleteEmployee(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting employee: $userId")
                db.collection("internal_users").document(userId).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting employee", e)
                _uiState.update { it.copy(error = "Не удалось удалить сотрудника") }
            }
        }
    }

    // === Очистка архива ===
    fun clearArchivedTasks() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting to clear archived tasks...")

                val allTasksSnapshot = db.collection("tasks").get().await()
                val allTasks = allTasksSnapshot.documents.mapNotNull { doc ->
                    try {
                        val task = doc.toObject(Task::class.java)
                        task?.copy(
                            id = doc.id,
                            status = doc.getString("status")?.let { TaskStatus.valueOf(it) } ?: TaskStatus.UNKNOWN
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                val tasksToDelete = allTasks.filter {
                    it.status == TaskStatus.COMPLETED || it.status == TaskStatus.CANCELED
                }

                if (tasksToDelete.isEmpty()) return@launch

                tasksToDelete.forEach { task ->
                    taskRemoteDataSource.deleteTask(task.id)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error clearing archived tasks", e)
                _uiState.update { it.copy(error = "Ошибка при очистке архива задач.") }
            }
        }
    }

    private fun subscribeToActiveTasks() {
        taskRepository.getAllActiveTasksStream()
            .onEach { tasks ->
                val sortedTasks = tasks.sortedByDescending { it.createdAt }
                _uiState.update { it.copy(activeTasks = sortedTasks) }
            }
            .catch { e ->
                Log.e(TAG, "Error collecting active tasks", e)
                _uiState.update { it.copy(error = "Ошибка загрузки задач") }
            }
            .launchIn(viewModelScope)
    }

    private fun subscribeToEmployeeUpdates() {
        _uiState.update { it.copy(isLoading = true) }
        employeesListener = db.collection("internal_users")
            .orderBy("displayName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to employee updates", error)
                    _uiState.update { it.copy(error = "Ошибка загрузки сотрудников", isLoading = false) }
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val employeesList = snapshot.documents.mapNotNull { doc ->
                    // ВНИМАНИЕ: Здесь я добавил чтение role, чтобы оно попадало в UI
                    EmployeeInfo(
                        id = doc.id,
                        name = doc.getString("displayName") ?: "Без имени",
                        role = doc.getString("role") ?: "", // <--- Добавлено чтение роли
                        status = doc.getString("status") ?: "offline"
                    )
                }
                _uiState.update { it.copy(allEmployees = employeesList) }
                loadActivityData()
            }
    }

    private fun loadActivityData() {
        viewModelScope.launch {
            try {
                val activeEmployeesDeferred = async { fetchActiveEmployeesToday() }
                val repairActivityDeferred = async { fetchRepairActivityToday() }

                val activeEmployees = activeEmployeesDeferred.await()
                val (repairsCount, activeElectriciansList) = repairActivityDeferred.await()

                val totalScansToday = activeEmployees.sumOf { it.totalScans }
                val totalLogsToday = activeEmployees.sumOf { it.logCount }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scansToday = totalScansToday,
                        logEntriesToday = totalLogsToday,
                        repairsToday = repairsCount,
                        activeElectricians = activeElectriciansList,
                        employeeActivities = activeEmployees
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading activity data", e)
                _uiState.update { it.copy(error = "Ошибка загрузки активности", isLoading = false) }
            }
        }
    }

    private suspend fun fetchRepairActivityToday(): Pair<Int, List<EmployeeActivity>> {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val snapshot = db.collection("battery_repair_log")
            .whereGreaterThanOrEqualTo("timestamp", startOfToday)
            .get()
            .await()

        val totalRepairs = snapshot.documents.sumOf { (it.get("repairs") as? List<*>)?.size ?: 0 }

        val allEmployees = _uiState.value.allEmployees
        val groupedByElectrician = snapshot.documents.groupBy { it.getString("electricianId") }

        val activeElectricians = groupedByElectrician.mapNotNull { (userId, userLogs) ->
            if (userId == null) return@mapNotNull null

            val repairsCount = userLogs.sumOf { (it.get("repairs") as? List<*>)?.size ?: 0 }
            val name = userLogs.firstNotNullOfOrNull { it.getString("electricianName") } ?: "Неизвестный"
            val status = allEmployees.find { it.id == userId }?.status ?: "offline"

            EmployeeActivity(id = userId, name = name, totalScans = repairsCount, logCount = userLogs.size, status = status)
        }.sortedByDescending { it.totalScans }

        return Pair(totalRepairs, activeElectricians)
    }

    private suspend fun fetchActiveEmployeesToday(): List<EmployeeActivity> {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val snapshot = db.collection("activity_log")
            .whereGreaterThanOrEqualTo("timestamp", startOfToday)
            .get()
            .await()

        val allEmployees = _uiState.value.allEmployees
        val groupedByEmployee = snapshot.documents.groupBy { it.getString("creatorId") }

        return groupedByEmployee.mapNotNull { (userId, userLogs) ->
            if (userId == null) return@mapNotNull null

            val totalScans = userLogs.sumOf { it.getLong("itemCount")?.toInt() ?: 0 }
            val logCount = userLogs.size
            val name = userLogs.firstNotNullOfOrNull { it.getString("creatorName") } ?: "Неизвестный"
            val status = allEmployees.find { it.id == userId }?.status ?: "offline"

            EmployeeActivity(
                id = userId,
                name = name,
                totalScans = totalScans,
                logCount = logCount,
                status = status
            )
        }.sortedByDescending { it.totalScans }
    }

    fun loadDetailsForEmployee(employeeId: String, employeeName: String) {
        _uiState.update { it.copy(isDetailsLoading = true) }
        viewModelScope.launch {
            try {
                val startOfToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val snapshot = db.collection("activity_log")
                    .whereEqualTo("creatorId", employeeId)
                    .whereGreaterThanOrEqualTo("timestamp", startOfToday)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val entries = snapshot.documents.mapNotNull { doc ->
                    val timestamp = doc.getLong("timestamp") ?: return@mapNotNull null
                    val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                    ActivityLogEntry(timestamp = timestamp, itemCount = itemCount)
                }

                _uiState.update {
                    it.copy(
                        isDetailsLoading = false,
                        selectedEmployeeDetails = EmployeeDetails(name = employeeName, entries = entries)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading employee details", e)
                _uiState.update { it.copy(isDetailsLoading = false, error = "Ошибка загрузки деталей") }
            }
        }
    }

    fun clearEmployeeDetails() {
        _uiState.update { it.copy(selectedEmployeeDetails = null) }
    }

    override fun onCleared() {
        super.onCleared()
        employeesListener?.remove()
    }
}