package com.example.qrscannerapp.features.profile.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.features.profile.domain.model.EmployeeProfileUiState
import com.example.qrscannerapp.features.profile.domain.model.UserActivityLog
import com.example.qrscannerapp.features.profile.domain.model.UserProfile
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EmployeeProfileViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val userId: String = savedStateHandle.get<String>("userId")!!
    private val db = Firebase.firestore

    private var activityListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(EmployeeProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (userId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Ошибка: ID пользователя не найден.") }
        } else {
            loadUserProfile()
            listenForLastActivity()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userDoc = db.collection("internal_users").document(userId).get().await()
                if (!userDoc.exists()) throw Exception("User document not found")

                val dateOfBirthStr = userDoc.getString("dateOfBirth")
                val age = if (dateOfBirthStr != null) calculateAge(dateOfBirthStr) else 0

                val profile = UserProfile(
                    name = userDoc.getString("displayName") ?: "Без имени",
                    username = userDoc.getString("username") ?: "",
                    role = if (userDoc.getBoolean("isAdmin") == true) "Администратор" else "Сотрудник",
                    age = age,
                    deviceInfo = userDoc.getString("deviceInfo") ?: "Нет данных",
                    appVersion = userDoc.getString("appVersion") ?: "-",
                    lastBatteryLevel = userDoc.getLong("lastBatteryLevel")?.toInt() ?: -1
                )

                _uiState.update { it.copy(userProfile = profile) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить профиль пользователя.") }
            }
        }
    }

    private fun listenForLastActivity() {
        activityListener?.remove()

        activityListener = db.collection("activity_log")
            .whereEqualTo("creatorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EmployeeProfileVM", "Listen for last activity failed", error)
                    _uiState.update { it.copy(isLoading = false, error = "Ошибка загрузки активности.") }
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents[0]

                    // --- НАЧАЛО ДОБАВЛЕНИЙ: Считываем новые поля из документа ---
                    val lastActivity = UserActivityLog(
                        id = doc.id,
                        timestamp = doc.getLong("timestamp") ?: 0,
                        activityType = doc.getString("activityType") ?: "UNKNOWN",
                        itemCount = doc.getLong("itemCount")?.toInt() ?: 0,
                        manualEntryCount = doc.getLong("manualEntryCount")?.toInt() ?: 0,
                        durationSeconds = doc.getLong("durationSeconds") ?: 0,
                        // Старая телеметрия
                        appVersion = doc.getString("appVersion") ?: "N/A",
                        lastBatteryLevel = doc.getLong("lastBatteryLevel")?.toInt() ?: -1,
                        isCharging = doc.getBoolean("isCharging") ?: false,
                        networkState = doc.getString("networkState") ?: "N/A",
                        freeRam = doc.getString("freeRam") ?: "N/A",
                        freeStorage = doc.getString("freeStorage") ?: "N/A",
                        deviceUptime = doc.getString("deviceUptime") ?: "N/A",
                        // Новая телеметрия
                        batteryHealth = doc.getString("batteryHealth") ?: "N/A",
                        isPowerSaveMode = doc.getBoolean("isPowerSaveMode") ?: false,
                        networkPing = doc.getString("networkPing") ?: "N/A"
                    )
                    // --- КОНЕЦ ДОБАВЛЕНИЙ ---

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            activityHistory = listOf(lastActivity)
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, activityHistory = emptyList()) }
                }
            }
    }

    private fun calculateAge(dateOfBirthString: String): Int {
        return try {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val birthDate = sdf.parse(dateOfBirthString)!!

            val birthCal = Calendar.getInstance().apply { time = birthDate }
            val todayCal = Calendar.getInstance()

            var age = todayCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (todayCal.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) {
            0
        }
    }

    override fun onCleared() {
        super.onCleared()
        activityListener?.remove()
    }
}