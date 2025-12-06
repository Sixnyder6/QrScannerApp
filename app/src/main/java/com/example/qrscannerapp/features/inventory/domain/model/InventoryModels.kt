package com.example.qrscannerapp.features.inventory.domain.model

import java.util.UUID

// Модель для отсканированной запчасти на складе
data class SparePartItem(
    val id: String = UUID.randomUUID().toString(), // Уникальный ID для работы в списках
    val code: String,       // Код из 1С (то, что сканируем)
    val name: String,       // Расшифрованное название (пока будет заглушка)
    var quantity: Int,      // Количество
    val imageUrl: String? = null // Опционально: ссылка на фото для будущего UI
)