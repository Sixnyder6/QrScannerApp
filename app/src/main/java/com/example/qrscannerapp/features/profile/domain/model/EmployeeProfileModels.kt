// Файл: features/profile/domain/model/EmployeeProfileModels.kt
package com.example.qrscannerapp.features.profile.domain.model

/**
 * Класс производительности устройства на основе общего объема RAM.
 */
enum class PerformanceClass {
    LOW,     // Низкая (Ведро)
    MEDIUM,  // Средняя (Рабочая лошадка)
    HIGH,    // Высокая (Ракета)
    UNKNOWN  // Неизвестно (Ошибка)
}

/**
 * Детализированная информация о производительности устройства.
 */
data class DevicePerformanceDetails(
    val performanceClass: PerformanceClass = PerformanceClass.UNKNOWN,
    val totalRamGb: Double = 0.0
)

// Data class для личных данных пользователя
data class UserProfile(
    val name: String = "Загрузка...",
    val username: String = "",
    val role: String = "",
    val age: Int = 0,
    val deviceInfo: String = "",
    val appVersion: String = "",
    val lastBatteryLevel: Int = -1
)

/**
 * Data class для одной записи в истории активности.
 * Включает в себя метрики сессии и полную телеметрию устройства на тот момент.
 */
data class UserActivityLog(
    val id: String = "",
    val timestamp: Long = 0,
    val activityType: String = "UNKNOWN", // Тип действия: "SESSION_SAVED", "COPY_ALL"
    // Метрики сессии
    val itemCount: Int = 0,
    val manualEntryCount: Int = 0,
    val durationSeconds: Long = 0,
    // Телеметрия устройства
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
    // Добавляем поле для общего RAM, которое может приходить из логов
    val totalRamInGb: Double? = null
)

// Data class для общего состояния экрана
data class EmployeeProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userProfile: UserProfile = UserProfile(),
    val activityHistory: List<UserActivityLog> = emptyList(),
    // Новый контейнер для данных о производительности
    val performanceDetails: DevicePerformanceDetails = DevicePerformanceDetails()
)