// Полная, измененная версия файла: features/tasks/data/mapper/TaskMapper.kt

package com.example.qrscannerapp.features.tasks.data.mapper

import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.data.local.entity.TaskEntity
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Класс-маппер для преобразования моделей данных задач между слоями
 * (сетевым и локальной базой данных).
 * Использует Hilt для внедрения (хотя в данном случае зависимости отсутствуют,
 * это хорошая практика для консистентности).
 */
@Singleton
class TaskMapper @Inject constructor() {

    /**
     * Преобразует сетевую модель [Task] (из Firestore) в локальную сущность [TaskEntity] (для Room).
     */
    fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = this.id,
            title = this.title,
            description = this.description,
            status = this.status,
            priority = this.priority,
            creatorId = this.creatorId,
            creatorName = this.creatorName,
            assigneeId = this.assigneeId,
            assigneeName = this.assigneeName,
            createdAt = this.createdAt?.time, // Преобразуем Date в Long (timestamp)
            updatedAt = this.updatedAt?.time,
            completedAt = this.completedAt?.time,
            // V-- НАЧАЛО ИЗМЕНЕНИЙ (1 из 2) --V
            startedAt = this.startedAt?.time,
            // ^-- КОНЕЦ ИЗМЕНЕНИЙ (1 из 2) --^
            steps = this.steps
        )
    }

    /**
     * Преобразует список сетевых моделей [Task] в список локальных сущностей [TaskEntity].
     */
    fun List<Task>.toEntityList(): List<TaskEntity> {
        return this.map { it.toEntity() }
    }

    /**
     * Преобразует локальную сущность [TaskEntity] (из Room) обратно в сетевую модель [Task].
     * Это может понадобиться для отображения данных в UI слое.
     */
    fun TaskEntity.toDomain(): Task {
        return Task(
            id = this.id,
            title = this.title,
            description = this.description,
            status = this.status,
            priority = this.priority,
            creatorId = this.creatorId,
            creatorName = this.creatorName,
            assigneeId = this.assigneeId,
            assigneeName = this.assigneeName,
            createdAt = this.createdAt?.let { Date(it) }, // Преобразуем Long (timestamp) в Date
            updatedAt = this.updatedAt?.let { Date(it) },
            completedAt = this.completedAt?.let { Date(it) },
            // V-- НАЧАЛО ИЗМЕНЕНИЙ (2 из 2) --V
            startedAt = this.startedAt?.let { Date(it) },
            // ^-- КОНЕЦ ИЗМЕНЕНИЙ (2 из 2) --^
            steps = this.steps
        )
    }

    /**
     * Преобразует список локальных сущностей [TaskEntity] в список сетевых моделей [Task].
     */
    fun List<TaskEntity>.toDomainList(): List<Task> {
        return this.map { it.toDomain() }
    }

    // --- НОВАЯ ФУНКЦИЯ ДЛЯ TaskRepository ---
    /**
     * Используется для конвертации списка Task (от удаленного источника)
     * в список Task (для домена). Технически просто возвращает список, но
     * удовлетворяет требованию Repository о необходимости маппинга.
     */
    fun List<Task>.toDomainListFromRemote(): List<Task> {
        return this
    }
    // ----------------------------------------
}