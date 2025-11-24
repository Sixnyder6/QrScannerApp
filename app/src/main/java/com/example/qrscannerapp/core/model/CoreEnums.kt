package com.example.qrscannerapp.core.model

enum class ActiveTab { SCOOTERS, BATTERIES }
enum class SessionType { SCOOTERS, BATTERIES }
sealed interface ScanEvent { object Success : ScanEvent; object Duplicate : ScanEvent }
sealed interface UiEffect { object ScanSuccess : UiEffect; object SessionSaved : UiEffect }