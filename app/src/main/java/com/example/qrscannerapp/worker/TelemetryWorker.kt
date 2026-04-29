package com.example.qrscannerapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.qrscannerapp.data.repository.TelemetryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

private const val TAG = "TelemetryWorker"
private const val MAX_RETRY_ATTEMPTS = 3

@HiltWorker
class TelemetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val telemetryRepository: TelemetryRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Начало отправки телеметрии (попытка $runAttemptCount / $MAX_RETRY_ATTEMPTS)")

        // Не начинаем работу если буфер пустой
        val bufferCount = telemetryRepository.getBufferCount()
        if (bufferCount == 0) {
            Log.d(TAG, "Буфер пустой, нечего отправлять")
            return Result.success()
        }

        Log.d(TAG, "В буфере $bufferCount записей")

        return try {
            val result = telemetryRepository.sendBufferedTelemetry()

            result.fold(
                onSuccess = { sentCount ->
                    Log.d(TAG, "Успешно отправлено $sentCount записей")
                    Result.success()
                },
                onFailure = { error ->
                    Log.e(TAG, "Ошибка отправки: ${error.message}", error)
                    if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                        Log.d(TAG, "Повтор через экспоненциальный backoff")
                        Result.retry()
                    } else {
                        Log.e(TAG, "Исчерпаны все попытки, сдаёмся")
                        Result.failure()
                    }
                }
            )
        } catch (e: CancellationException) {
            Log.d(TAG, "Работа отменена")
            throw e // WorkManager должен сам обработать отмену
        } catch (e: Exception) {
            Log.e(TAG, "Неожиданная ошибка", e)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        }
    }
}