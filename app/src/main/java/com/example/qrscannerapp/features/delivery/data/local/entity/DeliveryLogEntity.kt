package com.example.qrscannerapp.features.delivery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.qrscannerapp.features.delivery.domain.model.DeliveryType

@Entity(tableName = "delivery_logs")
data class DeliveryLogEntity(
    @PrimaryKey
    val id: String,
    val type: String, // "EXPECTED", "RECEIVE", "SEND"
    val licensePlate: String,
    val itemCount: Int,
    val description: String,
    val photoUrls: String, // JSON array как строка
    val timestamp: Long,
    val plannedDate: Long?,
    val employeeName: String,
    val addressFrom: String?,
    val addressTo: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isSynced: Boolean = false // для будущей синхронизации с сервером
) {
    fun toDomainModel(): com.example.qrscannerapp.features.delivery.domain.model.DeliveryLog {
        val deliveryType = when (type) {
            "RECEIVE" -> DeliveryType.RECEIVE
            "SEND" -> DeliveryType.SEND
            else -> DeliveryType.EXPECTED
        }

        // Парсим JSON фото
        val photosList = try {
            org.json.JSONArray(photoUrls).let { json ->
                List(json.length()) { json.getString(it) }
            }
        } catch (e: Exception) {
            emptyList()
        }

        return com.example.qrscannerapp.features.delivery.domain.model.DeliveryLog(
            id = id,
            type = deliveryType,
            licensePlate = licensePlate,
            itemCount = itemCount,
            description = description,
            photoUrls = photosList,
            timestamp = timestamp,
            plannedDate = plannedDate,
            employeeName = employeeName
        )
    }

    companion object {
        fun fromDomainModel(
            log: com.example.qrscannerapp.features.delivery.domain.model.DeliveryLog,
            addressFrom: String? = null,
            addressTo: String? = null,
            latitude: Double? = null,
            longitude: Double? = null
        ): DeliveryLogEntity {
            // Конвертируем список фото в JSON строку
            val photosJson = org.json.JSONArray(log.photoUrls).toString()

            return DeliveryLogEntity(
                id = log.id,
                type = log.type.name,
                licensePlate = log.licensePlate,
                itemCount = log.itemCount,
                description = log.description,
                photoUrls = photosJson,
                timestamp = log.timestamp,
                plannedDate = log.plannedDate,
                employeeName = log.employeeName,
                addressFrom = addressFrom,
                addressTo = addressTo,
                latitude = latitude,
                longitude = longitude,
                isSynced = true // пока считаем что синхронизировано
            )
        }
    }
}