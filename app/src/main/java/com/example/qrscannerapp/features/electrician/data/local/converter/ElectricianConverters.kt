// ElectricianConverters.kt (в features/electrician/data/local/converter)

package com.example.qrscannerapp.features.electrician.data.local.converter

import androidx.room.TypeConverter
import com.example.qrscannerapp.features.electrician.domain.model.RepairType // Возможно, понадобится

/**
Конвертер для фичи Electrician.
Отвечает за преобразование List<String> (ремонты) в строку.
 */
class ElectricianConverters {

    private val separator = ","

    @TypeConverter
    fun fromRepairsList(repairs: List<String>?): String {
        return repairs?.joinToString(separator) ?: ""
    }

    @TypeConverter
    fun toRepairsList(repairsString: String?): List<String> {
        if (repairsString.isNullOrBlank()) {
            return emptyList()
        }
        return repairsString.split(separator)
    }
}