// Полная, измененная версия файла: features/tasks/domain/model/Task.kt

package com.example.qrscannerapp.features.tasks.domain.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

// --- Enums for Type Safety ---

enum class TaskStatus {
    NEW, IN_PROGRESS, COMPLETED, CANCELED, UNKNOWN
}

enum class TaskPriority(val value: Int) {
    HIGH(0), MEDIUM(1), LOW(2);

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: MEDIUM
    }
}

enum class StepType {
    SCAN_QR, TAKE_PHOTO, CONFIRMATION, GEO_STAMP, UNKNOWN
}

enum class StepStatus {
    PENDING, COMPLETED
}


// --- Main Data Models ---

/**
 * Модель, представляющая документ из коллекции 'tasks' в Firestore.
 * @param id Уникальный идентификатор документа Firestore. Не хранится в самом документе,
 *           а присваивается после его получения.
 */
data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.NEW,
    val priority: Int = TaskPriority.MEDIUM.value,
    @get:PropertyName("creator_id") @set:PropertyName("creator_id")
    var creatorId: String = "",
    @get:PropertyName("creator_name") @set:PropertyName("creator_name")
    var creatorName: String = "",
    @get:PropertyName("assignee_id") @set:PropertyName("assignee_id")
    var assigneeId: String = "",
    @get:PropertyName("assignee_name") @set:PropertyName("assignee_name")
    var assigneeName: String = "",
    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Date? = null,
    @get:PropertyName("updated_at") @set:PropertyName("updated_at")
    var updatedAt: Date? = null,
    @get:PropertyName("completed_at") @set:PropertyName("completed_at")
    var completedAt: Date? = null,

    // V-- НАЧАЛО ИЗМЕНЕНИЙ --V
    // Новое поле для фиксации времени начала работы над задачей
    @get:PropertyName("started_at") @set:PropertyName("started_at")
    var startedAt: Date? = null,
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^

    val steps: List<TaskStep> = emptyList()
)

/**
 * Модель, представляющая один шаг в массиве 'steps' внутри документа Task.
 */
data class TaskStep(
    @get:PropertyName("step_id") @set:PropertyName("step_id")
    var stepId: Int = 0,
    val title: String = "",
    val type: StepType = StepType.UNKNOWN,
    val status: StepStatus = StepStatus.PENDING,
    /**
     * Конфигурация шага. Хранится как Map для совместимости с Firestore.
     * Пример для SCAN_QR: {"target_type": "BATTERY", "required_count": 15}
     * Пример для TAKE_PHOTO: {"required_count": 2}
     */
    val config: Map<String, Any> = emptyMap()
)