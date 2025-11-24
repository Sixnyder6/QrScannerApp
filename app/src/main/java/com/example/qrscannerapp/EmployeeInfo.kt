package com.example.qrscannerapp

/**
 * Модель данных, представляющая основную информацию о сотруднике.
 * Используется для отображения в списках и выбора в качестве исполнителя.
 *
 * @param id Уникальный идентификатор пользователя (UID из Firebase Auth).
 * @param name Имя пользователя.
 * @param role Роль пользователя (например, 'admin', 'employee').
 * @param status Текущий статус ('online' или 'offline').
 */
data class EmployeeInfo(
    val id: String = "",
    val name: String = "",
    val role: String = "",
    val status: String = "offline"
)