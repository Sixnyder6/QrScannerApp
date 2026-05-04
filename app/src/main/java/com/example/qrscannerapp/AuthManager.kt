package com.example.qrscannerapp

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "internal_auth_store")
private const val TAG = "InternalAuth"

// Интервал обновления lastSeen — 3 минуты
private const val HEARTBEAT_INTERVAL_MS = 3 * 60 * 1000L

// Порог "онлайн" — если lastSeen был менее 5 минут назад
const val ONLINE_THRESHOLD_MS = 5 * 60 * 1000L

enum class UserRole(val key: String, val displayName: String) {
    ADMIN("admin", "Администратор"),
    MOVER("muver", "Мувер"),
    ELECTRICIAN("electrician", "Электрик"),
    INVENTORY_MANAGER("inventory_manager", "Кладовщик"),
    TECHNIC("technic", "Техник"),
    SUPERVISOR("supervisor", "Супервайзер"),
    SECURITY("security", "СБ"),
    USER("user", "Пользователь");

    companion object {
        fun fromKey(key: String?): UserRole =
            entries.find { it.key == key } ?: USER

        fun getSelectableRoles(): List<UserRole> =
            entries.filter { it != USER }
    }
}

enum class ShiftRequestStatus(val key: String) {
    NONE("NONE"),
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    DENIED("DENIED");

    companion object {
        fun fromKey(key: String?): ShiftRequestStatus =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: NONE
    }
}

data class AuthState(
    val isLoggedIn: Boolean,
    val userId: String? = null,
    val userName: String? = null,
    val isAdmin: Boolean = false,
    val role: UserRole = UserRole.USER,
    val error: String? = null,
    val isLoading: Boolean = true,
    val isShiftActive: Boolean = false,
    val shiftStartTime: Long = 0L,
    val isAllowedToWork: Boolean = false,
    val shiftRequestStatus: ShiftRequestStatus = ShiftRequestStatus.NONE
)

