package com.example.qrscannerapp.features.delivery.data.local


import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.qrscannerapp.features.delivery.data.local.entity.DeliveryLogEntity
import com.example.qrscannerapp.features.delivery.data.local.dao.DeliveryDao  // ← ДОБАВИТЬ ЭТУ СТРОКУ!

@Database(
    entities = [DeliveryLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DeliveryDatabase : RoomDatabase() {
    abstract fun deliveryDao(): DeliveryDao
}