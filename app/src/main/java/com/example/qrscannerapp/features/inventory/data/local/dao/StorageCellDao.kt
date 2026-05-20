package com.example.qrscannerapp.features.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qrscannerapp.features.inventory.data.local.entity.StorageCellEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageCellDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cells: List<StorageCellEntity>)

    @Query("SELECT * FROM storage_cells ORDER BY cellNumber ASC")
    fun getAllCells(): Flow<List<StorageCellEntity>>

    @Query("UPDATE storage_cells SET items = :items, isDirty = 1 WHERE id = :cellId")
    suspend fun updateItems(cellId: String, items: List<String>)

    @Query("UPDATE storage_cells SET description = :description, capacity = :capacity, isDirty = 1 WHERE id = :id")
    suspend fun updateDescriptionAndCapacity(id: String, description: String, capacity: Int)

    // <<< НОВОЕ: обновить операции локально
    @Query("UPDATE storage_cells SET operations = :operations, isDirty = 1 WHERE id = :cellId")
    suspend fun updateOperations(cellId: String, operations: List<String>)

    @Query("UPDATE storage_cells SET stickerDirections = :json WHERE id = :cellId")
    suspend fun updateStickerDirections(cellId: String, json: String)

    @Query("SELECT * FROM storage_cells WHERE isDirty = 1")
    suspend fun getDirtyCells(): List<StorageCellEntity>

    @Query("UPDATE storage_cells SET isDirty = 0 WHERE id IN (:ids)")
    suspend fun resetDirtyFlags(ids: List<String>)

    @Query("DELETE FROM storage_cells WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM storage_cells WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}