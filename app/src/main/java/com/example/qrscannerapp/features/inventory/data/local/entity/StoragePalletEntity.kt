// Path: app/src/main/java/com/example/qrscannerapp/features/inventory/data/local/entity/StoragePalletEntity.kt

package com.example.qrscannerapp.features.inventory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_pallets")
data class StoragePalletEntity(
    @PrimaryKey
    val id: String,
    val palletNumber: Int,
    val items: List<String>, // Будет храниться как JSON с помощью TypeConverter
    val isFull: Boolean,

    // Новые поля для задачи
    val manufacturer: String?, // Производитель АКБ (FUJIAN, BYD и т.д.)
    val isDirty: Boolean = false // Флаг для синхронизации с Firestore
)