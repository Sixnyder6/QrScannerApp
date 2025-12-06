package com.example.qrscannerapp.core.model

enum class ActiveTab {
    SCOOTERS,
    WAREHOUSE, // <-- ИЗМЕНЕНИЕ
    BATTERIES
}

enum class SessionType { SCOOTERS, BATTERIES } // Пока не трогаем, для склада может понадобиться своя логика сессий

sealed interface ScanEvent { object Success : ScanEvent; object Duplicate : ScanEvent }
sealed interface UiEffect { object ScanSuccess : UiEffect; object SessionSaved : UiEffect }