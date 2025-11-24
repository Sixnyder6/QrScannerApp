package com.example.qrscannerapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// --- НАЧАЛО ИСПРАВЛЕНИЙ: Добавлены недостающие импорты ---
import com.example.qrscannerapp.core.model.SessionType
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem
// --- КОНЕЦ ИСПРАВЛЕНИЙ ---


// --- ТОЛЬКО СУЩНОСТЬ СЕССИИ СКАНИРОВАНИЯ ОСТАЕТСЯ ЗДЕСЬ ---
/**
Entity-класс для хранения сессии сканирования, которая ожидает отправки в Firebase.
Используется в локальной базе данных Room для оффлайн-режима.
 */
@Entity(tableName = "pending_scan_sessions")
data class ScanSessionLocal(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
// Включаем все поля из нашего основного класса ScanSession
    val id: String, // UUID, который будет использоваться в Firestore
    val name: String?,
    val timestamp: Long,
    val items: List<ScanItem>, // Теперь этот тип известен
    val type: SessionType,     // Теперь этот тип известен
    val creatorId: String?,
    val creatorName: String?
)
// --- ТОЛЬКО КОНВЕРТЕРЫ СЕССИИ СКАНИРОВАНИЯ ОСТАЮТСЯ ЗДЕСЬ ---
/**
Класс-конвертер для Room.
Предоставляет методы для преобразования сложных типов (например, List<String>)
в примитивные типы, которые Room может хранить в базе данных.
 */
class Converters {

    private val gson = Gson()
    /**
    Преобразует список [ScanItem] в JSON-строку для хранения в базе данных.
     */
    @TypeConverter
    fun fromScanItemList(items: List<ScanItem>?): String {
        return gson.toJson(items)
    }
    /**
    Преобразует JSON-строку из базы данных обратно в список [ScanItem].
     */
    @TypeConverter
    fun toScanItemList(json: String?): List<ScanItem> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        val type = object : TypeToken<List<ScanItem>>() {}.type
        return gson.fromJson(json, type)
    }
    /**
    Преобразует enum [SessionType] в его строковое представление для хранения.
     */
    @TypeConverter
    fun fromSessionType(type: SessionType?): String {
        return type?.name ?: SessionType.SCOOTERS.name
    }
    /**
    Преобразует строку из базы данных обратно в enum [SessionType].
     */
    @TypeConverter
    fun toSessionType(name: String?): SessionType {
        return try {
            if (name == null) SessionType.SCOOTERS else SessionType.valueOf(name)
        } catch (e: IllegalArgumentException) {
            SessionType.SCOOTERS // Значение по умолчанию в случае ошибки
        }
    }
}