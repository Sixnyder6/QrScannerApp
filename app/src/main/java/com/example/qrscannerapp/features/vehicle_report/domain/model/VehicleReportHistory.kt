// Полная, новая версия файла: features/vehicle_report/domain/model/VehicleReportHistory.kt

package com.example.qrscannerapp.features.vehicle_report.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle_report_history")
data class VehicleReportHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var firestoreDocumentId: String? = null,
    val timestamp: Long,
    val fileName: String,

    // --- Поля с количеством (остаются для быстрого отображения в списке) ---
    val readyForExportCount: Int,
    val storageCount: Int,
    val awaitingTestingCount: Int,
    val testingChargedCount: Int,
    val testingDischargedCount: Int,
    val awaitingRepairCount: Int,

    // --- V-- НАЧАЛО НОВЫХ ПОЛЕЙ --V ---
    // --- Добавляем поля для хранения самих списков номеров ---
    val readyForExportList: List<String> = emptyList(),
    val storageList: List<String> = emptyList(),
    val awaitingRepairList: List<String> = emptyList(),
    val awaitingTestingList: List<String> = emptyList(),
    val testingChargedList: List<String> = emptyList(),
    val testingDischargedList: List<String> = emptyList()
    // --- ^-- КОНЕЦ НОВЫХ ПОЛЕЙ --^ ---
)