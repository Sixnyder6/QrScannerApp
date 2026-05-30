package com.example.qrscannerapp.features.shift.data.repository

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.features.shift.data.worker.ShiftAutoEndWorker
import com.example.qrscannerapp.features.shift.domain.model.Shift
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShiftRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager,
    @ApplicationContext private val context: Context
) {
    private val TAG = "ShiftRepository"

    companion object {
        const val MAX_SHIFT_DURATION_HOURS = 12L
        const val AUTO_END_WORK_TAG = "shift_auto_end"
    }

    private val workManager = WorkManager.getInstance(context)

    // ── Memory cache для истории смен (чтобы не дёргать Firestore каждый раз) ──
    private var cachedShifts: MutableMap<String, List<Shift>> = mutableMapOf() // userId -> shifts
    private var cachedShiftTimes: MutableMap<String, Long> = mutableMapOf()    // userId -> last fetch time
    private val cacheTtlMs = 30_000L // 30 секунд

    // ── Cache для activeShiftId ──
    private var cachedActiveShiftIds: MutableMap<String, String?> = mutableMapOf()
    private var cachedActiveShiftIdTimes: MutableMap<String, Long> = mutableMapOf()

    // ============================================================================================
    // НАЧАЛО СМЕНЫ
    // ============================================================================================

    suspend fun startNewShift(): Result<Unit> {
        val user = authManager.authState.value
        val userId = user.userId ?: return Result.failure(Exception("User not logged in"))

        if (user.isShiftActive) {
            return Result.failure(Exception("Смена уже активна!"))
        }

        val newShiftId = "${userId}_${System.currentTimeMillis()}"
        val newShift = Shift(
            id = newShiftId,
            userId = userId,
            userName = user.userName ?: "Неизвестный",
            userRole = user.role.key,
            startTime = System.currentTimeMillis(),
            status = "ACTIVE"
        )

        return try {
            Log.d(TAG, "Starting a new shift for user: $userId")

            val batch = firestore.batch()

            // 1. Создаём документ смены
            val shiftRef = firestore.collection("shifts").document(newShiftId)
            batch.set(shiftRef, newShift)

            // 2. Обновляем статус юзера
            val userRef = firestore.collection("internal_users").document(userId)
            batch.update(userRef, mapOf(
                "isShiftActive" to true,
                "activeShiftId" to newShiftId,
                "shiftStartTime" to newShift.startTime
            ))

            batch.commit().await()

            // 3. Ставим таймер автозавершения через WorkManager
            scheduleAutoEnd(newShiftId, userId)

            // 4. Инвалидируем кеш (изменился статус)
            invalidateCache()

            Log.d(TAG, "Shift started successfully! ID: $newShiftId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "ERROR starting shift", e)
            Result.failure(e)
        }
    }

    // ============================================================================================
    // ЗАВЕРШЕНИЕ СМЕНЫ
    // ============================================================================================

    suspend fun endShift(
        reason: String = "manual",
        shiftId: String? = null,
        targetUserId: String? = null
    ): Result<Unit> {
        val userId = targetUserId ?: authManager.authState.value.userId
        ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Находим активную смену
            val activeShiftId = shiftId ?: findActiveShiftId(userId)
            ?: return Result.failure(Exception("Активная смена не найдена"))

            val now = System.currentTimeMillis()

            // Получаем данные смены для расчёта статистики
            val shiftDoc = firestore.collection("shifts").document(activeShiftId).get().await()
            val startTime = shiftDoc.getLong("startTime") ?: now
            val durationMinutes = ((now - startTime) / 60000).toInt()

            // Собираем статистику за смену
            val stats = collectShiftStats(userId, startTime, now)

            val endStatus = when (reason) {
                "auto_12h" -> "AUTO_ENDED"
                "admin_force" -> "FORCE_ENDED"
                else -> "COMPLETED"
            }

            val batch = firestore.batch()

            // 1. Обновляем документ смены
            val shiftRef = firestore.collection("shifts").document(activeShiftId)
            batch.update(shiftRef, mapOf(
                "endTime" to now,
                "durationMinutes" to durationMinutes,
                "status" to endStatus,
                "endReason" to reason,
                "scootersScanCount" to stats.scootersScanCount,
                "batteriesScanCount" to stats.batteriesScanCount,
                "totalScanCount" to stats.totalScanCount,
                "sessionsCreated" to stats.sessionsCreated,
                "cellsModified" to stats.cellsModified
            ))

            // 2. Сбрасываем статус юзера (isAllowedToWork НЕ трогаем!)
            val userRef = firestore.collection("internal_users").document(userId)
            batch.update(userRef, mapOf(
                "isShiftActive" to false,
                "activeShiftId" to FieldValue.delete(),
                "shiftStartTime" to FieldValue.delete()
            ))

            batch.commit().await()

            // 3. Отменяем таймер автозавершения
            cancelAutoEnd()

            // 4. Обновляем локальный стейт
            if (targetUserId == null) {
                // Только если завершаем свою смену
                authManager.endShiftLocally()
            }

            // 5. Инвалидируем кеш (изменился статус)
            invalidateCache()

            Log.d(TAG, "Shift ended: $activeShiftId (reason: $reason, duration: ${durationMinutes}min)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "ERROR ending shift", e)
            Result.failure(e)
        }
    }

    // ============================================================================================
    // АВТОЗАВЕРШЕНИЕ — WorkManager
    // ============================================================================================

    private fun scheduleAutoEnd(shiftId: String, userId: String) {
        val inputData = Data.Builder()
            .putString("shiftId", shiftId)
            .putString("userId", userId)
            .build()

        val autoEndRequest = OneTimeWorkRequestBuilder<ShiftAutoEndWorker>()
            .setInitialDelay(MAX_SHIFT_DURATION_HOURS, TimeUnit.HOURS)
            .setInputData(inputData)
            .addTag(AUTO_END_WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "${AUTO_END_WORK_TAG}_$userId",
            ExistingWorkPolicy.REPLACE,
            autoEndRequest
        )

        Log.d(TAG, "Auto-end scheduled in ${MAX_SHIFT_DURATION_HOURS}h for shift: $shiftId")
    }

    private fun cancelAutoEnd() {
        workManager.cancelAllWorkByTag(AUTO_END_WORK_TAG)
        Log.d(TAG, "Auto-end timer cancelled")
    }

    // ============================================================================================
    // СТАТИСТИКА ЗА СМЕНУ
    // ============================================================================================

    private data class ShiftStats(
        val scootersScanCount: Int = 0,
        val batteriesScanCount: Int = 0,
        val totalScanCount: Int = 0,
        val sessionsCreated: Int = 0,
        val cellsModified: Int = 0
    )

    private suspend fun collectShiftStats(
        userId: String,
        startTime: Long,
        endTime: Long
    ): ShiftStats {
        return try {
            // Сессии сканирования
            val activitySnapshot = firestore.collection("activity_log")
                .whereEqualTo("creatorId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .whereLessThanOrEqualTo("timestamp", endTime)
                .get().await()

            var totalScans = 0
            var sessionsCount = 0

            for (doc in activitySnapshot.documents) {
                val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                val activityType = doc.getString("activityType") ?: ""
                totalScans += itemCount
                if (activityType == "SESSION_SAVED") sessionsCount++
            }

            // Операции с ячейками хранения
            val storageLogSnapshot = firestore.collection("storage_activity_log")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .whereLessThanOrEqualTo("timestamp", endTime)
                .get().await()

            val cellsModified = storageLogSnapshot.size()

            ShiftStats(
                scootersScanCount = 0,  // TODO: разделить по типам если нужно
                batteriesScanCount = 0,
                totalScanCount = totalScans,
                sessionsCreated = sessionsCount,
                cellsModified = cellsModified
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting shift stats", e)
            ShiftStats()
        }
    }

    // ============================================================================================
    // ВСПОМОГАТЕЛЬНЫЕ И ИСТОРИЯ
    // ============================================================================================

    private suspend fun findActiveShiftId(userId: String): String? {
        // Проверяем memory cache
        val now = System.currentTimeMillis()
        val lastFetch = cachedActiveShiftIdTimes[userId] ?: 0L
        if (cachedActiveShiftIds.containsKey(userId) && (now - lastFetch) < cacheTtlMs) {
            return cachedActiveShiftIds[userId]
        }

        return try {
            val userDoc = firestore.collection("internal_users")
                .document(userId).get().await()
            val result = userDoc.getString("activeShiftId")
            cachedActiveShiftIds[userId] = result
            cachedActiveShiftIdTimes[userId] = System.currentTimeMillis()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error finding active shift for $userId", e)
            cachedActiveShiftIds[userId] // возвращаем старый кеш если есть
        }
    }

    suspend fun forceEndShift(targetUserId: String): Result<Unit> {
        val currentUser = authManager.authState.value
        if (!currentUser.isAdmin) {
            return Result.failure(Exception("Только админ может завершить чужую смену"))
        }
        return endShift(
            reason = "admin_force",
            targetUserId = targetUserId
        )
    }

    /**
     * Получает историю последних смен пользователя.
     * Использует memory cache (30s TTL) чтобы не дёргать Firestore при частых запросах.
     */
    suspend fun getUserShifts(userId: String, limitCount: Long = 30): Result<List<Shift>> {
        // Проверяем memory cache
        val now = System.currentTimeMillis()
        val lastFetch = cachedShiftTimes[userId] ?: 0L
        if (cachedShifts.containsKey(userId) && (now - lastFetch) < cacheTtlMs) {
            return Result.success(cachedShifts[userId]!!)
        }

        return try {
            val snapshot = firestore.collection("shifts")
                .whereEqualTo("userId", userId)
                .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limitCount)
                .get()
                .await()

            val shifts = snapshot.toObjects(Shift::class.java)
            cachedShifts[userId] = shifts
            cachedShiftTimes[userId] = System.currentTimeMillis()
            Result.success(shifts)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching shifts for user: $userId", e)
            // Возвращаем кеш если он есть, иначе ошибку
            cachedShifts[userId]?.let { return Result.success(it) }
            Result.failure(e)
        }
    }

    // ── Инвалидация кеша после изменений ──
    private fun invalidateCache() {
        cachedShifts.clear()
        cachedShiftTimes.clear()
        cachedActiveShiftIds.clear()
        cachedActiveShiftIdTimes.clear()
    }
}