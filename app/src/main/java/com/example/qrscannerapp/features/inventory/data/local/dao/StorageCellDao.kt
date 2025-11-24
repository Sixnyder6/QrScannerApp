// Path: app/src/main/java/com/example/qrscannerapp/features/inventory/data/local/dao/StorageCellDao.kt

package com.example.qrscannerapp.features.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qrscannerapp.features.inventory.data.local.entity.StorageCellEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageCellDao {

    // Вставляет или обновляет список ячеек
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cells: List<StorageCellEntity>)

    // Получает все ячейки, отсортированные по номеру, как Flow
    @Query("SELECT * FROM storage_cells ORDER BY cellNumber ASC")
    fun getAllCells(): Flow<List<StorageCellEntity>>

    // Обновляет список предметов в ячейке и помечает ее как "грязную"
    @Query("UPDATE storage_cells SET items = :items, isDirty = 1 WHERE id = :cellId")
    suspend fun updateItems(cellId: String, items: List<String>)

    // Получает "грязные" ячейки для синхронизации
    @Query("SELECT * FROM storage_cells WHERE isDirty = 1")
    suspend fun getDirtyCells(): List<StorageCellEntity>

    // Сбрасывает флаг isDirty после успешной синхронизации
    @Query("UPDATE storage_cells SET isDirty = 0 WHERE id IN (:ids)")
    suspend fun resetDirtyFlags(ids: List<String>)

    // --- ДОБАВЛЕН МЕТОД ДЛЯ УДАЛЕНИЯ ---
    @Query("DELETE FROM storage_cells WHERE id = :id")
    suspend fun deleteById(id: String)
}