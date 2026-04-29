package com.example.qrscannerapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_buffer")
data class TelemetryBuffer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val jsonData: String,
    val isSent: Boolean = false,
    val retryCount: Int = 0
)