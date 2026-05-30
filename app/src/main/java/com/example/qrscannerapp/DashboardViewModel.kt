package com.example.qrscannerapp

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.tasks.data.remote.TaskRemoteDataSource
import com.example.qrscannerapp.features.tasks.data.repository.TaskRepository
import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

data class HourlyActivityPoint(
    val hour: Int,
    val scans: Int
)

// ── Прогресс техника для дашборда ──
data class TechnicianStat(
    val id: String,
    val name: String,
    val total: Int,
    val done: Int
)

enum class ActivityChartMode { TODAY, YESTERDAY, WEEK }

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val scansToday: Int = 0,
    val logEntriesToday: Int = 0,
    val repairsToday: Int = 0,
    val activeElectricians: List<EmployeeActivity> = emptyList(),
    val employeeActivities: List<EmployeeActivity> = emptyList(),
    val allEmployees: List<EmployeeInfo> = emptyList(),
    val pendingShiftRequests: List<EmployeeInfo> = emptyList(),
    val selectedEmployeeDetails: EmployeeDetails? = null,
    val isDetailsLoading: Boolean = false,
    val activeTasks: List<Task> = emptyList(),
    val employeesOnShift: Int = 0,
    val scansYesterday: Int = 0,
    val hourlyActivity: List<HourlyActivityPoint> = emptyList(),
    val yesterdayActivity: List<HourlyActivityPoint> = emptyList(),
    val weekActivity: List<HourlyActivityPoint> = emptyList(),
    val chartMode: ActivityChartMode = ActivityChartMode.TODAY,
    val employeesOnShiftDetails: List<EmployeeActivity> = emptyList(),
    // ── Полевой ремонт ──
    val fieldTotalToday: Int = 0,
    val fieldDoneToday: Int = 0,
    val fieldInProgressToday: Int = 0,
    val fieldToStorageToday: Int = 0,
    val fieldNotFoundToday: Int = 0,
    val fieldTechnicianProgress: List<TechnicianStat> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskRemoteDataSource: TaskRemoteDataSource,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val db = Firebase.firestore
    private val tag = "DashboardViewModel"
    private var employeesListener: ListenerRegistration? = null
    private var fieldRepairListener: ListenerRegistration? = null
    private var tasksListener: ListenerRegistration? = null
    // ── Snapshot listeners для activity_log и ремонтов (вместо get()) ──
    private var activityTodayListener: ListenerRegistration? = null
    private var repairsTodayListener: ListenerRegistration? = null
    private var migrationTriggered = false
    private var cleanupTriggered = false

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    private val _notificationEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val notificationEvent = _notificationEvent.asSharedFlow()

    private var previousEmployeeStatuses = mapOf<String, String>()

    // ── Кеш для activity данных (чтобы не делать get() каждый раз) ──
    private var cachedActiveEmployees: List<EmployeeActivity> = emptyList()
    private var cachedRepairsCount = 0
    private var cachedRepairElectricians: List<EmployeeActivity> = emptyList()
    private var cachedHourlyActivity: List<HourlyActivityPoint> = emptyList()
    private var cachedYesterdayScans = 0
    private var cachedYesterdayActivity: List<HourlyActivityPoint> = emptyList()
    private var cachedWeekActivity: List<HourlyActivityPoint> = emptyList()

    private var activityLoadJob: Job? = null
    private val activityDebounceMs = 15_000L
    private var lastActivityLoadTime = 0L

    init {
        subscribeToEmployeeUpdates()
        subscribeToActiveTasks()
        subscribeToFieldRepair()
        subscribeToActivityLog()   // ✅ новый listener вместо get()
        subscribeToRepairLog()     // ✅ новый listener вместо get()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // НОВЫЙ LISTENER: activity_log — realtime вместо get()
    // ═══════════════════════════════════════════════════════════════════════════
    private fun subscribeToActivityLog() {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        activityTodayListener = db.collection("activity_log")
            .whereGreaterThanOrEqualTo("timestamp", startOfToday)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Activity log listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val allEmployees = _uiState.value.allEmployees

                // ── Активные сотрудники сегодня ──
                val groupedByEmployee = snapshot.documents.groupBy { it.getString("creatorId") }
                cachedActiveEmployees = groupedByEmployee.mapNotNull { (userId, userLogs) ->
                    if (userId == null) return@mapNotNull null
                    val totalScans = userLogs.sumOf { it.getLong("itemCount")?.toInt() ?: 0 }
                    val logCount = userLogs.size
                    val name = userLogs.firstNotNullOfOrNull { it.getString("creatorName") } ?: "Неизвестный"
                    val employeeInfo = allEmployees.find { it.id == userId }
                    val statusStr = if (employeeInfo?.isOnline == true) "online" else "offline"
                    EmployeeActivity(id = userId, name = name, totalScans = totalScans, logCount = logCount, status = statusStr)
                }.sortedByDescending { it.totalScans }

                // ── Почасовая активность сегодня ──
                val hourlyCounts = IntArray(24)
                snapshot.documents.forEach { doc ->
                    val timestamp = doc.getLong("timestamp") ?: return@forEach
                    val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
                    hourlyCounts[c.get(Calendar.HOUR_OF_DAY)] += itemCount
                }
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                cachedHourlyActivity = (6..maxOf(currentHour, 6)).map { hour ->
                    HourlyActivityPoint(hour = hour, scans = hourlyCounts[hour])
                }

                refreshDashboardFromCache()
            }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // НОВЫЙ LISTENER: battery_repair_log — realtime вместо get()
    // ═══════════════════════════════════════════════════════════════════════════
    private fun subscribeToRepairLog() {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        repairsTodayListener = db.collection("battery_repair_log")
            .whereGreaterThanOrEqualTo("timestamp", startOfToday)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Repair log listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val allEmployees = _uiState.value.allEmployees
                var totalRepairs = 0
                val validDocs = snapshot.documents.mapNotNull { doc ->
                    try {
                        val repairs = (doc.get("repairs") as? List<*>)?.size ?: 0
                        totalRepairs += repairs
                        doc
                    } catch (e: Exception) { Log.w(tag, "Skipping malformed repair doc ${doc.id}: ${e.message}"); null }
                }
                val groupedByElectrician = validDocs.groupBy { it.getString("electricianId") }
                cachedRepairElectricians = groupedByElectrician.mapNotNull { (userId, userLogs) ->
                    if (userId == null) return@mapNotNull null
                    val repairsCount = userLogs.sumOf { try { (it.get("repairs") as? List<*>)?.size ?: 0 } catch (_: Exception) { 0 } }
                    val name = userLogs.firstNotNullOfOrNull { it.getString("electricianName") } ?: "Неизвестный"
                    val employeeInfo = allEmployees.find { it.id == userId }
                    val statusStr = if (employeeInfo?.isOnline == true) "online" else "offline"
                    EmployeeActivity(id = userId, name = name, totalScans = repairsCount, logCount = userLogs.size, status = statusStr)
                }.sortedByDescending { it.totalScans }
                cachedRepairsCount = totalRepairs

                refreshDashboardFromCache()
            }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Обновляет UI из кеша (без get() запросов к Firestore)
    // ═══════════════════════════════════════════════════════════════════════════
    private fun refreshDashboardFromCache() {
        val allEmployees = _uiState.value.allEmployees
        val onShiftDetails = allEmployees
            .filter { it.isOnline }
            .map { employee ->
                val activity = cachedActiveEmployees.find { it.id == employee.id }
                EmployeeActivity(
                    id = employee.id,
                    name = employee.displayName,
                    totalScans = activity?.totalScans ?: 0,
                    logCount = activity?.logCount ?: 0,
                    status = "online"
                )
            }
            .sortedByDescending { it.totalScans }

        _uiState.update {
            it.copy(
                isLoading = false,
                scansToday = cachedActiveEmployees.sumOf { e -> e.totalScans },
                logEntriesToday = cachedActiveEmployees.sumOf { e -> e.logCount },
                repairsToday = cachedRepairsCount,
                activeElectricians = cachedRepairElectricians,
                employeeActivities = cachedActiveEmployees,
                scansYesterday = cachedYesterdayScans,
                hourlyActivity = cachedHourlyActivity,
                employeesOnShiftDetails = onShiftDetails
            )
        }
    }

    // ── Подписка на полевой ремонт ────────────────────────────────────────────
    private fun subscribeToFieldRepair() {
        // Слушаем активные сессии
        fieldRepairListener = db.collection("field_repair_sessions")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Field repair sessions error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) {
                    _uiState.update { it.copy(
                        fieldTotalToday = 0, fieldDoneToday = 0,
                        fieldInProgressToday = 0, fieldToStorageToday = 0,
                        fieldNotFoundToday = 0, fieldTechnicianProgress = emptyList()
                    )}
                    return@addSnapshotListener
                }

                val activeSessionIds = snapshot.documents.map { it.id }
                loadFieldTasksForSessions(activeSessionIds)
            }
    }

    // ✅ ЗАМЕНЕНО НА REALTIME-СЛУШАТЕЛЬ
    private fun loadFieldTasksForSessions(sessionIds: List<String>) {
        tasksListener?.remove()
        if (sessionIds.isEmpty()) {
            _uiState.update { it.copy(
                fieldTotalToday = 0, fieldDoneToday = 0,
                fieldInProgressToday = 0, fieldToStorageToday = 0,
                fieldNotFoundToday = 0, fieldTechnicianProgress = emptyList()
            )}
            return
        }
        tasksListener = db.collection("field_repair_tasks")
            .whereIn("sessionId", sessionIds.take(10))
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e(tag, "Field tasks listener error", error); return@addSnapshotListener }
                if (snapshot == null) return@addSnapshotListener

                val tasks = snapshot.documents.mapNotNull { doc ->
                    try {
                        mapOf(
                            "assignedToId"   to (doc.getString("assignedToId") ?: ""),
                            "assignedToName" to (doc.getString("assignedToName") ?: "Неизвестный"),
                            "status"         to (doc.getString("status") ?: "new")
                        )
                    } catch (e: Exception) { null }
                }

                val total      = tasks.size
                val done       = tasks.count { it["status"] == "done" }
                val inProgress = tasks.count { it["status"] == "in_progress" }
                val toStorage  = tasks.count { it["status"] == "to_storage" }
                val notFound   = tasks.count { it["status"] == "not_found" }

                val byTech = tasks.groupBy { it["assignedToId"] as String }
                val techStats = byTech.mapNotNull { (techId, techTasks) ->
                    if (techId.isBlank()) return@mapNotNull null
                    TechnicianStat(
                        id    = techId,
                        name  = techTasks.firstOrNull()?.get("assignedToName") as? String ?: "Неизвестный",
                        total = techTasks.size,
                        done  = techTasks.count { it["status"] == "done" }
                    )
                }.sortedByDescending { it.done }

                _uiState.update { it.copy(
                    fieldTotalToday         = total,
                    fieldDoneToday          = done,
                    fieldInProgressToday    = inProgress,
                    fieldToStorageToday     = toStorage,
                    fieldNotFoundToday      = notFound,
                    fieldTechnicianProgress = techStats
                )}
            }
    }

    // ── Остальной код без изменений ───────────────────────────────────────────

    fun setChartMode(mode: ActivityChartMode) {
        _uiState.update { it.copy(chartMode = mode) }
        when (mode) {
            ActivityChartMode.YESTERDAY -> if (_uiState.value.yesterdayActivity.isEmpty()) loadYesterdayActivity()
            ActivityChartMode.WEEK -> if (_uiState.value.weekActivity.isEmpty()) loadWeekActivity()
            else -> {}
        }
    }

    private fun loadYesterdayActivity() {
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val startOfToday = cal.timeInMillis
                val startOfYesterday = startOfToday - 24 * 60 * 60 * 1000L
                val snapshot = db.collection("activity_log")
                    .whereGreaterThanOrEqualTo("timestamp", startOfYesterday)
                    .whereLessThan("timestamp", startOfToday)
                    .get().await()
                val hourlyCounts = IntArray(24)
                snapshot.documents.forEach { doc ->
                    val timestamp = doc.getLong("timestamp") ?: return@forEach
                    val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
                    hourlyCounts[c.get(Calendar.HOUR_OF_DAY)] += itemCount
                }
                val points = (6..23).map { hour -> HourlyActivityPoint(hour = hour, scans = hourlyCounts[hour]) }
                _uiState.update { it.copy(yesterdayActivity = points) }
            } catch (e: Exception) { Log.e(tag, "Error loading yesterday activity", e) }
        }
    }

    private fun loadWeekActivity() {
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val startOfToday = cal.timeInMillis
                val startOfWeek = startOfToday - 6 * 24 * 60 * 60 * 1000L
                val snapshot = db.collection("activity_log")
                    .whereGreaterThanOrEqualTo("timestamp", startOfWeek)
                    .get().await()
                val dailyCounts = IntArray(7)
                snapshot.documents.forEach { doc ->
                    val timestamp = doc.getLong("timestamp") ?: return@forEach
                    val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                    val dayIndex = ((timestamp - startOfWeek) / (24 * 60 * 60 * 1000L)).toInt().coerceIn(0, 6)
                    dailyCounts[dayIndex] += itemCount
                }
                val points = (0..6).map { i -> HourlyActivityPoint(hour = i, scans = dailyCounts[i]) }
                _uiState.update { it.copy(weekActivity = points) }
            } catch (e: Exception) { Log.e(tag, "Error loading week activity", e) }
        }
    }

    fun approveShiftRequest(userId: String) {
        viewModelScope.launch {
            try {
                db.collection("internal_users").document(userId).update(
                    mapOf("isAllowedToWork" to true, "shiftRequestStatus" to "APPROVED")
                ).await()
            } catch (e: Exception) {
                Log.e(tag, "Error approving shift request", e)
                _uiState.update { it.copy(error = "Не удалось одобрить запрос: ${e.message}") }
            }
        }
    }

    fun denyShiftRequest(userId: String) {
        viewModelScope.launch {
            try {
                db.collection("internal_users").document(userId).update(
                    mapOf("isAllowedToWork" to false, "shiftRequestStatus" to "DENIED")
                ).await()
            } catch (e: Exception) {
                Log.e(tag, "Error denying shift request", e)
                _uiState.update { it.copy(error = "Не удалось отклонить запрос: ${e.message}") }
            }
        }
    }

    fun createEmployee(name: String, username: String, pass: String, role: String, warehouseId: String) {
        viewModelScope.launch {
            try {
                val newUser = hashMapOf(
                    "username" to username, "password" to pass, "role" to role,
                    "warehouseId" to warehouseId, "displayName" to name,
                    "isAdmin" to (role == "admin"), "status" to "offline",
                    "lastSeen" to 0L, "isAllowedToWork" to false,
                    "shiftRequestStatus" to "NONE",
                    "registrationTimestamp" to System.currentTimeMillis(),
                    "appVersion" to "1.0.0", "lastAppUpdate" to System.currentTimeMillis(),
                    "deviceInfo" to "Created by Admin"
                )
                db.collection("internal_users").add(newUser).await()
            } catch (e: Exception) {
                Log.e(tag, "Error creating employee", e)
                _uiState.update { it.copy(error = "Не удалось создать сотрудника: ${e.message}") }
            }
        }
    }

    fun updateEmployee(id: String, name: String, username: String, password: String, role: String, warehouseId: String) {
        viewModelScope.launch {
            try {
                val updates = hashMapOf<String, Any>(
                    "displayName" to name, "role" to role,
                    "isAdmin" to (role == "admin"), "warehouseId" to warehouseId
                )
                if (username.isNotBlank()) updates["username"] = username
                if (password.isNotBlank()) updates["password"] = password
                db.collection("internal_users").document(id).update(updates).await()
            } catch (e: Exception) {
                Log.e(tag, "Error updating employee", e)
                _uiState.update { it.copy(error = "Не удалось обновить данные: ${e.message}") }
            }
        }
    }

    fun deleteEmployee(userId: String) {
        viewModelScope.launch {
            try {
                db.collection("internal_users").document(userId).delete().await()
            } catch (e: Exception) {
                Log.e(tag, "Error deleting employee", e)
                _uiState.update { it.copy(error = "Не удалось удалить сотрудника") }
            }
        }
    }

    fun clearArchivedTasks() {
        viewModelScope.launch {
            try {
                val allTasksSnapshot = db.collection("tasks").get().await()
                val tasksToDelete = allTasksSnapshot.documents.mapNotNull { doc ->
                    try {
                        val status = doc.getString("status")?.let { TaskStatus.valueOf(it) }
                        if (status == TaskStatus.COMPLETED || status == TaskStatus.CANCELED) doc.id else null
                    } catch (_: Exception) { null }
                }
                if (tasksToDelete.isEmpty()) return@launch
                tasksToDelete.chunked(500).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { taskId -> batch.delete(db.collection("tasks").document(taskId)) }
                    batch.commit().await()
                }
                Log.d(tag, "Cleared ${tasksToDelete.size} archived tasks via batch")
            } catch (e: Exception) {
                Log.e(tag, "Error clearing archived tasks", e)
                _uiState.update { it.copy(error = "Ошибка при очистке архива задач.") }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
            } catch (e: Exception) {
                Log.e(tag, "Error deleting task $taskId", e)
                _uiState.update { it.copy(error = "Не удалось удалить задачу: ${e.message}") }
            }
        }
    }

    private fun subscribeToActiveTasks() {
        taskRepository.getAllActiveTasksStream()
            .onEach { tasks -> _uiState.update { it.copy(activeTasks = tasks.sortedByDescending { t -> t.createdAt }) } }
            .catch { e -> Log.e(tag, "Error collecting active tasks", e); _uiState.update { it.copy(error = "Ошибка загрузки задач") } }
            .launchIn(viewModelScope)
    }

    private fun subscribeToEmployeeUpdates() {
        _uiState.update { it.copy(isLoading = true) }
        employeesListener = db.collection("internal_users")
            .orderBy("displayName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to employee updates", error)
                    _uiState.update { it.copy(error = "Ошибка загрузки сотрудников", isLoading = false) }
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val employeesList = snapshot.documents.mapNotNull { doc ->
                    val lastSeen = try { doc.getLong("lastSeen") ?: 0L } catch (_: Exception) { 0L }
                    val appVersion = try { doc.getString("appVersion") } catch (_: Exception) { null }
                    val lastAppUpdate = try { doc.getLong("lastAppUpdate") ?: 0L } catch (_: Exception) { 0L }
                    EmployeeInfo(
                        id = doc.id,
                        name = doc.getString("displayName") ?: "Без имени",
                        role = doc.getString("role") ?: "",
                        status = doc.getString("status") ?: "offline",
                        lastSeen = lastSeen,
                        warehouseId = doc.getString("warehouseId") ?: "bestuzhevskaya_10",
                        displayName = doc.getString("displayName") ?: "Без имени",
                        shiftRequestStatus = doc.getString("shiftRequestStatus") ?: "NONE",
                        isAllowedToWork = doc.getBoolean("isAllowedToWork") ?: false,
                        appVersion = appVersion,
                        lastAppUpdate = lastAppUpdate
                    )
                }

                val statusChanges = employeesList.filter { employee ->
                    val previousStatus = previousEmployeeStatuses[employee.id]
                    previousStatus != null && previousStatus != employee.shiftRequestStatus
                }
                statusChanges.forEach { employee ->
                    val statusText = when (employee.shiftRequestStatus) {
                        "PENDING"  -> "🔔 ${employee.displayName} запросил доступ к смене"
                        "APPROVED" -> "✅ ${employee.displayName} — доступ одобрен"
                        "DENIED"   -> "❌ ${employee.displayName} — доступ отклонён"
                        else -> null
                    }
                    statusText?.let { _notificationEvent.tryEmit(it) }
                }
                previousEmployeeStatuses = employeesList.associate { it.id to it.shiftRequestStatus }

                val pendingRequests = employeesList.filter { it.shiftRequestStatus == "PENDING" }
                val onShiftCount = snapshot.documents.count { doc -> doc.getBoolean("isShiftActive") == true }

                _uiState.update {
                    it.copy(
                        allEmployees = employeesList,
                        pendingShiftRequests = pendingRequests,
                        employeesOnShift = onShiftCount
                    )
                }
                // Данные активности обновляются через subscribeToActivityLog() и subscribeToRepairLog()

                if (!migrationTriggered) {
                    migrationTriggered = true
                    migrateTelemetryToDeviceCollection(snapshot.documents)
                }
                if (!cleanupTriggered) {
                    cleanupTriggered = true
                    cleanupOldTelemetryFromInternalUsers(snapshot.documents)
                }
            }
    }

    private fun migrateTelemetryToDeviceCollection(employees: List<DocumentSnapshot>) {
        val prefs = context.getSharedPreferences("telemetry_migration", Context.MODE_PRIVATE)
        if (prefs.getBoolean("telemetry_migration_done_v1", false)) return

        viewModelScope.launch {
            try {
                // Один запрос — все существующие device_telemetry документы с полем updatedAt
                val existingTelemetry = db.collection("device_telemetry").get().await()
                val migratedIds = existingTelemetry.documents
                    .filter { it.contains("updatedAt") }
                    .map { it.id }
                    .toSet()

                val toMigrate = employees.filter { it.id !in migratedIds }

                if (toMigrate.isEmpty()) {
                    prefs.edit().putBoolean("telemetry_migration_done_v1", true).apply()
                    Log.d(tag, "Migration done: 0 users (all already have device_telemetry)")
                    return@launch
                }

                val telemetryFields = listOf(
                    "lastBatteryLevel", "isCharging", "batteryHealth", "isPowerSaveMode",
                    "networkState", "networkPing", "freeRam", "freeStorage", "deviceUptime",
                    "deviceInfo", "locationLat", "locationLng", "locationTimestamp",
                    "activeDeviceId", "appVersion"
                )
                val now = System.currentTimeMillis()
                var migratedCount = 0

                toMigrate.chunked(500).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { userDoc ->
                        val data = hashMapOf<String, Any?>()
                        telemetryFields.forEach { field -> data[field] = userDoc.get(field) }
                        data["updatedAt"] = now
                        data["migratedAt"] = now
                        val ref = db.collection("device_telemetry").document(userDoc.id)
                        batch.set(ref, data, SetOptions.merge())
                    }
                    batch.commit().await()
                    migratedCount += chunk.size
                }

                prefs.edit().putBoolean("telemetry_migration_done_v1", true).apply()
                Log.d(tag, "Migration done: $migratedCount users")
            } catch (e: Exception) {
                Log.e(tag, "Migration failed (will retry next start)", e)
                // Флаг не ставим — повторит при следующем старте
            }
        }
    }

    private fun cleanupOldTelemetryFromInternalUsers(employees: List<DocumentSnapshot>) {
        val prefs = context.getSharedPreferences("telemetry_migration", Context.MODE_PRIVATE)
        if (prefs.getBoolean("telemetry_cleanup_done_v1", false)) return

        viewModelScope.launch {
            try {
                val fieldsToDelete = listOf(
                    "lastBatteryLevel", "isCharging", "batteryHealth", "isPowerSaveMode",
                    "networkState", "networkPing", "freeRam", "freeStorage", "deviceUptime",
                    "locationLat", "locationLng", "locationTimestamp"
                )
                val deleteEntry = FieldValue.delete()
                var cleanedCount = 0

                employees.chunked(500).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { userDoc ->
                        val updates = hashMapOf<String, Any>()
                        fieldsToDelete.forEach { field -> updates[field] = deleteEntry }
                        batch.update(db.collection("internal_users").document(userDoc.id), updates)
                    }
                    batch.commit().await()
                    cleanedCount += chunk.size
                }

                prefs.edit().putBoolean("telemetry_cleanup_done_v1", true).apply()
                Log.d(tag, "Cleanup done: ${fieldsToDelete.size} fields removed from $cleanedCount users")
            } catch (e: Exception) {
                Log.e(tag, "Cleanup failed (will retry next start)", e)
            }
        }
    }

    private fun loadActivityDataDebounced() {
        val now = System.currentTimeMillis()
        val timeSinceLastLoad = now - lastActivityLoadTime
        activityLoadJob?.cancel()
        if (lastActivityLoadTime == 0L || timeSinceLastLoad >= activityDebounceMs) {
            lastActivityLoadTime = now
            loadActivityData()
        } else {
            activityLoadJob = viewModelScope.launch {
                delay(activityDebounceMs - timeSinceLastLoad)
                lastActivityLoadTime = System.currentTimeMillis()
                loadActivityData()
            }
        }
    }

    private fun loadActivityData() {
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance()
                val startOfToday = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val startOfYesterday = startOfToday - 24 * 60 * 60 * 1000L
                val endOfYesterday = startOfToday - 1L

                val activeEmployeesDeferred = async { fetchActiveEmployeesToday(startOfToday) }
                val repairActivityDeferred  = async { fetchRepairActivityToday(startOfToday) }
                val yesterdayScansDeferred  = async { fetchScansYesterday(startOfYesterday, endOfYesterday) }
                val hourlyActivityDeferred  = async { fetchHourlyActivity(startOfToday) }

                val activeEmployees = activeEmployeesDeferred.await()
                val (repairsCount, activeElectriciansList) = repairActivityDeferred.await()
                val scansYesterday  = yesterdayScansDeferred.await()
                val hourlyActivity  = hourlyActivityDeferred.await()

                val allEmployees = _uiState.value.allEmployees
                val onShiftDetails = allEmployees
                    .filter { it.isOnline }
                    .map { employee ->
                        val activity = activeEmployees.find { it.id == employee.id }
                        EmployeeActivity(id = employee.id, name = employee.displayName, totalScans = activity?.totalScans ?: 0, logCount = activity?.logCount ?: 0, status = "online")
                    }
                    .sortedByDescending { it.totalScans }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scansToday = activeEmployees.sumOf { e -> e.totalScans },
                        logEntriesToday = activeEmployees.sumOf { e -> e.logCount },
                        repairsToday = repairsCount,
                        activeElectricians = activeElectriciansList,
                        employeeActivities = activeEmployees,
                        scansYesterday = scansYesterday,
                        hourlyActivity = hourlyActivity,
                        employeesOnShiftDetails = onShiftDetails
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading activity data", e)
                _uiState.update { it.copy(error = "Ошибка загрузки активности", isLoading = false) }
            }
        }
    }

    private suspend fun fetchScansYesterday(start: Long, end: Long): Int {
        return try {
            val snapshot = db.collection("activity_log")
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end)
                .get().await()
            snapshot.documents.sumOf { it.getLong("itemCount")?.toInt() ?: 0 }
        } catch (e: Exception) { Log.e(tag, "Error fetching yesterday scans", e); 0 }
    }

    private suspend fun fetchHourlyActivity(startOfToday: Long): List<HourlyActivityPoint> {
        return try {
            val snapshot = db.collection("activity_log")
                .whereGreaterThanOrEqualTo("timestamp", startOfToday)
                .get().await()
            val hourlyCounts = IntArray(24)
            snapshot.documents.forEach { doc ->
                val timestamp = doc.getLong("timestamp") ?: return@forEach
                val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                val c = Calendar.getInstance().apply { timeInMillis = timestamp }
                hourlyCounts[c.get(Calendar.HOUR_OF_DAY)] += itemCount
            }
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            (6..maxOf(currentHour, 6)).map { hour -> HourlyActivityPoint(hour = hour, scans = hourlyCounts[hour]) }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching hourly activity", e)
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            (6..maxOf(currentHour, 6)).map { HourlyActivityPoint(hour = it, scans = 0) }
        }
    }

    private suspend fun fetchRepairActivityToday(startOfToday: Long): Pair<Int, List<EmployeeActivity>> {
        val snapshot = db.collection("battery_repair_log")
            .whereGreaterThanOrEqualTo("timestamp", startOfToday)
            .get().await()
        val allEmployees = _uiState.value.allEmployees
        var totalRepairs = 0
        val validDocs = snapshot.documents.mapNotNull { doc ->
            try {
                val repairs = (doc.get("repairs") as? List<*>)?.size ?: 0
                totalRepairs += repairs
                doc
            } catch (e: Exception) { Log.w(tag, "Skipping malformed repair doc ${doc.id}: ${e.message}"); null }
        }
        val groupedByElectrician = validDocs.groupBy { it.getString("electricianId") }
        val activeElectricians = groupedByElectrician.mapNotNull { (userId, userLogs) ->
            if (userId == null) return@mapNotNull null
            val repairsCount = userLogs.sumOf { try { (it.get("repairs") as? List<*>)?.size ?: 0 } catch (_: Exception) { 0 } }
            val name = userLogs.firstNotNullOfOrNull { it.getString("electricianName") } ?: "Неизвестный"
            val employeeInfo = allEmployees.find { it.id == userId }
            val statusStr = if (employeeInfo?.isOnline == true) "online" else "offline"
            EmployeeActivity(id = userId, name = name, totalScans = repairsCount, logCount = userLogs.size, status = statusStr)
        }.sortedByDescending { it.totalScans }
        return Pair(totalRepairs, activeElectricians)
    }

    private suspend fun fetchActiveEmployeesToday(startOfToday: Long): List<EmployeeActivity> {
        val snapshot = db.collection("activity_log")
            .whereGreaterThanOrEqualTo("timestamp", startOfToday)
            .get().await()
        val allEmployees = _uiState.value.allEmployees
        val groupedByEmployee = snapshot.documents.groupBy { it.getString("creatorId") }
        return groupedByEmployee.mapNotNull { (userId, userLogs) ->
            if (userId == null) return@mapNotNull null
            val totalScans = userLogs.sumOf { it.getLong("itemCount")?.toInt() ?: 0 }
            val logCount = userLogs.size
            val name = userLogs.firstNotNullOfOrNull { it.getString("creatorName") } ?: "Неизвестный"
            val employeeInfo = allEmployees.find { it.id == userId }
            val statusStr = if (employeeInfo?.isOnline == true) "online" else "offline"
            EmployeeActivity(id = userId, name = name, totalScans = totalScans, logCount = logCount, status = statusStr)
        }.sortedByDescending { it.totalScans }
    }

    @Suppress("unused")
    fun loadDetailsForEmployee(employeeId: String, employeeName: String) {
        _uiState.update { it.copy(isDetailsLoading = true) }
        viewModelScope.launch {
            try {
                val startOfToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val snapshot = db.collection("activity_log")
                    .whereEqualTo("creatorId", employeeId)
                    .whereGreaterThanOrEqualTo("timestamp", startOfToday)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get().await()
                val entries = snapshot.documents.mapNotNull { doc ->
                    val timestamp = doc.getLong("timestamp") ?: return@mapNotNull null
                    val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                    ActivityLogEntry(timestamp = timestamp, itemCount = itemCount)
                }
                _uiState.update { it.copy(isDetailsLoading = false, selectedEmployeeDetails = EmployeeDetails(name = employeeName, entries = entries)) }
            } catch (e: Exception) {
                Log.e(tag, "Error loading employee details", e)
                _uiState.update { it.copy(isDetailsLoading = false, error = "Ошибка загрузки деталей") }
            }
        }
    }

    fun clearEmployeeDetails() { _uiState.update { it.copy(selectedEmployeeDetails = null) } }

    override fun onCleared() {
        super.onCleared()
        activityLoadJob?.cancel()
        employeesListener?.remove()
        fieldRepairListener?.remove()
        tasksListener?.remove()
        activityTodayListener?.remove()
        repairsTodayListener?.remove()
    }
}