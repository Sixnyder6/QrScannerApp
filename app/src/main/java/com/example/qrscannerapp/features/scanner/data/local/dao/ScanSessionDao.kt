package com.example.qrscannerapp.features.scanner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qrscannerapp.features.scanner.data.local.entity.ScanSessionEntity

/**
 * DAO (Data Access Object) для работы с таблицей `pending_scan_sessions`.
 * ...
 */
@Dao
interface ScanSessionDao {

    /**
     * Вставляет одну запись о сессии в локальную базу данных.
     * ...
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(session: ScanSessionEntity) // <-- ИСПРАВЛЕНО

    /**
     * Возвращает все сохраненные сессии из локальной базы данных.
     * ...
     */
    @Query("SELECT * FROM pending_scan_sessions ORDER BY timestamp DESC")
    suspend fun getAllPending(): List<ScanSessionEntity> // <-- ИСПРАВЛЕНО

    /**
     * Удаляет указанную запись о сессии из локальной базы данных.
     * ...
     */
    @Delete
    suspend fun delete(session: ScanSessionEntity) // <-- ИСПРАВЛЕНО
}