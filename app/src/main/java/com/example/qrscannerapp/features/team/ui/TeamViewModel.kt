package com.example.qrscannerapp.features.team.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.EmployeeActivity
import com.example.qrscannerapp.EmployeeInfo
import com.example.qrscannerapp.UserRole
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

// ============================================================================================
// ФИЛЬТРЫ И СОРТИРОВКА
// ============================================================================================

enum class TeamRoleFilter(val displayName: String) {
    ALL("Все"),
    MUVER("Муверы"),
    ELECTRICIAN("Электрики"),
    TECHNIC("Техники"),
    MANAGER("Менеджеры")
}

enum class TeamSortMode(val displayName: String) {
    BY_ACTIVITY("По активности"),
    BY_NAME("По имени"),
    ONLINE_FIRST("Онлайн")
}

// ============================================================================================
// UI STATE
// ============================================================================================

data class TeamMember(
    val info: EmployeeInfo,
    val scansToday: Int = 0,
    val batchesToday: Int = 0,
    val scanRatePerHour: Int = 0,
    val minutesSinceLastActivity: Long = -1L
)

data class TeamUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val members: List<TeamMember> = emptyList(),
    val searchQuery: String = "",
    val roleFilter: TeamRoleFilter = TeamRoleFilter.ALL,
    val sortMode: TeamSortMode = TeamSortMode.BY_ACTIVITY,
    // Счётчики по ролям
    val totalCount: Int = 0,
    val muverCount: Int = 0,
    val electricianCount: Int = 0,
    val technicCount: Int = 0,
    val managerCount: Int = 0,
    val onlineCount: Int = 0
)

// ============================================================================================
// VIEWMODEL
// ============================================================================================

@HiltViewModel
class TeamViewModel @Inject constructor() : ViewModel() {

    private val db = Firebase.firestore
    private val tag = "TeamViewModel"

    private var employeesListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState = _uiState.asStateFlow()

    // Сырые данные — для фильтрации
    private var allMembers: List<TeamMember> = emptyList()

    init {
        subscribeToEmployees()
    }

    // ============================================================================================
    // ПОДПИСКА НА СОТРУДНИКОВ
    // ============================================================================================

