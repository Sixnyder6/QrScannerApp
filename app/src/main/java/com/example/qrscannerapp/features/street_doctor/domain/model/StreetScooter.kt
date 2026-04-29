package com.example.qrscannerapp.features.street_doctor.domain.model

data class StreetScooter(
    val id: String = "",
    val code: String = "",
    val status: ScooterFieldStatus = ScooterFieldStatus.NEW,
    val distanceMeters: Int = 0,
    val batteryPct: Int = 0,
    val address: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val model: String = "",
    val mileage: Int = 0,
    val region: String = "",
    val vin: String = "",
    val isUnlocked: Boolean = false,
    val workerName: String? = null,
    val workerInitials: String? = null,
    val workerRole: String? = null,
    val workStartTime: Long? = null,
    val isMine: Boolean = false,
)

enum class ScooterFieldStatus {
    NEW,
    IN_PROGRESS,
    DONE,
    NOT_FOUND,
    TO_STORAGE
}