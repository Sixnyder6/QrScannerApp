// Полная, измененная версия файла: TaskRemoteDataSource.kt

package com.example.qrscannerapp.features.tasks.data.remote

import android.util.Log
import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val TASKS_COLLECTION = "tasks"
private const val TAG = "TaskRemoteDataSource"

@Singleton
class TaskRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getAllActiveTasksFlow(): Flow<List<Task>> {
        val activeStatuses = listOf(TaskStatus.NEW, TaskStatus.IN_PROGRESS).map { it.name }

        val query = firestore.collection(TASKS_COLLECTION)
            .whereIn("status", activeStatuses)
            .orderBy("priority", Query.Direction.ASCENDING) // ИЗМЕНЕНО: Сортировка по приоритету
            .orderBy("created_at", Query.Direction.ASCENDING)

        return query.snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(Task::class.java).mapIndexed { index, task ->
                    task.copy(id = querySnapshot.documents[index].id)
                }
            }
            .catch { exception ->
                Log.e(TAG, "Error getting active tasks from Firestore", exception)
                emit(emptyList())
            }
    }

    fun getTasksFlow(userId: String): Flow<List<Task>> {
        val query = firestore.collection(TASKS_COLLECTION)
            .whereEqualTo("assignee_id", userId)
            .orderBy("priority", Query.Direction.ASCENDING)
            .orderBy("created_at", Query.Direction.DESCENDING)

        return query.snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(Task::class.java).mapIndexed { index, task ->
                    task.copy(id = querySnapshot.documents[index].id)
                }
            }
            .catch { exception ->
                Log.e(TAG, "Error getting tasks from Firestore", exception)
                emit(emptyList())
            }
    }

    suspend fun createTask(task: Task) {
        try {
            firestore.collection(TASKS_COLLECTION).add(task).await()
            Log.d(TAG, "Task successfully created in Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating task in Firestore", e)
            throw e
        }
    }

    // V-- НАЧАЛО ИЗМЕНЕНИЙ --V

    /**
     * Универсальный метод для обновления одного или нескольких полей задачи.
     * Автоматически добавляет поле "updated_at" к каждому обновлению.
     *
     * @param taskId ID документа задачи в Firestore.
     * @param fields Карта полей для обновления, например: "status" to "IN_PROGRESS", "started_at" to Date().
     */
    suspend fun updateTaskFields(taskId: String, fields: Map<String, Any>) {
        try {
            val taskRef = firestore.collection(TASKS_COLLECTION).document(taskId)

            // Создаем изменяемую карту и добавляем к ней автоматическое обновление `updated_at`
            val updates = fields.toMutableMap()
            updates["updated_at"] = Date()

            taskRef.update(updates).await()
            Log.d(TAG, "Task fields successfully updated for task: $taskId with data: $updates")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task fields in Firestore", e)
            throw e
        }
    }

    /**
     * Обновляет только статус задачи.
     * Для обратной совместимости оставлен, но теперь он просто вызывает `updateTaskFields`.
     */
    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        val fields = mapOf("status" to newStatus.name)
        updateTaskFields(taskId, fields)
    }

    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^


    suspend fun deleteTask(taskId: String) {
        try {
            firestore.collection(TASKS_COLLECTION).document(taskId).delete().await()
            Log.d(TAG, "Task successfully deleted from Firestore: $taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task from Firestore", e)
            throw e
        }
    }
}