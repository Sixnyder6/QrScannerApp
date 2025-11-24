// Path: app/src/main/java/com/example/qrscannerapp/features/inventory/data/repository/StorageRepository.kt

package com.example.qrscannerapp.features.inventory.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.StorageActivityLogEntry
import com.example.qrscannerapp.StorageCell
import com.example.qrscannerapp.features.inventory.data.local.dao.StorageCellDao
import com.example.qrscannerapp.features.inventory.data.local.dao.StoragePalletDao
import com.example.qrscannerapp.features.inventory.data.mapper.toDomain
import com.example.qrscannerapp.features.inventory.data.mapper.toEntity
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet
import com.example.qrscannerapp.features.inventory.data.worker.InventorySyncWorker
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storageCellDao: StorageCellDao,
    private val storagePalletDao: StoragePalletDao,
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager,
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)
    private val cellsCollection = firestore.collection("storage_cells")
    private val scootersCollection = firestore.collection("scooters")
    private val activityLogCollection = firestore.collection("storage_activity_log")
    private val palletsCollection = firestore.collection("storage_pallets")
    private val batteriesCollection = firestore.collection("batteries")

    // --- ИСТОЧНИКИ ДАННЫХ ДЛЯ UI (РАБОТАЮТ С ЛОКАЛЬНОЙ БАЗОЙ) ---

    fun getCellsFlow(): Flow<List<StorageCell>> {
        return storageCellDao.getAllCells().map { entities -> entities.map { it.toDomain() } }
    }

    fun getPalletsFlow(): Flow<List<StoragePallet>> {
        return storagePalletDao.getAllPallets().map { entities -> entities.map { it.toDomain() } }
    }

    // --- ОФЛАЙН-МЕТОДЫ ДЛЯ НОВЫХ ЗАДАЧ ---

    suspend fun bulkAddScootersToCell(cellId: String, newScootersText: String): Result<Unit> {
        return try {
            val cellFlow = storageCellDao.getAllCells().map { list -> list.firstOrNull { it.id == cellId } }
            val currentCell = cellFlow.first() ?: return Result.failure(Exception("Ячейка не найдена локально"))
            val newScooterIds = newScootersText.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (newScooterIds.isEmpty()) return Result.success(Unit)
            val updatedItems = (currentCell.items + newScooterIds).distinct()
            storageCellDao.updateItems(cellId, updatedItems)
            triggerSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Failed to bulk add scooters", e)
            Result.failure(e)
        }
    }

    suspend fun setPalletManufacturer(palletId: String, manufacturer: String?): Result<Unit> {
        return try {
            val finalManufacturer = if (manufacturer == "Нет") null else manufacturer
            storagePalletDao.updateManufacturer(palletId, finalManufacturer)
            triggerSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Failed to set manufacturer", e)
            Result.failure(e)
        }
    }

    // --- ОНЛАЙН-МЕТОДЫ ДЛЯ ЯЧЕЕК (STORAGE CELLS) ---

    suspend fun createNewCell(description: String, capacity: Int): Result<Unit> {
        val currentUser = authManager.authState.value
        if (currentUser.userId == null) return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            val lastCellSnapshot = cellsCollection.orderBy("cellNumber", Query.Direction.DESCENDING).limit(1).get().await()
            val nextCellNumber = lastCellSnapshot.documents.firstOrNull()?.getLong("cellNumber")?.toInt()?.plus(1) ?: 1
            val newCell = StorageCell(
                cellNumber = nextCellNumber,
                description = description,
                capacity = capacity,
                createdBy = currentUser.userId,
                createdByName = currentUser.userName
            )
            cellsCollection.document(newCell.id).set(newCell).await()
            logActivity("CREATED", "Создал ячейку '${newCell.name}' (ёмкость: ${newCell.capacity})")
            refreshDataFromServer() // Обновляем локальный кеш
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error creating new cell", e)
            Result.failure(e)
        }
    }

    suspend fun distributeScootersToCell(cell: StorageCell, scooterIds: List<String>): Result<Int> {
        if (scooterIds.isEmpty()) return Result.success(0)
        return try {
            var addedCount = 0
            firestore.runTransaction { transaction ->
                val cellRef = cellsCollection.document(cell.id)
                val currentItems = (transaction.get(cellRef).get("items") as? List<String>) ?: emptyList()
                val itemsToAdd = scooterIds.filter { !currentItems.contains(it) }
                if (currentItems.size + itemsToAdd.size > cell.capacity) throw Exception("Недостаточно места в ячейке.")

                itemsToAdd.forEach { scooterId ->
                    val scooterRef = scootersCollection.document(scooterId)
                    transaction.set(scooterRef, mapOf("status" to "in_storage", "cellId" to cell.id, "lastUpdate" to FieldValue.serverTimestamp()))
                }
                if (itemsToAdd.isNotEmpty()) {
                    transaction.update(cellRef, "items", FieldValue.arrayUnion(*itemsToAdd.toTypedArray()))
                }
                addedCount = itemsToAdd.size
            }.await()
            if (addedCount > 0) logActivity("SCOOTERS_ADDED", "Добавил $addedCount самокатов в '${cell.name}'")
            refreshDataFromServer()
            Result.success(addedCount)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error distributing scooters", e)
            Result.failure(e)
        }
    }

    suspend fun updateCell(cellId: String, newDescription: String, newCapacity: Int): Result<Unit> {
        return try {
            cellsCollection.document(cellId).update(mapOf("description" to newDescription, "capacity" to newCapacity)).await()
            logActivity("EDITED", "Изменил ячейку (описание: '$newDescription', ёмкость: $newCapacity)")
            refreshDataFromServer()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeItemFromCell(cell: StorageCell, scooterId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                transaction.update(cellsCollection.document(cell.id), "items", FieldValue.arrayRemove(scooterId))
                transaction.update(scootersCollection.document(scooterId), mapOf("status" to "available", "cellId" to FieldValue.delete()))
            }.await()
            logActivity("ITEM_REMOVED", "Удалил самокат $scooterId из '${cell.name}'")
            refreshDataFromServer()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCell(cell: StorageCell): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                cell.items.forEach { scooterId ->
                    transaction.update(scootersCollection.document(scooterId), mapOf("status" to "available", "cellId" to FieldValue.delete()))
                }
                transaction.delete(cellsCollection.document(cell.id))
            }.await()
            // Удаляем локально сразу, чтобы не ждать синхронизации
            storageCellDao.deleteById(cell.id)

            logActivity("DELETED", "Удалил ячейку '${cell.name}' (${cell.items.size} самокатов)")
            refreshDataFromServer()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ОНЛАЙН-МЕТОДЫ ДЛЯ ПАЛЕТОВ (PALLETS) ---
    // Добавлен метод удаления палета, чтобы чистить и локальный кеш

    suspend fun deletePallet(pallet: StoragePallet): Result<Unit> {
        return try {
            // Удаляем из Firestore
            val palletRef = palletsCollection.document(pallet.id)
            val itemsToReset = pallet.items
            val batch = firestore.batch()

            itemsToReset.forEach { batteryId ->
                val batteryRef = batteriesCollection.document(batteryId)
                batch.update(batteryRef, "status", FieldValue.delete(), "palletId", FieldValue.delete())
            }
            batch.commit().await()
            palletRef.delete().await()

            // Удаляем из локального кеша (DAO), чтобы не мерцало при обновлении
            storagePalletDao.deleteById(pallet.id)

            // Логируем в Firestore (не в activityLog, а в pallet_activity_log, но это делает VM, тут просто чистка данных)
            // Обновляем данные с сервера на всякий случай
            refreshDataFromServer()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error deleting pallet", e)
            Result.failure(e)
        }
    }


    // --- СИНХРОНИЗАЦИЯ И УПРАВЛЕНИЕ КЕШЕМ ---

    suspend fun refreshDataFromServer() {
        try {
            val cellsSnapshot = cellsCollection.orderBy("cellNumber").get().await()
            storageCellDao.upsertAll(cellsSnapshot.toObjects(StorageCell::class.java).map { it.toEntity() })

            val palletsSnapshot = palletsCollection.orderBy("palletNumber").get().await()
            val serverPallets = palletsSnapshot.toObjects(StoragePallet::class.java)

            // Важно: Полная синхронизация (удаление локальных, которых нет на сервере)
            val serverIds = serverPallets.map { it.id }
            val localPallets = storagePalletDao.getAllPallets().first()
            val localIdsToDelete = localPallets.filter { it.id !in serverIds }.map { it.id }

            if (localIdsToDelete.isNotEmpty()) {
                storagePalletDao.deleteByIds(localIdsToDelete)
            }

            storagePalletDao.upsertAll(serverPallets.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error refreshing data from server", e)
        }
    }

    private fun triggerSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncRequest = OneTimeWorkRequestBuilder<InventorySyncWorker>().setConstraints(constraints).build()
        workManager.enqueueUniqueWork("inventory_sync_work", androidx.work.ExistingWorkPolicy.REPLACE, syncRequest)
        Log.d("StorageRepository", "Inventory sync work enqueued.")
    }

    private suspend fun logActivity(action: String, details: String) {
        val currentUser = authManager.authState.value
        if (currentUser.userId == null) return
        try {
            activityLogCollection.add(StorageActivityLogEntry(
                userId = currentUser.userId,
                userName = currentUser.userName ?: "Неизвестно",
                action = action,
                details = details
            )).await()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Failed to write activity log", e)
        }
    }
}