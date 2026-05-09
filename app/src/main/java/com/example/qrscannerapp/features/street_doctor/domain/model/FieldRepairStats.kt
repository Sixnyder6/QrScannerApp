package com.example.qrscannerapp.features.street_doctor.domain.model

data class FieldRepairStats(
    val doneToday: Int = 0,
    val remainingToday: Int = 0,
    val totalToday: Int = 0,
    val doneAllTime: Int = 0,
    val totalAllTime: Int = 0,
    val avgMinutesPerScooter: Int = 0
)