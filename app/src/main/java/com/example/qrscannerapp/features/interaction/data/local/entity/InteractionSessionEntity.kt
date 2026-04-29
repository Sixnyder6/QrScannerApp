package com.example.qrscannerapp.features.interaction.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interaction_sessions")
data class InteractionSessionEntity(
    @PrimaryKey val id: String,
    val operationType: String, // Храним Enum как String (WASHING, SANDING и тд)
    val creatorId: String,
    val creatorName: String,
    val timestamp: Long,
    val scooterCount: Int,
    val scooterCodes: List<String>, // <-- Оставили List<String>, так как у нас есть глобальный конвертер!
    val isSynced: Boolean
)