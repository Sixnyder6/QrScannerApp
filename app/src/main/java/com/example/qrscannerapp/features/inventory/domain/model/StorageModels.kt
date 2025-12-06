// Path: app/src/main/java/com/example/qrscannerapp/features/inventory/domain/model/StorageModels.kt

package com.example.qrscannerapp.features.inventory.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

/**
 * Модель данных для одного палета, как она хранится в Firestore.
 */
data class StoragePallet(
    val id: String = UUID.randomUUID().toString(),
    val palletNumber: Int = 0,
    val items: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null,
    val isFull: Boolean = false,
    val manufacturer: String? = null // Производитель (BYD/FUJIAN)
) {
    // Пустой конструктор для Firebase десериализации
    constructor() : this("", 0, emptyList(), null, false, null)
}

/**
 * Модель данных, описывающая состояние UI для экрана распределения по палетам.
 */
data class PalletDistributionUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val pallets: List<StoragePallet> = emptyList(),
    val undistributedItemCount: Int = 0,
    val isDistributing: Boolean = false,
    val distributionResult: String? = null,
    val activityLog: List<PalletActivityLogEntry> = emptyList()
)

/**
 * Модель данных для записи активности по палетам (для истории).
 */
data class PalletActivityLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val userName: String? = null,
    val action: String,
    val palletNumber: Int? = null,
    val itemCount: Int? = null,
    val palletId: String? = null
) {
    // Пустой конструктор для Firebase десериализации
    constructor() : this("", 0, null, null, "", null, null, null)
}