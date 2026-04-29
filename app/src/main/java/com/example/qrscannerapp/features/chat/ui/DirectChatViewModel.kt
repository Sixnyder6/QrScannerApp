package com.example.qrscannerapp.features.chat.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

// ============================================================================================
// МОДЕЛЬ
// ============================================================================================

data class DirectMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Date? = null,
    val readBy: List<String> = emptyList()
)

data class DirectChatUiState(
    val messages: List<DirectMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val peerName: String = "",
    val peerRole: String = "",
    // НОВОЕ: печатает ли собеседник прямо сейчас
    val isPeerTyping: Boolean = false
)

// ============================================================================================
// VIEWMODEL
// ============================================================================================

@HiltViewModel
class DirectChatViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow(DirectChatUiState())
    val uiState = _uiState.asStateFlow()

    private var messagesListener: ListenerRegistration? = null
    private var typingListener: ListenerRegistration? = null
    private var conversationId: String = ""
    private var peerId: String = ""

    // Job для авто-сброса статуса "печатает" через 4 сек после последнего ввода
    private var stopTypingJob: Job? = null
    private var isCurrentlyTyping = false

    internal val currentUserId get() = authManager.authState.value.userId
    internal val currentUserName get() = authManager.authState.value.userName ?: "Сотрудник"

    /**
     * Открыть диалог с пользователем.
     * conversationId строится детерминированно: меньший uid + "_" + больший uid.
     */
    fun openConversation(newPeerId: String, peerName: String, peerRole: String) {
        val myId = currentUserId ?: return
        peerId = newPeerId
        conversationId = listOf(myId, newPeerId).sorted().joinToString("_")
        _uiState.update { it.copy(peerName = peerName, peerRole = peerRole, isLoading = true) }
        startListening()
        startTypingListener()
    }

    // ============================================================================================
    // СООБЩЕНИЯ
    // ============================================================================================

    private fun startListening() {
        messagesListener?.remove()
        messagesListener = db.collection("direct_messages")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DirectChat", "Listen failed", error)
                    _uiState.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        DirectMessage(
                            id        = doc.id,
                            text      = doc.getString("text") ?: "",
                            senderId  = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.toDate(),
                            readBy    = (doc.get("readBy") as? List<*>)?.map { it.toString() } ?: emptyList()
                        )
                    } catch (e: Exception) {
                        Log.e("DirectChat", "Parse error doc ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                _uiState.update { it.copy(messages = messages, isLoading = false) }
                markAsRead(messages)
            }
    }

    fun sendMessage(text: String) {
        val uid = currentUserId ?: return
        if (text.isBlank()) return
        stopTyping() // сбрасываем "печатает" при отправке
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            try {
                db.collection("direct_messages")
                    .document(conversationId)
                    .collection("messages")
                    .add(hashMapOf(
                        "text"       to text.trim(),
                        "senderId"   to uid,
                        "senderName" to currentUserName,
                        "timestamp"  to FieldValue.serverTimestamp(),
                        "readBy"     to listOf(uid)
                    )).await()

                // Метаданные диалога — для будущего списка бесед
                db.collection("direct_messages")
                    .document(conversationId)
                    .set(hashMapOf(
                        "lastMessage"    to text.trim().take(60),
                        "lastSenderName" to currentUserName,
                        "lastTimestamp"  to FieldValue.serverTimestamp()
                    ), com.google.firebase.firestore.SetOptions.merge()).await()

            } catch (e: Exception) {
                Log.e("DirectChat", "Send failed", e)
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                db.collection("direct_messages")
                    .document(conversationId)
                    .collection("messages")
                    .document(messageId)
                    .delete().await()
            } catch (e: Exception) {
                Log.e("DirectChat", "Delete failed", e)
            }
        }
    }

    private fun markAsRead(messages: List<DirectMessage>) {
        val uid = currentUserId ?: return
        // Батчим все непрочитанные в один batch вместо N отдельных запросов
        val unread = messages.filter { it.senderId != uid && !it.readBy.contains(uid) }
        if (unread.isEmpty()) return
        viewModelScope.launch {
            try {
                val batch = db.batch()
                unread.forEach { msg ->
                    val ref = db.collection("direct_messages")
                        .document(conversationId)
                        .collection("messages")
                        .document(msg.id)
                    batch.update(ref, "readBy", FieldValue.arrayUnion(uid))
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("DirectChat", "markAsRead batch failed", e)
            }
        }
    }

    // ============================================================================================
    // TYPING — пишем в direct_messages/{conversationId}/typing/{myUid}
    // ============================================================================================

    /**
     * Вызывается при каждом изменении текста в поле ввода.
     * Пишет запись в Firestore и запускает таймер авто-сброса.
     */
    fun onTyping() {
        val uid = currentUserId ?: return
        if (!isCurrentlyTyping) {
            isCurrentlyTyping = true
            db.collection("direct_messages")
                .document(conversationId)
                .collection("typing")
                .document(uid)
                .set(mapOf(
                    "name"      to currentUserName,
                    "timestamp" to FieldValue.serverTimestamp()
                ))
        }
        // Перезапускаем таймер — если 4 сек нет новых символов, снимаем статус
        stopTypingJob?.cancel()
        stopTypingJob = viewModelScope.launch {
            delay(4_000)
            stopTyping()
        }
    }

    /**
     * Явный сброс — при отправке или уходе с экрана.
     */
    private fun stopTyping() {
        val uid = currentUserId ?: return
        if (!isCurrentlyTyping) return
        isCurrentlyTyping = false
        stopTypingJob?.cancel()
        db.collection("direct_messages")
            .document(conversationId)
            .collection("typing")
            .document(uid)
            .delete()
    }

    /**
     * Слушаем typing-коллекцию: показываем статус только если там есть запись
     * от собеседника (не от себя) и она свежее 6 сек.
     */
    private fun startTypingListener() {
        val uid = currentUserId ?: return
        typingListener?.remove()
        typingListener = db.collection("direct_messages")
            .document(conversationId)
            .collection("typing")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val now = System.currentTimeMillis()
                val peerTyping = snapshot.documents.any { doc ->
                    if (doc.id == uid) return@any false // игнорируем себя
                    val ts = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    (now - ts) < 6_000
                }
                _uiState.update { it.copy(isPeerTyping = peerTyping) }
            }
    }

    override fun onCleared() {
        super.onCleared()
        stopTyping()
        messagesListener?.remove()
        typingListener?.remove()
    }
}