package com.example.qrscannerapp.features.chat.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.features.chat.data.CloudinaryUploader
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UserProfileInfo(
    val displayName: String = "",
    val role: String = "",
    val avatarUrl: String? = null,
    val bio: String = "",
    val phone: String = "",
    val lastSeen: Long = 0L,
    val isOnline: Boolean = false
)

data class UserProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfileInfo? = null,
    val isUploadingAvatar: Boolean = false,
    val avatarUploadError: String? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val cloudinaryUploader: CloudinaryUploader
) : ViewModel() {

    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState = _uiState.asStateFlow()

    private var loadedUserId: String? = null

    internal val currentUserId get() = authManager.authState.value.userId

    fun loadProfile(userId: String, fallbackName: String, fallbackRole: String) {
        if (loadedUserId == userId && _uiState.value.profile != null) return
        loadedUserId = userId
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val doc = db.collection("internal_users").document(userId).get().await()
                val lastSeen: Long = when (val raw = doc.get("lastSeen")) {
                    is Long -> raw
                    is Number -> raw.toLong()
                    is com.google.firebase.Timestamp -> raw.toDate().time
                    else -> 0L
                }
                val now = System.currentTimeMillis()
                val profile = UserProfileInfo(
                    displayName = doc.getString("displayName") ?: fallbackName,
                    role        = doc.getString("role") ?: fallbackRole,
                    avatarUrl   = doc.getString("imgUrl"),
                    bio         = doc.getString("bio") ?: "",
                    phone       = doc.getString("phone") ?: "",
                    lastSeen    = lastSeen,
                    isOnline    = lastSeen > 0 && (now - lastSeen) < 5 * 60 * 1000L
                )
                _uiState.update { it.copy(isLoading = false, profile = profile) }
            } catch (e: Exception) {
                Log.e("UserProfileVM", "Failed to load profile for $userId", e)
                _uiState.update {
                    it.copy(isLoading = false, profile = UserProfileInfo(displayName = fallbackName, role = fallbackRole))
                }
            }
        }
    }

    /**
     * Загрузка нового аватара — только для своего профиля.
     * Использует CloudinaryUploader который уже есть в проекте.
     */
    fun uploadAvatar(context: Context, uri: Uri, userId: String) {
        if (userId != currentUserId) return // чужой профиль не меняем
        _uiState.update { it.copy(isUploadingAvatar = true, avatarUploadError = null) }

        viewModelScope.launch {
            try {
                val imageUrl = cloudinaryUploader.uploadImage(context, uri)
                if (imageUrl == null) {
                    _uiState.update { it.copy(isUploadingAvatar = false, avatarUploadError = "Не удалось загрузить фото") }
                    return@launch
                }

                // Сохраняем URL в Firestore
                db.collection("internal_users").document(userId)
                    .update("imgUrl", imageUrl).await()

                // Обновляем локальное состояние
                _uiState.update { state ->
                    state.copy(
                        isUploadingAvatar = false,
                        profile = state.profile?.copy(avatarUrl = imageUrl)
                    )
                }
            } catch (e: Exception) {
                Log.e("UserProfileVM", "Avatar upload failed", e)
                _uiState.update { it.copy(isUploadingAvatar = false, avatarUploadError = "Ошибка загрузки") }
            }
        }
    }

    fun clearAvatarError() { _uiState.update { it.copy(avatarUploadError = null) } }
}