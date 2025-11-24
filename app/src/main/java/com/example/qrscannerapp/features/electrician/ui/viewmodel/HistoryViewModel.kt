package com.example.qrscannerapp.features.electrician.ui.viewmodel
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.features.electrician.domain.model.AnalyticsData
import com.example.qrscannerapp.features.electrician.domain.model.BatteryRepairLog
import com.example.qrscannerapp.features.electrician.domain.model.DateRange
import com.example.qrscannerapp.features.electrician.domain.model.ElectricianHistoryUiState
import com.example.qrscannerapp.features.electrician.domain.model.ManufacturerBreakdown
import com.example.qrscannerapp.features.electrician.domain.model.ProblematicBattery
import com.example.qrscannerapp.features.electrician.utils.pdf.ReportGenerator
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
class HistoryViewModel(private val authManager: AuthManager) : ViewModel() {
    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow(ElectricianHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        subscribeToHistoryUpdates(null)
        loadUserRegistrationDate()
    }

    fun onDateHeaderClick(date: String) {
        _uiState.update { currentState ->
            val newExpandedDate = if (currentState.expandedDate == date) null else date
            currentState.copy(expandedDate = newExpandedDate)
        }
    }

    fun setDateRange(startDateMillis: Long, endDateMillis: Long) {
        val dateRange = DateRange(startDateMillis, endDateMillis)
        _uiState.update { it.copy(selectedDateRange = dateRange) }
        subscribeToHistoryUpdates(dateRange)
    }

    fun generateAnalyticsReport(context: Context, uri: Uri) {
        val data = _uiState.value.analyticsData
        val electricianName = authManager.authState.value.userName ?: "Сотрудник"

        if (data.logs.isEmpty() || data.dateRange == null) {
            Toast.makeText(context, "Нет данных для экспорта за выбранный период.", Toast.LENGTH_SHORT).show()
            return
        }

        ReportGenerator.createAnalyticsReport(
            context = context,
            uri = uri,
            logs = data.logs,
            startDate = data.dateRange.start,
            endDate = data.dateRange.end,
            adminName = electricianName
        )
    }

    private fun loadUserRegistrationDate() {
        val userId = authManager.authState.value.userId ?: return
        viewModelScope.launch {
            try {
                val userDocument = db.collection("internal_users").document(userId).get().await()
                val registrationTimestamp = userDocument.getTimestamp("registrationDate")

                val formattedDate = if (registrationTimestamp != null) {
                    val sdf = SimpleDateFormat("dd MMMM yyyy г.", Locale("ru"))
                    sdf.format(registrationTimestamp.toDate())
                } else {
                    "Неизвестно"
                }
                _uiState.update { it.copy(registrationDate = formattedDate) }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error loading registration date", e)
                _uiState.update { it.copy(registrationDate = "Ошибка загрузки") }
            }
        }
    }

    private fun calculateOverallStats(allLogs: List<BatteryRepairLog>) {
        _uiState.update { it.copy(isOverallStatsLoading = true) }

        if (allLogs.isEmpty()) {
            _uiState.update { it.copy(isOverallStatsLoading = false) }
            return
        }

        val overallRepairsTotal = allLogs.size

        val topPerformer = allLogs
            .groupingBy { it.electricianName }
            .eachCount()
            .maxByOrNull { it.value }
            ?.toPair()

        val mostCommonOverallRepair = allLogs
            .flatMap { it.repairs }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "-"

        _uiState.update {
            it.copy(
                isOverallStatsLoading = false,
                overallRepairsTotal = overallRepairsTotal,
                topPerformer = topPerformer,
                mostCommonOverallRepair = mostCommonOverallRepair
            )
        }
    }

