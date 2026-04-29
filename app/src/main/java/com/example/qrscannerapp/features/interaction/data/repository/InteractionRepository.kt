package com.example.qrscannerapp.features.interaction.data.repository

import android.util.Log
import com.example.qrscannerapp.features.interaction.data.local.dao.InteractionDao
import com.example.qrscannerapp.features.interaction.data.mapper.toDomainModel
import com.example.qrscannerapp.features.interaction.data.mapper.toEntity
import com.example.qrscannerapp.features.interaction.domain.model.InteractionSession
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InteractionRepository @Inject constructor(
    private val interactionDao: InteractionDao,
    private val firestore: FirebaseFirestore
) {
    // 1. Сохранение (Сначала локально, потом попытка в облако)
    suspend fun saveSession(session: InteractionSession): Result<Unit> {
        return try {
            // Сначала всегда сохраняем в Room (оффлайн режим по умолчанию)
            val entity = session.toEntity().copy(isSynced = false)
            interactionDao.insertSession(entity)

            // Пытаемся отправить в Firebase
            try {
                firestore.collection("interaction_sessions")
                    .document(session.id)
                    .set(session)
                    .await()

                // Если успешно ушло в облако, ставим галочку в Room
                interactionDao.markAsSynced(session.id)
                Log.d("InteractionRepo", "Сессия ${session.id} успешно улетела в облако.")
            } catch (networkError: Exception) {
                Log.w("InteractionRepo", "Нет интернета. Сессия ${session.id} сохранена локально.", networkError)
                // Ошибку сети игнорируем, данные уже в Room, SyncManager потом их заберет
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InteractionRepo", "Ошибка сохранения сессии", e)
            Result.failure(e)
        }
    }

    // 2. Получение истории (Room - единственный источник правды для UI)
    fun getSessionsHistory(): Flow<List<InteractionSession>> {
        return interactionDao.getAllSessionsFlow().map { list ->
            list.map { it.toDomainModel() }
        }
    }
}