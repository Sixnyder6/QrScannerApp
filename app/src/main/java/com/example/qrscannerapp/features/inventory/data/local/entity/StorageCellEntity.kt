// Path: app/src/main/java/com/example/qrscannerapp/features/inventory/data/local/entity/StorageCellEntity.kt

package com.example.qrscannerapp.features.inventory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_cells")
data class StorageCellEntity(
    @PrimaryKey
    val id: String,
    val cellNumber: Int,
    val description: String,
    val capacity: Int,
    val items: List<String>, // Будет храниться как JSON с помощью TypeConverter
    val createdByName: String?,

    // Флаг для синхронизации
    val isDirty: Boolean = false
) {
    // Вспомогательное свойство для UI, как и в оригинальной модели
    val name: String
        get() = "Ячейка $cellNumber"
}