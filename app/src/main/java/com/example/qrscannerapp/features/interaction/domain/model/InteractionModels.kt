package com.example.qrscannerapp.features.interaction.domain.model

// Типы операций, которые мы обсуждали
enum class OperationType(val displayName: String) {
    WASHING("Мойка"),
    SANDING("Шлифовка"),
    DECALING("Оклейка"),
    BATTERY_SWAP("Перезарядка АКБ");

    // Вспомогательная функция, чтобы красиво парсить из базы/строки
    companion object {
        fun fromString(value: String): OperationType {
            return entries.find { it.name == value } ?: WASHING
        }
    }
}

// Главный класс сессии для логики и отправки в Firebase
data class InteractionSession(
    val id: String,
    val operationType: OperationType,
    val creatorId: String,
    val creatorName: String,
    val timestamp: Long,
    val scooterCount: Int,
    val scooterCodes: List<String>,
    val isSynced: Boolean = false // Флаг для Room (отправлено в облако или нет)
)