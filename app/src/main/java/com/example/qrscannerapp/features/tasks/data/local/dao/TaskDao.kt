// Полная версия файла TaskDao.kt

package com.example.qrscannerapp.features.tasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.qrscannerapp.features.tasks.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для работы с сущностями задач (TaskEntity) в локальной базе данных.
 */
@Dao
interface TaskDao {

    /**
     * Вставляет список задач. Если задача с таким же id уже существует, она будет заменена.
     * Используется для синхронизации данных с Firestore.
     * @param tasks Список задач для вставки или обновления.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    /**
     * Получает реактивный поток (Flow) со списком задач, назначенных конкретному исполнителю.
     * Список отсортирован по приоритету (сначала высокий) и дате создания (сначала новые).
     * @param assigneeId ID назначенного исполнителя.
     * @return Flow, эмитирующий список задач при каждом их изменении в БД.
     */
    @Query("SELECT * FROM tasks WHERE assigneeId = :assigneeId ORDER BY priority ASC, createdAt DESC")
    fun getTasksForAssignee(assigneeId: String): Flow<List<TaskEntity>>

    /**
     * Получает реактивный поток (Flow) со всеми задачами в базе данных.
     * Используется для экрана администратора.
     * Список отсортирован по дате последнего обновления (сначала самые свежие).
     * @return Flow, эмитирующий полный список задач.
     */
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /**
     * Получает одну задачу по ее уникальному идентификатору.
     * @param taskId ID искомой задачи.
     * @return [TaskEntity] или null, если задача не найдена.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    /**
     * Обновляет существующую задачу в базе данных.
     * @param task Объект задачи с обновленными данными.
     */
    @Update
    suspend fun updateTask(task: TaskEntity)

    // === ДОБАВЛЕНИЕ ЗДЕСЬ ===
    /**
     * Удаляет одну задачу по ее уникальному идентификатору.
     * @param taskId ID задачи, которую нужно удалить.
     */
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)
    // ===========================

    /**
     * Удаляет все задачи из таблицы. Может использоваться для очистки кеша.
     */
    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}