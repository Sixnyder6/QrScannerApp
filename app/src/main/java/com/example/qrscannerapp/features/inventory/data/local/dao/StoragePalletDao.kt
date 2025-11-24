// Path: app/src/main/java/com/example/qrscannerapp/features/inventory/data/local/dao/StoragePalletDao.kt

package com.example.qrscannerapp.features.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qrscannerapp.features.inventory.data.local.entity.StoragePalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoragePalletDao {

    // Вставляет или обновляет список палетов. Используется при синхронизации с Firestore
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(pallets: List<StoragePalletEntity>)

    // Получает все палеты, отсортированные по номеру, как Flow для автоматического обновления UI
    @Query("SELECT * FROM storage_pallets ORDER BY palletNumber ASC")
    fun getAllPallets(): Flow<List<StoragePalletEntity>>

    // Обновляет производителя для конкретного палета и помечает его как "грязный"
    @Query("UPDATE storage_pallets SET manufacturer = :manufacturer, isDirty = 1 WHERE id = :palletId")
    suspend fun updateManufacturer(palletId: String, manufacturer: String?)

    // Получает список "грязных" палетов для отправки в Firestore
    @Query("SELECT * FROM storage_pallets WHERE isDirty = 1")
    suspend fun getDirtyPallets(): List<StoragePalletEntity>

    // Сбрасывает флаг isDirty после успешной синхронизации
    @Query("UPDATE storage_pallets SET isDirty = 0 WHERE id IN (:ids)")
    suspend fun resetDirtyFlags(ids: List<String>)

    // --- ДОБАВЛЕНЫ МЕТОДЫ ДЛЯ УДАЛЕНИЯ ---
    @Query("DELETE FROM storage_pallets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM storage_pallets WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}