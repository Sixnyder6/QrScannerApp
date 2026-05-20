package com.example.qrscannerapp.features.interaction.data.repository

import android.util.Log
import com.example.qrscannerapp.features.interaction.data.local.dao.InteractionDao
import com.example.qrscannerapp.features.interaction.data.mapper.toDomainModel
import com.example.qrscannerapp.features.interaction.domain.model.BatteryIssuance
import com.example.qrscannerapp.features.interaction.domain.model.BatteryReception
import com.example.qrscannerapp.features.interaction.domain.model.InteractionSession
import com.example.qrscannerapp.features.interaction.domain.model.SbEmployee
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InteractionRepository @Inject constructor(
    private val interactionDao: InteractionDao,
    private val firestore: FirebaseFirestore
) {
    // Room history — kept for backward compat (old data)
    fun getSessionsHistory(): Flow<List<InteractionSession>> =
        interactionDao.getAllSessionsFlow().map { list -> list.map { it.toDomainModel() } }

    // Active issuances (Firestore realtime)
    fun getActiveIssuances(): Flow<List<BatteryIssuance>> = callbackFlow {
        val listener = firestore.collection("battery_issuances")
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("InteractionRepo", "getActiveIssuances", err); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { doc ->
                    try {
                        BatteryIssuance(
                            id = doc.id,
                            batteryCount = (doc.getLong("batteryCount")?.toInt())
                                ?: (doc.get("batteryCodes") as? List<*>)?.size ?: 0,
                            reanimatorCount = (doc.getLong("reanimatorCount") ?: 0L).toInt(),
                            photoUrl = doc.getString("photoUrl"),
                            comment = doc.getString("comment") ?: "",
                            issuedById = doc.getString("issuedById") ?: "",
                            issuedByName = doc.getString("issuedByName") ?: "",
                            issuedByRole = doc.getString("issuedByRole") ?: "",
                            issuedToId = doc.getString("issuedToId") ?: "",
                            issuedToName = doc.getString("issuedToName") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isActive = doc.getBoolean("isActive") ?: true
                        )
                    } catch (e: Exception) { Log.e("InteractionRepo", "parse error doc=${doc.id}", e); null }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSbEmployees(): List<SbEmployee> {
        return try {
            firestore.collection("internal_users")
                .whereEqualTo("role", "security")
                .get().await()
                .documents.mapNotNull { doc ->
                    val name = doc.getString("displayName")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    SbEmployee(id = doc.id, displayName = name)
                }
        } catch (e: Exception) {
            Log.e("InteractionRepo", "getSbEmployees", e)
            emptyList()
        }
    }

    fun getRecentReceptions(): Flow<List<BatteryReception>> = callbackFlow {
        val listener = firestore.collection("battery_receptions")
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("InteractionRepo", "getRecentReceptions", err); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { doc ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        BatteryReception(
                            id = doc.id,
                            batteryCount = (doc.getLong("batteryCount")?.toInt())
                                ?: (doc.get("batteryCodes") as? List<*>)?.size ?: 0,
                            scooterCodes = doc.get("scooterCodes") as? List<String> ?: emptyList(),
                            reanimatorCount = (doc.getLong("reanimatorCount") ?: 0L).toInt(),
                            photoUrl = doc.getString("photoUrl"),
                            comment = doc.getString("comment") ?: "",
                            receivedById = doc.getString("receivedById") ?: "",
                            receivedByName = doc.getString("receivedByName") ?: "",
                            receivedFromId = doc.getString("receivedFromId") ?: "",
                            receivedFromName = doc.getString("receivedFromName") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            closedIssuanceId = doc.getString("closedIssuanceId"),
                            expectedBatteryCount = (doc.getLong("expectedBatteryCount")?.toInt()) ?: 0
                        )
                    } catch (e: Exception) { Log.e("InteractionRepo", "parse error doc=${doc.id}", e); null }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveReception(reception: BatteryReception): Result<Unit> {
        return try {
            val data = hashMapOf<String, Any?>(
                "batteryCount" to reception.batteryCount,
                "scooterCodes" to reception.scooterCodes,
                "reanimatorCount" to reception.reanimatorCount,
                "photoUrl" to reception.photoUrl,
                "comment" to reception.comment,
                "receivedById" to reception.receivedById,
                "receivedByName" to reception.receivedByName,
                "receivedFromId" to reception.receivedFromId,
                "receivedFromName" to reception.receivedFromName,
                "timestamp" to reception.timestamp,
                "closedIssuanceId" to reception.closedIssuanceId,
                "expectedBatteryCount" to reception.expectedBatteryCount
            )
            firestore.collection("battery_receptions")
                .document(reception.id)
                .set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InteractionRepo", "saveReception", e)
            Result.failure(e)
        }
    }

    suspend fun deleteIssuance(id: String): Result<Unit> {
        return try {
            firestore.collection("battery_issuances").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InteractionRepo", "deleteIssuance", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReception(id: String): Result<Unit> {
        return try {
            firestore.collection("battery_receptions").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InteractionRepo", "deleteReception", e)
            Result.failure(e)
        }
    }

    suspend fun closeIssuance(id: String): Result<Unit> {
        return try {
            firestore.collection("battery_issuances").document(id)
                .update("isActive", false).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InteractionRepo", "closeIssuance", e)
            Result.failure(e)
        }
    }

    suspend fun saveIssuance(issuance: BatteryIssuance): Result<Unit> {
        return try {
            val data = hashMapOf<String, Any?>(
                "batteryCount" to issuance.batteryCount,
                "reanimatorCount" to issuance.reanimatorCount,
                "photoUrl" to issuance.photoUrl,
                "comment" to issuance.comment,
                "issuedById" to issuance.issuedById,
                "issuedByName" to issuance.issuedByName,
                "issuedByRole" to issuance.issuedByRole,
                "issuedToId" to issuance.issuedToId,
                "issuedToName" to issuance.issuedToName,
                "timestamp" to issuance.timestamp,
                "isActive" to issuance.isActive
            )
            firestore.collection("battery_issuances")
                .document(issuance.id)
                .set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InteractionRepo", "saveIssuance", e)
            Result.failure(e)
        }
    }
}