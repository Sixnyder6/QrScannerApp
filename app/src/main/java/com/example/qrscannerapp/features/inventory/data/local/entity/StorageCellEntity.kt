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
    val items: List<String>,
    val createdByName: String?,
    val createdBy: String? = null,
    val createdByRole: String? = null,      // <<< НОВОЕ: роль создателя
    val createdAt: Long? = null,
    val operations: List<String> = emptyList(), // <<< НОВОЕ: JSON-строки CellOperation

    val isDirty: Boolean = false
) {
    val name: String
        get() = "Ячейка $cellNumber"
}