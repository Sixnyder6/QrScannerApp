package com.example.qrscannerapp.features.street_doctor.domain.model

data class RepairJob(
    val id: String = "",
    val scooterCode: String = "",
    val technicianId: String = "",
    val technicianName: String = "",
    val repairTypes: List<String> = emptyList(),  // "brakes", "wheel", "battery" и т.д.
    val batteryPct: Int = 0,
    val status: ScooterFieldStatus = ScooterFieldStatus.DONE,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)