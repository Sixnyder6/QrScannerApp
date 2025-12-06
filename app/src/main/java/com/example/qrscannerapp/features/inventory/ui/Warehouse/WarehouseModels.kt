// Полное содержимое для ИСПРАВЛЕННОГО файла WarehouseModels.kt

package com.example.qrscannerapp.features.inventory.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
// --- УДАЛЕНО --- PropertyName больше не нужен
// import com.google.firebase.firestore.PropertyName

/**
 * Основная модель товара на складе.
 * Соответствует документу в коллекции "warehouse_items" в Firestore.
 * Все названия полей теперь будут в camelCase (например, "fullName").
 */
data class WarehouseItem(
    @get:Exclude var id: String = "",

    // --- УДАЛЕНО --- @get:PropertyName("full_name")
    val fullName: String = "",

    // --- УДАЛЕНО --- @get:PropertyName("short_name")
    val shortName: String = "",

    val sku: String? = null,

    val category: String = "Общее",
    val unit: String = "шт.",

    // --- Количественные показатели ---

    // --- УДАЛЕНО --- @get:PropertyName("stock_count")
    val stockCount: Int = 0,

    // --- УДАЛЕНО --- @get:PropertyName("total_stock")
    val totalStock: Int = 0,

    // --- УДАЛЕНО --- @get:PropertyName("low_stock_threshold")
    val lowStockThreshold: Int = 10,

    // --- Медиа и Поиск ---

    // --- УДАЛЕНО --- @get:PropertyName("image_url")
    val imageUrl: String? = null,

    val keywords: List<String> = emptyList()
) {
    // ВАЖНО: Пустой конструктор необходим для Firebase
    constructor() : this(id = "")
}

/**
 * Модель записи в журнале операций.
 * Соответствует документу в коллекции "warehouse_logs".
 */
data class WarehouseLog(
    @get:Exclude var id: String = "",

    // --- УДАЛЕНО --- @get:PropertyName("item_id")
    val itemId: String = "",

    // --- УДАЛЕНО --- @get:PropertyName("item_name")
    val itemName: String = "",

    // --- УДАЛЕНО --- @get:PropertyName("user_name")
    val userName: String = "Неизвестный",

    // --- УДАЛЕНО --- @get:PropertyName("quantity_change")
    val quantityChange: Int = 0,

    val timestamp: Timestamp = Timestamp.now()
) {
    // ВАЖНО: Пустой конструктор необходим для Firebase
    constructor() : this(id = "")
}