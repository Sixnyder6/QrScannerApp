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

private const val HEARTBEAT_INTERVAL_MS = 3 * 60 * 1000L

// ⭐ МИНИМАЛЬНАЯ ТРЕБУЕМАЯ ВЕРСИЯ ПРИЛОЖЕНИЯ
// В продакшене можно хранить в Firestore в документе app_config
private const val MIN_REQUIRED_APP_VERSION = "1.4.5"

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
    val shiftRequestStatus: ShiftRequestStatus = ShiftRequestStatus.NONE,
    val versionError: Boolean = false  // ⭐ НОВОЕ ПОЛЕ: флаг ошибки версии
)

class AuthManager(
    private val context: Context,
    private val presenceManager: PresenceManager,
    private val telemetryManagerProvider: () -> TelemetryManager
) {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val LOGGED_IN_USER_ID_KEY = stringPreferencesKey("logged_in_user_id")

    private val _authState = MutableStateFlow(AuthState(isLoggedIn = false))
    val authState = _authState.asStateFlow()

    private var userListener: ListenerRegistration? = null
    private var heartbeatJob: Job? = null

    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    @Volatile
    private var isKickedByAnotherDevice = false

    val currentUserId: String?
        get() = _authState.value.userId

    init {
        scope.launch {
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

    // ⭐ НОВЫЙ МЕТОД: получение текущей версии приложения
    private fun getCurrentAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get app version", e)
            "0"
        }
    }

    // ⭐ НОВЫЙ МЕТОД: сравнение версий
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val p1 = if (i < parts1.size) parts1[i] else 0
            val p2 = if (i < parts2.size) parts2[i] else 0
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    // ⭐ НОВЫЙ МЕТОД: проверка версии (можно загружать с сервера динамически)
    private suspend fun checkVersionAndBlock(): Boolean {
        val currentVersion = getCurrentAppVersion()

        // Вариант 1: жёсткая минимальная версия (захардкожена)
        if (compareVersions(currentVersion, MIN_REQUIRED_APP_VERSION) < 0) {
            withContext(Dispatchers.Main) {
                _authState.value = AuthState(
                    isLoggedIn = false,
                    error = "Ваша версия приложения ($currentVersion) устарела. " +
                            "Пожалуйста, обновитесь до версии $MIN_REQUIRED_APP_VERSION или выше",
                    isLoading = false,
                    versionError = true
                )
            }
            return false
        }

        // Вариант 2: загружать минимальную версию из Firestore (раскомментировать при наличии документа app_config)
        // try {
        //     val configDoc = firestore.collection("app_config").document("version_config").get().await()
        //     val minVersion = configDoc.getString("minRequiredVersion") ?: MIN_REQUIRED_APP_VERSION
        //     if (compareVersions(currentVersion, minVersion) < 0) {
        //         withContext(Dispatchers.Main) {
        //             _authState.value = AuthState(
        //                 isLoggedIn = false,
        //                 error = "Требуется обновление. Минимальная версия: $minVersion",
        //                 isLoading = false,
        //                 versionError = true
        //             )
        //         }
        //         return false
        //     }
        // } catch (e: Exception) {
        //     Log.e(TAG, "Failed to check min version from server", e)
        // }

        return true
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
                    // ⭐ ПРОВЕРКА ПРИВЯЗКИ УСТРОЙСТВА
                    val activeDevice = userDocument.getString("activeDeviceId")
                    if (activeDevice != null && activeDevice != deviceId) {
                        Log.w(TAG, "Session taken by another device ($activeDevice). Forcing logout.")
                        isKickedByAnotherDevice = true
                        scope.launch {
                            forceLogoutLocal("Аккаунт используется на другом устройстве")
                        }
                        return@addSnapshotListener
                    }

                    // ⭐ ПРОВЕРКА ВЕРСИИ ПРИЛОЖЕНИЯ
                    val userAppVersion = userDocument.getString("appVersion") ?: "0"
                    if (compareVersions(userAppVersion, MIN_REQUIRED_APP_VERSION) < 0) {
                        Log.w(TAG, "User app version $userAppVersion is below required $MIN_REQUIRED_APP_VERSION")
                        scope.launch {
                            forceLogoutLocal("Требуется обновление приложения. Версия $MIN_REQUIRED_APP_VERSION или выше")
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
                        shiftRequestStatus = shiftRequestStatus,
                        versionError = false
                    )
                } else {
                    Log.d(TAG, "User document for UID $uid not found. Logging out.")
                    scope.launch { logout() }
                }
            }

        presenceManager.startTracking(uid)
        startHeartbeat(uid)
    }

    private fun startHeartbeat(uid: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    val telemetryManager = telemetryManagerProvider()
                    val telemetry = telemetryManager.getAllTelemetry(includePing = false)
                    val currentVersion = getCurrentAppVersion()

                    // ⭐ ДОБАВЛЕНА ПРОВЕРКА ВЕРСИИ ПРИ HEARTBEAT
                    if (compareVersions(currentVersion, MIN_REQUIRED_APP_VERSION) < 0) {
                        Log.w(TAG, "Version check failed during heartbeat. Forcing logout.")
                        withContext(Dispatchers.Main) {
                            forceLogoutLocal("Требуется обновление приложения")
                        }
                        return@launch
                    }

                    presenceManager.pingNow()

                    // Автозавершение смены по 12ч — надёжный fallback вместо WorkManager
                    val state = _authState.value
                    if (state.isShiftActive && state.shiftStartTime > 0L) {
                        val elapsed = System.currentTimeMillis() - state.shiftStartTime
                        if (elapsed >= 12 * 60 * 60 * 1000L) {
                            Log.w(TAG, "Shift exceeded 12h — auto-ending via heartbeat")
                            try {
                                val activeShiftId = firestore.collection("internal_users").document(uid)
                                    .get().await().getString("activeShiftId")
                                val batch = firestore.batch()
                                batch.update(
                                    firestore.collection("internal_users").document(uid),
                                    mapOf(
                                        "isShiftActive" to false,
                                        "activeShiftId" to FieldValue.delete(),
                                        "shiftStartTime" to FieldValue.delete()
                                    )
                                )
                                if (activeShiftId != null) {
                                    batch.update(
                                        firestore.collection("shifts").document(activeShiftId),
                                        mapOf(
                                            "endTime" to System.currentTimeMillis(),
                                            "status" to "AUTO_ENDED",
                                            "endReason" to "auto_12h"
                                        )
                                    )
                                }
                                batch.commit().await()
                                endShiftLocally()
                                Log.d(TAG, "Shift auto-ended via heartbeat. ShiftId: $activeShiftId")
                            } catch (e: Exception) {
                                Log.e(TAG, "Heartbeat shift auto-end failed", e)
                            }
                        }
                    }

                    firestore.collection("internal_users").document(uid)
                        .update(mapOf("activeDeviceId" to deviceId, "appVersion" to currentVersion))
                        .await()
                    Log.d(TAG, "Heartbeat OK for $uid")

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
                            "appVersion" to currentVersion,
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
        scope.launch {
            try {
                presenceManager.stopTracking()
            } catch (e: Exception) {
                Log.e(TAG, "Error setting offline status", e)
            }
        }
    }

    fun login(username: String, password: String) {
        _authState.value = _authState.value.copy(isLoading = true, error = null, versionError = false)
        isKickedByAnotherDevice = false

        scope.launch {
            try {
                // ⭐ ПРОВЕРКА ВЕРСИИ ПРИЛОЖЕНИЯ ПЕРЕД ЛОГИНОМ
                val versionCheckPassed = checkVersionAndBlock()
                if (!versionCheckPassed) {
                    return@launch
                }

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
                    val currentVersion = getCurrentAppVersion()

                    presenceManager.startTracking(userId)
                    firestore.collection("internal_users").document(userId)
                        .update(mapOf("activeDeviceId" to deviceId, "appVersion" to currentVersion))
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
                        isLoading = false,
                        versionError = false
                    )
                }
            }
        }
    }

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
                error = errorMessage,
                versionError = errorMessage.contains("версия", ignoreCase = true) ||
                        errorMessage.contains("обновление", ignoreCase = true)
            )
        }
    }

    fun logout() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        goOffline()

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
        _authState.value = _authState.value.copy(error = null, versionError = false)
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