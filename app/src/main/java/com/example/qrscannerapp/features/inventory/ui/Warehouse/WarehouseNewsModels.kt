// Полное содержимое для НОВОГО файла WarehouseNewsModels.kt

package com.example.qrscannerapp.features.inventory.data

import androidx.compose.ui.graphics.Color
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSuccess
import com.google.firebase.firestore.Exclude

/**
 * Enum для категорий (тегов) новостей.
 * Хранит в себе отображаемое имя и соответствующий цвет.
 * В Firestore будет храниться как строка (TODAY, TOMORROW, URGENT).
 */
enum class NewsTag(val displayName: String, val color: Color) {
    TODAY("Сегодня", StardustPrimary),
    TOMORROW("Завтра", StardustSuccess),
    URGENT("Срочно", StardustError)
}

/**
 * Модель данных для новости на складе.
 * Соответствует документу в коллекции "warehouse_news" в Firestore.
 */
data class NewsItem(
    @get:Exclude var id: String = "",
    val title: String = "",
    val content: String = "",
    val tag: NewsTag = NewsTag.TODAY
) {
    // ВАЖНО: Пустой конструктор необходим для Firebase
    constructor() : this(id = "")
}