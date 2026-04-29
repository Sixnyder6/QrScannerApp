package com.example.qrscannerapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.shift.data.repository.ShiftRepository
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ChartDataPoint(val day: String, val count: Int)

// Статистика полевого ремонта для техника
data class FieldRepairStats(
    val totalToday: Int = 0,
    val doneToday: Int = 0,
    val toStorageToday: Int = 0,
    val notFoundToday: Int = 0,
    val inProgressToday: Int = 0,
    val totalAllTime: Int = 0,
    val doneAllTime: Int = 0,
    val avgMinutesPerScooter: Int = 0  // среднее время на самокат в минутах
)

data class AccountUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userName: String = "Загрузка...",
    val userRole: String = "Сотрудник",
    val userRoleEnum: UserRole = UserRole.USER,   // ← для сравнения в UI
    val registrationDate: String = "-",
    val totalScans: Int = 0,
    val totalSessions: Int = 0,
    val scansToday: Int = 0,
    val sessionsToday: Int = 0,
    val weeklyScans: List<ChartDataPoint> = emptyList(),
    val streakDays: Int = 0,
    val personalRecord: Int = 0,
    val isShiftActive: Boolean = false,
    val shiftStartTime: Long = 0L,
    val isAllowedToWork: Boolean = false,
    val shiftRequestStatus: ShiftRequestStatus = ShiftRequestStatus.NONE,
    val shiftHistory: List<com.example.qrscannerapp.features.shift.domain.model.Shift> = emptyList(),
    // Полевой ремонт — только для TECHNIC
    val fieldRepairStats: FieldRepairStats = FieldRepairStats(),
    val fieldRepairLoading: Boolean = false
)

private data class UserActivitySummary(
    val totalScans: Int,
    val totalSessions: Int,
    val scansToday: Int,
    val sessionsToday: Int,
    val weeklyScans: List<ChartDataPoint>,
    val streakDays: Int,
    val personalRecord: Int
)

