package com.example.qrscannerapp.features.tasks.data.local.converter

import androidx.room.TypeConverter
import com.example.qrscannerapp.features.tasks.domain.model.TaskStep // <-- ИСПРАВЛЕННЫЙ ИМПОРТ
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
/**
 * TypeConverter для Room, который преобразует List<TaskStep> в JSON-строку и обратно.
 * Это необходимо, так как Room не может хранить сложные типы данных напрямую.
 */
class TaskConverters {

    private val gson = Gson()

    /**
     * Преобразует список шагов [TaskStep] в строку JSON для сохранения в базе данных.
     */
    @TypeConverter
    fun fromTaskStepList(steps: List<TaskStep>): String {
        return gson.toJson(steps)
    }

    /**
     * Преобразует строку JSON из базы данных обратно в список шагов [TaskStep].
     */
    @TypeConverter
    fun toTaskStepList(stepsJson: String): List<TaskStep> {
        // Создаем токен типа, чтобы Gson понял, что мы десериализуем именно список TaskStep
        val listType = object : TypeToken<List<TaskStep>>() {}.type
        return gson.fromJson(stepsJson, listType)
    }
}