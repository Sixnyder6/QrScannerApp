// Полная версия нового файла: features/vehicle_report/data/local/converters/StringListConverter.kt

package com.example.qrscannerapp.features.vehicle_report.data.local.converters

import androidx.room.TypeConverter

class StringListConverter {
    // Используем сложный разделитель, чтобы случайно не встретить его в номерах самокатов
    private val separator = "<-_-_->"

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString(separator)
    }

    @TypeConverter
    fun toStringList(data: String): List<String> {
        return if (data.isBlank()) {
            emptyList()
        } else {
            data.split(separator)
        }
    }
}