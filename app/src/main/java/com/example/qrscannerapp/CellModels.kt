// Файл: CellModels.kt
package com.example.qrscannerapp

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

/**
 * Модель данных для одной ячейки хранения, как она будет храниться в Firestore.
 * Название "Ячейка N" генерируется автоматически и не редактируется.
 */
data class StorageCell(
    val id: String = UUID.randomUUID().toString(),
    val cellNumber: Int = 0,            // Порядковый номер: 1, 2, 3...
    var description: String = "",       // Описание, которое вводит и редактирует пользователь
    var capacity: Int = 700,            // Ёмкость, которую вводит и редактирует пользователь
    val items: List<String> = emptyList(), // Список номеров самокатов
    @ServerTimestamp val createdAt: Date? = null,
    val createdBy: String? = null,      // ID пользователя-создателя
    val createdByName: String? = null   // Имя пользователя-создателя
) {
    // Пустой конструктор для Firestore
    constructor() : this("", 0, "", 700, emptyList(), null, null, null)

    // Вспомогательное свойство для отображения в UI ("Ячейка 1", "Ячейка 2"...)
    val name: String
        get() = "Ячейка $cellNumber"
}

/**
 * Модель для отслеживания статуса каждого самоката в отдельной коллекции 'scooters'.
 * ID документа в Firestore будет равен номеру самоката.
 */
data class ScooterStatus(
    val status: String = "available", // "in_storage" (на хранении)
    val cellId: String? = null,       // ID ячейки, где он хранится
    @ServerTimestamp val lastUpdate: Date? = null
) {
    // Пустой конструктор для Firestore
    constructor() : this("available", null, null)
}

/**
 * Модель для записи в "терминал" (журнал активности) для ячеек хранения.
 * Хранится в коллекции 'storage_activity_log'.
 */
data class StorageActivityLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val userName: String = "",
    val action: String = "", // "CREATED", "DELETED", "EDITED", "SCOOTERS_ADDED"
    val details: String = "" // "Создал 'Ячейка 3'", "Добавил 50 самокатов в 'Ячейка 3'"
) {
    // Пустой конструктор для Firestore
    constructor() : this("", 0L, "", "", "", "")
}