    // --- НОВАЯ ФУНКЦИЯ ДЛЯ ПРОДВИНУТОЙ АНАЛИТИКИ ---
    private fun calculateAdvancedAnalytics(allLogs: List<BatteryRepairLog>) {
        _uiState.update { it.copy(isAdvancedAnalyticsLoading = true) }

        if (allLogs.isEmpty()) {
            _uiState.update { it.copy(isAdvancedAnalyticsLoading = false) }
            return
        }

        // 1. Поиск "проблемных" АКБ (топ-3 по числу ремонтов)
        val problematicBatteries = allLogs
            .groupBy { it.batteryId } // Группируем все логи по ID аккумулятора
            .mapNotNull { (batteryId, logs) ->
                if (batteryId.isBlank()) return@mapNotNull null // Пропускаем пустые ID
                ProblematicBattery(
                    batteryId = batteryId,
                    repairCount = logs.size,
                    lastRepairDate = logs.maxOf { it.timestamp } // Находим дату последнего ремонта
                )
            }
            .filter { it.repairCount > 1 } // Интересуют только те, что ломались больше одного раза
            .sortedByDescending { it.repairCount } // Сортируем по убыванию числа ремонтов
            .take(3) // Берем первые 3

        // 2. Анализ поломок по производителям
        val manufacturerBreakdowns = allLogs
            .filter { it.manufacturer.isNotBlank() } // Берем только логи с указанным производителем
            .groupBy { it.manufacturer } // Группируем по производителю
            .map { (manufacturerName, logs) ->
                val breakdown = logs
                    .flatMap { it.repairs } // Собираем все типы ремонтов для этого производителя
                    .groupingBy { it }
                    .eachCount() // Считаем, сколько раз встречался каждый тип
                ManufacturerBreakdown(name = manufacturerName, breakdown = breakdown)
            }
            .sortedByDescending { it.breakdown.values.sum() } // Сортируем по общему числу ремонтов

        _uiState.update {
            it.copy(
                isAdvancedAnalyticsLoading = false,
                problematicBatteries = problematicBatteries,
                manufacturerBreakdowns = manufacturerBreakdowns
            )
        }
    }

    private fun subscribeToHistoryUpdates(dateRange: DateRange?) {
        val currentUserId = authManager.authState.value.userId
        if (currentUserId == null) {
            _uiState.update { it.copy(isLoading = false, error = "Ошибка: пользователь не найден.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        var query: Query = db.collection("battery_repair_log")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        if (dateRange != null) {
            val adjustedEndDateMillis = dateRange.end + TimeUnit.DAYS.toMillis(1)
            query = query
                .whereGreaterThanOrEqualTo("timestamp", dateRange.start)
                .whereLessThan("timestamp", adjustedEndDateMillis)
        }

        query.limit(1000).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("HistoryViewModel", "Error listening to history", error)
                _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить историю.") }
                return@addSnapshotListener
            }

            if (snapshot == null) return@addSnapshotListener

            val logs = snapshot.documents.mapNotNull { doc ->
                BatteryRepairLog(
                    id = doc.id,
                    batteryId = doc.getString("batteryId") ?: "",
                    electricianId = doc.getString("electricianId") ?: "",
                    electricianName = doc.getString("electricianName") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    repairs = doc.get("repairs") as? List<String> ?: emptyList(),
                    manufacturer = doc.getString("manufacturer") ?: ""
                )
            }

            // Вызываем расчет всей статистики
            calculateOverallStats(logs)
            calculateAdvancedAnalytics(logs) // <-- ДОБАВЛЕН ВЫЗОВ

            // Расчет персональной статистики (без изменений)
            val currentUserLogs = logs.filter { it.electricianId == currentUserId }
            val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
            val repairsLast30Days = currentUserLogs.count { it.timestamp >= thirtyDaysAgo }

            val mostCommonRepair = currentUserLogs
                .flatMap { it.repairs }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: "-"

            val favoriteManufacturer = currentUserLogs
                .filter { it.manufacturer.isNotBlank() }
                .groupingBy { it.manufacturer }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: "-"

            val sdf = SimpleDateFormat("dd MMMM yyyy г.", Locale("ru"))
            val groupedLogs = currentUserLogs.groupBy { log ->
                sdf.format(Date(log.timestamp))
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    groupedRepairLogs = groupedLogs,
                    analyticsData = AnalyticsData(logs = logs, dateRange = dateRange),
                    expandedDate = null,
                    repairsLast30Days = repairsLast30Days,
                    mostCommonRepair = mostCommonRepair,
                    favoriteManufacturer = favoriteManufacturer,
                    totalRepairs = currentUserLogs.size
                )
            }
        }
    }
}
class HistoryViewModelFactory(private val authManager: AuthManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECK-ED_CAST")
            return HistoryViewModel(authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}