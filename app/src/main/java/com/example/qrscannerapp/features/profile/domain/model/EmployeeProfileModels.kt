package com.example.qrscannerapp.features.profile.domain.model
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
Data class для одной записи в истории активности.
Включает в себя метрики сессии и полную телеметрию устройства на тот момент.
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
// --- НАЧАЛО ДОБАВЛЕНИЙ: Новые поля телеметрии ---
    val batteryHealth: String = "N/A",
    val isPowerSaveMode: Boolean = false,
    val networkPing: String = "N/A"
// --- КОНЕЦ ДОБАВЛЕНИЙ ---
)
// Data class для общего состояния экрана
data class EmployeeProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userProfile: UserProfile = UserProfile(),
// По-прежнему храним список, но в нем всегда будет 0 или 1 элемент
    val activityHistory: List<UserActivityLog> = emptyList()
)