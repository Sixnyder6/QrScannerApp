// Полная, измененная версия файла: features/vehicle_report/ui/VehicleReportViewModel.kt

package com.example.qrscannerapp.features.vehicle_report.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.vehicle_report.data.repository.VehicleReportRepository
import com.example.qrscannerapp.features.vehicle_report.domain.model.VehicleReportHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import javax.inject.Inject

private const val DEBUG_TAG = "AwaitingTestingDebug"

data class VehicleReportUiState(
    val isLoading: Boolean = false,
    val readyForExportList: List<String> = emptyList(),
    val storageList: List<String> = emptyList(),
    val awaitingRepairList: List<String> = emptyList(),
    val awaitingTestingList: List<String> = emptyList(),
    val testingChargedList: List<String> = emptyList(),
    val testingDischargedList: List<String> = emptyList(),
    val errorMessage: String? = null,
    val fileName: String? = null
)

@HiltViewModel
class VehicleReportViewModel @Inject constructor(
    private val application: Application,
    private val reportRepository: VehicleReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleReportUiState())
    val uiState = _uiState.asStateFlow()

    fun processExcelFile(uri: Uri, displayName: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = VehicleReportUiState(isLoading = true, fileName = displayName)
            try {
                application.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseAndSaveExcelStream(inputStream, displayName ?: "unknown_file.xlsx")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Ошибка при чтении файла: ${e.message}"
                )
            }
        }
    }

    private suspend fun parseAndSaveExcelStream(inputStream: InputStream, fileName: String) {
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0)

        val readyForExport = mutableListOf<String>()
        val storage = mutableListOf<String>()
        val awaitingRepair = mutableListOf<String>()
        val awaitingTesting = mutableListOf<String>()
        val testingCharged = mutableListOf<String>()
        val testingDischarged = mutableListOf<String>()

        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue

            val scooterNumberCell = row.getCell(1)
            val processCell = row.getCell(12)
            val chargeCell = row.getCell(15)
            val lagCell = row.getCell(26) // <-- Ячейка с датой/временем

            val scooterNumber = getCellStringValue(scooterNumberCell)
            val process = getCellStringValue(processCell).lowercase()
            val charge = getCellNumericValue(chargeCell)

            if (scooterNumber.isBlank()) continue

            when {
                process.contains("готов к вывозу") -> readyForExport.add(scooterNumber)
                process.contains("на хранении") -> storage.add(scooterNumber)
                process.contains("ремонт") -> awaitingRepair.add(scooterNumber)
                process.contains("ожидает тестирования") -> {
                    val rawLagValueForDebug = if (lagCell?.cellType == CellType.NUMERIC) lagCell.numericCellValue.toString() else getCellStringValue(lagCell)
                    Log.d(DEBUG_TAG, "--- Проверка самоката: $scooterNumber ---")
                    Log.d(DEBUG_TAG, "Входные данные: charge=$charge, rawLagValue='$rawLagValueForDebug'")

                    val lagInMinutes = parseExcelDateToMinutes(lagCell)
                    Log.d(DEBUG_TAG, "Рассчитано: lagInMinutes=$lagInMinutes")

                    val isLagOk = lagInMinutes == null || lagInMinutes <= 20
                    val isChargeOk = charge != null && charge != 0.0
                    Log.d(DEBUG_TAG, "Условия: isLagOk=$isLagOk, isChargeOk=$isChargeOk (порог 20 мин)")

                    if (isLagOk && isChargeOk) {
                        awaitingTesting.add(scooterNumber)
                        Log.d(DEBUG_TAG, "РЕЗУЛЬТАТ: ДОБАВЛЕН в 'Ожидает тестирования'")
                    } else {
                        Log.d(DEBUG_TAG, "РЕЗУЛЬТАТ: ПРОПУЩЕН")
                    }

                    if (charge != null) {
                        if (charge >= 70) {
                            testingCharged.add(scooterNumber)
                        } else if (charge < 50) {
                            testingDischarged.add(scooterNumber)
                        }
                    }
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            readyForExportList = readyForExport,
            storageList = storage,
            awaitingRepairList = awaitingRepair,
            awaitingTestingList = awaitingTesting,
            testingChargedList = testingCharged,
            testingDischargedList = testingDischarged,
            errorMessage = null
        )

        // V-- НАЧАЛО ИЗМЕНЕНИЙ --V
        // Теперь мы передаем в объект для сохранения не только количество, но и сами списки.
        val reportToSave = VehicleReportHistory(
            timestamp = System.currentTimeMillis(),
            fileName = fileName,
            // Считаем количество, как и раньше
            readyForExportCount = readyForExport.size,
            storageCount = storage.size,
            awaitingTestingCount = awaitingTesting.size,
            testingChargedCount = testingCharged.size,
            testingDischargedCount = testingDischarged.size,
            awaitingRepairCount = awaitingRepair.size,
            // И добавляем сами списки в новые поля
            readyForExportList = readyForExport,
            storageList = storage,
            awaitingRepairList = awaitingRepair,
            awaitingTestingList = awaitingTesting,
            testingChargedList = testingCharged,
            testingDischargedList = testingDischarged
        )
        // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^

        reportRepository.uploadAndSaveReport(reportToSave)
    }

    private fun parseExcelDateToMinutes(cell: Cell?): Int? {
        if (cell == null || cell.cellType != CellType.NUMERIC) {
            return null
        }
        val totalMinutes = cell.numericCellValue * 1440.0
        return totalMinutes.toInt()
    }

    private fun getCellNumericValue(cell: Cell?): Double? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull()
            else -> null
        }
    }

    private fun getCellStringValue(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> {
                val numericValue = cell.numericCellValue
                val stringValue = if (numericValue == numericValue.toLong().toDouble()) {
                    numericValue.toLong().toString()
                } else {
                    numericValue.toString()
                }
                stringValue.trim()
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString().trim()
            CellType.FORMULA -> {
                when (cell.cachedFormulaResultType) {
                    CellType.STRING -> cell.richStringCellValue.string.trim()
                    CellType.NUMERIC -> {
                        val numericValue = cell.numericCellValue
                        val stringValue = if (numericValue == numericValue.toLong().toDouble()) {
                            numericValue.toLong().toString()
                        } else {
                            numericValue.toString()
                        }
                        stringValue.trim()
                    }
                    else -> ""
                }
            }
            else -> ""
        }
    }
}