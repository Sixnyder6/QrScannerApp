// BatteryRepairLogEntity.kt (в features/electrician/data/local/entity)

package com.example.qrscannerapp.features.electrician.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
// Импорты, которые нужны BatteryRepairLogLocal
import com.example.qrscannerapp.features.electrician.domain.model.BatteryRepairLog // Нужен, если он используется ниже
// ...

/**
Entity-класс для хранения лога ремонта, который ожидает отправки в Firebase.
 */
@Entity(tableName = "pending_repair_logs")
data class BatteryRepairLogEntity( // <-- ПЕРЕИМЕНОВАН
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    val batteryId: String,
    val electricianId: String,
    val electricianName: String,
    val timestamp: Long,
    val repairs: List<String>,
    val manufacturer: String
)