// Полная, исправленная версия: VehicleReportHistoryViewModel.kt
// Реализовано: Обработка ошибок (вместо TODO) и уведомления пользователю

package com.example.qrscannerapp.features.vehicle_report.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.vehicle_report.data.repository.VehicleReportRepository
import com.example.qrscannerapp.features.vehicle_report.domain.model.VehicleReportHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Добавили поле userMessage для вывода ошибок или уведомлений
data class VehicleReportHistoryUiState(
    val reports: List<VehicleReportHistory> = emptyList(),
    val userMessage: String? = null
)

@HiltViewModel
class VehicleReportHistoryViewModel @Inject constructor(
    private val reportRepository: VehicleReportRepository
) : ViewModel() {

    // Локальное состояние для хранения сообщения (ошибки или успеха)
    private val _userMessage = MutableStateFlow<String?>(null)

    // Мы объединяем (combine) поток данных из БД и поток наших сообщений в один UiState
    val uiState: StateFlow<VehicleReportHistoryUiState> =
        combine(
            reportRepository.allReports,
            _userMessage
        ) { reportList, message ->
            VehicleReportHistoryUiState(
                reports = reportList,
                userMessage = message
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = VehicleReportHistoryUiState()
            )

    fun deleteReport(report: VehicleReportHistory) {
        viewModelScope.launch {
            try {
                reportRepository.deleteReport(report)
                // При успешном одиночном удалении можно не спамить сообщением, список просто обновится
            } catch (e: Exception) {
                Log.e("HistoryVM", "Ошибка при удалении одного отчета", e)
                _userMessage.value = "Не удалось удалить отчет: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Запускает процесс полного удаления всех отчетов.
     */
    fun deleteAllReports() {
        viewModelScope.launch {
            try {
                reportRepository.deleteAllReports()
                _userMessage.value = "История успешно очищена"
            } catch (e: Exception) {
                Log.e("HistoryVM", "Ошибка при полном удалении отчетов", e)
                _userMessage.value = "Ошибка очистки истории: ${e.localizedMessage}"
            }
        }
    }

    // Эту функцию вызовет UI, когда покажет Snackbar, чтобы очистить состояние
    fun messageShown() {
        _userMessage.value = null
    }
}