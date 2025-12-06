package com.example.qrscannerapp

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "internal_auth_store")
private const val TAG = "InternalAuth"

// --- НОВАЯ СТРУКТУРА РОЛЕЙ ---
enum class UserRole(val key: String, val displayName: String) {
    ADMIN("admin", "Администратор"),
    MOVER("muver", "Мувер"), // Ключ оставили старый для совместимости
    ELECTRICIAN("electrician", "Электрик"),
    INVENTORY_MANAGER("inventory_manager", "Кладовщик"), // Ключ старый, название новое
    TECHNIC("technic", "Техник"), // НОВАЯ РОЛЬ
    USER("user", "Пользователь"); // Роль по умолчанию

    companion object {
        fun fromKey(key: String?): UserRole {
            return entries.find { it.key == key } ?: USER
        }

        // Список для выпадающего меню при создании сотрудника
        fun getSelectableRoles(): List<UserRole> {
            return entries.filter { it != USER }
        }
    }
}

data class AuthState(
    val isLoggedIn: Boolean,
    val userId: String? = null,
    val userName: String? = null,
    val isAdmin: Boolean = false,
    val role: UserRole = UserRole.USER, // Теперь здесь тип UserRole
    val error: String? = null,
    val isLoading: Boolean = true
)

class AuthManager(private val context: Context) {
    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val LOGGED_IN_USER_ID_KEY = stringPreferencesKey("logged_in_user_id")

    private val _authState = MutableStateFlow(AuthState(isLoggedIn = false))
    val authState = _authState.asStateFlow()

    val currentUserId: String?
        get() = _authState.value.userId

    init {
        scope.launch {
            val preferences = context.dataStore.data.first()
            val loggedInUserId = preferences[LOGGED_IN_USER_ID_KEY]
            if (loggedInUserId != null) {
                Log.d(TAG, "Found saved user session for UID: $loggedInUserId. Fetching data...")
                loadUserData(loggedInUserId)
            } else {
                Log.d(TAG, "No saved user session found.")
                _authState.value = AuthState(isLoggedIn = false, isLoading = false)
            }
        }
    }

    fun goOnline() {
        val uid = _authState.value.userId ?: return
        Log.d(TAG, "Setting status to 'online' for user $uid")
        try {
            val userDocRef = firestore.collection("internal_users").document(uid)
            userDocRef.update("status", "online")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting online status for user $uid", e)
        }
    }

    fun goOffline() {
        val uid = _authState.value.userId ?: return
        Log.d(TAG, "Setting status to 'offline' for user $uid")
        try {
            val userDocRef = firestore.collection("internal_users").document(uid)
            val offlineStatus = mapOf(
                "status" to "offline",
                "lastSeen" to FieldValue.serverTimestamp()
            )
            userDocRef.update(offlineStatus)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting offline status for user $uid", e)
        }
    }

    private suspend fun loadUserData(uid: String) {
        try {
            val userDocument = firestore.collection("internal_users").document(uid).get().await()
            if (userDocument.exists()) {
                val displayName = userDocument.getString("displayName") ?: "Без имени"
                val isAdmin = userDocument.getBoolean("isAdmin") ?: false

                // Считываем строку и превращаем в Enum
                val roleString = userDocument.getString("role")
                val userRole = UserRole.fromKey(roleString)

                _authState.value = AuthState(
                    isLoggedIn = true,
                    userId = uid,
                    userName = displayName,
                    isAdmin = isAdmin,
                    role = userRole, // Сохраняем как Enum
                    isLoading = false
                )
                Log.d(TAG, "Successfully loaded data for user: $displayName, isAdmin: $isAdmin, role: ${userRole.displayName}")

                goOnline()

            } else {
                Log.w(TAG, "Saved session for UID $uid, but user not found in Firestore. Logging out.")
                logout()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user data for UID $uid", e)
            _authState.value = _authState.value.copy(error = "Ошибка загрузки профиля", isLoading = false)
        }
    }

    fun login(username: String, password: String) {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        scope.launch {
            try {
                Log.d(TAG, "Attempting to log in with username: $username")
                val querySnapshot = firestore.collection("internal_users")
                    .whereEqualTo("username", username)
                    .limit(1)
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    throw Exception("Пользователь не найден")
                }

                val userDocument = querySnapshot.documents.first()
                val correctPassword = userDocument.getString("password")

                if (correctPassword == password) {
                    Log.d(TAG, "Password is correct. Login successful.")
                    val userId = userDocument.id

                    // Telemetry update logic remains here...
                    launch {
                        try {
                            // Примечание: предполагается, что TelemetryManager существует в проекте
                            // Если код не компилируется, удалите этот блок или импортируйте TelemetryManager
                            /*
                             val telemetryManager = TelemetryManager(context)
                             val telemetryData = telemetryManager.getAllTelemetry()
                             firestore.collection("internal_users").document(userId)
                                 .update(telemetryData)
                                 .await()
                            */
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update telemetry", e)
                        }
                    }

                    context.dataStore.edit { preferences ->
                        preferences[LOGGED_IN_USER_ID_KEY] = userId
                    }
                    loadUserData(userId)
                } else {
                    throw Exception("Неверный пароль")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Login failed", e)
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState(isLoggedIn = false, error = e.message ?: "Ошибка входа", isLoading = false)
                }
            }
        }
    }

    fun logout() {
        goOffline()

        scope.launch {
            context.dataStore.edit { preferences ->
                preferences.remove(LOGGED_IN_USER_ID_KEY)
            }
            _authState.value = AuthState(isLoggedIn = false, isLoading = false)
            Log.d(TAG, "User logged out.")
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
}