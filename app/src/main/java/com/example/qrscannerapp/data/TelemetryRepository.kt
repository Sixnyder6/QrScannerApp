package com.example.qrscannerapp.data.repository

import android.util.Log
import com.example.qrscannerapp.data.local.dao.TelemetryDao
import com.example.qrscannerapp.data.local.entity.TelemetryBuffer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TelemetryRepository"
private const val MAX_RETRY_COUNT = 5       // после 5 неудачных попыток запись удаляется
private const val BATCH_SIZE = 500          // Firestore лимит на батч — 500 операций
private const val RETENTION_DAYS = 7L

@Singleton
class TelemetryRepository @Inject constructor(
    private val telemetryDao: TelemetryDao,
    private val firestore: FirebaseFirestore,
    private val gson: Gson
) {

    val unsentTelemetry: Flow<List<TelemetryBuffer>> = telemetryDao.getUnsentTelemetry()

    suspend fun saveTelemetry(telemetryData: Map<String, Any>) {
        val jsonData = gson.toJson(telemetryData)
        val buffer = TelemetryBuffer(
            timestamp = System.currentTimeMillis(),
            jsonData = jsonData
        )
        telemetryDao.insert(buffer)
        Log.d(TAG, "Телеметрия сохранена в буфер")
    }

    suspend fun sendBufferedTelemetry(): Result<Int> {
        return try {
            val unsent = telemetryDao.getUnsentTelemetryList()
            if (unsent.isEmpty()) {
                Log.d(TAG, "Нет неотправленных записей")
                return Result.success(0)
            }

            Log.d(TAG, "Отправляем ${unsent.size} записей батчами по $BATCH_SIZE")

            // Разбиваем на батчи по 500 (лимит Firestore)
            val chunks = unsent.chunked(BATCH_SIZE)
            var totalSent = 0

            for (chunk in chunks) {
                try {
                    val batch = firestore.batch()

                    for (telemetry in chunk) {
                        @Suppress("UNCHECKED_CAST")
                        val data = gson.fromJson(telemetry.jsonData, Map::class.java) as Map<String, Any>
                        val ref = firestore.collection("telemetry").document()
                        batch.set(ref, data + mapOf("serverTimestamp" to System.currentTimeMillis()))
                    }

                    // Один коммит на весь батч — атомарно
                    batch.commit().await()

                    // Помечаем как отправленные только после успешного коммита
                    chunk.forEach { telemetryDao.update(it.copy(isSent = true, retryCount = 0)) }
                    totalSent += chunk.size
                    Log.d(TAG, "Батч из ${chunk.size} записей отправлен")

                } catch (e: Exception) {
                    // Батч упал — увеличиваем счётчик попыток
                    Log.e(TAG, "Ошибка отправки батча: ${e.message}")
                    chunk.forEach { telemetry ->
                        val newRetryCount = telemetry.retryCount + 1
                        if (newRetryCount >= MAX_RETRY_COUNT) {
                            // Слишком много попыток — удаляем мусор
                            Log.w(TAG, "Запись ${telemetry.id} удалена после $MAX_RETRY_COUNT попыток")
                            telemetryDao.update(telemetry.copy(isSent = true)) // помечаем как отправленную чтобы не мешала
                        } else {
                            telemetryDao.update(telemetry.copy(retryCount = newRetryCount))
                        }
                    }
                    // Продолжаем следующий батч
                }
            }

            // Чистим старые записи старше RETENTION_DAYS
            val cutoff = System.currentTimeMillis() - RETENTION_DAYS * 24 * 60 * 60 * 1000
            telemetryDao.deleteOlderThan(cutoff)
            Log.d(TAG, "Очистка записей старше $RETENTION_DAYS дней выполнена")

            Log.d(TAG, "Итого отправлено: $totalSent / ${unsent.size}")
            Result.success(totalSent)

        } catch (e: Exception) {
            Log.e(TAG, "Критическая ошибка sendBufferedTelemetry", e)
            Result.failure(e)
        }
    }

    suspend fun getBufferCount(): Int = telemetryDao.getCount()

    /** Принудительная очистка буфера (например при смене аккаунта) */
    suspend fun clearBuffer() {
        telemetryDao.deleteOlderThan(System.currentTimeMillis() + 1)
        Log.d(TAG, "Буфер телеметрии очищен")
    }
}