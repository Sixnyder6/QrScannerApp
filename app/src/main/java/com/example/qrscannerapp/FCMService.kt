package com.example.qrscannerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    companion object {
        // ── Каналы уведомлений ──────────────────────────────────────────────
        const val CHANNEL_CHAT         = "channel_chat"
        const val CHANNEL_CHAT_MENTION = "channel_chat_mention"
        const val CHANNEL_SHIFTS       = "channel_shifts"
        const val CHANNEL_TASKS        = "channel_tasks"
        const val CHANNEL_SYSTEM       = "channel_system"

        private const val TAG = "FCMService"

        // Создаём все каналы при старте приложения
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // Чат — обычные сообщения
            NotificationChannel(CHANNEL_CHAT, "Сообщения чата", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Новые сообщения в комнатах чата"
                setSound(defaultSound, audioAttr)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150)
                nm.createNotificationChannel(this)
            }

            // Чат — @упоминания (высокий приоритет)
            NotificationChannel(CHANNEL_CHAT_MENTION, "Упоминания", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Когда вас упомянули через @"
                setSound(defaultSound, audioAttr)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 100, 200)
                nm.createNotificationChannel(this)
            }

            // Запросы смены
            NotificationChannel(CHANNEL_SHIFTS, "Смены", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Запросы и подтверждения смен"
                setSound(defaultSound, audioAttr)
                enableVibration(true)
                nm.createNotificationChannel(this)
            }

            // Задачи
            NotificationChannel(CHANNEL_TASKS, "Задачи", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Новые и обновлённые задачи"
                setSound(defaultSound, audioAttr)
                nm.createNotificationChannel(this)
            }

            // Системные (обновления и т.д.)
            NotificationChannel(CHANNEL_SYSTEM, "Система", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Системные уведомления приложения"
                nm.createNotificationChannel(this)
            }
        }
    }

    // ── Новый FCM токен — сохраняем в Firestore ─────────────────────────────
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: $token")
        saveFcmTokenToFirestore(token)
    }

    private fun saveFcmTokenToFirestore(token: String) {
        val telemetryManager = TelemetryManager(applicationContext)
        val authManager = AuthManager(applicationContext, telemetryManagerProvider = { telemetryManager })
        val userId = authManager.authState.value.userId ?: return

        Firebase.firestore
            .collection("internal_users")
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener { Log.d(TAG, "FCM token saved for user $userId") }
            .addOnFailureListener { Log.e(TAG, "Failed to save FCM token", it) }
    }

    // ── Входящее уведомление ────────────────────────────────────────────────
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM received: ${message.data}")

        val data = message.data
        val type = data["type"] ?: "system"
        val title = data["title"] ?: message.notification?.title ?: "QrScannerApp"
        val body = data["body"] ?: message.notification?.body ?: ""
        val roomId = data["roomId"]
        val isMention = data["isMention"] == "true"

        when (type) {
            "chat" -> showChatNotification(
                title = title,
                body = body,
                roomId = roomId,
                isMention = isMention,
                notificationId = System.currentTimeMillis().toInt()
            )
            "shift_request" -> showShiftNotification(
                title = title,
                body = body,
                notificationId = 2000
            )
            "shift_response" -> showShiftNotification(
                title = title,
                body = body,
                notificationId = 2001
            )
            "task" -> showTaskNotification(
                title = title,
                body = body,
                notificationId = System.currentTimeMillis().toInt()
            )
            "update" -> showSystemNotification(
                title = title,
                body = body,
                notificationId = 9000
            )
            else -> showSystemNotification(title, body, System.currentTimeMillis().toInt())
        }
    }

    // ── ПОКАЗ УВЕДОМЛЕНИЙ ───────────────────────────────────────────────────

    private fun showChatNotification(
        title: String,
        body: String,
        roomId: String?,
        isMention: Boolean,
        notificationId: Int
    ) {
        val channel = if (isMention) CHANNEL_CHAT_MENTION else CHANNEL_CHAT

        // Intent — открывает MainActivity и переходит в чат
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "chat")
            roomId?.let { putExtra("chat_room_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (isMention) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("chat_group_${roomId ?: "general"}")
            // Вибрация
            .setVibrate(
                if (isMention) longArrayOf(0, 100, 100, 200)
                else longArrayOf(0, 150)
            )
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    private fun showShiftNotification(title: String, body: String, notificationId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "scanner") // на главный экран
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SHIFTS)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    private fun showTaskNotification(title: String, body: String, notificationId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "my_tasks")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_TASKS)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    private fun showSystemNotification(title: String, body: String, notificationId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SYSTEM)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }
}