class AuthManager(
    private val context: Context,
    // TelemetryManager инжектируем через lazy чтобы избежать циклической зависимости
    // при старте — он создаётся только при первом heartbeat
    private val telemetryManagerProvider: () -> TelemetryManager
) {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val LOGGED_IN_USER_ID_KEY = stringPreferencesKey("logged_in_user_id")

    private val _authState = MutableStateFlow(AuthState(isLoggedIn = false))
    val authState = _authState.asStateFlow()

    private var userListener: ListenerRegistration? = null

    // Job для heartbeat — чтобы можно было отменить при logout
    private var heartbeatJob: Job? = null

    // Уникальный ID текущего устройства
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    // Флаг: нас выкинуло другое устройство (не трогаем activeDeviceId при logout)
    @Volatile
    private var isKickedByAnotherDevice = false

    val currentUserId: String?
        get() = _authState.value.userId

    init {
        scope.launch {
            // Даём Firebase время инициализироваться
            delay(1500)

            val preferences = context.dataStore.data.first()
            val loggedInUserId = preferences[LOGGED_IN_USER_ID_KEY]
            if (loggedInUserId != null) {
                withContext(Dispatchers.Main) {
                    attachUserListener(loggedInUserId)
                }
            } else {
                _authState.value = AuthState(isLoggedIn = false, isLoading = false)
            }
        }
    }

    private fun attachUserListener(uid: String) {
        userListener?.remove()

        userListener = firestore.collection("internal_users").document(uid)
            .addSnapshotListener { userDocument, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error)
                    _authState.value = _authState.value.copy(
                        error = "Ошибка связи с сервером",
                        isLoading = false
                    )
                    return@addSnapshotListener
                }

                if (userDocument != null && userDocument.exists()) {
                    // ═══════════════════════════════════════════════════════════
                    // ПРОВЕРКА ПРИВЯЗКИ УСТРОЙСТВА
                    // Если activeDeviceId изменился — нас выкинули
                    // ═══════════════════════════════════════════════════════════
                    val activeDevice = userDocument.getString("activeDeviceId")
                    if (activeDevice != null && activeDevice != deviceId) {
                        Log.w(TAG, "Session taken by another device ($activeDevice). Forcing logout.")
                        isKickedByAnotherDevice = true
                        scope.launch {
                            forceLogoutLocal("Аккаунт используется на другом устройстве")
                        }
                        return@addSnapshotListener
                    }

                    val displayName = userDocument.getString("displayName") ?: "Без имени"
                    val isAdmin = userDocument.getBoolean("isAdmin") ?: false
                    val roleString = userDocument.getString("role")
                    val userRole = UserRole.fromKey(roleString)
                    val isShiftActive = userDocument.getBoolean("isShiftActive") ?: false
                    val shiftStartTime = userDocument.getLong("shiftStartTime") ?: 0L
                    val isAllowedToWork = userDocument.getBoolean("isAllowedToWork") ?: false
                    val statusString = userDocument.getString("shiftRequestStatus")
                    val shiftRequestStatus = ShiftRequestStatus.fromKey(statusString)

                    _authState.value = AuthState(
                        isLoggedIn = true,
                        userId = uid,
                        userName = displayName,
                        isAdmin = isAdmin,
                        role = userRole,
                        isShiftActive = isShiftActive,
                        shiftStartTime = shiftStartTime,
                        isLoading = false,
                        isAllowedToWork = isAllowedToWork,
                        shiftRequestStatus = shiftRequestStatus
                    )
                } else {
                    Log.d(TAG, "User document for UID $uid not found. Logging out.")
                    scope.launch { logout() }
                }
            }

        // Сразу пишем lastSeen + deviceId и запускаем heartbeat
        startHeartbeat(uid)
    }

    // ════════════════════════════════════════════════════════════════════════
    // HEARTBEAT — каждые 3 минуты:
    //   1. lastSeen + activeDeviceId (онлайн-статус)
    //   2. Полная телеметрия устройства → пишем прямо в internal_users
    //   3. GPS координаты для всех ролей (если разрешение выдано)
    //      Если разрешения нет — старые поля удаляются
    // ════════════════════════════════════════════════════════════════════════
    private fun startHeartbeat(uid: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    val telemetryManager = telemetryManagerProvider()

                    // Собираем телеметрию (без пинга — он медленный)
                    val telemetry = telemetryManager.getAllTelemetry(includePing = false)

                    // internal_users: только онлайн-маркеры и идентификатор устройства
                    val internalData: Map<String, Any> = hashMapOf(
                        "lastSeen" to System.currentTimeMillis(),
                        "activeDeviceId" to deviceId,
                        "appVersion" to (telemetry["appVersion"]?.toString() ?: "")
                    )
                    firestore.collection("internal_users").document(uid)
                        .update(internalData)
                        .await()
                    Log.d(TAG, "Heartbeat OK: lastSeen updated for $uid")

                    // device_telemetry: все телеметрийные поля + GPS
                    try {
                        val location = telemetryManager.getCurrentLocation()
                        val telemetryFields = hashMapOf<String, Any?>(
                            "lastBatteryLevel" to telemetry["lastBatteryLevel"],
                            "isCharging" to telemetry["isCharging"],
                            "batteryHealth" to telemetry["batteryHealth"],
                            "isPowerSaveMode" to telemetry["isPowerSaveMode"],
                            "networkState" to telemetry["networkState"],
                            "networkPing" to telemetry["networkPing"],
                            "freeRam" to telemetry["freeRam"],
                            "freeStorage" to telemetry["freeStorage"],
                            "deviceUptime" to telemetry["deviceUptime"],
                            "deviceInfo" to telemetry["deviceInfo"],
                            "totalRamInGb" to telemetry["totalRamInGb"],
                            "activeDeviceId" to deviceId,
                            "appVersion" to (telemetry["appVersion"]?.toString() ?: ""),
                            "locationLat" to (location?.latitude ?: FieldValue.delete()),
                            "locationLng" to (location?.longitude ?: FieldValue.delete()),
                            "locationTimestamp" to (location?.timestamp ?: FieldValue.delete()),
                            "updatedAt" to System.currentTimeMillis()
                        )
                        firestore.collection("device_telemetry").document(uid)
                            .set(telemetryFields, SetOptions.merge())
                            .await()
                        Log.d(TAG, "Telemetry updated in device_telemetry/$uid")
                    } catch (e: Exception) {
                        Log.e(TAG, "device_telemetry update failed (non-critical)", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat error for $uid", e)
                }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    fun goOffline() {
        val uid = _authState.value.userId ?: return
        scope.launch {
            try {
                firestore.collection("internal_users").document(uid)
                    .update("lastSeen", 0L)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error setting offline status", e)
            }
        }
    }

    fun login(username: String, password: String) {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        isKickedByAnotherDevice = false

        scope.launch {
            try {
                val querySnapshot = firestore.collection("internal_users")
                    .whereEqualTo("username", username)
                    .limit(1)
                    .get()
                    .await()
                if (querySnapshot.isEmpty) throw Exception("Пользователь не найден")
                val userDocument = querySnapshot.documents.first()
                val correctPassword = userDocument.getString("password")
                if (correctPassword == password) {
                    val userId = userDocument.id

                    // ═══════════════════════════════════════════════════════════
                    // ПРИВЯЗКА УСТРОЙСТВА — записываем наш deviceId
                    // Предыдущее устройство увидит изменение через
                    // snapshot listener и автоматически разлогинится
                    // ═══════════════════════════════════════════════════════════
                    firestore.collection("internal_users").document(userId)
                        .update(
                            mapOf(
                                "activeDeviceId" to deviceId,
                                "lastSeen" to System.currentTimeMillis()
                            )
                        )
                        .await()

                    context.dataStore.edit { preferences ->
                        preferences[LOGGED_IN_USER_ID_KEY] = userId
                    }
                    withContext(Dispatchers.Main) {
                        attachUserListener(userId)
                    }
                } else {
                    throw Exception("Неверный пароль")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState(
                        isLoggedIn = false,
                        error = e.message ?: "Ошибка входа",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Принудительный локальный logout — БЕЗ сброса activeDeviceId в Firestore.
     * Вызывается когда другое устройство перехватило сессию.
     */
    private suspend fun forceLogoutLocal(errorMessage: String) {
        heartbeatJob?.cancel()
        heartbeatJob = null

        userListener?.remove()
        userListener = null

        context.dataStore.edit { preferences ->
            preferences.remove(LOGGED_IN_USER_ID_KEY)
        }

        withContext(Dispatchers.Main) {
            _authState.value = AuthState(
                isLoggedIn = false,
                isLoading = false,
                error = errorMessage
            )
        }
    }

    fun logout() {
        // Останавливаем heartbeat и сбрасываем lastSeen
        heartbeatJob?.cancel()
        heartbeatJob = null
        goOffline()

        // При ручном logout — очищаем activeDeviceId, чтобы можно было
        // залогиниться с любого устройства
        val uid = _authState.value.userId
        if (uid != null && !isKickedByAnotherDevice) {
            scope.launch {
                try {
                    firestore.collection("internal_users").document(uid)
                        .update(
                            mapOf(
                                "activeDeviceId" to FieldValue.delete()
                            )
                        )
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing activeDeviceId", e)
                }
            }
        }

        isKickedByAnotherDevice = false

        userListener?.remove()
        userListener = null

        scope.launch {
            context.dataStore.edit { preferences ->
                preferences.remove(LOGGED_IN_USER_ID_KEY)
            }
            _authState.value = AuthState(isLoggedIn = false, isLoading = false)
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun startShiftLocally() {
        _authState.value = _authState.value.copy(
            isShiftActive = true,
            shiftStartTime = System.currentTimeMillis()
        )
    }

    fun endShiftLocally() {
        _authState.value = _authState.value.copy(
            isShiftActive = false,
            shiftStartTime = 0L
        )
    }
}