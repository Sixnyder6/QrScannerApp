package com.example.qrscannerapp.features.delivery.domain.model

import java.util.UUID

enum class DeliveryType(val title: String) {
    EXPECTED("Ожидается"), // НОВЫЙ СТАТУС
    RECEIVE("Принято"),
    SEND("Отправлено")
}

data class DeliveryLog(
    val id: String = UUID.randomUUID().toString(),
    val type: DeliveryType = DeliveryType.RECEIVE,
    val licensePlate: String = "",
    val itemCount: Int = 0,
    val description: String = "",
    val photoUrls: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val plannedDate: Long? = null,                 // НОВОЕ: Дата когда ожидается груз
    val employeeName: String = ""
)