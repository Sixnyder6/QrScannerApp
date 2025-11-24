// ScanSessionEntity.kt (в features/scanner/data/local/entity)

package com.example.qrscannerapp.features.scanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters // <-- НОВЫЙ ИМПОРТ
import com.example.qrscannerapp.core.model.SessionType
import com.example.qrscannerapp.features.scanner.data.local.converter.ScanConverters // <-- НОВЫЙ ИМПОРТ
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem

@Entity(tableName = "pending_scan_sessions")
@TypeConverters(ScanConverters::class) // <-- НОВАЯ АННОТАЦИЯ
data class ScanSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    val id: String,
    val name: String?,
    val timestamp: Long,
    val items: List<ScanItem>,
    val type: SessionType,
    val creatorId: String?,
    val creatorName: String?
)