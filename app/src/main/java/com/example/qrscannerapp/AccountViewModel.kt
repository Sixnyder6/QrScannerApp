package com.example.qrscannerapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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

// Модель для одной точки на графике
data class ChartDataPoint(val day: String, val count: Int)

data class AccountUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userName: String = "Загрузка...",
    val registrationDate: String = "-",
    // Общая статистика
    val totalScans: Int = 0,
    val totalSessions: Int = 0,
    // Статистика за сегодня
    val scansToday: Int = 0,
    val sessionsToday: Int = 0,
    // Данные для графика
    val weeklyScans: List<ChartDataPoint> = emptyList(),
    // --- НОВЫЕ ПОЛЯ ДЛЯ УПРАВЛЕНИЯ СМЕНОЙ ---
    val isShiftActive: Boolean = false,
    val shiftStartTime: Long = 0L
)

/**
 * Data class для структурированного возврата результатов из fetchUserActivitySummary.
 */
private data class UserActivitySummary(
    val totalScans: Int,
    val totalSessions: Int,
    val scansToday: Int,
    val sessionsToday: Int,
    val weeklyScans: List<ChartDataPoint>
)


class AccountViewModel(private val authManager: AuthManager) : ViewModel() {

    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState = _uiState.asStateFlow()

    private val userId: String?
        get() = authManager.authState.value.userId

    init {
        loadInitialData()
    }

    // --- НОВЫЕ ФУНКЦИИ ДЛЯ УПРАВЛЕНИЯ СМЕНОЙ ---
    fun startShift() {
        val uid = userId ?: return
        viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                // Сохраняем в Firestore
                db.collection("internal_users").document(uid).update(
                    mapOf(
                        "isShiftActive" to true,
                        "shiftStartTime" to startTime
                    )
                ).await()
                // Обновляем локальное состояние
                _uiState.update {
                    it.copy(
                        isShiftActive = true,
                        shiftStartTime = startTime
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка начала смены: ${e.message}") }
            }
        }
    }

    fun endShift() {
        val uid = userId ?: return
        viewModelScope.launch {
            try {
                // Удаляем поля в Firestore
                db.collection("internal_users").document(uid).update(
                    mapOf(
                        "isShiftActive" to FieldValue.delete(),
                        "shiftStartTime" to FieldValue.delete()
                    )
                ).await()
                // Обновляем локальное состояние
                _uiState.update {
                    it.copy(
                        isShiftActive = false,
                        shiftStartTime = 0L
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка завершения смены: ${e.message}") }
            }
        }
    }
    // --- КОНЕЦ НОВЫХ ФУНКЦИЙ ---

    private fun loadInitialData() {
        viewModelScope.launch {
            val uid = userId
            if (uid == null) {
                _uiState.update { it.copy(isLoading = false, error = "Ошибка: пользователь не найден.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            try {
                // Запускаем все асинхронные задачи параллельно
                val profileDeferred = async { fetchUserProfile(uid) }
                val activitySummaryDeferred = async { fetchUserActivitySummary(uid) }

                // Ждем выполнения всех задач
                val (name, regDate, isShiftActive, shiftStartTime) = profileDeferred.await()
                val activitySummary = activitySummaryDeferred.await()

                // Обновляем UI одним махом со всеми полученными данными
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userName = name,
                        registrationDate = regDate,
                        isShiftActive = isShiftActive,
                        shiftStartTime = shiftStartTime,
                        totalScans = activitySummary.totalScans,
                        totalSessions = activitySummary.totalSessions,
                        scansToday = activitySummary.scansToday,
                        sessionsToday = activitySummary.sessionsToday,
                        weeklyScans = activitySummary.weeklyScans
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить статистику.") }
            }
        }
    }

    // Теперь эта функция возвращает кортеж с 4 значениями
    private suspend fun fetchUserProfile(userId: String): Quadruple<String, String, Boolean, Long> {
        val userDoc = db.collection("internal_users").document(userId).get().await()
        if (!userDoc.exists()) throw Exception("User document not found")

        val name = userDoc.getString("displayName") ?: "Без имени"
        val regTimestamp = userDoc.getLong("registrationTimestamp")
        val regDate = if (regTimestamp != null) {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(regTimestamp))
        } else {
            "-"
        }

        // --- НОВЫЙ КОД: Загружаем статус смены ---
        val isShiftActive = userDoc.getBoolean("isShiftActive") ?: false
        val shiftStartTime = userDoc.getLong("shiftStartTime") ?: 0L

        return Quadruple(name, regDate, isShiftActive, shiftStartTime)
    }

    private suspend fun fetchUserActivitySummary(userId: String): UserActivitySummary {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_WEEK, -6)
        val startOfWeek = calendar.timeInMillis

        val snapshot = db.collection("activity_log")
            .whereEqualTo("creatorId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startOfWeek)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

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
            if (timestamp >= startOfToday) {
                scansToday += itemCount
                sessionsToday++
            }
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
            .get()
            .await()

        val totalScans = totalSnapshot.documents.sumOf { it.getLong("itemCount")?.toInt() ?: 0 }
        val totalSessions = totalSnapshot.size()

        return UserActivitySummary(
            totalScans = totalScans,
            totalSessions = totalSessions,
            scansToday = scansToday,
            sessionsToday = sessionsToday,
            weeklyScans = weeklyScans
        )
    }
}

// Вспомогательный data class для возврата 4 значений
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


// Фабрика остается без изменений
class AccountViewModelFactory(private val authManager: AuthManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}