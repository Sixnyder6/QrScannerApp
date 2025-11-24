package com.example.qrscannerapp
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.qrscannerapp.features.electrician.domain.model.BatteryRepairLog
import com.example.qrscannerapp.features.electrician.domain.model.ElectricianHistoryUiState
import com.example.qrscannerapp.features.electrician.utils.pdf.PdfExporter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
class AdminRepairLogViewModel(
    private val authManager: AuthManager
) : ViewModel() {
    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow(ElectricianHistoryUiState(isLoading = false))
    val uiState = _uiState.asStateFlow()

    fun onDateHeaderClick(date: String) {
        _uiState.update { currentState ->
            val newExpandedDate = if (currentState.expandedDate == date) null else date
            currentState.copy(expandedDate = newExpandedDate)
        }
    }

    fun exportHistoryToPdf(context: Context, uri: Uri) {
        val historyData = _uiState.value.groupedRepairLogs
        val adminName = authManager.authState.value.userName ?: "Администратор"

        if (historyData.isEmpty()) {
            Toast.makeText(context, "Нет данных для экспорта.", Toast.LENGTH_SHORT).show()
            return
        }

        PdfExporter.createPdf(context, uri, historyData, adminName)
    }

    fun loadLogsForDateRange(startDateMillis: Long, endDateMillis: Long) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        val adjustedEndDateMillis = endDateMillis + TimeUnit.DAYS.toMillis(1)

        db.collection("battery_repair_log")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereGreaterThanOrEqualTo("timestamp", startDateMillis)
            .whereLessThan("timestamp", adjustedEndDateMillis)
            .limit(1000)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminRepairLogViewModel", "Error listening to repair log", error)
                    _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить журнал ремонтов.") }
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    return@addSnapshotListener
                }

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

                val sdf = SimpleDateFormat("dd MMMM yyyy г.", Locale("ru"))
                val groupedLogs = logs.groupBy { log ->
                    sdf.format(Date(log.timestamp))
                }

                _uiState.update { it.copy(isLoading = false, groupedRepairLogs = groupedLogs, expandedDate = null) }
            }
    }
}
// Фабрика для создания AdminRepairLogViewModel с зависимостью AuthManager
class AdminRepairLogViewModelFactory(private val authManager: AuthManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminRepairLogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminRepairLogViewModel(authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}