data class UserProfileData(
    val name: String,
    val role: String,
    val roleEnum: UserRole,
    val regDate: String,
    val isShiftActive: Boolean,
    val shiftStartTime: Long,
    val isAllowedToWork: Boolean,
    val shiftRequestStatus: ShiftRequestStatus
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val shiftRepository: ShiftRepository
) : ViewModel() {

    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState = _uiState.asStateFlow()

    private val userId: String?
        get() = authManager.authState.value.userId

    init {
        loadInitialData()
        viewModelScope.launch {
            authManager.authState.collect { authState ->
                _uiState.update {
                    it.copy(
                        isShiftActive      = authState.isShiftActive,
                        shiftStartTime     = authState.shiftStartTime,
                        isAllowedToWork    = authState.isAllowedToWork,
                        shiftRequestStatus = authState.shiftRequestStatus
                    )
                }
            }
        }
    }

    fun requestShiftAccess() {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                db.collection("internal_users").document(uid)
                    .update("shiftRequestStatus", ShiftRequestStatus.PENDING.key)
                    .await()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка отправки запроса: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun startShift() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = shiftRepository.startNewShift()
            if (result.isSuccess) {
                authManager.startShiftLocally()
                loadInitialData()
            } else {
                _uiState.update { it.copy(error = "Ошибка начала смены: ${result.exceptionOrNull()?.message}") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun endShift() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = shiftRepository.endShift(reason = "manual")
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Ошибка завершения смены: ${result.exceptionOrNull()?.message}") }
            } else {
                loadInitialData()
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    suspend fun endShiftOnLogout(): Result<Unit> {
        return shiftRepository.endShift(reason = "logout")
    }

    fun forceEndShift(targetUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = shiftRepository.forceEndShift(targetUserId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Ошибка: ${result.exceptionOrNull()?.message}") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val uid = userId
            if (uid == null) {
                _uiState.update { it.copy(isLoading = false, error = "Ошибка: пользователь не найден.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            try {
                val profileDeferred      = async { fetchUserProfile(uid) }
                val activityDeferred     = async { fetchUserActivitySummary(uid) }
                val shiftsDeferred       = async { shiftRepository.getUserShifts(uid).getOrDefault(emptyList()) }

                val profile      = profileDeferred.await()
                val activity     = activityDeferred.await()
                val shifts       = shiftsDeferred.await()

                _uiState.update {
                    it.copy(
                        isLoading          = false,
                        userName           = profile.name,
                        userRole           = profile.role,
                        userRoleEnum       = profile.roleEnum,
                        registrationDate   = profile.regDate,
                        isShiftActive      = profile.isShiftActive,
                        shiftStartTime     = profile.shiftStartTime,
                        isAllowedToWork    = profile.isAllowedToWork,
                        shiftRequestStatus = profile.shiftRequestStatus,
                        totalScans         = activity.totalScans,
                        totalSessions      = activity.totalSessions,
                        scansToday         = activity.scansToday,
                        sessionsToday      = activity.sessionsToday,
                        weeklyScans        = activity.weeklyScans,
                        streakDays         = activity.streakDays,
                        personalRecord     = activity.personalRecord,
                        shiftHistory       = shifts
                    )
                }

                // Для техника грузим статистику полевого ремонта отдельно
                if (profile.roleEnum == UserRole.TECHNIC) {
                    loadFieldRepairStats(uid)
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить статистику.") }
            }
        }
    }

    // ── Полевой ремонт для техника ────────────────────────────────────────────
    private fun loadFieldRepairStats(uid: String) {
        _uiState.update { it.copy(fieldRepairLoading = true) }
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfToday = cal.timeInMillis

                // Все задания этого техника
                val allSnapshot = db.collection("field_repair_tasks")
                    .whereEqualTo("assignedToId", uid)
                    .get().await()

                val allTasks = allSnapshot.documents

                // Задания за сегодня — фильтруем по completedAt или createdAt
                val todayTasks = allTasks.filter { doc ->
                    val ts = doc.getLong("completedAt") ?: doc.getLong("createdAt") ?: 0L
                    ts >= startOfToday
                }

                val doneToday      = todayTasks.count { it.getString("status") == "done" }
                val toStorageToday = todayTasks.count { it.getString("status") == "to_storage" }
                val notFoundToday  = todayTasks.count { it.getString("status") == "not_found" }
                val inProgToday    = todayTasks.count { it.getString("status") == "in_progress" }

                val doneAllTime      = allTasks.count { it.getString("status") == "done" }
                val toStorageAllTime = allTasks.count { it.getString("status") == "to_storage" }

                // Среднее время на самокат — по задачам где есть updatedAt и createdAt
                val avgMinutes = calculateAvgMinutes(allTasks)

                _uiState.update {
                    it.copy(
                        fieldRepairLoading = false,
                        fieldRepairStats = FieldRepairStats(
                            totalToday           = todayTasks.size,
                            doneToday            = doneToday,
                            toStorageToday       = toStorageToday,
                            notFoundToday        = notFoundToday,
                            inProgressToday      = inProgToday,
                            totalAllTime         = allTasks.size,
                            doneAllTime          = doneAllTime + toStorageAllTime,
                            avgMinutesPerScooter = avgMinutes
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(fieldRepairLoading = false) }
            }
        }
    }

    private fun calculateAvgMinutes(
        docs: List<com.google.firebase.firestore.DocumentSnapshot>
    ): Int {
        val times = docs.mapNotNull { doc ->
            val created   = doc.getLong("createdAt") ?: return@mapNotNull null
            val completed = doc.getLong("completedAt") ?: return@mapNotNull null
            val status    = doc.getString("status") ?: return@mapNotNull null
            if (status != "done" && status != "to_storage") return@mapNotNull null
            val diff = (completed - created) / 60_000  // в минутах
            if (diff in 1..120) diff else null          // игнорируем аномалии
        }
        return if (times.isEmpty()) 0 else (times.sum() / times.size).toInt()
    }

    // ── Профиль пользователя ──────────────────────────────────────────────────
    private suspend fun fetchUserProfile(userId: String): UserProfileData {
        val userDoc = db.collection("internal_users").document(userId).get().await()
        if (!userDoc.exists()) throw Exception("User document not found")

        val name          = userDoc.getString("displayName") ?: "Без имени"
        val rawRole       = userDoc.getString("role")
        val isAdmin       = userDoc.getBoolean("isAdmin") ?: false
        val roleEnum      = UserRole.fromKey(rawRole)
        val roleDisplay   = if (rawRole != null) roleEnum.displayName
        else if (isAdmin) "Администратор" else "Сотрудник"
        val regTimestamp  = userDoc.getLong("registrationTimestamp")
        val regDate       = if (regTimestamp != null)
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(regTimestamp))
        else "-"
        val isShiftActive      = userDoc.getBoolean("isShiftActive") ?: false
        val shiftStartTime     = userDoc.getLong("shiftStartTime") ?: 0L
        val isAllowedToWork    = userDoc.getBoolean("isAllowedToWork") ?: false
        val shiftRequestStatus = ShiftRequestStatus.fromKey(userDoc.getString("shiftRequestStatus"))

        return UserProfileData(name, roleDisplay, roleEnum, regDate, isShiftActive, shiftStartTime, isAllowedToWork, shiftRequestStatus)
    }

    // ── Активность (сканы/партии) — для не-техников ───────────────────────────
    private suspend fun fetchUserActivitySummary(userId: String): UserActivitySummary {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_WEEK, -6)
        val startOfWeek = calendar.timeInMillis

        val snapshot = db.collection("activity_log")
            .whereEqualTo("creatorId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startOfWeek)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()

        var scansToday = 0
        var sessionsToday = 0
        val dayFormat = SimpleDateFormat("E", Locale("ru"))
        val dailyCounts = mutableMapOf<String, Int>()
        val tempCal = Calendar.getInstance()
        repeat(7) {
            dailyCounts[dayFormat.format(tempCal.time)] = 0
            tempCal.add(Calendar.DAY_OF_WEEK, -1)
        }

        for (doc in snapshot.documents) {
            val timestamp = doc.getLong("timestamp") ?: 0L
            val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
            if (timestamp >= startOfToday) { scansToday += itemCount; sessionsToday++ }
            val dayKey = dayFormat.format(Date(timestamp))
            dailyCounts[dayKey] = (dailyCounts[dayKey] ?: 0) + itemCount
        }

        val weeklyScans = mutableListOf<ChartDataPoint>()
        tempCal.timeInMillis = now
        tempCal.add(Calendar.DAY_OF_WEEK, -6)
        repeat(7) {
            val dayKey = dayFormat.format(tempCal.time)
            weeklyScans.add(ChartDataPoint(dayKey.replaceFirstChar { it.uppercase() }, dailyCounts[dayKey] ?: 0))
            tempCal.add(Calendar.DAY_OF_WEEK, 1)
        }

        val totalSnapshot = db.collection("activity_log")
            .whereEqualTo("creatorId", userId)
            .get().await()
        val totalScans    = totalSnapshot.documents.sumOf { it.getLong("itemCount")?.toInt() ?: 0 }
        val totalSessions = totalSnapshot.size()
        val personalRecord = calculatePersonalRecord(totalSnapshot.documents)
        val streakDays = fetchStreakDays(userId)

        return UserActivitySummary(totalScans, totalSessions, scansToday, sessionsToday, weeklyScans, streakDays, personalRecord)
    }

    private fun calculatePersonalRecord(docs: List<com.google.firebase.firestore.DocumentSnapshot>): Int {
        val dayFormat  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dailyTotals = mutableMapOf<String, Int>()
        for (doc in docs) {
            val timestamp = doc.getLong("timestamp") ?: continue
            val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
            val dayKey = dayFormat.format(Date(timestamp))
            dailyTotals[dayKey] = (dailyTotals[dayKey] ?: 0) + itemCount
        }
        return dailyTotals.values.maxOrNull() ?: 0
    }

    private suspend fun fetchStreakDays(userId: String): Int {
        return try {
            val snapshot = db.collection("shifts")
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(60)
                .get().await()
            if (snapshot.isEmpty) return 0
            val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val shiftDays = snapshot.documents.mapNotNull { doc ->
                val startTime = doc.getLong("startTime") ?: return@mapNotNull null
                dayFormat.format(Date(startTime))
            }.toSortedSet(compareByDescending { it })
            if (shiftDays.isEmpty()) return 0
            val calStreak = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val todayStr     = dayFormat.format(calStreak.time)
            calStreak.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dayFormat.format(calStreak.time)
            if (!shiftDays.contains(todayStr) && !shiftDays.contains(yesterdayStr)) return 0
            val checkCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            var streak = 0
            while (true) {
                val dayStr = dayFormat.format(checkCal.time)
                if (shiftDays.contains(dayStr)) { streak++; checkCal.add(Calendar.DAY_OF_YEAR, -1) }
                else break
            }
            streak
        } catch (e: Exception) { 0 }
    }
}