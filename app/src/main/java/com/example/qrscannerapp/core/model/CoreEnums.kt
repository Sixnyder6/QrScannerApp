package com.example.qrscannerapp.core.model

enum class ActiveTab {
    SCOOTERS,
    WAREHOUSE,
    BATTERIES,
    NEW_BATTERIES  // Новые АКБ (5BB...)
}

enum class SessionType { SCOOTERS, BATTERIES, NEW_BATTERIES }

sealed interface ScanEvent { object Success : ScanEvent; object Duplicate : ScanEvent }
sealed interface UiEffect { object ScanSuccess : UiEffect; object SessionSaved : UiEffect }