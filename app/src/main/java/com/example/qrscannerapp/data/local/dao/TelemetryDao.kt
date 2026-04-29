package com.example.qrscannerapp.data.local.dao

import androidx.room.*
import com.example.qrscannerapp.data.local.entity.TelemetryBuffer
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(telemetry: TelemetryBuffer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(telemetry: List<TelemetryBuffer>)

    @Query("SELECT * FROM telemetry_buffer WHERE isSent = 0 ORDER BY timestamp ASC")
    fun getUnsentTelemetry(): Flow<List<TelemetryBuffer>>

    @Query("SELECT * FROM telemetry_buffer WHERE isSent = 0 ORDER BY timestamp ASC")
    suspend fun getUnsentTelemetryList(): List<TelemetryBuffer>

    @Update
    suspend fun update(telemetry: TelemetryBuffer)

    @Query("DELETE FROM telemetry_buffer WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    @Query("SELECT COUNT(*) FROM telemetry_buffer")
    suspend fun getCount(): Int
}