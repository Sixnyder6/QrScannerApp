package com.example.qrscannerapp.features.interaction.domain.model

data class BatteryIssuance(
    val id: String = "",
    val batteryCodes: List<String> = emptyList(),
    val reanimatorCount: Int = 0,
    val photoUrl: String? = null,
    val comment: String = "",
    val issuedById: String = "",
    val issuedByName: String = "",
    val issuedByRole: String = "",
    val issuedToId: String = "",
    val issuedToName: String = "",
    val timestamp: Long = 0L,
    val isActive: Boolean = true
)

data class SbEmployee(
    val id: String,
    val displayName: String
)

data class BatteryReception(
    val id: String = "",
    val batteryCodes: List<String> = emptyList(),
    val scooterCodes: List<String> = emptyList(),
    val reanimatorCount: Int = 0,
    val photoUrl: String? = null,
    val comment: String = "",
    val receivedById: String = "",
    val receivedByName: String = "",
    val receivedFromId: String = "",
    val receivedFromName: String = "",
    val timestamp: Long = 0L
)

// Kept for Room backward compatibility (table still exists in DB schema)
enum class OperationType(val displayName: String) {
    WASHING("Мойка"), SANDING("Шлифовка"), DECALING("Оклейка"), BATTERY_SWAP("Перезарядка АКБ");
    companion object { fun fromString(value: String) = entries.find { it.name == value } ?: WASHING }
}

data class InteractionSession(
    val id: String, val operationType: OperationType, val creatorId: String,
    val creatorName: String, val timestamp: Long, val scooterCount: Int,
    val scooterCodes: List<String>, val isSynced: Boolean = false
)