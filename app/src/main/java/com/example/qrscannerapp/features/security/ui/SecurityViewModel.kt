package com.example.qrscannerapp.features.security.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.TelemetryManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

// ============================================================================================
// КОНСТАНТЫ
// ============================================================================================

private const val TAG = "SecurityVM"

object SecurityHubs {
    const val BESTUZH = "bestuzh"
    const val SOFIY = "sofiy"
    fun displayName(id: String) = when (id) {
        BESTUZH -> "Бестужевская 10Б"
        SOFIY   -> "Софийская 62 (Ферма)"
        else    -> id
    }
}

// ============================================================================================
// ТЕГИ САМОКАТА
// ============================================================================================

enum class ScooterTag(
    val key: String,
    val label: String,
    val emoji: String,
    val colorHex: String
) {
    STOLEN    ("stolen",     "Угон",       "!", "#F44336"),
    BURNED    ("burned",     "Поджог",     "!", "#FF5722"),
    OPENED    ("opened",     "Вскрытый",   "!", "#FF9800"),
    NO_BATTERY("no_battery", "Без АКБ",   "!", "#9E9E9E"),
    DROWNED   ("drowned",    "Утопленник", "!", "#2196F3"),
    FRAME     ("frame",      "Рама",       "!", "#607D8B"),
    DAMAGED   ("damaged",    "Повреждён",  "!", "#E91E63"),
    FOUND     ("found",      "Найден",     "+", "#4CAF50");

    companion object {
        fun fromKey(key: String) = entries.find { it.key == key }
    }
}

// ============================================================================================
// МОДЕЛИ ДАННЫХ
// ============================================================================================

