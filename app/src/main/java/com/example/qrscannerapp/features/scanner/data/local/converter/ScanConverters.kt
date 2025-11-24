// ScanConverters.kt (в features/scanner/data/local/converter)

package com.example.qrscannerapp.features.scanner.data.local.converter

import androidx.room.TypeConverter
import com.example.qrscannerapp.core.model.SessionType
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScanConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromScanItemList(items: List<ScanItem>?): String {
        return gson.toJson(items)
    }

    @TypeConverter
    fun toScanItemList(json: String?): List<ScanItem> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<ScanItem>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromSessionType(type: SessionType?): String {
        return type?.name ?: SessionType.SCOOTERS.name
    }

    @TypeConverter
    fun toSessionType(name: String?): SessionType {
        return try {
            if (name == null) SessionType.SCOOTERS else SessionType.valueOf(name)
        } catch (e: IllegalArgumentException) {
            SessionType.SCOOTERS
        }
    }
}