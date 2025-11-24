// Path: app/src/main/java/com/example/qrscannerapp/features/inventory/data/mapper/InventoryMappers.kt

package com.example.qrscannerapp.features.inventory.data.mapper

import com.example.qrscannerapp.StorageCell
import com.example.qrscannerapp.features.inventory.data.local.entity.StorageCellEntity
import com.example.qrscannerapp.features.inventory.data.local.entity.StoragePalletEntity
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet

// --- StorageCell Mappers ---

fun StorageCell.toEntity(): StorageCellEntity {
    return StorageCellEntity(
        id = this.id,
        cellNumber = this.cellNumber,
        description = this.description,
        capacity = this.capacity,
        items = this.items,
        createdByName = this.createdByName,
        isDirty = false // При получении с сервера флаг всегда чистый
    )
}

fun StorageCellEntity.toDomain(): StorageCell {
    return StorageCell(
        id = this.id,
        cellNumber = this.cellNumber,
        description = this.description,
        capacity = this.capacity,
        items = this.items,
        createdByName = this.createdByName
        // createdAt и createdBy опускаем, т.к. они важны в основном для Firestore
    )
}

// --- StoragePallet Mappers ---

fun StoragePallet.toEntity(): StoragePalletEntity {
    return StoragePalletEntity(
        id = this.id,
        palletNumber = this.palletNumber,
        items = this.items,
        isFull = this.isFull,
        manufacturer = this.manufacturer, // Теперь маппим и новое поле
        isDirty = false
    )
}

fun StoragePalletEntity.toDomain(): StoragePallet {
    return StoragePallet(
        id = this.id,
        palletNumber = this.palletNumber,
        items = this.items,
        isFull = this.isFull,
        manufacturer = this.manufacturer // И здесь тоже
    )
}