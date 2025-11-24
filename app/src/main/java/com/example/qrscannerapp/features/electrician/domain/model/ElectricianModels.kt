package com.example.qrscannerapp.features.electrician.domain.model

import java.util.UUID

// Модели, добавленные для аналитики (остаются без изменений)
data class DateRange(val start: Long, val end: Long)
data class AnalyticsData(
    val logs: List<BatteryRepairLog> = emptyList(),
    val dateRange: DateRange? = null
)

/**
 * Перечисление всех возможных производителей АКБ. (без изменений)
 */
enum class Manufacturer(val displayName: String) {
    BYD("BYD"),
    FUJIAN("Fujian")
}

/**
 * Перечисление всех возможных типов ремонта АКБ. (без изменений)
 */
enum class RepairType(val displayName: String) {
    BMS("BMS"),
    SMALL_CONNECTOR_CHARGER("Разъем Малый З/У"),
    POWER_CONNECTOR("Силовой разъем"),
    OK("ОК"),
    INDICATION("Индикация"),
    OTHER("Другое")
}

/**
 * Модель данных для одной записи о ремонте АКБ. (без изменений)
 */
data class BatteryRepairLog(
    val id: String = UUID.randomUUID().toString(),
    val batteryId: String = "",
    val electricianId: String = "",
    val electricianName: String = "",
    val timestamp: Long = 0L,
    val repairs: List<String> = emptyList(),
    val manufacturer: String = ""
)


// --- НОВЫЕ МОДЕЛИ ДЛЯ ПРОДВИНУТОЙ АНАЛИТИКИ ---

data class ProblematicBattery(
    val batteryId: String,
    val repairCount: Int,
    val lastRepairDate: Long
)

data class ManufacturerBreakdown(
    val name: String,
    val breakdown: Map<String, Int>
)


/**
 * Модель состояния для экрана истории и статистики электрика. (без изменений)
 */
data class ElectricianHistoryUiState(
    // Основные данные и состояние
    val isLoading: Boolean = true,
    val error: String? = null,
    val groupedRepairLogs: Map<String, List<BatteryRepairLog>> = emptyMap(),
    val expandedDate: String? = null,
    val selectedDateRange: DateRange? = null,
    val analyticsData: AnalyticsData = AnalyticsData(),

    // Персональная статистика
    val repairsLast30Days: Int = 0,
    val mostCommonRepair: String = "-",
    val favoriteManufacturer: String = "-",
    val totalRepairs: Int = 0,
    val registrationDate: String = "-",

    // Общая статистика (для карточки на экране истории)
    val isOverallStatsLoading: Boolean = true,
    val overallRepairsTotal: Int = 0,
    val topPerformer: Pair<String, Int>? = null,
    val mostCommonOverallRepair: String = "-",

    // Продвинутая аналитика (Вариант Г)
    val isAdvancedAnalyticsLoading: Boolean = true,
    val problematicBatteries: List<ProblematicBattery> = emptyList(),
    val manufacturerBreakdowns: List<ManufacturerBreakdown> = emptyList()
)


// --- МОДЕЛЬ ЭКРАНА РЕМОНТА (ОБНОВЛЕНО) ---

data class ElectricianUiState(
    // Состояние сканирования
    val scannedBatteryId: String? = null,
    val isCheckingHistory: Boolean = false,
    val batteryHistory: List<BatteryRepairLog>? = null,
    val showManualInputDialog: Boolean = false,
    val manualInputText: String = "",

    // Состояние фонарика
    val isFlashlightOn: Boolean = false,

    // Состояние режима ремонта
    val isRepairMode: Boolean = false,
    val selectedRepairs: Set<RepairType> = emptySet(),
    val selectedManufacturer: Manufacturer = Manufacturer.BYD,
    val customRepairText: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveCompleted: Boolean = false
)