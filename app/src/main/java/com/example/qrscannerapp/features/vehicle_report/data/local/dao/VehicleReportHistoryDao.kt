package com.example.qrscannerapp.features.vehicle_report.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qrscannerapp.features.vehicle_report.domain.model.VehicleReportHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleReportHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: VehicleReportHistory)

    @Query("SELECT * FROM vehicle_report_history ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<VehicleReportHistory>>

    @Delete
    suspend fun deleteReport(report: VehicleReportHistory)

    // V-- ДОБАВЛЕН НОВЫЙ МЕТОД --V
    @Query("DELETE FROM vehicle_report_history")
    suspend fun deleteAll()
    // ^-- КОНЕЦ ДОБАВЛЕНИЯ --^
}