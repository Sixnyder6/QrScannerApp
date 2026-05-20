package com.example.qrscannerapp

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

/**
 * Одна операция внутри ячейки — кто, когда, что сделал.
 * Хранится как вложенный список в документе ячейки в Firestore.
 */
data class CellOperation(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val userName: String = "",
    val action: String = "",   // "CREATED", "EDITED", "ITEMS_ADDED", "ITEM_REMOVED", "BULK_ADDED"
    val details: String = "",  // "Добавил 50 самокатов", "Удалил 00544745"
    val itemCount: Int = 0     // Количество затронутых элементов (для быстрых подсчётов)
) {
    constructor() : this("", 0L, "", "", "", "", 0)
}

/**
 * Модель данных для одной ячейки хранения, как она будет храниться в Firestore.
 * Название "Ячейка N" генерируется автоматически и не редактируется.
 */
data class StorageCell(
    val id: String = UUID.randomUUID().toString(),
    val cellNumber: Int = 0,
    var description: String = "",
    var capacity: Int = 700,
    val items: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Date? = null,
    val createdBy: String? = null,
    val createdByName: String? = null,
    val createdByRole: String? = null,   // "admin" / "worker"
    val operations: List<CellOperation> = emptyList(),
    val stickerDirections: Map<String, List<String>>? = null
) {
    constructor() : this("", 0, "", 700, emptyList(), null, null, null, null, emptyList(), null)

    val name: String
        get() = "Ячейка $cellNumber"
}

/**
 * Модель для отслеживания статуса каждого самоката в отдельной коллекции 'scooters'.
 */
data class ScooterStatus(
    val status: String = "available",
    val cellId: String? = null,
    @ServerTimestamp val lastUpdate: Date? = null
) {
    constructor() : this("available", null, null)
}

/**
 * Глобальный лог активности (коллекция 'storage_activity_log').
 * Остаётся для общей истории на экране.
 */
data class StorageActivityLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val userName: String = "",
    val action: String = "",
    val details: String = ""
) {
    constructor() : this("", 0L, "", "", "", "")
}