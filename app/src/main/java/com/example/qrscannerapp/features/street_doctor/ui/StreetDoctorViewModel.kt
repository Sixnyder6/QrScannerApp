package com.example.qrscannerapp.features.street_doctor.ui

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.TelemetryManager
import com.example.qrscannerapp.features.street_doctor.domain.model.ScooterFieldStatus
import com.example.qrscannerapp.features.street_doctor.domain.model.StreetScooter
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class StreetDoctorUiState(
    val scooters: List<StreetScooter> = emptyList(),
    val isLoading: Boolean = true,
    val toastMessage: String? = null,
    val sessionId: String? = null,
    val error: String? = null
)

@HiltViewModel
class StreetDoctorViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val telemetryManager: TelemetryManager
) : ViewModel() {

    private val firestore = Firebase.firestore
    private val TAG = "StreetDoctorVM"

    private val _uiState = MutableStateFlow(StreetDoctorUiState())
    val uiState = _uiState.asStateFlow()

    private var tasksListener: ListenerRegistration? = null

    init {
        loadActiveSession()
    }

    private fun loadActiveSession() {
        firestore.collection("field_repair_sessions")
            .whereEqualTo("status", "active")
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Session listener error", error)
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) {
                    _uiState.update { it.copy(isLoading = false, scooters = emptyList()) }
                    return@addSnapshotListener
                }
                val sessionId = snapshot.documents.first().id
                if (_uiState.value.sessionId == sessionId) return@addSnapshotListener
                _uiState.update { it.copy(sessionId = sessionId) }
                startTasksListener(sessionId)
            }
    }

    private fun startTasksListener(sessionId: String) {
        val myId = authManager.authState.value.userId ?: ""
        tasksListener?.remove()
        tasksListener = firestore.collection("field_repair_tasks")
            .whereEqualTo("sessionId", sessionId)
            .whereEqualTo("assignedToId", myId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Tasks listener error", error)
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val scooters = snapshot.documents.mapNotNull { doc ->
                        try {
                            val status = when (doc.getString("status") ?: "new") {
                                "in_progress" -> ScooterFieldStatus.IN_PROGRESS
                                "done"        -> ScooterFieldStatus.DONE
                                "not_found"   -> ScooterFieldStatus.NOT_FOUND
                                "to_storage"  -> ScooterFieldStatus.TO_STORAGE
                                else          -> ScooterFieldStatus.NEW
                            }
                            val lat = when (val v = doc.get("lat")) {
                                is Double -> v; is String -> v.toDoubleOrNull() ?: 0.0
                                is Long -> v.toDouble(); else -> 0.0
                            }
                            val lon = when (val v = doc.get("lon")) {
                                is Double -> v; is String -> v.toDoubleOrNull() ?: 0.0
                                is Long -> v.toDouble(); else -> 0.0
                            }
                            val charge = when (val v = doc.get("charge")) {
                                is Long -> v.toInt(); is Double -> v.toInt()
                                is String -> v.toIntOrNull() ?: 0; else -> 0
                            }
                            StreetScooter(
                                id             = doc.id,
                                code           = doc.getString("scooterNumber") ?: "",
                                status         = status,
                                distanceMeters = 0,
                                batteryPct     = charge,
                                address        = doc.getString("address") ?: doc.getString("processStage") ?: "",
                                lat            = lat,
                                lon            = lon,
                                model          = doc.getString("model") ?: "",
                                isMine         = true,
                                workerName     = doc.getString("assignedToName"),
                                workerInitials = doc.getString("assignedToName")
                                    ?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString(""),
                                workerRole     = "Техник",
                                workStartTime  = if (status == ScooterFieldStatus.IN_PROGRESS)
                                    doc.getLong("updatedAt") ?: doc.getLong("createdAt") else null
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing task ${doc.id}", e); null
                        }
                    }
                    _uiState.update { it.copy(scooters = scooters, isLoading = false) }
                    updateDistances()
                }
            }
    }

    private fun updateDistances() {
        viewModelScope.launch {
            try {
                val myLocation = telemetryManager.getCurrentLocation() ?: return@launch
                val updated = _uiState.value.scooters.map { scooter ->
                    if (scooter.lat == 0.0 && scooter.lon == 0.0) return@map scooter
                    val results = FloatArray(1)
                    Location.distanceBetween(myLocation.latitude, myLocation.longitude, scooter.lat, scooter.lon, results)
                    scooter.copy(distanceMeters = results[0].toInt())
                }.sortedBy { it.distanceMeters }
                _uiState.update { it.copy(scooters = updated) }
            } catch (e: Exception) {
                Log.w(TAG, "updateDistances error", e)
            }
        }
    }

    fun takeScooter(scooterId: String, myName: String, myInitials: String) {
        val auth = authManager.authState.value
        val myId = auth.userId ?: return
        viewModelScope.launch {
            try {
                firestore.collection("field_repair_tasks").document(scooterId).update(
                    mapOf(
                        "status"         to "in_progress",
                        "assignedToId"   to myId,
                        "assignedToName" to (auth.userName ?: myName),
                        "updatedAt"      to System.currentTimeMillis()
                    )
                ).await()
                _uiState.update { it.copy(toastMessage = "Самокат взят в работу") }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Ошибка: ${e.message}") }
            }
        }
    }

    // ── Завершить ремонт — сохраняет виды работ, заряд, заметку, фото ───────
    fun completeRepair(
        scooterId: String,
        repairTypes: List<String> = emptyList(),
        batteryPctReal: Int? = null,
        notes: String = "",
        photoUrls: List<String> = emptyList()   // ← URLs после загрузки в Cloudinary
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val data = mutableMapOf<String, Any>(
                    "status"      to "done",
                    "finalStatus" to "done",
                    "completedAt" to now,
                    "updatedAt"   to now,
                    "repairTypes" to repairTypes
                )
                if (batteryPctReal != null) data["batteryPctReal"] = batteryPctReal
                if (notes.isNotBlank())     data["notes"] = notes
                if (photoUrls.isNotEmpty()) data["photoUrls"] = photoUrls

                firestore.collection("field_repair_tasks").document(scooterId)
                    .update(data).await()
                updateSessionDoneCount()
                _uiState.update { it.copy(toastMessage = "Ремонт завершён ✓") }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Ошибка: ${e.message}") }
            }
        }
    }

    // ── На склад — тоже сохраняет фото ───────────────────────────────────────
    fun sendToStorage(
        scooterId: String,
        repairTypes: List<String> = emptyList(),
        batteryPctReal: Int? = null,
        notes: String = "",
        photoUrls: List<String> = emptyList()   // ← URLs после загрузки в Cloudinary
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val data = mutableMapOf<String, Any>(
                    "status"      to "to_storage",
                    "finalStatus" to "to_storage",
                    "completedAt" to now,
                    "updatedAt"   to now,
                    "repairTypes" to repairTypes
                )
                if (batteryPctReal != null) data["batteryPctReal"] = batteryPctReal
                if (notes.isNotBlank())     data["notes"] = notes
                if (photoUrls.isNotEmpty()) data["photoUrls"] = photoUrls

                firestore.collection("field_repair_tasks").document(scooterId)
                    .update(data).await()
                updateSessionDoneCount()
                _uiState.update { it.copy(toastMessage = "Отправлен на склад") }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Ошибка: ${e.message}") }
            }
        }
    }

    fun markNotFound(scooterId: String) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                firestore.collection("field_repair_tasks").document(scooterId).update(
                    mapOf(
                        "status"      to "not_found",
                        "finalStatus" to "not_found",
                        "completedAt" to now,
                        "updatedAt"   to now
                    )
                ).await()
                _uiState.update { it.copy(toastMessage = "Отмечен как не найден") }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Ошибка: ${e.message}") }
            }
        }
    }

    private suspend fun updateSessionDoneCount() {
        val sessionId = _uiState.value.sessionId ?: return
        try {
            firestore.collection("field_repair_sessions").document(sessionId)
                .update("doneCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "updateSessionDoneCount error", e)
        }
    }

    fun clearToast() { _uiState.update { it.copy(toastMessage = null) } }

    override fun onCleared() {
        super.onCleared()
        tasksListener?.remove()
    }
}