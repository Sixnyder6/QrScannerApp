// Полная, измененная версия файла: features/tasks/data/repository/TaskRepository.kt

package com.example.qrscannerapp.features.tasks.data.repository

import com.example.qrscannerapp.features.tasks.data.local.dao.TaskDao
import com.example.qrscannerapp.features.tasks.data.mapper.TaskMapper
import com.example.qrscannerapp.features.tasks.data.remote.TaskRemoteDataSource
import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val remoteDataSource: TaskRemoteDataSource,
    private val mapper: TaskMapper
) {

    fun getAllActiveTasksStream(): Flow<List<Task>> {
        return remoteDataSource.getAllActiveTasksFlow().map { firestoreTasks ->
            with(mapper) {
                firestoreTasks.toDomainListFromRemote()
            }
        }
    }

    fun getTasks(userId: String): Flow<List<Task>> {
        return taskDao.getTasksForAssignee(userId).map { entityList ->
            with(mapper) {
                entityList.toDomainList()
            }
        }
    }

    fun getTaskById(taskId: String): Flow<Task?> {
        return flow {
            val taskEntity = taskDao.getTaskById(taskId)
            val task = taskEntity?.let {
                with(mapper) {
                    it.toDomain()
                }
            }
            emit(task)
        }
    }

    suspend fun syncTasks(userId: String) {
        remoteDataSource.getTasksFlow(userId)
            .collect { firestoreTasks ->
                val taskEntities = with(mapper) {
                    firestoreTasks.toEntityList()
                }
                taskDao.upsertAll(taskEntities)
            }
    }

    suspend fun createTask(task: Task) {
        remoteDataSource.createTask(task)
    }

    // V-- НАЧАЛО ИЗМЕНЕНИЙ --V
    /**
     * Обновляет статус и другие поля задачи.
     * Если новый статус - IN_PROGRESS, автоматически устанавливает время начала работы.
     */
    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        // Создаем изменяемую карту для полей, которые нужно обновить в Firestore
        val fieldsToUpdate = mutableMapOf<String, Any>(
            "status" to newStatus
        )

        // Если задача переводится в статус "В РАБОТЕ",
        // добавляем в карту поле "started_at" с текущим временем.
        // Мы не добавляем проверку if (task.startedAt == null), так как репозиторий
        // не должен знать о текущем состоянии задачи. Эта логика должна быть
        // во ViewModel, но для простоты реализуем здесь: первое обновление до IN_PROGRESS запишет время.
        if (newStatus == TaskStatus.IN_PROGRESS) {
            fieldsToUpdate["started_at"] = Date()
        }

        // Вызываем более гибкий метод в remoteDataSource, который может обновлять несколько полей
        remoteDataSource.updateTaskFields(taskId, fieldsToUpdate)
    }
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^


    suspend fun deleteTask(taskId: String) {
        remoteDataSource.deleteTask(taskId)
        taskDao.deleteTaskById(taskId)
    }
}