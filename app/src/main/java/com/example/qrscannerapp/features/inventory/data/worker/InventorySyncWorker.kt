// Path: app/src/main/java/com/example/qrscannerapp/features/inventory/data/worker/InventorySyncWorker.kt

package com.example.qrscannerapp.features.inventory.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.qrscannerapp.features.inventory.data.local.dao.StorageCellDao
import com.example.qrscannerapp.features.inventory.data.local.dao.StoragePalletDao
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class InventorySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    // Hilt внедряет наши зависимости
    private val storageCellDao: StorageCellDao,
    private val storagePalletDao: StoragePalletDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("InventorySyncWorker", "Starting sync work...")

        // 1. Получаем все измененные ("грязные") записи из локальной базы
        val dirtyCells = storageCellDao.getDirtyCells()
        val dirtyPallets = storagePalletDao.getDirtyPallets()

        // Если нет работы, завершаемся успешно
        if (dirtyCells.isEmpty() && dirtyPallets.isEmpty()) {
            Log.d("InventorySyncWorker", "No dirty items to sync. Work finished.")
            return Result.success()
        }

        try {
            // 2. Создаем пакетную запись в Firestore для атомарности
            val batch = firestore.batch()

            // 3. Добавляем операции обновления для ячеек
            dirtyCells.forEach { cell ->
                val cellRef = firestore.collection("storage_cells").document(cell.id)
                // Обновляем только список самокатов
                batch.update(cellRef, "items", cell.items)
                Log.d("InventorySyncWorker", "Syncing cell: ${cell.name}")
            }

            // 4. Добавляем операции обновления для палетов
            dirtyPallets.forEach { pallet ->
                val palletRef = firestore.collection("storage_pallets").document(pallet.id)
                // Обновляем только производителя
                batch.update(palletRef, "manufacturer", pallet.manufacturer)
                Log.d("InventorySyncWorker", "Syncing pallet: №${pallet.palletNumber}")
            }

            // 5. Отправляем все изменения на сервер одним запросом
            batch.commit().await()

            // 6. Если все прошло успешно, сбрасываем флаги 'isDirty' в локальной базе
            if (dirtyCells.isNotEmpty()) {
                storageCellDao.resetDirtyFlags(dirtyCells.map { it.id })
            }
            if (dirtyPallets.isNotEmpty()) {
                storagePalletDao.resetDirtyFlags(dirtyPallets.map { it.id })
            }

            Log.d("InventorySyncWorker", "Sync successful!")
            return Result.success()

        } catch (e: Exception) {
            Log.e("InventorySyncWorker", "Sync failed. Retrying...", e)
            // Если произошла ошибка (например, нет интернета), WorkManager попробует снова позже
            return Result.retry()
        }
    }
}