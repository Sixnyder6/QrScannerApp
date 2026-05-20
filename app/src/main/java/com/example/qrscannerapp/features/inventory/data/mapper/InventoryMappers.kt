package com.example.qrscannerapp.features.inventory.data.mapper

import com.example.qrscannerapp.CellOperation
import com.example.qrscannerapp.StorageCell
import com.example.qrscannerapp.features.inventory.data.local.entity.StorageCellEntity
import com.example.qrscannerapp.features.inventory.data.local.entity.StoragePalletEntity
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet
import org.json.JSONArray
import org.json.JSONObject

// ============================================================================================
// CellOperation <-> JSON
// ============================================================================================

fun CellOperation.toJson(): String {
    val json = JSONObject()
    json.put("id", id)
    json.put("timestamp", timestamp)
    json.put("userId", userId)
    json.put("userName", userName)
    json.put("action", action)
    json.put("details", details)
    json.put("itemCount", itemCount)
    return json.toString()
}

fun String.toCellOperation(): CellOperation? {
    return try {
        val json = JSONObject(this)
        CellOperation(
            id = json.optString("id", ""),
            timestamp = json.optLong("timestamp", 0L),
            userId = json.optString("userId", ""),
            userName = json.optString("userName", ""),
            action = json.optString("action", ""),
            details = json.optString("details", ""),
            itemCount = json.optInt("itemCount", 0)
        )
    } catch (e: Exception) {
        null
    }
}

// ============================================================================================
// StorageCell Mappers
// ============================================================================================

fun Map<String, List<String>>.toJson(): String {
    val obj = JSONObject()
    forEach { (key, dirs) ->
        val arr = JSONArray()
        dirs.forEach { arr.put(it) }
        obj.put(key, arr)
    }
    return obj.toString()
}

fun String.toStickerDirectionsMap(): Map<String, List<String>> {
    return try {
        val obj = JSONObject(this)
        val result = mutableMapOf<String, List<String>>()
        obj.keys().forEach { key ->
            val arr = obj.getJSONArray(key)
            val list = (0 until arr.length()).map { arr.getString(it) }
            result[key] = list
        }
        result
    } catch (e: Exception) {
        emptyMap()
    }
}

fun StorageCell.toEntity(): StorageCellEntity {
    return StorageCellEntity(
        id = this.id,
        cellNumber = this.cellNumber,
        description = this.description,
        capacity = this.capacity,
        items = this.items,
        createdByName = this.createdByName,
        createdBy = this.createdBy,
        createdByRole = this.createdByRole,
        createdAt = this.createdAt?.time,
        operations = this.operations.map { it.toJson() },
        stickerDirections = this.stickerDirections?.toJson(),
        isDirty = false
    )
}

fun StorageCellEntity.toDomain(): StorageCell {
    return StorageCell(
        id = this.id,
        cellNumber = this.cellNumber,
        description = this.description,
        capacity = this.capacity,
        items = this.items,
        createdByName = this.createdByName,
        createdBy = this.createdBy,
        createdByRole = this.createdByRole,
        createdAt = this.createdAt?.let { java.util.Date(it) },
        operations = this.operations.mapNotNull { it.toCellOperation() },
        stickerDirections = this.stickerDirections?.toStickerDirectionsMap()
    )
}

// ============================================================================================
// StoragePallet Mappers
// ============================================================================================

fun StoragePallet.toEntity(): StoragePalletEntity {
    return StoragePalletEntity(
        id = this.id,
        palletNumber = this.palletNumber,
        items = this.items,
        isFull = this.isFull,
        manufacturer = this.manufacturer,
        isDirty = false
    )
}

fun StoragePalletEntity.toDomain(): StoragePallet {
    return StoragePallet(
        id = this.id,
        palletNumber = this.palletNumber,
        items = this.items,
        isFull = this.isFull,
        manufacturer = this.manufacturer
    )
}