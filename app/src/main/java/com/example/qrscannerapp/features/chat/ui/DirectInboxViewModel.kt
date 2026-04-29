package com.example.qrscannerapp.features.chat.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

// ============================================================================================
// МОДЕЛЬ — один диалог в списке
// ============================================================================================

data class DirectInboxItem(
    val conversationId: String = "",     // uid1_uid2
    val peerId: String = "",
    val peerName: String = "",
    val peerRole: String = "",
    val peerAvatarUrl: String? = null,
    val lastMessage: String = "",
    val lastSenderName: String = "",
    val lastTimestamp: Date? = null,
    val unreadCount: Int = 0,
    val isPeerOnline: Boolean = false
)

data class DirectInboxUiState(
    val items: List<DirectInboxItem> = emptyList(),
    val isLoading: Boolean = true
)

// ============================================================================================
// VIEWMODEL
// ============================================================================================

@HiltViewModel
class DirectInboxViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow(DirectInboxUiState())
    val uiState = _uiState.asStateFlow()

    private val listeners = mutableListOf<ListenerRegistration>()

    internal val currentUserId get() = authManager.authState.value.userId

    init { loadInbox() }

    /**
     * Загружаем все диалоги где участвует текущий пользователь.
     *
     * Firestore структура: direct_messages/{conversationId}
     * conversationId = uid1_uid2 (сортированные)
     * Документ содержит: lastMessage, lastSenderName, lastTimestamp, participants: [uid1, uid2]
     *
     * Фильтруем на клиенте по participants т.к. Firestore не поддерживает
     * array-contains с динамическим значением в простом listener без индекса.
     */
    private fun loadInbox() {
        val uid = currentUserId ?: return
        _uiState.update { it.copy(isLoading = true) }

        val listener = db.collection("direct_messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DirectInbox", "Listen failed", error)
                    _uiState.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                // Берём только диалоги где conversationId содержит наш uid
                val myConversations = snapshot.documents.filter { doc ->
                    doc.id.contains(uid)
                }

                viewModelScope.launch {
                    val items = mutableListOf<DirectInboxItem>()

                    myConversations.forEach { doc ->
                        try {
                            val convId = doc.id
                            // Вычисляем peerId из conversationId
                            val parts = convId.split("_")
                            val peerId = parts.firstOrNull { it != uid } ?: return@forEach

                            // Загружаем данные собеседника из internal_users
                            val peerDoc = db.collection("internal_users").document(peerId).get().await()
                            val peerName = peerDoc.getString("displayName") ?: peerDoc.getString("username") ?: "Сотрудник"
                            val peerRole = peerDoc.getString("role") ?: ""
                            val peerAvatarUrl = peerDoc.getString("imgUrl")

                            // Онлайн статус
                            val lastSeen: Long = when (val raw = peerDoc.get("lastSeen")) {
                                is Long -> raw
                                is Number -> raw.toLong()
                                is com.google.firebase.Timestamp -> raw.toDate().time
                                else -> 0L
                            }
                            val isPeerOnline = lastSeen > 0 && (System.currentTimeMillis() - lastSeen) < 5 * 60 * 1000L

                            // Подсчёт непрочитанных
                            val messagesSnap = db.collection("direct_messages")
                                .document(convId)
                                .collection("messages")
                                .get().await()

                            val unreadCount = messagesSnap.documents.count { msgDoc ->
                                val senderId = msgDoc.getString("senderId") ?: ""
                                val readBy = msgDoc.get("readBy") as? List<*> ?: emptyList<String>()
                                senderId != uid && !readBy.contains(uid)
                            }

                            val lastTimestamp = doc.getTimestamp("lastTimestamp")?.toDate()

                            items.add(DirectInboxItem(
                                conversationId = convId,
                                peerId = peerId,
                                peerName = peerName,
                                peerRole = peerRole,
                                peerAvatarUrl = peerAvatarUrl,
                                lastMessage = doc.getString("lastMessage") ?: "",
                                lastSenderName = doc.getString("lastSenderName") ?: "",
                                lastTimestamp = lastTimestamp,
                                unreadCount = unreadCount,
                                isPeerOnline = isPeerOnline
                            ))
                        } catch (e: Exception) {
                            Log.e("DirectInbox", "Failed to load conversation ${doc.id}", e)
                        }
                    }

                    // Сортируем по времени последнего сообщения
                    val sorted = items.sortedByDescending { it.lastTimestamp?.time ?: 0L }
                    _uiState.update { it.copy(items = sorted, isLoading = false) }
                }
            }

        listeners.add(listener)
    }

    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
    }
}