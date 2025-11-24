// Полная, измененная версия файла: features/tasks/data/local/entity/TaskEntity.kt

package com.example.qrscannerapp.features.tasks.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.qrscannerapp.features.tasks.domain.model.TaskPriority
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import com.example.qrscannerapp.features.tasks.domain.model.TaskStep

/**
 * Сущность для хранения задачи в локальной базе данных Room.
 * Эта структура оптимизирована для хранения в SQLite.
 *
 * @param steps Список шагов задачи. Будет преобразован в JSON-строку для хранения
 *              с помощью TypeConverter, который будет создан на следующем шаге.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val priority: Int,
    val creatorId: String,
    val creatorName: String,
    val assigneeId: String,
    val assigneeName: String,
    val createdAt: Long?,
    val updatedAt: Long?,
    val completedAt: Long?,
    // V-- НАЧАЛО ИЗМЕНЕНИЙ --V
    // Новое поле для фиксации времени начала работы над задачей. Тип Long для Room.
    val startedAt: Long?,
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
    val steps: List<TaskStep>
)