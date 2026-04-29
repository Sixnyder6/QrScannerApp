package com.example.qrscannerapp.features.inventory.data

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import java.util.UUID

// --- Цвета статусов (Hardcoded для моделей, чтобы не тянуть UI зависимостей лишних) ---
val StatusColorNew = Color(0xFF2196F3)      // Синий (Новый)
val StatusColorProcess = Color(0xFFFF9800)  // Оранжевый (В сборке)
val StatusColorReady = Color(0xFF4CAF50)    // Зеленый (Готов)
val StatusColorCompleted = Color(0xFF9E9E9E)// Серый (Выдан)

/**
 * Основная модель товара на складе.
 */
data class WarehouseItem(
    @get:Exclude var id: String = "",
    val fullName: String = "",
    val shortName: String = "",
    val sku: String? = null,
    val category: String = "Общее",
    val unit: String = "шт.",
    val stockCount: Int = 0,
    val totalStock: Int = 0,
    val lowStockThreshold: Int = 10,
    val imageUrl: String? = null,
    val keywords: List<String> = emptyList()
) {
    constructor() : this(id = "")
}

/**
 * Модель записи в журнале операций.
 */
data class WarehouseLog(
    @get:Exclude var id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val userName: String = "Неизвестный",
    val quantityChange: Int = 0, // Отрицательное - взяли, Положительное - добавили
    val timestamp: Timestamp = Timestamp.now()
) {
    constructor() : this(id = "")
}

// ==========================================
// НОВЫЕ МОДЕЛИ ДЛЯ СИСТЕМЫ ЗАКАЗОВ
// ==========================================

enum class OrderStatus(val displayName: String, val color: Color, val description: String) {
    CREATED("Новый", StatusColorNew, "Ожидает подтверждения кладовщиком"),
    PROCESSING("В сборке", StatusColorProcess, "Кладовщик собирает заказ"),
    READY("Готов к выдаче", StatusColorReady, "Можно забирать на складе"),
    COMPLETED("Выдан", StatusColorCompleted, "Заказ завершен и списан"),
    CANCELLED("Отменен", Color.Red, "Заказ отменен")
}

/**
 * Элемент внутри заказа (какой товар и сколько)
 */
data class OrderItem(
    val itemId: String = "",
    val itemName: String = "",
    val itemImageUrl: String? = null,
    val quantity: Int = 0,
    val unit: String = "шт."
) {
    // Пустой конструктор для Firebase
    constructor() : this(itemId = "")
}

/**
 * Сам заказ
 */
data class WarehouseOrder(
    @get:Exclude var id: String = "",
    val userId: String = "",         // ID того, кто заказал
    val userName: String = "",       // Имя того, кто заказал
    val userRole: String = "",       // Роль заказчика (Техник/Электрик)
    val items: List<OrderItem> = emptyList(),
    val status: OrderStatus = OrderStatus.CREATED,
    val createdAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null // Когда был выдан
) {
    constructor() : this(id = "")

    @get:Exclude
    val totalItemsCount: Int
        get() = items.sumOf { it.quantity }
}