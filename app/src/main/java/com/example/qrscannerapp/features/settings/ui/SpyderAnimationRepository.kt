package com.example.qrscannerapp.features.settings.ui

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "spyder_animation_logs")
data class SpyderAnimationLog(
    @PrimaryKey val id: String,
    val animationType: String,
    val durationMs: Long,
    val fpsAtMoment: Float,
    val frameTimeMs: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SpyderAnimationDao {
    @Query("SELECT * FROM spyder_animation_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentAnimations(): Flow<List<SpyderAnimationLog>>

    @Insert
    suspend fun insert(log: SpyderAnimationLog)

    @Query("DELETE FROM spyder_animation_logs WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    @Query("SELECT COUNT(*) FROM spyder_animation_logs")
    suspend fun getTotalCount(): Int

    @Query("SELECT AVG(fpsAtMoment) FROM spyder_animation_logs WHERE timestamp > :since")
    suspend fun getAverageFpsSince(since: Long): Float?
}

@Singleton
class SpyderAnimationRepository @Inject constructor(
    private val dao: SpyderAnimationDao
) {
    fun getRecentAnimations(): Flow<List<SpyderAnimationLog>> = dao.getRecentAnimations()

    suspend fun logAnimation(type: String, durationMs: Long, fps: Float, frameTimeMs: Float) {
        dao.insert(
            SpyderAnimationLog(
                id = "${type}_${System.currentTimeMillis()}_${(0..1000).random()}",
                animationType = type,
                durationMs = durationMs,
                fpsAtMoment = fps,
                frameTimeMs = frameTimeMs
            )
        )
    }

    suspend fun clearOldLogs(daysToKeep: Int = 7) {
        val cutoff = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000)
        dao.deleteOlderThan(cutoff)
    }

    suspend fun getAverageFpsLastMinute(): Float? {
        val oneMinuteAgo = System.currentTimeMillis() - 60000
        return dao.getAverageFpsSince(oneMinuteAgo)
    }
}