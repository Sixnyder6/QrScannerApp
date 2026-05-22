package com.example.qrscannerapp.features.chat.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.FcmSender
import com.example.qrscannerapp.UserRole
import com.example.qrscannerapp.common.upload.CloudinaryUploader
import com.example.qrscannerapp.common.upload.UploadFolder
import com.example.qrscannerapp.features.chat.domain.model.ChatMessage
import com.example.qrscannerapp.features.chat.domain.model.ChatRoom
import com.example.qrscannerapp.features.chat.domain.model.ChatRoomInfo
import com.example.qrscannerapp.features.chat.domain.model.MessageType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class RoomMember(
    val userId: String = "",
    val displayName: String = "",
    val role: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val avatarUrl: String? = null
)

data class ChatUiState(
    val availableRooms: List<ChatRoomInfo> = emptyList(),
    val activeRoom: ChatRoom = ChatRoom.GENERAL,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val replyTo: ChatMessage? = null,
    val totalUnread: Int = 0,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val searchResults: List<ChatMessage> = emptyList(),
    val currentSearchIndex: Int = 0,
    val typingUsers: List<String> = emptyList(),
    val isRoomMenuOpen: Boolean = false,
    val roomMembers: List<RoomMember> = emptyList(),
    val isMembersLoading: Boolean = false,
    val isAutoDeleteEnabled: Boolean = false,
    val autoDeleteIntervalHours: Int = 24,
    val showScrollToBottom: Boolean = false,
    val unreadBelowCount: Int = 0,
    val isSelectionMode: Boolean = false,
    val selectedMessageIds: Set<String> = emptySet(),
    val isClearingChat: Boolean = false,
    val isUploadingImage: Boolean = false,
    val emojiPickerMessageId: String? = null,
    val pinnedMessage: ChatMessage? = null,
    // ПАГИНАЦИЯ
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val cloudinaryUploader: CloudinaryUploader
) : ViewModel() {

    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    val totalUnread: StateFlow<Int> = _uiState
        .map { it.totalUnread }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val lastMessagePreview: StateFlow<String> = _uiState
        .map { state ->
            val room = state.availableRooms.firstOrNull()
            val sender = room?.lastSenderName
            val msg = room?.lastMessage
            if (!sender.isNullOrBlank() && !msg.isNullOrBlank()) "$sender: $msg" else ""
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    private var messagesListener: ListenerRegistration? = null
    private var pinnedListener: ListenerRegistration? = null
    private val unreadListeners = mutableMapOf<String, ListenerRegistration>()
    private var typingListener: ListenerRegistration? = null
    private var membersListener: ListenerRegistration? = null
    private var stopTypingJob: Job? = null
    private var isCurrentlyTyping = false
    private var autoDeleteJob: Job? = null
    // ПАГИНАЦИЯ: последний загруженный документ для cursor-based пагинации
    private var lastLoadedDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    private val PAGE_SIZE = 50L

    internal val currentUserId get() = authManager.authState.value.userId
    internal val currentUserName get() = authManager.authState.value.userName ?: "Сотрудник"
    internal val isAdmin get() = authManager.authState.value.isAdmin
    private val currentUserRole get() = authManager.authState.value.role

    val accessibleRooms: List<ChatRoom>
        get() = when (currentUserRole) {
            UserRole.ADMIN -> ChatRoom.entries.toList()
            UserRole.INVENTORY_MANAGER -> listOf(ChatRoom.GENERAL, ChatRoom.MANAGERS, ChatRoom.SHIFTS, ChatRoom.ALERTS)
            UserRole.MOVER -> listOf(ChatRoom.GENERAL, ChatRoom.MUVERS, ChatRoom.SHIFTS)
            UserRole.ELECTRICIAN -> listOf(ChatRoom.GENERAL, ChatRoom.SHIFTS)
            UserRole.TECHNIC -> listOf(ChatRoom.GENERAL, ChatRoom.SHIFTS)
            else -> listOf(ChatRoom.GENERAL)
        }

    init {
        _uiState.update { it.copy(availableRooms = accessibleRooms.map { r -> ChatRoomInfo(room = r) }) }
        listenToUnreadCounts()
        selectRoom(ChatRoom.GENERAL)
    }

    // ============================================================================================
    // ПАРСИНГ ДОКУМЕНТА
    // ============================================================================================

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toChatMessage(): ChatMessage? {
        return try {
            val typeStr = getString("type") ?: "TEXT"
            val messageType = try {
                MessageType.valueOf(typeStr.trim().uppercase())
            } catch (e: IllegalArgumentException) {
                Log.w("Chat", "Unknown MessageType '$typeStr' in doc $id, using TEXT")
                MessageType.TEXT
            }

            val reactionsRaw = get("reactions") as? Map<*, *>
            val reactions = reactionsRaw?.mapNotNull { (key, value) ->
                val emoji = key?.toString() ?: return@mapNotNull null
                val userIds = (value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                emoji to userIds
            }?.toMap() ?: emptyMap()

            ChatMessage(
                id = id,
                chatRoomId = getString("chatRoomId") ?: "",
                text = getString("text") ?: "",
                senderId = getString("senderId") ?: "",
                senderName = getString("senderName") ?: "",
                senderRole = getString("senderRole") ?: "",
                type = messageType,
                timestamp = getTimestamp("timestamp")?.toDate(),
                readBy = (get("readBy") as? List<*>)?.map { it.toString() } ?: emptyList(),
                mentionedUsers = (get("mentionedUsers") as? List<*>)?.map { it.toString() } ?: emptyList(),
                mentionedNames = (get("mentionedNames") as? List<*>)?.map { it.toString() } ?: emptyList(),
                replyToId = getString("replyToId"),
                replyToText = getString("replyToText"),
                replyToSenderName = getString("replyToSenderName"),
                expiresAt = getTimestamp("expiresAt")?.toDate(),
                imageUrl = getString("imageUrl"),
                imageThumbnailUrl = getString("imageThumbnailUrl"),
                reactions = reactions
            )
        } catch (e: Exception) {
            Log.e("Chat", "Failed to parse message doc $id", e)
            null
        }
    }

    // ============================================================================================
    // КОМНАТЫ
    // ============================================================================================

    fun selectRoom(room: ChatRoom) {
        if (!accessibleRooms.contains(room)) return
        stopTyping()
        typingListener?.remove()
        pinnedListener?.remove()
        _uiState.update {
            it.copy(
                activeRoom = room, isLoading = true, messages = emptyList(),
                replyTo = null, typingUsers = emptyList(),
                isSearchActive = false, searchQuery = "",
                isRoomMenuOpen = false,
                showScrollToBottom = false, unreadBelowCount = 0,
                isSelectionMode = false, selectedMessageIds = emptySet(),
                emojiPickerMessageId = null,
                pinnedMessage = null,
                isLoadingMore = false, hasMoreMessages = true
            )
        }
        lastLoadedDocument = null
        messagesListener?.remove()
        messagesListener = db.collection("chats").document(room.id)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Chat", "Listen failed", error)
                    _uiState.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc -> doc.toChatMessage() } ?: emptyList()
                // Сохраняем последний документ для пагинации
                lastLoadedDocument = snapshot?.documents?.lastOrNull()
                val hasMore = (snapshot?.documents?.size ?: 0) >= PAGE_SIZE.toInt()
                val prevSize = _uiState.value.messages.size
                val newMessagesCount = (messages.size - prevSize).coerceAtLeast(0)
                _uiState.update { state ->
                    state.copy(
                        messages = messages, isLoading = false,
                        hasMoreMessages = hasMore,
                        unreadBelowCount = if (state.showScrollToBottom && newMessagesCount > 0)
                            state.unreadBelowCount + newMessagesCount else state.unreadBelowCount
                    )
                }
                markRoomAsRead(room.id, messages)
                if (_uiState.value.isSearchActive && _uiState.value.searchQuery.isNotBlank())
                    updateSearchResults(_uiState.value.searchQuery, messages)
            }
        listenToTyping(room.id)
        listenToPinnedMessage(room.id)
    }

    private fun listenToUnreadCounts() {
        val uid = currentUserId ?: return
        accessibleRooms.forEach { room ->
            val listener = db.collection("chats").document(room.id)
                .collection("messages")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot == null) return@addSnapshotListener
                    val unread = snapshot.documents.count { doc ->
                        val senderId = doc.getString("senderId") ?: ""
                        val readBy = doc.get("readBy") as? List<*> ?: emptyList<String>()
                        senderId != uid && !readBy.contains(uid)
                    }
                    val hasMention = snapshot.documents.any { doc ->
                        val readBy = doc.get("readBy") as? List<*> ?: emptyList<String>()
                        val mentions = doc.get("mentionedNames") as? List<*> ?: emptyList<String>()
                        val myFirstName = currentUserName.split(" ").firstOrNull()?.lowercase() ?: ""
                        !readBy.contains(uid) && mentions.any { it.toString().lowercase().contains(myFirstName) }
                    }
                    val lastDoc = snapshot.documents.lastOrNull()
                    _uiState.update { state ->
                        val updatedRooms = state.availableRooms.map {
                            if (it.room == room) it.copy(
                                unreadCount = unread, hasUnreadMention = hasMention,
                                lastSenderName = lastDoc?.getString("senderName"),
                                lastMessage = lastDoc?.getString("text")?.take(50)
                            ) else it
                        }
                        state.copy(availableRooms = updatedRooms, totalUnread = updatedRooms.sumOf { it.unreadCount })
                    }
                }
            unreadListeners[room.id] = listener
        }
    }

    private fun markRoomAsRead(roomId: String, messages: List<ChatMessage>) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            messages.filter { it.senderId != uid && !it.readBy.contains(uid) }.forEach { msg ->
                try {
                    db.collection("chats").document(roomId)
                        .collection("messages").document(msg.id)
                        .update("readBy", FieldValue.arrayUnion(uid))
                } catch (e: Exception) { Log.e("Chat", "Mark read failed", e) }
            }
        }
    }

    // ============================================================================================
    // ПАГИНАЦИЯ — подгрузка старых сообщений при скролле вверх
    // ============================================================================================

    fun loadMoreMessages() {
        val lastDoc = lastLoadedDocument ?: return
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreMessages) return
        val roomId = _uiState.value.activeRoom.id
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            try {
                val snapshot = db.collection("chats").document(roomId)
                    .collection("messages")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .startAfter(lastDoc)
                    .limit(PAGE_SIZE)
                    .get().await()
                val newMessages = snapshot.documents.mapNotNull { it.toChatMessage() }
                lastLoadedDocument = snapshot.documents.lastOrNull() ?: lastDoc
                val hasMore = snapshot.documents.size >= PAGE_SIZE.toInt()
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + newMessages,
                        isLoadingMore = false,
                        hasMoreMessages = hasMore
                    )
                }
            } catch (e: Exception) {
                Log.e("Chat", "loadMoreMessages failed", e)
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    // ============================================================================================
    // ЗАКРЕПЛЁННОЕ СООБЩЕНИЕ
    // Firestore: chats/{roomId}.pinnedMessage = { id, text, senderName, senderRole, type }
    // Храним только нужные поля чтобы не дублировать всё сообщение целиком.
    // ============================================================================================

    /**
     * Слушаем документ комнаты на наличие поля pinnedMessage.
     * Вызывается при каждом selectRoom.
     */
    private fun listenToPinnedMessage(roomId: String) {
        pinnedListener?.remove()
        pinnedListener = db.collection("chats").document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("Chat", "Pinned listener failed", error); return@addSnapshotListener }
                if (snapshot == null) return@addSnapshotListener

                @Suppress("UNCHECKED_CAST")
                val pinned = snapshot.get("pinnedMessage") as? Map<String, Any?>
                val pinnedMsg = if (pinned != null) {
                    try {
                        ChatMessage(
                            id          = pinned["id"]?.toString() ?: "",
                            text        = pinned["text"]?.toString() ?: "",
                            senderName  = pinned["senderName"]?.toString() ?: "",
                            senderRole  = pinned["senderRole"]?.toString() ?: "",
                            type        = try { MessageType.valueOf((pinned["type"]?.toString() ?: "TEXT").uppercase()) } catch (e: Exception) { MessageType.TEXT },
                            imageUrl    = pinned["imageUrl"]?.toString()
                        )
                    } catch (e: Exception) { null }
                } else null

                _uiState.update { it.copy(pinnedMessage = pinnedMsg) }
            }
    }

    /**
     * Закрепить сообщение в комнате. Только для админа.
     * Сохраняем компактный снапшот нужных полей в документе комнаты.
     */
    fun pinMessage(message: ChatMessage) {
        if (!isAdmin) return
        val roomId = _uiState.value.activeRoom.id
        viewModelScope.launch {
            try {
                val pinData = hashMapOf(
                    "id"         to message.id,
                    "text"       to message.text.take(200),
                    "senderName" to message.senderName,
                    "senderRole" to message.senderRole,
                    "type"       to message.type.name,
                    "imageUrl"   to message.imageUrl
                )
                db.collection("chats").document(roomId)
                    .update("pinnedMessage", pinData).await()
            } catch (e: Exception) {
                Log.e("Chat", "pinMessage failed", e)
            }
        }
    }

    /**
     * Снять закреп. Только для админа.
     */
    fun unpinMessage() {
        if (!isAdmin) return
        val roomId = _uiState.value.activeRoom.id
        viewModelScope.launch {
            try {
                db.collection("chats").document(roomId)
                    .update("pinnedMessage", FieldValue.delete()).await()
            } catch (e: Exception) {
                Log.e("Chat", "unpinMessage failed", e)
            }
        }
    }

    // ============================================================================================
    // РЕАКЦИИ
    // ============================================================================================

    fun showEmojiPicker(messageId: String) {
        _uiState.update { it.copy(emojiPickerMessageId = messageId, isSelectionMode = false, selectedMessageIds = emptySet()) }
    }

    fun hideEmojiPicker() {
        _uiState.update { it.copy(emojiPickerMessageId = null) }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val uid = currentUserId ?: return
        val roomId = _uiState.value.activeRoom.id
        hideEmojiPicker()
        viewModelScope.launch {
            try {
                val msgRef = db.collection("chats").document(roomId).collection("messages").document(messageId)
                val doc = msgRef.get().await()
                @Suppress("UNCHECKED_CAST")
                val reactionsRaw = doc.get("reactions") as? Map<String, List<String>> ?: emptyMap()
                val currentUsers = reactionsRaw[emoji] ?: emptyList()
                if (uid in currentUsers) {
                    val updated = currentUsers - uid
                    if (updated.isEmpty()) msgRef.update("reactions.$emoji", FieldValue.delete()).await()
                    else msgRef.update("reactions.$emoji", updated).await()
                } else {
                    msgRef.update("reactions.$emoji", FieldValue.arrayUnion(uid)).await()
                }
            } catch (e: Exception) { Log.e("Chat", "toggleReaction failed", e) }
        }
    }

    // ============================================================================================
    // ОТПРАВКА ТЕКСТА
    // ============================================================================================

    fun sendMessage(text: String) {
        val uid = currentUserId ?: return
        if (text.isBlank()) return
        val replyTo = _uiState.value.replyTo
        val room = _uiState.value.activeRoom
        val senderName = currentUserName
        val mentionedNames = Regex("@([А-Яа-яёA-Za-z]+(?:\\s+[А-Яа-яёA-Za-z]+)?)")
            .findAll(text).map { it.groupValues[1] }.toList()
        stopTyping()
        val expiresAt = if (_uiState.value.isAutoDeleteEnabled)
            java.util.Date(System.currentTimeMillis() + _uiState.value.autoDeleteIntervalHours * 3_600_000L) else null
        val message = hashMapOf(
            "chatRoomId" to room.id, "text" to text.trim(),
            "senderId" to uid, "senderName" to senderName,
            "senderRole" to currentUserRole.key, "type" to MessageType.TEXT.name,
            "timestamp" to FieldValue.serverTimestamp(), "readBy" to listOf(uid),
            "mentionedNames" to mentionedNames,
            "replyToId" to (replyTo?.id ?: ""), "replyToText" to (replyTo?.text?.take(80) ?: ""),
            "replyToSenderName" to (replyTo?.senderName ?: ""), "expiresAt" to expiresAt,
            "reactions" to emptyMap<String, List<String>>()
        )
        viewModelScope.launch {
            try {
                db.collection("chats").document(room.id).collection("messages").add(message).await()
                clearReply()
                sendChatNotifications(uid, senderName, room, text.trim(), mentionedNames)
            } catch (e: Exception) { Log.e("Chat", "Send failed", e) }
        }
    }

    // ============================================================================================
    // ОТПРАВКА ФОТО
    // ============================================================================================

    fun sendImageMessage(context: Context, uri: Uri, caption: String = "") {
        val uid = currentUserId ?: return
        val room = _uiState.value.activeRoom
        val senderName = currentUserName
        val replyTo = _uiState.value.replyTo
        _uiState.update { it.copy(isUploadingImage = true) }
        viewModelScope.launch {
            try {
                val imageUrl = cloudinaryUploader.uploadImage(context, uri, UploadFolder.CHAT)
                if (imageUrl == null) { _uiState.update { it.copy(isUploadingImage = false) }; return@launch }
                val thumbnailUrl = cloudinaryUploader.getThumbnailUrl(imageUrl)
                val expiresAt = if (_uiState.value.isAutoDeleteEnabled)
                    java.util.Date(System.currentTimeMillis() + _uiState.value.autoDeleteIntervalHours * 3_600_000L) else null
                val message = hashMapOf(
                    "chatRoomId" to room.id, "text" to caption.trim(),
                    "imageUrl" to imageUrl, "imageThumbnailUrl" to thumbnailUrl,
                    "senderId" to uid, "senderName" to senderName,
                    "senderRole" to currentUserRole.key, "type" to MessageType.IMAGE.name,
                    "timestamp" to FieldValue.serverTimestamp(), "readBy" to listOf(uid),
                    "mentionedNames" to emptyList<String>(),
                    "replyToId" to (replyTo?.id ?: ""), "replyToText" to (replyTo?.text?.take(80) ?: ""),
                    "replyToSenderName" to (replyTo?.senderName ?: ""), "expiresAt" to expiresAt,
                    "reactions" to emptyMap<String, List<String>>()
                )
                db.collection("chats").document(room.id).collection("messages").add(message).await()
                clearReply()
                sendChatNotifications(uid, senderName, room, "📷 Фото", emptyList())
            } catch (e: Exception) { Log.e("Chat", "sendImageMessage failed", e) }
            finally { _uiState.update { it.copy(isUploadingImage = false) } }
        }
    }

    private suspend fun sendChatNotifications(senderId: String, senderName: String, room: ChatRoom, text: String, mentionedNames: List<String>) {
        try {
            val usersSnap = db.collection("internal_users").whereNotEqualTo("fcmToken", null).get().await()
            usersSnap.documents.forEach { userDoc ->
                val userId = userDoc.id
                if (userId == senderId) return@forEach
                val userRole = userDoc.getString("role") ?: return@forEach
                if (!hasRoomAccess(userRole, room.id)) return@forEach
                val displayName = userDoc.getString("displayName") ?: ""
                val firstName = displayName.split(" ").firstOrNull()?.lowercase() ?: ""
                val isMention = mentionedNames.any { it.lowercase().contains(firstName) }
                FcmSender.sendToUser(
                    recipientUserId = userId,
                    title = if (isMention) "📣 $senderName упомянул вас" else "💬 $senderName в ${room.displayName}",
                    body = text.take(100), type = "chat", roomId = room.id, isMention = isMention
                )
            }
        } catch (e: Exception) { Log.e("Chat", "Failed to send notifications", e) }
    }

    fun sendSystemMessage(roomId: String, text: String, isAlert: Boolean = false) {
        viewModelScope.launch {
            try {
                db.collection("chats").document(roomId).collection("messages").add(hashMapOf(
                    "chatRoomId" to roomId, "text" to text,
                    "senderId" to "system", "senderName" to "Система", "senderRole" to "system",
                    "type" to if (isAlert) MessageType.ALERT.name else MessageType.SYSTEM.name,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "readBy" to emptyList<String>(), "mentionedNames" to emptyList<String>(),
                    "reactions" to emptyMap<String, List<String>>()
                )).await()
            } catch (e: Exception) { Log.e("Chat", "System msg failed", e) }
        }
    }

    // ============================================================================================
    // УДАЛЕНИЕ
    // ============================================================================================

    fun deleteMessage(message: ChatMessage) {
        val uid = currentUserId ?: return
        if (message.senderId != uid && !isAdmin) return
        viewModelScope.launch {
            try {
                db.collection("chats").document(_uiState.value.activeRoom.id)
                    .collection("messages").document(message.id).delete().await()
                // Если удаляем закреплённое — снимаем закреп автоматически
                if (_uiState.value.pinnedMessage?.id == message.id) unpinMessage()
            } catch (e: Exception) { Log.e("Chat", "Delete failed", e) }
        }
    }

    fun enterSelectionMode(messageId: String) {
        _uiState.update { it.copy(isSelectionMode = true, selectedMessageIds = setOf(messageId), replyTo = null, emojiPickerMessageId = null) }
    }

    fun toggleMessageSelection(messageId: String) {
        _uiState.update { state ->
            val selected = state.selectedMessageIds.toMutableSet()
            if (messageId in selected) selected.remove(messageId) else selected.add(messageId)
            if (selected.isEmpty()) state.copy(isSelectionMode = false, selectedMessageIds = emptySet())
            else state.copy(selectedMessageIds = selected)
        }
    }

    fun exitSelectionMode() { _uiState.update { it.copy(isSelectionMode = false, selectedMessageIds = emptySet()) } }

    fun selectAllMessages() {
        val state = _uiState.value
        val uid = currentUserId
        val deletable = state.messages.filter { it.senderId == uid || isAdmin }.map { it.id }.toSet()
        if (state.selectedMessageIds == deletable) _uiState.update { it.copy(selectedMessageIds = emptySet(), isSelectionMode = false) }
        else _uiState.update { it.copy(selectedMessageIds = deletable, isSelectionMode = true) }
    }

    fun deleteSelectedMessages() {
        val state = _uiState.value
        val ids = state.selectedMessageIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                val roomId = state.activeRoom.id
                ids.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { id -> batch.delete(db.collection("chats").document(roomId).collection("messages").document(id)) }
                    batch.commit().await()
                }
                // Если среди удалённых было закреплённое — снимаем
                if (state.pinnedMessage?.id in ids) unpinMessage()
            } catch (e: Exception) { Log.e("Chat", "deleteSelected failed", e) }
            finally { _uiState.update { it.copy(isSelectionMode = false, selectedMessageIds = emptySet()) } }
        }
    }

    fun clearEntireRoom() {
        if (!isAdmin) return
        val roomId = _uiState.value.activeRoom.id
        _uiState.update { it.copy(isClearingChat = true) }
        viewModelScope.launch {
            try {
                while (true) {
                    val snapshot = db.collection("chats").document(roomId).collection("messages").limit(400).get().await()
                    if (snapshot.isEmpty) break
                    val batch = db.batch()
                    snapshot.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
                unpinMessage() // При полной очистке снимаем закреп
            } catch (e: Exception) { Log.e("Chat", "clearEntireRoom failed", e) }
            finally { _uiState.update { it.copy(isClearingChat = false) } }
        }
    }

    // ============================================================================================
    // АВТОУДАЛЕНИЕ
    // ============================================================================================

    fun setAutoDelete(enabled: Boolean, intervalHours: Int = 24) {
        _uiState.update { it.copy(isAutoDeleteEnabled = enabled, autoDeleteIntervalHours = intervalHours) }
        autoDeleteJob?.cancel()
        if (enabled) autoDeleteJob = viewModelScope.launch { while (true) { runAutoDelete(); delay(3_600_000L) } }
    }

    private suspend fun runAutoDelete() {
        val nowDate = java.util.Date(System.currentTimeMillis())
        accessibleRooms.forEach { room ->
            try {
                val snapshot = db.collection("chats").document(room.id)
                    .collection("messages").whereLessThan("expiresAt", nowDate).get().await()
                if (snapshot.documents.isNotEmpty()) {
                    val batch = db.batch()
                    snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit().await()
                }
            } catch (e: Exception) { Log.e("AutoDelete", "Failed for ${room.id}", e) }
        }
    }

    fun runAutoDeleteNow() { viewModelScope.launch { runAutoDelete() } }

    // ============================================================================================
    // ПРОКРУТКА
    // ============================================================================================

    fun onScrolledUp() { _uiState.update { it.copy(showScrollToBottom = true) } }
    fun onScrolledToBottom() { _uiState.update { it.copy(showScrollToBottom = false, unreadBelowCount = 0) } }

    // ============================================================================================
    // TYPING
    // ============================================================================================

    fun onTyping() {
        val uid = currentUserId ?: return
        val roomId = _uiState.value.activeRoom.id
        if (!isCurrentlyTyping) {
            isCurrentlyTyping = true
            db.collection("chats").document(roomId).collection("typing").document(uid)
                .set(mapOf("name" to currentUserName, "timestamp" to FieldValue.serverTimestamp()))
        }
        stopTypingJob?.cancel()
        stopTypingJob = viewModelScope.launch { delay(4000); stopTyping() }
    }

    private fun stopTyping() {
        val uid = currentUserId ?: return
        if (!isCurrentlyTyping) return
        isCurrentlyTyping = false
        stopTypingJob?.cancel()
        db.collection("chats").document(_uiState.value.activeRoom.id).collection("typing").document(uid).delete()
    }

    private fun listenToTyping(roomId: String) {
        val uid = currentUserId ?: return
        typingListener?.remove()
        typingListener = db.collection("chats").document(roomId).collection("typing")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val now = System.currentTimeMillis()
                val typingNames = snapshot.documents.filter { it.id != uid }.mapNotNull { doc ->
                    val ts = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    if (now - ts < 6000) doc.getString("name") else null
                }
                _uiState.update { it.copy(typingUsers = typingNames) }
            }
    }

    // ============================================================================================
    // ПОИСК
    // ============================================================================================

    fun toggleSearch() {
        val isActive = !_uiState.value.isSearchActive
        _uiState.update { it.copy(isSearchActive = isActive, searchQuery = if (!isActive) "" else it.searchQuery, searchResults = if (!isActive) emptyList() else it.searchResults, currentSearchIndex = 0) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query, currentSearchIndex = 0) }
        updateSearchResults(query, _uiState.value.messages)
    }

    private fun updateSearchResults(query: String, messages: List<ChatMessage>) {
        if (query.isBlank()) { _uiState.update { it.copy(searchResults = emptyList()) }; return }
        val results = messages.filter { it.text.contains(query, ignoreCase = true) || it.senderName.contains(query, ignoreCase = true) }
        _uiState.update { it.copy(searchResults = results, currentSearchIndex = 0) }
    }

    fun searchNext() { val r = _uiState.value.searchResults; if (r.isEmpty()) return; _uiState.update { it.copy(currentSearchIndex = (it.currentSearchIndex + 1) % r.size) } }
    fun searchPrev() { val r = _uiState.value.searchResults; if (r.isEmpty()) return; _uiState.update { it.copy(currentSearchIndex = (it.currentSearchIndex - 1 + r.size) % r.size) } }
    fun clearSearch() { _uiState.update { it.copy(isSearchActive = false, searchQuery = "", searchResults = emptyList(), currentSearchIndex = 0) } }

    fun setReplyTo(message: ChatMessage) { _uiState.update { it.copy(replyTo = message) } }
    fun clearReply() { _uiState.update { it.copy(replyTo = null) } }

    // ============================================================================================
    // УЧАСТНИКИ
    // ============================================================================================

    fun toggleRoomMenu() {
        val isOpen = !_uiState.value.isRoomMenuOpen
        _uiState.update { it.copy(isRoomMenuOpen = isOpen) }
        if (isOpen) startMembersListener() else stopMembersListener()
    }

    fun closeRoomMenu() { _uiState.update { it.copy(isRoomMenuOpen = false) }; stopMembersListener() }

    private fun startMembersListener() {
        val roomId = _uiState.value.activeRoom.id
        _uiState.update { it.copy(isMembersLoading = true) }
        membersListener?.remove()
        membersListener = db.collection("internal_users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("Chat", "Members listener failed", error); _uiState.update { it.copy(isMembersLoading = false) }; return@addSnapshotListener }
                if (snapshot == null) return@addSnapshotListener
                val now = System.currentTimeMillis()
                val members = snapshot.documents
                    .filter { hasRoomAccess(it.getString("role") ?: "", roomId) }
                    .map { doc ->
                        val lastSeen: Long = when (val raw = doc.get("lastSeen")) {
                            is Long -> raw; is Number -> raw.toLong()
                            is com.google.firebase.Timestamp -> raw.toDate().time; else -> 0L
                        }
                        RoomMember(userId = doc.id, displayName = doc.getString("displayName") ?: doc.getString("username") ?: "Сотрудник", role = doc.getString("role") ?: "", isOnline = lastSeen > 0 && (now - lastSeen) < 5 * 60 * 1000L, lastSeen = lastSeen, avatarUrl = doc.getString("imgUrl"))
                    }
                    .sortedWith(compareByDescending<RoomMember> { it.isOnline }.thenBy { it.displayName })
                _uiState.update { it.copy(roomMembers = members, isMembersLoading = false) }
            }
    }

    private fun stopMembersListener() { membersListener?.remove(); membersListener = null }

    private fun hasRoomAccess(role: String, roomId: String): Boolean {
        val access = mapOf(
            "admin" to listOf("general", "muvers", "managers", "shifts", "alerts"),
            "inventory_manager" to listOf("general", "managers", "shifts", "alerts"),
            "muver" to listOf("general", "muvers", "shifts"),
            "electrician" to listOf("general", "shifts"),
            "technic" to listOf("general", "shifts")
        )
        return (access[role] ?: listOf("general")).contains(roomId)
    }

    override fun onCleared() {
        super.onCleared()
        stopTyping()
        autoDeleteJob?.cancel()
        messagesListener?.remove()
        pinnedListener?.remove()
        typingListener?.remove()
        membersListener?.remove()
        unreadListeners.values.forEach { it.remove() }
    }
}