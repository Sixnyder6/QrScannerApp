package com.example.qrscannerapp.features.street_doctor.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SessionReportViewModel @Inject constructor() : ViewModel() {

    private val db  = Firebase.firestore
    private val TAG = "SessionReportVM"

    private val _uiState = MutableStateFlow(SessionReportUiState())
    val uiState = _uiState.asStateFlow()

    fun loadReport(sessionId: String) {
        if (_uiState.value.sessionId == sessionId && !_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, sessionId = sessionId) }

        viewModelScope.launch {
            try {
                val snapshot = db.collection("field_repair_tasks")
                    .whereEqualTo("sessionId", sessionId)
                    .get().await()

                val scooters = snapshot.documents.mapNotNull { doc ->
                    try {
                        val battReal = doc.getLong("batteryPctReal")?.toInt()?.takeIf { it > 0 } ?: 0
                        val battExcel = when (val v = doc.get("charge")) {
                            is Long -> v.toInt(); is Double -> v.toInt()
                            is String -> v.toIntOrNull() ?: 0; else -> 0
                        }
                        val lat = when (val v = doc.get("lat")) {
                            is Double -> v; is String -> v.toDoubleOrNull() ?: 0.0
                            is Long -> v.toDouble(); else -> 0.0
                        }
                        val lon = when (val v = doc.get("lon")) {
                            is Double -> v; is String -> v.toDoubleOrNull() ?: 0.0
                            is Long -> v.toDouble(); else -> 0.0
                        }
                        @Suppress("UNCHECKED_CAST")
                        val repairTypes = doc.get("repairTypes") as? List<String> ?: emptyList()

                        ReportScooter(
                            id             = doc.id,
                            code           = doc.getString("scooterNumber") ?: "",
                            status         = doc.getString("status") ?: "new",
                            technicianName = doc.getString("assignedToName") ?: "Неизвестный",
                            technicianId   = doc.getString("assignedToId") ?: "",
                            repairTypes    = repairTypes,
                            batteryPct     = battExcel,
                            batteryReal    = battReal,
                            lat            = lat,
                            lon            = lon,
                            notes          = doc.getString("notes") ?: "",
                            completedAt    = doc.getLong("completedAt") ?: 0L,
                            model          = doc.getString("model") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "parse error ${doc.id}", e)
                        null
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading  = false,
                        scooters   = scooters,
                        totalCount = scooters.size
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadReport error", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}