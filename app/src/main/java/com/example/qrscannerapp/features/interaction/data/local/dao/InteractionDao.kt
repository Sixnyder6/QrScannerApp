package com.example.qrscannerapp.features.interaction.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qrscannerapp.features.interaction.data.local.entity.InteractionSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {
    // Сохранить новую сессию
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InteractionSessionEntity)

    // Получить историю для экрана отчетов (Flow будет сам обновлять UI)
    @Query("SELECT * FROM interaction_sessions ORDER BY timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<InteractionSessionEntity>>

    // Получить те, что еще не улетели в Firebase (для фоновой синхронизации)
    @Query("SELECT * FROM interaction_sessions WHERE isSynced = 0")
    suspend fun getUnsyncedSessions(): List<InteractionSessionEntity>

    // Пометить как отправленное
    @Query("UPDATE interaction_sessions SET isSynced = 1 WHERE id = :sessionId")
    suspend fun markAsSynced(sessionId: String)

    // Удалить старые сессии (например, старше 30 дней), чтобы не забивать память телефона
    @Query("DELETE FROM interaction_sessions WHERE timestamp < :timeThreshold")
    suspend fun deleteOldSessions(timeThreshold: Long)
}