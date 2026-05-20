package com.example.qrscannerapp.features.inventory.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.CellOperation
import com.example.qrscannerapp.StorageActivityLogEntry
import com.example.qrscannerapp.StorageCell
import com.example.qrscannerapp.features.scanner.domain.model.StickerItem
import com.example.qrscannerapp.features.inventory.data.local.dao.StorageCellDao
import com.example.qrscannerapp.features.inventory.data.local.dao.StoragePalletDao
import com.example.qrscannerapp.features.inventory.data.mapper.toDomain
import com.example.qrscannerapp.features.inventory.data.mapper.toEntity
import com.example.qrscannerapp.features.inventory.data.mapper.toJson
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet
import com.example.qrscannerapp.features.inventory.data.worker.InventorySyncWorker
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Collections
import java.util.UUID
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

    private var palletsListenerRegistration: ListenerRegistration? = null
    private var cellsListenerRegistration: ListenerRegistration? = null

    // ========================================================================================
    // ЗАЩИТА ОТ ВОССТАНОВЛЕНИЯ УДАЛЁННЫХ ЯЧЕЕК
    // Храним ID ячеек которые удаляются прямо сейчас — listener их не трогает
    // ========================================================================================

    private val pendingDeleteCellIds: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    // ============================================================================================
    // ИСТОЧНИКИ ДАННЫХ ДЛЯ UI
    // ============================================================================================

    fun getCellsFlow(): Flow<List<StorageCell>> {
        return storageCellDao.getAllCells().map { entities ->
            entities.map { it.toDomain() }.sortedByDescending { it.cellNumber }
        }
    }

    fun getPalletsFlow(): Flow<List<StoragePallet>> {
        return storagePalletDao.getAllPallets().map { entities -> entities.map { it.toDomain() } }
    }

    // ============================================================================================
    // REAL-TIME LISTENERS
    // ============================================================================================

    fun startCellsRealtimeSync() {
        if (cellsListenerRegistration != null) return

        cellsListenerRegistration = cellsCollection
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("StorageRepository", "Cells listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val serverCells = snapshots.toObjects(StorageCell::class.java)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val localCells = storageCellDao.getAllCells().first()
                            val localIds = localCells.map { it.id }.toSet()

                            // Фильтруем сервер — убираем те что мы сами удаляем прямо сейчас
                            val filteredServerCells = serverCells.filter { it.id !in pendingDeleteCellIds }
                            val serverIds = filteredServerCells.map { it.id }.toSet()

                            // Удаляем локально то чего нет на сервере
                            val idsToDelete = localIds.filter { it !in serverIds && it !in pendingDeleteCellIds }
                            if (idsToDelete.isNotEmpty()) {
                                storageCellDao.deleteByIds(idsToDelete)
                            }

                            // Upsert только не-dirty записи (чтобы не затереть локальные изменения)
                            val dirtyCellIds = storageCellDao.getDirtyCells().map { it.id }.toSet()
                            val cellsToUpsert = filteredServerCells
                                .filter { it.id !in dirtyCellIds }
                                .map { it.toEntity() }

                            if (cellsToUpsert.isNotEmpty()) {
                                storageCellDao.upsertAll(cellsToUpsert)
                            }
                        } catch (ex: Exception) {
                            Log.e("StorageRepository", "Error syncing cells from real-time", ex)
                        }
                    }
                }
            }
    }

    fun stopCellsRealtimeSync() {
        cellsListenerRegistration?.remove()
        cellsListenerRegistration = null
    }

    fun startPalletsRealtimeSync() {
        if (palletsListenerRegistration != null) return

        palletsListenerRegistration = palletsCollection
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("StorageRepository", "Pallets listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val serverPallets = snapshots.toObjects(StoragePallet::class.java)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val localPallets = storagePalletDao.getAllPallets().first()
                            val localIds = localPallets.map { it.id }
                            val serverIds = serverPallets.map { it.id }

                            val idsToDelete = localIds.filter { it !in serverIds }
                            if (idsToDelete.isNotEmpty()) {
                                storagePalletDao.deleteByIds(idsToDelete)
                            }

                            storagePalletDao.upsertAll(serverPallets.map { it.toEntity() })
                        } catch (ex: Exception) {
                            Log.e("StorageRepository", "Error syncing pallets from real-time", ex)
                        }
                    }
                }
            }
    }

    fun stopPalletsRealtimeSync() {
        palletsListenerRegistration?.remove()
        palletsListenerRegistration = null
    }

    // ============================================================================================
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД: добавить операцию в ячейку
    // ============================================================================================

    private suspend fun addOperationToCell(
        cellId: String,
        action: String,
        details: String,
        itemCount: Int = 0
    ) {
        val currentUser = authManager.authState.value
        val op = CellOperation(
            userId = currentUser.userId ?: "unknown",
            userName = currentUser.userName ?: "Неизвестно",
            action = action,
            details = details,
            itemCount = itemCount
        )

        try {
            val localCells = storageCellDao.getAllCells().first()
            val entity = localCells.find { it.id == cellId }
            if (entity != null) {
                val updatedOps = listOf(op.toJson()) + entity.operations
                val trimmed = updatedOps.take(50)
                storageCellDao.updateOperations(cellId, trimmed)
            }
        } catch (e: Exception) {
            Log.w("StorageRepository", "Failed to update operations locally: ${e.message}")
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = cellsCollection.document(cellId).get().await()
                val cell = doc.toObject(StorageCell::class.java)
                if (cell != null) {
                    val updatedOps = listOf(op) + cell.operations
                    val trimmed = updatedOps.take(50)
                    cellsCollection.document(cellId).update("operations", trimmed).await()
                }
            } catch (e: Exception) {
                Log.w("StorageRepository", "Failed to update operations on server: ${e.message}")
            }
        }
    }

    // ============================================================================================
    // ОФЛАЙН-МЕТОДЫ
    // ============================================================================================

    suspend fun createNewCell(description: String, capacity: Int): Result<Unit> {
        val currentUser = authManager.authState.value
        val userId = currentUser.userId ?: "offline_user"
        val userName = currentUser.userName ?: "Offline User"

        return try {
            val localCells = storageCellDao.getAllCells().first()
            val maxNumber = localCells.maxOfOrNull { it.cellNumber } ?: 0
            val newId = UUID.randomUUID().toString()

            val creationOp = CellOperation(
                userId = userId,
                userName = userName,
                action = "CREATED",
                details = "Создал ячейку"
            )

            val newCell = StorageCell(
                id = newId,
                cellNumber = maxNumber + 1,
                description = description,
                capacity = capacity,
                createdBy = userId,
                createdByName = userName,
                createdByRole = if (currentUser.isAdmin) "admin" else "worker",
                items = emptyList(),
                operations = listOf(creationOp)
            )

            storageCellDao.upsertAll(listOf(newCell.toEntity()))

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    cellsCollection.document(newCell.id).set(newCell).await()
                    logActivity("CREATED", "Создал ячейку '${newCell.name}'")
                } catch (e: Exception) {
                    Log.w("StorageRepository", "Offline create — queued for sync: ${e.message}")
                    triggerSync()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error creating cell locally", e)
            Result.failure(e)
        }
    }

    suspend fun distributeScootersToCell(cell: StorageCell, scooterIds: List<String>): Result<Int> {
        if (scooterIds.isEmpty()) return Result.success(0)

        return try {
            val updatedItems = (cell.items + scooterIds).distinct()

            if (updatedItems.size > cell.capacity) {
                return Result.failure(Exception("Недостаточно места (${cell.items.size}/${cell.capacity})"))
            }

            storageCellDao.updateItems(cell.id, updatedItems)

            addOperationToCell(
                cellId = cell.id,
                action = "ITEMS_ADDED",
                details = "Добавил ${scooterIds.size} самокатов",
                itemCount = scooterIds.size
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val batch = firestore.batch()
                    batch.update(
                        cellsCollection.document(cell.id),
                        "items", FieldValue.arrayUnion(*scooterIds.toTypedArray())
                    )
                    scooterIds.forEach { scooterId ->
                        batch.set(
                            scootersCollection.document(scooterId),
                            mapOf(
                                "status" to "in_storage",
                                "cellId" to cell.id,
                                "lastUpdate" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                    logActivity("SCOOTERS_ADDED", "Добавил ${scooterIds.size} самокатов в '${cell.name}'")
                } catch (e: Exception) {
                    Log.w("StorageRepository", "Distribute offline — queued: ${e.message}")
                    triggerSync()
                }
            }

            Result.success(scooterIds.size)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error distributing scooters", e)
            Result.failure(e)
        }
    }

    suspend fun distributeNumberItemsToCell(cell: StorageCell, items: List<StickerItem>): Result<Int> {
        if (items.isEmpty()) return Result.success(0)

        return try {
            val scooterIds = items.map { it.code }
            val updatedItems = (cell.items + scooterIds).distinct()

            if (updatedItems.size > cell.capacity) {
                return Result.failure(Exception("Недостаточно места (${cell.items.size}/${cell.capacity})"))
            }

            storageCellDao.updateItems(cell.id, updatedItems)

            val existing = cell.stickerDirections ?: emptyMap()
            val incoming = items.associate { item -> item.code to item.directions.map { it.name } }
            val merged = existing + incoming
            storageCellDao.updateStickerDirections(cell.id, merged.toJson())

            addOperationToCell(
                cellId = cell.id,
                action = "ITEMS_ADDED",
                details = "Добавил ${scooterIds.size} номеров с метками",
                itemCount = scooterIds.size
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val existing = cell.stickerDirections ?: emptyMap()
                    val incoming = items.associate { item -> item.code to item.directions.map { it.name } }
                    val merged = existing + incoming

                    val batch = firestore.batch()
                    batch.update(
                        cellsCollection.document(cell.id),
                        mapOf(
                            "items" to FieldValue.arrayUnion(*scooterIds.toTypedArray()),
                            "stickerDirections" to merged
                        )
                    )
                    scooterIds.forEach { scooterId ->
                        batch.set(
                            scootersCollection.document(scooterId),
                            mapOf(
                                "status" to "in_storage",
                                "cellId" to cell.id,
                                "lastUpdate" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                    logActivity("SCOOTERS_ADDED", "Добавил ${scooterIds.size} номеров с метками в '${cell.name}'")
                } catch (e: Exception) {
                    Log.w("StorageRepository", "NumberItems offline — queued: ${e.message}")
                    triggerSync()
                }
            }

            Result.success(scooterIds.size)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error distributing number items", e)
            Result.failure(e)
        }
    }

    suspend fun bulkAddScootersToCell(cellId: String, newScootersText: String): Result<Unit> {
        return try {
            val currentCell = storageCellDao.getAllCells()
                .map { list -> list.firstOrNull { it.id == cellId } }
                .first() ?: return Result.failure(Exception("Ячейка не найдена"))

            val newIds = newScootersText.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (newIds.isEmpty()) return Result.success(Unit)

            val updatedItems = (currentCell.items + newIds).distinct()
            storageCellDao.updateItems(cellId, updatedItems)

            addOperationToCell(
                cellId = cellId,
                action = "BULK_ADDED",
                details = "Добавил ${newIds.size} номеров списком",
                itemCount = newIds.size
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    cellsCollection.document(cellId).update("items", updatedItems).await()
                } catch (e: Exception) {
                    Log.w("StorageRepository", "Bulk add offline — queued: ${e.message}")
                    triggerSync()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Failed bulk add", e)
            Result.failure(e)
        }
    }

    suspend fun updateCell(cellId: String, newDescription: String, newCapacity: Int): Result<Unit> {
        return try {
            storageCellDao.updateDescriptionAndCapacity(cellId, newDescription, newCapacity)

            addOperationToCell(
                cellId = cellId,
                action = "EDITED",
                details = "Изменил описание/ёмкость"
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    cellsCollection.document(cellId)
                        .update(mapOf("description" to newDescription, "capacity" to newCapacity)).await()
                    logActivity("EDITED", "Изменил ячейку")
                } catch (e: Exception) {
                    Log.w("StorageRepository", "Update cell offline — queued: ${e.message}")
                    triggerSync()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeItemFromCell(cell: StorageCell, scooterId: String): Result<Unit> {
        return try {
            val updatedItems = cell.items.filter { it != scooterId }
            storageCellDao.updateItems(cell.id, updatedItems)

            addOperationToCell(
                cellId = cell.id,
                action = "ITEM_REMOVED",
                details = "Удалил самокат $scooterId",
                itemCount = 1
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val batch = firestore.batch()
                    batch.update(
                        cellsCollection.document(cell.id),
                        "items", FieldValue.arrayRemove(scooterId)
                    )
                    batch.update(
                        scootersCollection.document(scooterId),
                        mapOf("status" to "available", "cellId" to FieldValue.delete())
                    )
                    batch.commit().await()
                } catch (e: Exception) {
                    Log.w("StorageRepository", "Remove item offline — queued: ${e.message}")
                    triggerSync()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================================================================
    // УДАЛЕНИЕ ЯЧЕЙКИ — ИСПРАВЛЕНО
    // Сначала помечаем в pendingDelete чтобы listener не восстановил,
    // затем удаляем на сервере (await), только потом локально.
    // ========================================================================================

    suspend fun deleteCell(cell: StorageCell): Result<Unit> {
        // 1. Сразу помечаем — listener её не тронет
        pendingDeleteCellIds.add(cell.id)

        return try {
            // 2. Удаляем локально немедленно (UI обновится мгновенно)
            storageCellDao.deleteById(cell.id)

            // 3. Удаляем на сервере — ждём результата
            try {
                val batch = firestore.batch()
                cell.items.forEach { scooterId ->
                    batch.update(
                        scootersCollection.document(scooterId),
                        mapOf("status" to "available", "cellId" to FieldValue.delete())
                    )
                }
                batch.delete(cellsCollection.document(cell.id))
                batch.commit().await()
                logActivity("DELETED", "Удалил ячейку '${cell.name}'")
            } catch (e: Exception) {
                Log.w("StorageRepository", "Delete cell server failed — will retry: ${e.message}")
                triggerSync()
            } finally {
                // 4. Снимаем метку — теперь listener больше не получит эту ячейку с сервера
                pendingDeleteCellIds.remove(cell.id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            pendingDeleteCellIds.remove(cell.id)
            Result.failure(e)
        }
    }

    suspend fun deleteMultipleCells(cells: List<StorageCell>): Result<Unit> {
        if (cells.isEmpty()) return Result.success(Unit)

        val ids = cells.map { it.id }

        // 1. Помечаем все сразу
        pendingDeleteCellIds.addAll(ids)

        return try {
            // 2. Удаляем локально
            storageCellDao.deleteByIds(ids)

            // 3. Удаляем на сервере
            try {
                // Firestore batch лимит 500 операций — чанкуем
                val allItems = cells.flatMap { it.items }
                val allOps = allItems.map { scooterId ->
                    scooterId to mapOf("status" to "available", "cellId" to FieldValue.delete())
                } + ids.map { it to null } // null = delete document

                // Разбиваем на чанки по 400
                val scooterChunks = allItems.chunked(400)
                scooterChunks.forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { scooterId ->
                        batch.update(
                            scootersCollection.document(scooterId),
                            mapOf("status" to "available", "cellId" to FieldValue.delete())
                        )
                    }
                    batch.commit().await()
                }

                // Удаляем сами ячейки
                val cellChunks = ids.chunked(400)
                cellChunks.forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { id -> batch.delete(cellsCollection.document(id)) }
                    batch.commit().await()
                }

            } catch (e: Exception) {
                Log.w("StorageRepository", "Bulk delete server failed — will retry: ${e.message}")
                triggerSync()
            } finally {
                pendingDeleteCellIds.removeAll(ids.toSet())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            pendingDeleteCellIds.removeAll(ids.toSet())
            Log.e("StorageRepository", "Bulk delete failed", e)
            Result.failure(e)
        }
    }

    suspend fun setPalletManufacturer(palletId: String, manufacturer: String?): Result<Unit> {
        return try {
            val final = if (manufacturer == "Нет") null else manufacturer
            storagePalletDao.updateManufacturer(palletId, final)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    palletsCollection.document(palletId).update("manufacturer", final).await()
                } catch (e: Exception) {
                    triggerSync()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Failed to set manufacturer", e)
            Result.failure(e)
        }
    }

    suspend fun deletePallet(pallet: StoragePallet): Result<Unit> {
        return try {
            storagePalletDao.deleteById(pallet.id)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val batch = firestore.batch()
                    pallet.items.forEach { batteryId ->
                        batch.update(
                            batteriesCollection.document(batteryId),
                            "status", FieldValue.delete(),
                            "palletId", FieldValue.delete()
                        )
                    }
                    batch.delete(palletsCollection.document(pallet.id))
                    batch.commit().await()
                } catch (e: Exception) {
                    Log.w("StorageRepository", "Delete pallet offline: ${e.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error deleting pallet", e)
            Result.failure(e)
        }
    }

    // ============================================================================================
    // ОБНОВЛЕНИЕ С СЕРВЕРА
    // ============================================================================================

    suspend fun refreshDataFromServer() {
        try {
            val cellsSnapshot = cellsCollection
                .orderBy("cellNumber", Query.Direction.DESCENDING).get().await()
            val serverCells = cellsSnapshot.toObjects(StorageCell::class.java)
                .filter { it.id !in pendingDeleteCellIds } // не восстанавливаем удаляемые
            val serverCellIds = serverCells.map { it.id }.toSet()

            val localCells = storageCellDao.getAllCells().first()
            val cellIdsToDelete = localCells.map { it.id }
                .filter { it !in serverCellIds && it !in pendingDeleteCellIds }
            if (cellIdsToDelete.isNotEmpty()) {
                storageCellDao.deleteByIds(cellIdsToDelete)
            }

            val dirtyCellIds = storageCellDao.getDirtyCells().map { it.id }.toSet()
            val cellsToUpsert = serverCells.filter { it.id !in dirtyCellIds }.map { it.toEntity() }
            if (cellsToUpsert.isNotEmpty()) storageCellDao.upsertAll(cellsToUpsert)

            val palletsSnapshot = palletsCollection.orderBy("palletNumber").get().await()
            val serverPallets = palletsSnapshot.toObjects(StoragePallet::class.java)
            val serverPalletIds = serverPallets.map { it.id }.toSet()

            val localPallets = storagePalletDao.getAllPallets().first()
            val palletIdsToDelete = localPallets.map { it.id }.filter { it !in serverPalletIds }
            if (palletIdsToDelete.isNotEmpty()) storagePalletDao.deleteByIds(palletIdsToDelete)

            storagePalletDao.upsertAll(serverPallets.map { it.toEntity() })

        } catch (e: Exception) {
            Log.w("StorageRepository", "Refresh failed (offline?): ${e.message}")
        }
    }

    // ============================================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================================================

    private fun triggerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<InventorySyncWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            "inventory_sync_work",
            androidx.work.ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    private suspend fun logActivity(action: String, details: String) {
        val currentUser = authManager.authState.value
        if (currentUser.userId == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                activityLogCollection.add(
                    StorageActivityLogEntry(
                        userId = currentUser.userId,
                        userName = currentUser.userName ?: "Неизвестно",
                        action = action,
                        details = details
                    )
                )
            } catch (e: Exception) {
                // Логи в оффлайне игнорируем
            }
        }
    }

    suspend fun findScooterInCell(scooterId: String): Pair<String, String>? {
        try {
            val localCells = storageCellDao.getAllCells().first()
            val found = localCells.find { it.items.contains(scooterId) }
            if (found != null) return scooterId to found.name
        } catch (e: Exception) {
            Log.e("StorageRepository", "Local search failed", e)
        }

        return try {
            val snapshot = cellsCollection
                .whereArrayContains("items", scooterId).limit(1).get().await()
            if (!snapshot.isEmpty) {
                val cellName = snapshot.documents.first().getString("name") ?: "Неизвестная ячейка"
                scooterId to cellName
            } else null
        } catch (e: Exception) {
            null
        }
    }
}