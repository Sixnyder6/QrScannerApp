package com.example.qrscannerapp.features.profile.domain.model

enum class PerformanceClass {
    LOW, MEDIUM, HIGH, UNKNOWN
}

data class DevicePerformanceDetails(
    val performanceClass: PerformanceClass = PerformanceClass.UNKNOWN,
    val totalRamGb: Double = 0.0
)

data class UserProfile(
    val name: String = "Загрузка...",
    val username: String = "",
    val role: String = "",
    val age: Int = 0,
    val deviceInfo: String = "",
    val activeDeviceId: String = "",
    val appVersion: String = "",
    val lastBatteryLevel: Int = -1,
    val totalRamInGb: Double? = null,
    val isShiftActive: Boolean = false,
    val isAllowedToWork: Boolean = false,
    val shiftRequestStatus: String = "NONE",
    val photoUrl: String? = null
)

data class UserActivityLog(
    val id: String = "",
    val timestamp: Long = 0,
    val activityType: String = "UNKNOWN",
    val itemCount: Int = 0,
    val manualEntryCount: Int = 0,
    val durationSeconds: Long = 0,
    val appVersion: String = "N/A",
    val lastBatteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val networkState: String = "N/A",
    val freeRam: String = "N/A",
    val freeStorage: String = "N/A",
    val deviceUptime: String = "N/A",
    val batteryHealth: String = "N/A",
    val isPowerSaveMode: Boolean = false,
    val networkPing: String = "N/A",
    val totalRamInGb: Double? = null
)

// [НОВОЕ] Статистика по одному типу операции
data class OperationStat(
    val operationType: String,       // "WASHING", "SANDING", etc.
    val totalCount: Int,             // суммарно самокатов
    val sessionCount: Int,           // количество сессий
    val lastTimestamp: Long          // когда последний раз
)

// [НОВОЕ] Полная статистика взаимодействий сотрудника
data class InteractionStats(
    val stats: List<OperationStat> = emptyList(),
    val totalAllTime: Int = 0        // всего самокатов за всё время
)

data class EmployeeProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userProfile: UserProfile = UserProfile(),
    val activityHistory: List<UserActivityLog> = emptyList(),
    val performanceDetails: DevicePerformanceDetails = DevicePerformanceDetails(),
    // [НОВОЕ]
    val interactionStats: InteractionStats = InteractionStats(),
    val isStatsLoading: Boolean = true
)