data class ScooterCoords(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class ScooterHistoryEntry(
    val action: String = "",
    val byUserId: String = "",
    val byUserName: String = "",
    val timestamp: Long = 0L,
    val note: String? = null
)

data class ScooterPassport(
    val scooterId: String = "",
    val tags: List<String> = emptyList(),
    val status: String = "lost",
    val foundAt: String? = null,
    val coords: ScooterCoords? = null,
    val foundBy: String? = null,
    val foundByName: String? = null,
    val deliveredToHub: String? = null,
    val historyLog: List<ScooterHistoryEntry> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val notes: String? = null,
    // Данные из флита (заполняются если источник — fleet_vehicles)
    val fleetCharge: Int? = null,
    val fleetModel: String? = null,
    val fleetLat: Double? = null,
    val fleetLon: Double? = null,
    val isFromFleet: Boolean = false
)

data class LostScooterEntry(
    val scooterId: String = "",
    val addedAt: Long = 0L,
    val addedBy: String = "",
    val addedByName: String = "",
    val searchAttempts: List<String> = emptyList(),
    val lastSearchedBy: String? = null,
    val lastSearchedAt: Long? = null
)

data class EquipmentIssue(
    val id: String = "",
    val type: String = "battery",
    val items: List<String> = emptyList(),
    val issuedTo: String = "",
    val issuedToName: String = "",
    val issuedBy: String = "",
    val issuedByName: String = "",
    val issuedAt: Long = 0L,
    val returnedAt: Long? = null,
    val status: String = "active",
    val hub: String = SecurityHubs.BESTUZH,
    val notes: String? = null
)

// ============================================================================================
// UI-СТЕЙТЫ
// ============================================================================================

data class SecurityScootersUiState(
    val isLoading: Boolean = true,
    val lostScooters: List<ScooterPassport> = emptyList(),
    val foundScooters: List<ScooterPassport> = emptyList(),
    val historyAll: List<ScooterPassport> = emptyList(),
    val selectedPassport: ScooterPassport? = null,
    val error: String? = null,
    val successMessage: String? = null
)

data class SecurityBatteryUiState(
    val isLoading: Boolean = true,
    val activeIssues: List<EquipmentIssue> = emptyList(),
    val returnedIssues: List<EquipmentIssue> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

data class SecurityStorageUiState(
    val isLoading: Boolean = true,
    val cells: List<SecurityStorageCell> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

data class SecurityStorageCell(
    val id: String = "",
    val name: String = "",
    val hub: String = SecurityHubs.BESTUZH,
    val items: List<String> = emptyList(),
    val capacity: Int = 50,
    val description: String? = null
)

data class SecurityDashboardUiState(
    val isLoading: Boolean = true,
    val totalLost: Int = 0,
    val totalFound: Int = 0,
    val foundToday: Int = 0,
    val lostToday: Int = 0,
    val tagBreakdown: Map<ScooterTag, Int> = emptyMap(),
    val hubBreakdown: Map<String, Int> = emptyMap(),
    val topFinders: List<Pair<String, Int>> = emptyList(),
    val activeEquipmentIssues: Int = 0,
    val recentActivity: List<ScooterPassport> = emptyList(),
    val error: String? = null
)

// ============================================================================================
// VIEWMODEL
// ============================================================================================

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val telemetryManager: TelemetryManager
) : ViewModel() {

    private val firestore = Firebase.firestore

    private val _scootersState = MutableStateFlow(SecurityScootersUiState())
    val scootersState: StateFlow<SecurityScootersUiState> = _scootersState.asStateFlow()

    private val _batteryState = MutableStateFlow(SecurityBatteryUiState())
    val batteryState: StateFlow<SecurityBatteryUiState> = _batteryState.asStateFlow()

    private val _storageState = MutableStateFlow(SecurityStorageUiState())
    val storageState: StateFlow<SecurityStorageUiState> = _storageState.asStateFlow()

    private val _dashboardState = MutableStateFlow(SecurityDashboardUiState())
    val dashboardState: StateFlow<SecurityDashboardUiState> = _dashboardState.asStateFlow()

    private val _passportState = MutableStateFlow<ScooterPassport?>(null)
    val passportState: StateFlow<ScooterPassport?> = _passportState.asStateFlow()

    private val _isOperating = MutableStateFlow(false)
    val isOperating: StateFlow<Boolean> = _isOperating.asStateFlow()

    // Кэш самокатов из флита с process=СБ
    private val _fleetLostScooters = MutableStateFlow<List<ScooterPassport>>(emptyList())

    private var passportsListener:     ListenerRegistration? = null
    private var fleetLostListener:     ListenerRegistration? = null
    private var equipmentListener:     ListenerRegistration? = null
    private var storageListener:       ListenerRegistration? = null
    private var singlePassportListener: ListenerRegistration? = null

    // ============================================================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================================================

    init {
        startPassportsListener()
        startFleetLostListener()
        startEquipmentListener()
        startStorageListener()
    }

    // ============================================================================================
    // ПАСПОРТА САМОКАТОВ — realtime listener
    // ============================================================================================

    private fun startPassportsListener() {
        passportsListener?.remove()
        passportsListener = firestore.collection("scooter_passports")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(500)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Passports listen failed", error)
                    _scootersState.update { it.copy(isLoading = false, error = "Ошибка загрузки: ${error.message}") }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val passports = snapshot.documents.mapNotNull { doc ->
                        try {
                            val tags = (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            val historyRaw = (doc.get("historyLog") as? List<*>) ?: emptyList<Any>()
                            val history = historyRaw.mapNotNull { entry ->
                                (entry as? Map<*, *>)?.let { m ->
                                    ScooterHistoryEntry(
                                        action     = m["action"] as? String ?: "",
                                        byUserId   = m["byUserId"] as? String ?: "",
                                        byUserName = m["byUserName"] as? String ?: "",
                                        timestamp  = m["timestamp"] as? Long ?: 0L,
                                        note       = m["note"] as? String
                                    )
                                }
                            }
                            val coordsRaw = doc.get("coords") as? Map<*, *>
                            val coords = coordsRaw?.let {
                                ScooterCoords(
                                    lat = (it["lat"] as? Double) ?: 0.0,
                                    lng = (it["lng"] as? Double) ?: 0.0
                                )
                            }
                            ScooterPassport(
                                scooterId      = doc.getString("scooterId") ?: doc.id,
                                tags           = tags,
                                status         = doc.getString("status") ?: "lost",
                                foundAt        = doc.getString("foundAt"),
                                coords         = coords,
                                foundBy        = doc.getString("foundBy"),
                                foundByName    = doc.getString("foundByName"),
                                deliveredToHub = doc.getString("deliveredToHub"),
                                historyLog     = history,
                                createdAt      = doc.getLong("createdAt") ?: 0L,
                                updatedAt      = doc.getLong("updatedAt") ?: 0L,
                                notes          = doc.getString("notes")
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing passport ${doc.id}", e)
                            null
                        }
                    }
                    rebuildScootersState(passports)
                }
            }
    }

    // ============================================================================================
    // ФЛИТ — самокаты СБ в розыске
    // ============================================================================================

    private fun startFleetLostListener() {
        fleetLostListener?.remove()
        fleetLostListener = firestore.collection("fleet_vehicles")
            .whereEqualTo("process", "СБ")
            .whereEqualTo("processStage", "Поиск (lost)")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Fleet lost listen failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val fleetPassports = snapshot.documents.mapNotNull { doc ->
                        try {
                            val number = doc.getString("number")?.trim()
                            if (number.isNullOrBlank()) return@mapNotNull null

                            val lat    = doc.getDouble("lat") ?: 0.0
                            val lon    = doc.getDouble("lon") ?: 0.0
                            val charge = (doc.getLong("charge") ?: 0L).toInt()
                            val model  = doc.getString("model") ?: ""
                            val importedAt = doc.getLong("importedAt") ?: 0L

                            ScooterPassport(
                                scooterId   = number,
                                status      = "lost",
                                notes       = "Источник: Флит · Модель: $model",
                                coords      = if (lat != 0.0 || lon != 0.0) ScooterCoords(lat, lon) else null,
                                foundAt     = if (lat != 0.0 || lon != 0.0) "${"%.6f".format(lat)}, ${"%.6f".format(lon)}" else null,
                                createdAt   = importedAt,
                                updatedAt   = importedAt,
                                fleetCharge = charge,
                                fleetModel  = model,
                                fleetLat    = if (lat != 0.0) lat else null,
                                fleetLon    = if (lon != 0.0) lon else null,
                                isFromFleet = true
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing fleet vehicle ${doc.id}", e)
                            null
                        }
                    }
                    Log.d(TAG, "Fleet lost scooters loaded: ${fleetPassports.size}")
                    _fleetLostScooters.value = fleetPassports

                    // Пересобираем стейт — объединяем с паспортами СБ
                    val currentPassports = _scootersState.value.historyAll
                    rebuildScootersState(currentPassports)
                }
            }
    }

    // ============================================================================================
    // ОБЪЕДИНЕНИЕ ПАСПОРТОВ И ФЛИТА
    // ============================================================================================

    private fun rebuildScootersState(passports: List<ScooterPassport>) {
        val lost  = passports.filter { it.status == "lost" }
        val found = passports.filter { it.status == "found" }

        // Объединяем утерянных из паспортов СБ и флита
        // Если самокат есть в обоих — приоритет у паспорта СБ (он содержит историю и теги)
        val sbIds = lost.map { it.scooterId }.toSet()
        val fleetOnly = _fleetLostScooters.value.filter { it.scooterId !in sbIds }
        val allLost = (lost + fleetOnly).sortedByDescending { it.updatedAt }

        _scootersState.update {
            it.copy(
                isLoading     = false,
                lostScooters  = allLost,
                foundScooters = found,
                historyAll    = passports
            )
        }
        updateDashboardFromPassports(passports, allLost)
    }

    // ============================================================================================
    // ПАСПОРТ КОНКРЕТНОГО САМОКАТА
    // ============================================================================================

    fun watchPassport(scooterId: String) {
        singlePassportListener?.remove()
        singlePassportListener = firestore.collection("scooter_passports")
            .whereEqualTo("scooterId", scooterId)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                val doc = snapshot?.documents?.firstOrNull()
                if (doc != null) {
                    val tags = (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val historyRaw = (doc.get("historyLog") as? List<*>) ?: emptyList<Any>()
                    val history = historyRaw.mapNotNull { entry ->
                        (entry as? Map<*, *>)?.let { m ->
                            ScooterHistoryEntry(
                                action     = m["action"] as? String ?: "",
                                byUserId   = m["byUserId"] as? String ?: "",
                                byUserName = m["byUserName"] as? String ?: "",
                                timestamp  = m["timestamp"] as? Long ?: 0L,
                                note       = m["note"] as? String
                            )
                        }
                    }
                    val coordsRaw = doc.get("coords") as? Map<*, *>
                    val coords = coordsRaw?.let {
                        ScooterCoords(
                            lat = (it["lat"] as? Double) ?: 0.0,
                            lng = (it["lng"] as? Double) ?: 0.0
                        )
                    }

                    // Ищем данные флита для этого самоката
                    val fleetData = _fleetLostScooters.value.find { it.scooterId == scooterId }

                    _passportState.value = ScooterPassport(
                        scooterId      = doc.getString("scooterId") ?: doc.id,
                        tags           = tags,
                        status         = doc.getString("status") ?: "lost",
                        foundAt        = doc.getString("foundAt"),
                        coords         = coords,
                        foundBy        = doc.getString("foundBy"),
                        foundByName    = doc.getString("foundByName"),
                        deliveredToHub = doc.getString("deliveredToHub"),
                        historyLog     = history,
                        createdAt      = doc.getLong("createdAt") ?: 0L,
                        updatedAt      = doc.getLong("updatedAt") ?: 0L,
                        notes          = doc.getString("notes"),
                        // Добавляем данные флита если есть
                        fleetCharge    = fleetData?.fleetCharge,
                        fleetModel     = fleetData?.fleetModel,
                        fleetLat       = fleetData?.fleetLat,
                        fleetLon       = fleetData?.fleetLon
                    )
                } else {
                    // Паспорта нет в СБ — проверяем флит
                    val fleetPassport = _fleetLostScooters.value.find { it.scooterId == scooterId }
                    _passportState.value = fleetPassport
                }
            }
    }

    fun stopWatchingPassport() {
        singlePassportListener?.remove()
        singlePassportListener = null
        _passportState.value = null
    }

    // ============================================================================================
    // ОПЕРАЦИИ С ПАСПОРТАМИ
    // ============================================================================================

    fun addLostScooter(
        scooterId: String,
        tags: List<ScooterTag> = emptyList(),
        notes: String? = null
    ) {
        val auth = authManager.authState.value
        if (auth.userId == null) return
        _isOperating.value = true

        viewModelScope.launch {
            try {
                val location = telemetryManager.getCurrentLocation()
                val now = System.currentTimeMillis()
                val historyEntry = mapOf(
                    "action"     to "ADDED_LOST",
                    "byUserId"   to auth.userId,
                    "byUserName" to (auth.userName ?: "СБ"),
                    "timestamp"  to now,
                    "note"       to notes
                )
                val passportData = hashMapOf<String, Any?>(
                    "scooterId"  to scooterId,
                    "tags"       to tags.map { it.key },
                    "status"     to "lost",
                    "createdAt"  to now,
                    "updatedAt"  to now,
                    "notes"      to notes,
                    "historyLog" to listOf(historyEntry)
                )
                if (location != null) {
                    passportData["coords"] = mapOf("lat" to location.latitude, "lng" to location.longitude)
                }
                firestore.collection("scooter_passports").document(scooterId).set(passportData).await()
                firestore.collection("security_lost_list").document(scooterId).set(
                    mapOf(
                        "scooterId"      to scooterId,
                        "addedAt"        to now,
                        "addedBy"        to auth.userId,
                        "addedByName"    to (auth.userName ?: "СБ"),
                        "searchAttempts" to emptyList<String>()
                    )
                ).await()
                _scootersState.update { it.copy(successMessage = "Самокат $scooterId добавлен в базу") }
            } catch (e: Exception) {
                Log.e(TAG, "addLostScooter error", e)
                _scootersState.update { it.copy(error = "Ошибка: ${e.message}") }
            } finally {
                _isOperating.value = false
            }
        }
    }

    fun markScooterFound(
        scooterId: String,
        foundAt: String,
        deliveredToHub: String,
        coords: ScooterCoords? = null,
        additionalTags: List<ScooterTag> = emptyList(),
        notes: String? = null
    ) {
        val auth = authManager.authState.value
        if (auth.userId == null) return
        _isOperating.value = true

        viewModelScope.launch {
            try {
                val finalCoords = coords ?: telemetryManager.getCurrentLocation()?.let {
                    ScooterCoords(lat = it.latitude, lng = it.longitude)
                }
                val now = System.currentTimeMillis()
                val historyEntry = mapOf(
                    "action"     to "MARKED_FOUND",
                    "byUserId"   to auth.userId,
                    "byUserName" to (auth.userName ?: "СБ"),
                    "timestamp"  to now,
                    "note"       to notes
                )
                val updates = mutableMapOf<String, Any?>(
                    "status"         to "found",
                    "foundAt"        to foundAt,
                    "deliveredToHub" to deliveredToHub,
                    "foundBy"        to auth.userId,
                    "foundByName"    to (auth.userName ?: "СБ"),
                    "updatedAt"      to now,
                    "historyLog"     to FieldValue.arrayUnion(historyEntry)
                )
                if (finalCoords != null) {
                    updates["coords"] = mapOf("lat" to finalCoords.lat, "lng" to finalCoords.lng)
                }
                if (additionalTags.isNotEmpty()) {
                    updates["tags"] = FieldValue.arrayUnion(*additionalTags.map { it.key }.toTypedArray())
                }
                if (notes != null) updates["notes"] = notes

                firestore.collection("scooter_passports").document(scooterId).update(updates).await()
                firestore.collection("security_lost_list").document(scooterId).delete().await()
                _scootersState.update { it.copy(successMessage = "Самокат $scooterId помечен найденным") }
            } catch (e: Exception) {
                Log.e(TAG, "markScooterFound error", e)
                _scootersState.update { it.copy(error = "Ошибка: ${e.message}") }
            } finally {
                _isOperating.value = false
            }
        }
    }

    fun updateScooterTags(scooterId: String, tags: List<ScooterTag>) {
        val auth = authManager.authState.value
        _isOperating.value = true
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val historyEntry = mapOf(
                    "action"     to "TAGS_UPDATED",
                    "byUserId"   to (auth.userId ?: ""),
                    "byUserName" to (auth.userName ?: "СБ"),
                    "timestamp"  to now,
                    "note"       to "Теги: ${tags.joinToString { it.label }}"
                )
                firestore.collection("scooter_passports").document(scooterId).update(
                    "tags", tags.map { it.key },
                    "updatedAt", now,
                    "historyLog", FieldValue.arrayUnion(historyEntry)
                ).await()
                _scootersState.update { it.copy(successMessage = "Теги обновлены") }
            } catch (e: Exception) {
                _scootersState.update { it.copy(error = "Ошибка обновления тегов: ${e.message}") }
            } finally {
                _isOperating.value = false
            }
        }
    }

    fun logSearchAttempt(scooterId: String, note: String? = null) {
        val auth = authManager.authState.value
        if (auth.userId == null) return
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val attemptNote = "${auth.userName ?: "?"} — ${note ?: "Поиск"} (${formatTimestamp(now)})"
                firestore.collection("security_lost_list").document(scooterId).update(
                    "searchAttempts", FieldValue.arrayUnion(attemptNote),
                    "lastSearchedBy", auth.userId,
                    "lastSearchedAt", now
                ).await()
                val historyEntry = mapOf(
                    "action"     to "SEARCH_ATTEMPT",
                    "byUserId"   to auth.userId,
                    "byUserName" to (auth.userName ?: "СБ"),
                    "timestamp"  to now,
                    "note"       to note
                )
                firestore.collection("scooter_passports").document(scooterId).update(
                    "updatedAt", now,
                    "historyLog", FieldValue.arrayUnion(historyEntry)
                ).await()
            } catch (e: Exception) {
                Log.e(TAG, "logSearchAttempt error", e)
            }
        }
    }

    fun deletePassport(scooterId: String) {
        _isOperating.value = true
        viewModelScope.launch {
            try {
                firestore.collection("scooter_passports").document(scooterId).delete().await()
                firestore.collection("security_lost_list").document(scooterId).delete().await()
                _scootersState.update { it.copy(successMessage = "Запись $scooterId удалена") }
            } catch (e: Exception) {
                _scootersState.update { it.copy(error = "Ошибка удаления: ${e.message}") }
            } finally {
                _isOperating.value = false
            }
        }
    }

    // ============================================================================================
    // ОБОРУДОВАНИЕ
    // ============================================================================================

    private fun startEquipmentListener() {
        equipmentListener?.remove()
        equipmentListener = firestore.collection("equipment_issues")
            .orderBy("issuedAt", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _batteryState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val issues = snapshot.documents.mapNotNull { doc ->
                        try {
                            val items = (doc.get("items") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            EquipmentIssue(
                                id           = doc.id,
                                type         = doc.getString("type") ?: "battery",
                                items        = items,
                                issuedTo     = doc.getString("issuedTo") ?: "",
                                issuedToName = doc.getString("issuedToName") ?: "",
                                issuedBy     = doc.getString("issuedBy") ?: "",
                                issuedByName = doc.getString("issuedByName") ?: "",
                                issuedAt     = doc.getLong("issuedAt") ?: 0L,
                                returnedAt   = doc.getLong("returnedAt"),
                                status       = doc.getString("status") ?: "active",
                                hub          = doc.getString("hub") ?: SecurityHubs.BESTUZH,
                                notes        = doc.getString("notes")
                            )
                        } catch (e: Exception) { null }
                    }
                    val active   = issues.filter { it.status == "active" }
                    val returned = issues.filter { it.status == "returned" }
                    _batteryState.update { it.copy(isLoading = false, activeIssues = active, returnedIssues = returned) }
                    _dashboardState.update { it.copy(activeEquipmentIssues = active.size) }
                }
            }
    }

    fun issueEquipment(
        type: String,
        items: List<String>,
        issuedToId: String,
        issuedToName: String,
        hub: String = SecurityHubs.BESTUZH,
        notes: String? = null
    ) {
        val auth = authManager.authState.value
        if (auth.userId == null) return
        _isOperating.value = true
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                firestore.collection("equipment_issues").document(UUID.randomUUID().toString()).set(
                    hashMapOf(
                        "type"         to type,
                        "items"        to items,
                        "issuedTo"     to issuedToId,
                        "issuedToName" to issuedToName,
                        "issuedBy"     to auth.userId,
                        "issuedByName" to (auth.userName ?: "СБ"),
                        "issuedAt"     to now,
                        "status"       to "active",
                        "hub"          to hub,
                        "notes"        to notes
                    )
                ).await()
                _batteryState.update { it.copy(successMessage = "Выдано ${items.size} единиц → $issuedToName") }
            } catch (e: Exception) {
                _batteryState.update { it.copy(error = "Ошибка выдачи: ${e.message}") }
            } finally {
                _isOperating.value = false
            }
        }
    }

    fun returnEquipment(issueId: String) {
        _isOperating.value = true
        viewModelScope.launch {
            try {
                firestore.collection("equipment_issues").document(issueId).update(
                    "status", "returned",
                    "returnedAt", System.currentTimeMillis()
                ).await()
                _batteryState.update { it.copy(successMessage = "Возврат зафиксирован") }
            } catch (e: Exception) {
                _batteryState.update { it.copy(error = "Ошибка: ${e.message}") }
            } finally {
                _isOperating.value = false
            }
        }
    }

    // ============================================================================================
    // СКЛАД
    // ============================================================================================

    private fun startStorageListener() {
        storageListener?.remove()
        storageListener = firestore.collection("security_storage_cells")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _storageState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val cells = snapshot.documents.mapNotNull { doc ->
                        try {
                            val items = (doc.get("items") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            SecurityStorageCell(
                                id          = doc.id,
                                name        = doc.getString("name") ?: "Ячейка",
                                hub         = doc.getString("hub") ?: SecurityHubs.BESTUZH,
                                items       = items,
                                capacity    = (doc.getLong("capacity") ?: 50L).toInt(),
                                description = doc.getString("description")
                            )
                        } catch (e: Exception) { null }
                    }
                    _storageState.update { it.copy(isLoading = false, cells = cells) }
                }
            }
    }

    fun createStorageCell(name: String, hub: String, capacity: Int, description: String? = null) {
        val auth = authManager.authState.value
        _isOperating.value = true
        viewModelScope.launch {
            try {
                firestore.collection("security_storage_cells").document(UUID.randomUUID().toString()).set(
                    hashMapOf(
                        "name"        to name,
                        "hub"         to hub,
                        "items"       to emptyList<String>(),
                        "capacity"    to capacity,
                        "description" to description,
                        "createdBy"   to (auth.userId ?: ""),
                        "createdAt"   to System.currentTimeMillis()
                    )
                ).await()
                _storageState.update { it.copy(successMessage = "Ячейка '$name' создана") }
            } catch (e: Exception) {
                _storageState.update { it.copy(error = "Ошибка: ${e.message}") }
            } finally {
                _isOperating.value = false
            }
        }
    }

    fun addItemToStorageCell(cellId: String, itemCode: String) {
        viewModelScope.launch {
            try {
                firestore.collection("security_storage_cells").document(cellId)
                    .update("items", FieldValue.arrayUnion(itemCode)).await()
            } catch (e: Exception) {
                _storageState.update { it.copy(error = "Ошибка добавления: ${e.message}") }
            }
        }
    }

    fun removeItemFromStorageCell(cellId: String, itemCode: String) {
        viewModelScope.launch {
            try {
                firestore.collection("security_storage_cells").document(cellId)
                    .update("items", FieldValue.arrayRemove(itemCode)).await()
            } catch (e: Exception) {
                _storageState.update { it.copy(error = "Ошибка удаления: ${e.message}") }
            }
        }
    }

    fun deleteStorageCell(cellId: String) {
        _isOperating.value = true
        viewModelScope.launch {
            try {
                firestore.collection("security_storage_cells").document(cellId).delete().await()
                _storageState.update { it.copy(successMessage = "Ячейка удалена") }
            } catch (e: Exception) {
                _storageState.update { it.copy(error = "Ошибка: ${e.message}") }
            } finally {
                _isOperating.value = false
            }
        }
    }

    // ============================================================================================
    // ДАШБОРД
    // ============================================================================================

    private fun updateDashboardFromPassports(
        passports: List<ScooterPassport>,
        allLost: List<ScooterPassport> = passports.filter { it.status == "lost" }
    ) {
        val now        = System.currentTimeMillis()
        val todayStart = now - 24 * 60 * 60 * 1000L

        val found      = passports.filter { it.status == "found" }
        val foundToday = found.count { it.updatedAt >= todayStart }
        val lostToday  = allLost.count { it.createdAt >= todayStart }

        val tagBreakdown = ScooterTag.entries.associateWith { tag ->
            passports.count { p -> tag.key in p.tags }
        }.filter { it.value > 0 }

        val hubBreakdown = found
            .groupBy { it.deliveredToHub ?: "unknown" }
            .mapValues { (_, v) -> v.size }

        val topFinders = found
            .mapNotNull { it.foundByName }
            .groupBy { it }
            .mapValues { (_, v) -> v.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        val recentActivity = passports.sortedByDescending { it.updatedAt }.take(10)

        _dashboardState.update {
            it.copy(
                isLoading      = false,
                totalLost      = allLost.size,
                totalFound     = found.size,
                foundToday     = foundToday,
                lostToday      = lostToday,
                tagBreakdown   = tagBreakdown,
                hubBreakdown   = hubBreakdown,
                topFinders     = topFinders,
                recentActivity = recentActivity
            )
        }
    }

    // ============================================================================================
    // ОЧИСТКА СООБЩЕНИЙ
    // ============================================================================================

    fun clearScootersMessage() { _scootersState.update { it.copy(error = null, successMessage = null) } }
    fun clearBatteryMessage()  { _batteryState.update  { it.copy(error = null, successMessage = null) } }
    fun clearStorageMessage()  { _storageState.update  { it.copy(error = null, successMessage = null) } }

    // ============================================================================================
    // УТИЛИТЫ
    // ============================================================================================

    private fun formatTimestamp(ts: Long): String {
        val sdf = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ts))
    }

    override fun onCleared() {
        super.onCleared()
        passportsListener?.remove()
        fleetLostListener?.remove()
        equipmentListener?.remove()
        storageListener?.remove()
        singlePassportListener?.remove()
    }
}