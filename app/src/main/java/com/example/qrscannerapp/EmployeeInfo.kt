package com.example.qrscannerapp

/**
 * Три состояния присутствия сотрудника:
 *  ONLINE   — lastSeen < 3 мин   🟢
 *  AWAY     — lastSeen 3–15 мин  🟡
 *  OFFLINE  — lastSeen > 15 мин  🔴
 */
enum class PresenceState {
    ONLINE, AWAY, OFFLINE
}

data class EmployeeInfo(
    val id: String = "",
    val name: String = "",
    val role: String = "",
    val status: String = "offline",
    val lastSeen: Long = 0L,
    val warehouseId: String = "bestuzhevskaya_10",
    val displayName: String = "",
    val shiftRequestStatus: String = "NONE",
    val isAllowedToWork: Boolean = false,
    val appVersion: String? = null,
    val lastAppUpdate: Long = 0L,
    val photoUrl: String? = null
) {
    companion object {
        private const val ONLINE_THRESHOLD_MS  = 3  * 60 * 1000L   // 3 мин
        private const val AWAY_THRESHOLD_MS    = 15 * 60 * 1000L   // 15 мин
    }

    /** Три состояния присутствия на основе lastSeen */
    val presenceState: PresenceState
        get() {
            if (lastSeen <= 0L) return PresenceState.OFFLINE
            val elapsed = System.currentTimeMillis() - lastSeen
            return when {
                elapsed < ONLINE_THRESHOLD_MS -> PresenceState.ONLINE
                elapsed < AWAY_THRESHOLD_MS   -> PresenceState.AWAY
                else                           -> PresenceState.OFFLINE
            }
        }

    /** Обратная совместимость — всё что не OFFLINE считается онлайн */
    val isOnline: Boolean
        get() = presenceState != PresenceState.OFFLINE

    /** Цвет индикатора присутствия */
    val presenceColor: Long
        get() = when (presenceState) {
            PresenceState.ONLINE  -> 0xFF4CAF50L  // зелёный
            PresenceState.AWAY    -> 0xFFFFCA28L  // жёлтый
            PresenceState.OFFLINE -> 0xFF607D8BL  // серый
        }

    /** Текст для UI */
    val presenceLabel: String
        get() = when (presenceState) {
            PresenceState.ONLINE  -> "онлайн"
            PresenceState.AWAY    -> "неактивен"
            PresenceState.OFFLINE -> "офлайн"
        }
}