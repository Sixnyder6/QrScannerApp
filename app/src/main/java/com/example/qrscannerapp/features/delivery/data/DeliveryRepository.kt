package com.example.qrscannerapp.features.delivery.data

import android.net.Uri
import com.example.qrscannerapp.features.delivery.data.local.dao.DeliveryDao
import com.example.qrscannerapp.features.delivery.data.local.entity.DeliveryLogEntity
import com.example.qrscannerapp.features.delivery.domain.model.DeliveryLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map  // 🔹 ДОБАВЛЕН ЭТОЙ ИМПОРТ!
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val deliveryDao: DeliveryDao
) {
    private val deliveriesCollection = firestore.collection("deliveries")

    /**
     * Получение всех доставок из Room (локально)
     */
    fun getAllDeliveries(): Flow<List<DeliveryLog>> {
        return deliveryDao.getAllDeliveries()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    /**
     * Получение доставок за период
     */
    suspend fun getDeliveriesByDateRange(startTime: Long, endTime: Long): List<DeliveryLog> {
        return withContext(Dispatchers.IO) {
            deliveryDao.getDeliveriesByDateRange(startTime, endTime)
                .map { it.toDomainModel() }
        }
    }

    /**
     * Сохранение доставки (сначала в Room, потом в Firestore)
     */
    suspend fun saveDelivery(log: DeliveryLog, localPhotoUris: List<Uri>): Result<Unit> {
        return try {
            // Конвертируем в entity для Room
            val entity = DeliveryLogEntity.fromDomainModel(log)

            // Сохраняем локально в Room
            withContext(Dispatchers.IO) {
                deliveryDao.insertDelivery(entity)
            }

            // Сохраняем в Firestore
            val dummyPhotoUrls = localPhotoUris.map { uri -> uri.toString() }
            val finalLog = log.copy(photoUrls = dummyPhotoUrls)
            deliveriesCollection.document(finalLog.id).set(finalLog).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получение истории (для обратной совместимости)
     */
    suspend fun getDeliveryHistory(): Result<List<DeliveryLog>> {
        return try {
            val snapshot = deliveriesCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val logs = snapshot.toObjects(DeliveryLog::class.java)
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Удаление доставки
     */
    suspend fun deleteDelivery(log: DeliveryLog) {
        withContext(Dispatchers.IO) {
            val entity = DeliveryLogEntity.fromDomainModel(log)
            deliveryDao.deleteDelivery(entity)
        }
        deliveriesCollection.document(log.id).delete().await()
    }

    /**
     * Очистка локальной базы
     */
    suspend fun clearLocalDatabase() {
        withContext(Dispatchers.IO) {
            deliveryDao.deleteAll()
        }
    }
}