    private fun subscribeToEmployees() {
        _uiState.update { it.copy(isLoading = true) }

        employeesListener = db.collection("internal_users")
            .orderBy("displayName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to employees", error)
                    _uiState.update { it.copy(error = "Ошибка загрузки сотрудников", isLoading = false) }
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val employees = snapshot.documents.mapNotNull { doc ->
                    try {
                        EmployeeInfo(
                            id = doc.id,
                            name = doc.getString("displayName") ?: "Без имени",
                            role = doc.getString("role") ?: "",
                            status = doc.getString("status") ?: "offline",
                            lastSeen = doc.getLong("lastSeen") ?: 0L,
                            warehouseId = doc.getString("warehouseId") ?: "bestuzhevskaya_10",
                            displayName = doc.getString("displayName") ?: "Без имени",
                            shiftRequestStatus = doc.getString("shiftRequestStatus") ?: "NONE",
                            isAllowedToWork = doc.getBoolean("isAllowedToWork") ?: false,
                            appVersion = doc.getString("appVersion"),
                            lastAppUpdate = doc.getLong("lastAppUpdate") ?: 0L
                        )
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing employee", e)
                        null
                    }
                }

                // Считаем счётчики
                val muvers = employees.count { it.role == "muver" }
                val electricians = employees.count { it.role == "electrician" }
                val technics = employees.count { it.role == "technic" }
                val managers = employees.count { it.role == "admin" || it.role == "inventory_manager" }
                val online = employees.count { it.isOnline }

                // Создаём TeamMember без активности пока
                allMembers = employees.map { emp ->
                    val minutesAgo = if (emp.lastSeen > 0L) {
                        (System.currentTimeMillis() - emp.lastSeen) / 60_000L
                    } else -1L

                    TeamMember(
                        info = emp,
                        minutesSinceLastActivity = minutesAgo
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalCount = employees.size,
                        muverCount = muvers,
                        electricianCount = electricians,
                        technicCount = technics,
                        managerCount = managers,
                        onlineCount = online
                    )
                }

                // Загружаем активность
                loadTodayActivity()
            }
    }

    // ============================================================================================
    // АКТИВНОСТЬ ЗА СЕГОДНЯ
    // ============================================================================================

    private fun loadTodayActivity() {
        viewModelScope.launch {
            try {
                val startOfToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val snapshot = db.collection("activity_log")
                    .whereGreaterThanOrEqualTo("timestamp", startOfToday)
                    .get().await()

                // Группируем по creatorId
                val activityByUser = snapshot.documents.groupBy { it.getString("creatorId") }

                val now = System.currentTimeMillis()
                val hoursElapsed = ((now - startOfToday) / 3_600_000.0).coerceAtLeast(1.0)

                allMembers = allMembers.map { member ->
                    val userLogs = activityByUser[member.info.id]
                    if (userLogs != null) {
                        val totalScans = userLogs.sumOf { it.getLong("itemCount")?.toInt() ?: 0 }
                        val batchCount = userLogs.size
                        val scanRate = (totalScans / hoursElapsed).toInt()
                        member.copy(
                            scansToday = totalScans,
                            batchesToday = batchCount,
                            scanRatePerHour = scanRate
                        )
                    } else member
                }

                applyFilters()

            } catch (e: Exception) {
                Log.e(tag, "Error loading today activity", e)
                applyFilters() // Показываем что есть
            }
        }
    }

    // ============================================================================================
    // ФИЛЬТРАЦИЯ И СОРТИРОВКА
    // ============================================================================================

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setRoleFilter(filter: TeamRoleFilter) {
        _uiState.update { it.copy(roleFilter = filter) }
        applyFilters()
    }

    fun setSortMode(mode: TeamSortMode) {
        _uiState.update { it.copy(sortMode = mode) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value

        var filtered = allMembers

        // Фильтр по роли
        filtered = when (state.roleFilter) {
            TeamRoleFilter.ALL -> filtered
            TeamRoleFilter.MUVER -> filtered.filter { it.info.role == "muver" }
            TeamRoleFilter.ELECTRICIAN -> filtered.filter { it.info.role == "electrician" }
            TeamRoleFilter.TECHNIC -> filtered.filter { it.info.role == "technic" }
            TeamRoleFilter.MANAGER -> filtered.filter {
                it.info.role == "admin" || it.info.role == "inventory_manager"
            }
        }

        // Фильтр по поиску
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.info.displayName.lowercase().contains(query)
            }
        }

        // Сортировка
        filtered = when (state.sortMode) {
            TeamSortMode.BY_ACTIVITY -> filtered.sortedByDescending { it.scansToday }
            TeamSortMode.BY_NAME -> filtered.sortedBy { it.info.displayName }
            TeamSortMode.ONLINE_FIRST -> filtered.sortedWith(
                compareByDescending<TeamMember> { it.info.isOnline }
                    .thenByDescending { it.scansToday }
            )
        }

        _uiState.update { it.copy(members = filtered) }
    }

    // ============================================================================================
    // CRUD СОТРУДНИКОВ
    // ============================================================================================

    fun createEmployee(name: String, username: String, pass: String, role: String, warehouseId: String) {
        viewModelScope.launch {
            try {
                val newUser = hashMapOf(
                    "username" to username,
                    "password" to pass,
                    "role" to role,
                    "warehouseId" to warehouseId,
                    "displayName" to name,
                    "isAdmin" to (role == "admin"),
                    "status" to "offline",
                    "lastSeen" to 0L,
                    "isAllowedToWork" to false,
                    "shiftRequestStatus" to "NONE",
                    "registrationTimestamp" to System.currentTimeMillis(),
                    "appVersion" to "1.0.0",
                    "lastAppUpdate" to System.currentTimeMillis(),
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
                    "displayName" to name,
                    "role" to role,
                    "isAdmin" to (role == "admin"),
                    "warehouseId" to warehouseId
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        employeesListener?.remove()
    }
}