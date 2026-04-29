package com.example.qrscannerapp.features.chat.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class MessageType { TEXT, SYSTEM, ALERT, EMOJI_REACTION, IMAGE }

enum class ChatRoom(val id: String, val displayName: String, val emoji: String) {
    GENERAL("general", "Общий", "💬"),
    MUVERS("muvers", "Мувéры", "🛴"),
    MANAGERS("managers", "Склад", "📦"),
    SHIFTS("shifts", "Смена", "⏱"),
    ALERTS("alerts", "Оповещения", "🔔")
}

data class ChatMessage(
    val id: String = "",
    val chatRoomId: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val type: MessageType = MessageType.TEXT,
    @ServerTimestamp val timestamp: Date? = null,
    val readBy: List<String> = emptyList(),
    val mentionedUsers: List<String> = emptyList(),
    val mentionedNames: List<String> = emptyList(),
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val expiresAt: Date? = null,
    val imageUrl: String? = null,
    val imageThumbnailUrl: String? = null,
    // НОВОЕ: реакции — ключ это эмодзи, значение — список userId кто поставил
    // Например: {"👍": ["uid1", "uid2"], "❤️": ["uid3"]}
    val reactions: Map<String, List<String>> = emptyMap()
)

data class ChatRoomInfo(
    val room: ChatRoom,
    val unreadCount: Int = 0,
    val lastMessage: String? = null,
    val lastSenderName: String? = null,
    val lastMessageTime: Date? = null,
    val hasUnreadMention: Boolean = false
)