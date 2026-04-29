package com.example.qrscannerapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * NotificationHelper — утилита для работы с уведомлениями.
 *
 * Использование:
 * 1. При старте приложения: NotificationHelper.init(context)
 * 2. Для отправки уведомления другому пользователю: NotificationHelper.sendToUser(...)
 * 3. Проверка разрешения: NotificationHelper.hasPermission(context)
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"

    // ── Инициализация при старте ────────────────────────────────────────────

    fun init(context: Context) {
        // Создаём каналы уведомлений
        FCMService.createNotificationChannels(context)

        // Получаем и сохраняем FCM токен
        refreshToken(context)
    }

    fun refreshToken(context: Context) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM token: $token")
                saveTokenToFirestore(context, token)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to get FCM token", it)
            }
    }

    private fun saveTokenToFirestore(context: Context, token: String) {
        val telemetryManager = TelemetryManager(context)
        val authManager = AuthManager(context, telemetryManagerProvider = { telemetryManager })
        val userId = authManager.authState.value.userId ?: return

        Firebase.firestore
            .collection("internal_users")
            .document(userId)
            .update(
                mapOf(
                    "fcmToken" to token,
                    "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener { Log.d(TAG, "Token saved for $userId") }
            .addOnFailureListener { Log.e(TAG, "Token save failed", it) }
    }

    // ── Проверка разрешения ─────────────────────────────────────────────────

    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // ── Отправка уведомления через Firestore (Cloud Function подхватит) ────
    //
    // Принцип: мы пишем документ в коллекцию "fcm_queue",
    // Cloud Function читает его и отправляет FCM нужному токену.
    // Это безопаснее чем хранить Server Key на клиенте.

    suspend fun sendChatNotification(
        context: Context,
        recipientUserId: String,
        senderName: String,
        roomName: String,
        messageText: String,
        roomId: String,
        isMention: Boolean = false
    ) {
        try {
            val telemetryManager = TelemetryManager(context)
            val authManager = AuthManager(context, telemetryManagerProvider = { telemetryManager })
            val senderId = authManager.authState.value.userId ?: return
            if (senderId == recipientUserId) return // не отправляем себе

            val title = if (isMention) "📣 $senderName упомянул вас" else "💬 $senderName в $roomName"
            val body = messageText.take(100)

            Firebase.firestore.collection("fcm_queue").add(
                mapOf(
                    "recipientUserId" to recipientUserId,
                    "type" to "chat",
                    "title" to title,
                    "body" to body,
                    "roomId" to roomId,
                    "isMention" to isMention,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "processed" to false
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue chat notification", e)
        }
    }

    suspend fun sendShiftRequestNotification(
        context: Context,
        adminUserId: String,
        requesterName: String
    ) {
        try {
            Firebase.firestore.collection("fcm_queue").add(
                mapOf(
                    "recipientUserId" to adminUserId,
                    "type" to "shift_request",
                    "title" to "📋 Запрос смены",
                    "body" to "$requesterName запрашивает начало смены",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "processed" to false
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue shift notification", e)
        }
    }

    suspend fun sendShiftResponseNotification(
        context: Context,
        recipientUserId: String,
        approved: Boolean,
        adminName: String
    ) {
        try {
            Firebase.firestore.collection("fcm_queue").add(
                mapOf(
                    "recipientUserId" to recipientUserId,
                    "type" to "shift_response",
                    "title" to if (approved) "✅ Смена одобрена" else "❌ Смена отклонена",
                    "body" to if (approved) "$adminName одобрил вашу смену" else "$adminName отклонил запрос смены",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "processed" to false
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue shift response notification", e)
        }
    }

    suspend fun sendTaskNotification(
        context: Context,
        recipientUserId: String,
        taskTitle: String,
        assignerName: String
    ) {
        try {
            Firebase.firestore.collection("fcm_queue").add(
                mapOf(
                    "recipientUserId" to recipientUserId,
                    "type" to "task",
                    "title" to "📌 Новая задача",
                    "body" to "$assignerName назначил: $taskTitle",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "processed" to false
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue task notification", e)
        }
    }
}