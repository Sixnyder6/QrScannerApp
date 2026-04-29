package com.example.qrscannerapp.features.shift.domain.model

/**
 * Запись о смене в Firebase (коллекция 'shifts').
 *
 * Жизненный цикл:
 * 1. Юзер нажимает "Начать смену" → status = ACTIVE
 * 2. Юзер завершает сам ИЛИ автоматически через 12ч → status = COMPLETED
 * 3. Админ принудительно завершает → status = FORCE_ENDED
 *
 * После завершения isAllowedToWork НЕ сбрасывается —
 * юзер может начать новую смену без повторного запроса.
 */
data class Shift(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "",           // роль на момент начала смены

    // Время
    val startTime: Long = 0L,
    val endTime: Long? = null,           // null пока смена активна
    val durationMinutes: Int = 0,        // заполняется при завершении

    // Статус
    val status: String = "ACTIVE",       // ACTIVE / COMPLETED / FORCE_ENDED / AUTO_ENDED

    // Статистика за смену
    val scootersScanCount: Int = 0,      // сколько самокатов отсканировал
    val batteriesScanCount: Int = 0,     // сколько АКБ отсканировал
    val totalScanCount: Int = 0,         // общее количество сканов
    val sessionsCreated: Int = 0,        // сколько сессий сохранил
    val cellsModified: Int = 0,          // сколько ячеек изменил (добавлял/удалял)

    // Мета
    val endReason: String? = null        // "manual" / "auto_12h" / "admin_force" / "logout"
) {
    // Пустой конструктор для Firebase
    constructor() : this("", "", "", "", 0L, null, 0, "ACTIVE", 0, 0, 0, 0, 0, null)

    val isActive: Boolean get() = status == "ACTIVE"
}