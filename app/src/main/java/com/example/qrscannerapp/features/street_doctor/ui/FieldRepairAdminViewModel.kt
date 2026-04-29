package com.example.qrscannerapp.features.street_doctor.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Модели ────────────────────────────────────────────────────────────────────

data class FieldSession(
    val id: String = "",
    val createdAt: Long = 0L,
    val status: String = "active",
    val totalCount: Int = 0,
    val doneCount: Int = 0,
    val technicians: List<FieldTechnicianProgress> = emptyList()
)

data class FieldTechnicianProgress(
    val id: String = "",
    val name: String = "",
    val total: Int = 0,
    val done: Int = 0,
    val inProgress: Int = 0,
    val notFound: Int = 0,
    val toStorage: Int = 0
)

// Самокат на складе — для таба "На склад"
data class StorageScooter(
    val id: String = "",
    val code: String = "",
    val technicianName: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val batteryPct: Int = 0,
    val completedAt: Long = 0L,
    val model: String = "",
    val repairTypes: List<String> = emptyList(),
    val notes: String = ""
)

data class FieldRepairAdminUiState(
    val isLoading: Boolean = true,
    val activeSessions: List<FieldSession> = emptyList(),
    val closedSessions: List<FieldSession> = emptyList(),
    val totalToday: Int = 0,
    val doneToday: Int = 0,
    val toStorageToday: Int = 0,
    val notFoundToday: Int = 0,
    val inProgressToday: Int = 0,
    // Список самокатов на склад
    val storageScooters: List<StorageScooter> = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class FieldRepairAdminViewModel @Inject constructor() : ViewModel() {

    private val db  = Firebase.firestore
    private val TAG = "FieldRepairAdminVM"

    private val _uiState = MutableStateFlow(FieldRepairAdminUiState())
    val uiState = _uiState.asStateFlow()

    private var sessionsListener: ListenerRegistration? = null
    private var tasksListener:    ListenerRegistration? = null

    init { subscribeToSessions() }

    private fun subscribeToSessions() {
        sessionsListener = db.collection("field_repair_sessions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Sessions error", error)
                    _uiState.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val sessions = snapshot.documents.mapNotNull { doc ->
                    try {
                        FieldSession(
                            id         = doc.id,
                            createdAt  = doc.getLong("createdAt") ?: 0L,
                            status     = doc.getString("status") ?: "active",
                            totalCount = doc.getLong("totalCount")?.toInt() ?: 0,
                            doneCount  = doc.getLong("doneCount")?.toInt() ?: 0
                        )
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.createdAt }

                _uiState.update {
                    it.copy(
                        activeSessions = sessions.filter { s -> s.status == "active" },
                        closedSessions = sessions.filter { s -> s.status != "active" }
                    )
                }

                loadTasksForSessions(sessions.filter { it.status == "active" }.map { it.id })
            }
    }

    private fun loadTasksForSessions(sessionIds: List<String>) {
        tasksListener?.remove()
        if (sessionIds.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading       = false,
                    totalToday      = 0,
                    doneToday       = 0,
                    inProgressToday = 0,
                    toStorageToday  = 0,
                    notFoundToday   = 0,
                    storageScooters = emptyList()
                )
            }
            return
        }

        tasksListener = db.collection("field_repair_tasks")
            .whereIn("sessionId", sessionIds.take(10))
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e(TAG, "Tasks error", error); return@addSnapshotListener }
                if (snapshot == null) return@addSnapshotListener

                val tasks = snapshot.documents.mapNotNull { doc ->
                    try {
                        mapOf(
                            "id"             to doc.id,
                            "sessionId"      to (doc.getString("sessionId") ?: ""),
                            "assignedToId"   to (doc.getString("assignedToId") ?: ""),
                            "assignedToName" to (doc.getString("assignedToName") ?: "Неизвестный"),
                            "status"         to (doc.getString("status") ?: "new"),
                            "scooterNumber"  to (doc.getString("scooterNumber") ?: ""),
                            "model"          to (doc.getString("model") ?: ""),
                            "lat"            to (when (val v = doc.get("lat")) {
                                is Double -> v; is String -> v.toDoubleOrNull() ?: 0.0
                                is Long -> v.toDouble(); else -> 0.0
                            }),
                            "lon"            to (when (val v = doc.get("lon")) {
                                is Double -> v; is String -> v.toDoubleOrNull() ?: 0.0
                                is Long -> v.toDouble(); else -> 0.0
                            }),
                            "charge"         to (when (val v = doc.get("charge")) {
                                is Long -> v.toInt(); is Double -> v.toInt()
                                is String -> v.toIntOrNull() ?: 0; else -> 0
                            }),
                            "completedAt"    to (doc.getLong("completedAt") ?: 0L),
                            "repairTypes"    to (@Suppress("UNCHECKED_CAST") (doc.get("repairTypes") as? List<String> ?: emptyList<String>())),
                            "notes"          to (doc.getString("notes") ?: "")
                        )
                    } catch (e: Exception) { null }
                }

                // Общая статистика
                val total    = tasks.size
                val done     = tasks.count { it["status"] == "done" }
                val inProg   = tasks.count { it["status"] == "in_progress" }
                val toStore  = tasks.count { it["status"] == "to_storage" }
                val notFound = tasks.count { it["status"] == "not_found" }

                // Самокаты на склад — сортируем по времени завершения (свежие сверху)
                val storageList = tasks
                    .filter { it["status"] == "to_storage" }
                    .sortedByDescending { it["completedAt"] as Long }
                    .map { t ->
                        StorageScooter(
                            id            = t["id"] as String,
                            code          = t["scooterNumber"] as String,
                            technicianName = t["assignedToName"] as String,
                            lat           = t["lat"] as Double,
                            lon           = t["lon"] as Double,
                            batteryPct    = t["charge"] as Int,
                            completedAt   = t["completedAt"] as Long,
                            model         = t["model"] as String,
                            repairTypes   = @Suppress("UNCHECKED_CAST") (t["repairTypes"] as List<String>),
                            notes         = t["notes"] as String
                        )
                    }

                // Прогресс по технику
                val bySession = tasks.groupBy { it["sessionId"] as String }
                val updatedActive = _uiState.value.activeSessions.map { session ->
                    val sessionTasks = bySession[session.id] ?: emptyList()
                    val byTech = sessionTasks.groupBy { it["assignedToId"] as String }
                    val techList = byTech.map { (techId, techTasks) ->
                        FieldTechnicianProgress(
                            id         = techId,
                            name       = techTasks.firstOrNull()?.get("assignedToName") as? String ?: "Неизвестный",
                            total      = techTasks.size,
                            done       = techTasks.count { it["status"] == "done" },
                            inProgress = techTasks.count { it["status"] == "in_progress" },
                            notFound   = techTasks.count { it["status"] == "not_found" },
                            toStorage  = techTasks.count { it["status"] == "to_storage" }
                        )
                    }.sortedByDescending { it.done }

                    session.copy(totalCount = sessionTasks.size, technicians = techList)
                }

                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        activeSessions  = updatedActive,
                        totalToday      = total,
                        doneToday       = done,
                        inProgressToday = inProg,
                        toStorageToday  = toStore,
                        notFoundToday   = notFound,
                        storageScooters = storageList
                    )
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        sessionsListener?.remove()
        tasksListener?.remove()
    }
}