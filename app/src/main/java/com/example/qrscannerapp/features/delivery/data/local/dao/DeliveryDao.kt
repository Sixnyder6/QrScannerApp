package com.example.qrscannerapp.features.delivery.data.local.dao

import androidx.room.*
import com.example.qrscannerapp.features.delivery.data.local.entity.DeliveryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {

    @Query("SELECT * FROM delivery_logs ORDER BY timestamp DESC")
    fun getAllDeliveries(): Flow<List<DeliveryLogEntity>>

    @Query("SELECT * FROM delivery_logs WHERE id = :id")
    suspend fun getDeliveryById(id: String): DeliveryLogEntity?

    @Query("SELECT * FROM delivery_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getDeliveriesByType(type: String): Flow<List<DeliveryLogEntity>>

    @Query("SELECT * FROM delivery_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getDeliveriesByDateRange(startTime: Long, endTime: Long): List<DeliveryLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveries(deliveries: List<DeliveryLogEntity>)

    @Update
    suspend fun updateDelivery(delivery: DeliveryLogEntity)

    @Delete
    suspend fun deleteDelivery(delivery: DeliveryLogEntity)

    @Query("DELETE FROM delivery_logs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM delivery_logs")
    fun getTotalCount(): Flow<Int>
}