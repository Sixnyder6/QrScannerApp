package com.example.qrscannerapp.features.profile.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.qrscannerapp.TelemetryManager
import com.example.qrscannerapp.data.repository.TelemetryRepository
import com.example.qrscannerapp.features.profile.data.repository.EmployeeProfileRepository
import com.example.qrscannerapp.features.profile.domain.model.EmployeeProfileUiState
import com.example.qrscannerapp.features.profile.domain.model.InteractionStats
import com.example.qrscannerapp.features.profile.domain.model.OperationStat
import com.example.qrscannerapp.worker.TelemetryWorker
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class EmployeeProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmployeeProfileRepository,
    private val firestore: FirebaseFirestore,
    private val telemetryManager: TelemetryManager,
    private val telemetryRepository: TelemetryRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val userId: String = savedStateHandle.get<String>("userId")!!

    private val _uiState = MutableStateFlow(EmployeeProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _bufferedTelemetryCount = MutableStateFlow(0)
    val bufferedTelemetryCount = _bufferedTelemetryCount.asStateFlow()

    init {
        if (userId.isBlank()) {
            _uiState.update {
                it.copy(isLoading = false, error = "Ошибка: ID пользователя не найден.")
            }
        } else {
            loadInitialData()
            loadInteractionStats()
            setupTelemetryWorker()
            loadBufferedTelemetryCount()
        }
    }

    // ====================================================================
    // ТЕЛЕМЕТРИЯ (без согласия)
    // ====================================================================

    private fun setupTelemetryWorker() {
        val workRequest = PeriodicWorkRequestBuilder<TelemetryWorker>(
            1, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "telemetry_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun loadBufferedTelemetryCount() {
        viewModelScope.launch {
            _bufferedTelemetryCount.value = telemetryRepository.getBufferCount()
        }
    }

    fun saveTelemetrySnapshot() {
        viewModelScope.launch {
            try {
                val telemetry = telemetryManager.getAllTelemetry(includePing = true)
                telemetryRepository.saveTelemetry(telemetry)

                _bufferedTelemetryCount.value = telemetryRepository.getBufferCount()
                Log.d("Telemetry", "Снимок телеметрии сохранён в буфер")
            } catch (e: Exception) {
                Log.e("Telemetry", "Ошибка сохранения телеметрии", e)
            }
        }
    }

    fun sendBufferedTelemetry() {
        viewModelScope.launch {
            try {
                val result = telemetryRepository.sendBufferedTelemetry()
                result.fold(
                    onSuccess = { count ->
                        _bufferedTelemetryCount.value = telemetryRepository.getBufferCount()
                        Log.d("Telemetry", "Отправлено $count записей телеметрии")
                    },
                    onFailure = { error ->
                        Log.e("Telemetry", "Ошибка отправки телеметрии", error)
                    }
                )
            } catch (e: Exception) {
                Log.e("Telemetry", "Ошибка отправки буфера", e)
            }
        }
    }

    // ====================================================================
    // СТАТИСТИКА ВЗАИМОДЕЙСТВИЙ
    // ====================================================================

    private fun loadInteractionStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isStatsLoading = true) }
            try {
                val snapshot = firestore.collection("interaction_sessions")
                    .whereEqualTo("creatorId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val grouped = snapshot.documents.groupBy { doc ->
                    doc.getString("operationType") ?: "UNKNOWN"
                }

                val stats = grouped.map { (opType, docs) ->
                    val totalCount = docs.sumOf { doc ->
                        doc.getLong("scooterCount")?.toInt() ?: 0
                    }
                    val lastTimestamp = docs.mapNotNull { it.getLong("timestamp") }.maxOrNull() ?: 0L

                    OperationStat(
                        operationType = opType,
                        totalCount = totalCount,
                        sessionCount = docs.size,
                        lastTimestamp = lastTimestamp
                    )
                }.sortedByDescending { it.totalCount }

                val totalAllTime = stats.sumOf { it.totalCount }

                _uiState.update {
                    it.copy(
                        interactionStats = InteractionStats(
                            stats = stats,
                            totalAllTime = totalAllTime
                        ),
                        isStatsLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("EmployeeProfileVM", "Ошибка загрузки статистики операций", e)
                _uiState.update { it.copy(isStatsLoading = false) }
            }
        }
    }

    // ====================================================================
    // ПУЛЬТ АДМИНА
    // ====================================================================

    fun forceEndShift() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val userRef = firestore.collection("internal_users").document(userId)
                val snapshot = userRef.get().await()
                val activeShiftId = snapshot.getString("activeShiftId")
                val userUpdates = mapOf(
                    "isShiftActive" to FieldValue.delete(),
                    "shiftStartTime" to FieldValue.delete(),
                    "activeShiftId" to FieldValue.delete()
                )
                if (activeShiftId != null) {
                    val shiftRef = firestore.collection("shifts").document(activeShiftId)
                    firestore.runTransaction { transaction ->
                        transaction.update(userRef, userUpdates)
                        transaction.update(
                            shiftRef, mapOf(
                                "endTime" to System.currentTimeMillis(),
                                "status" to "COMPLETED"
                            )
                        )
                    }.await()
                } else {
                    userRef.update(userUpdates).await()
                }
                Log.d("AdminControl", "Смена принудительно завершена для $userId")
                loadInitialData()
            } catch (e: Exception) {
                Log.e("AdminControl", "Ошибка завершения смены", e)
                _uiState.update {
                    it.copy(error = "Ошибка закрытия смены: ${e.message}", isLoading = false)
                }
            }
        }
    }

    fun setWorkAccess(isAllowed: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val updates = mutableMapOf<String, Any>(
                    "isAllowedToWork" to isAllowed
                )
                if (isAllowed) updates["shiftRequestStatus"] = "APPROVED"
                else updates["shiftRequestStatus"] = "NONE"
                firestore.collection("internal_users").document(userId).update(updates).await()
                Log.d("AdminControl", "Доступ изменен на $isAllowed для $userId")
                loadInitialData()
            } catch (e: Exception) {
                Log.e("AdminControl", "Ошибка изменения доступа", e)
                _uiState.update {
                    it.copy(error = "Ошибка доступа: ${e.message}", isLoading = false)
                }
            }
        }
    }

    // ====================================================================
    // ЗАГРУЗКА ДАННЫХ
    // ====================================================================

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.getUserProfile(userId)
                .onSuccess { userProfile ->
                    _uiState.update { it.copy(userProfile = userProfile) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Не удалось загрузить профиль: ${error.message}"
                        )
                    }
                    return@launch
                }

            listenForLastActivity()
        }
    }

    private fun listenForLastActivity() {
        repository.listenForLastActivity(userId)
            .onEach { result ->
                result
                    .onSuccess { lastActivityLog ->
                        val performanceDetails = repository.getPerformanceDetails(lastActivityLog)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                activityHistory = listOfNotNull(lastActivityLog),
                                performanceDetails = performanceDetails
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Ошибка загрузки активности: ${error.message}"
                            )
                        }
                    }
            }
            .launchIn(viewModelScope)
    }
}