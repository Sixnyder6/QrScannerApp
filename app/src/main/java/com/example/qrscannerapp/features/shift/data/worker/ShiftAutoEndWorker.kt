package com.example.qrscannerapp.features.shift.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.qrscannerapp.features.shift.data.repository.ShiftRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager Worker для автоматического завершения смены через 12 часов.
 *
 * Планируется в ShiftRepository.scheduleAutoEnd() при начале каждой смены.
 * Отменяется при ручном завершении смены.
 *
 * Требует подключения к сети (NetworkType.CONNECTED) —
 * если сети нет, WorkManager дождётся подключения и выполнит.
 */
@HiltWorker
class ShiftAutoEndWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val shiftRepository: ShiftRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ShiftAutoEndWorker"
    }

    override suspend fun doWork(): Result {
        val shiftId = inputData.getString("shiftId")
        val userId = inputData.getString("userId")

        if (shiftId == null || userId == null) {
            Log.e(TAG, "Missing shiftId or userId in input data")
            return Result.failure()
        }

        Log.d(TAG, "Auto-ending shift: $shiftId for user: $userId")

        val result = shiftRepository.endShift(
            reason = "auto_12h",
            shiftId = shiftId,
            targetUserId = userId
        )

        return if (result.isSuccess) {
            Log.d(TAG, "Shift $shiftId auto-ended successfully")
            Result.success()
        } else {
            Log.e(TAG, "Failed to auto-end shift: ${result.exceptionOrNull()?.message}")
            // Retry — WorkManager попробует снова
            Result.retry()
        }
    }
}