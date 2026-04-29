package com.example.qrscannerapp.features.interaction.data.mapper

import com.example.qrscannerapp.features.interaction.data.local.entity.InteractionSessionEntity
import com.example.qrscannerapp.features.interaction.domain.model.InteractionSession
import com.example.qrscannerapp.features.interaction.domain.model.OperationType

// Из доменной модели в модель базы данных (Room)
fun InteractionSession.toEntity(): InteractionSessionEntity {
    return InteractionSessionEntity(
        id = this.id,
        operationType = this.operationType.name, // Сохраняем как текст ("WASHING")
        creatorId = this.creatorId,
        creatorName = this.creatorName,
        timestamp = this.timestamp,
        scooterCount = this.scooterCount,
        scooterCodes = this.scooterCodes,
        isSynced = this.isSynced
    )
}

// Из базы данных (Room) обратно в доменную модель (для UI)
fun InteractionSessionEntity.toDomainModel(): InteractionSession {
    return InteractionSession(
        id = this.id,
        operationType = OperationType.fromString(this.operationType),
        creatorId = this.creatorId,
        creatorName = this.creatorName,
        timestamp = this.timestamp,
        scooterCount = this.scooterCount,
        scooterCodes = this.scooterCodes,
        isSynced = this.isSynced
    )
}