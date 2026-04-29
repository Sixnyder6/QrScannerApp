package com.example.qrscannerapp

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * FcmSender — отправка push-уведомлений через Railway прокси-сервер.
 *
 * Схема: Android → Railway сервер → FCM HTTP v1 → телефон получателя
 *
 * RAILWAY_URL и API_SECRET хранятся в Firestore в документе app_config/server
 * Поля:
 *   railwayUrl  — "https://your-app.railway.app"  (URL твоего сервера)
 *   apiSecret   — "твой_секретный_ключ"           (придумай любую строку)
 */
object FcmSender {

    private const val TAG = "FcmSender"

    // Кэшируем конфиг сервера
    private var cachedRailwayUrl: String? = null
    private var cachedApiSecret: String? = null

    // ── Читаем конфиг из Firestore ───────────────────────────────────────────
    private suspend fun getServerConfig(): Pair<String, String>? {
        if (cachedRailwayUrl != null && cachedApiSecret != null) {
            return cachedRailwayUrl!! to cachedApiSecret!!
        }

        return try {
            val doc = Firebase.firestore
                .collection("app_config")
                .document("server")
                .get()
                .await()

            val url = doc.getString("railwayUrl") ?: return null
            val secret = doc.getString("apiSecret") ?: return null

            cachedRailwayUrl = url
            cachedApiSecret = secret
            url to secret
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get server config", e)
            null
        }
    }

    // ── Сбросить кэш (если URL изменился) ───────────────────────────────────
    fun clearCache() {
        cachedRailwayUrl = null
        cachedApiSecret = null
    }

    // ── Отправить уведомление одному пользователю ────────────────────────────
    suspend fun sendToUser(
        recipientUserId: String,
        title: String,
        body: String,
        type: String = "system",
        roomId: String = "",
        isMention: Boolean = false
    ) {
        try {
            val userDoc = Firebase.firestore
                .collection("internal_users")
                .document(recipientUserId)
                .get()
                .await()

            val fcmToken = userDoc.getString("fcmToken") ?: run {
                Log.d(TAG, "No FCM token for user $recipientUserId")
                return
            }

            sendToToken(
                token = fcmToken,
                title = title,
                body = body,
                type = type,
                roomId = roomId,
                isMention = isMention
            )
        } catch (e: Exception) {
            Log.e(TAG, "sendToUser failed", e)
        }
    }

    // ── Отправить нескольким пользователям ───────────────────────────────────
    suspend fun sendToUsers(
        recipientUserIds: List<String>,
        title: String,
        body: String,
        type: String = "system",
        roomId: String = "",
        isMention: Boolean = false
    ) {
        if (recipientUserIds.isEmpty()) return

        try {
            val tokens = mutableListOf<String>()

            recipientUserIds.chunked(10).forEach { chunk ->
                val snap = Firebase.firestore
                    .collection("internal_users")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()

                snap.documents.forEach { doc ->
                    doc.getString("fcmToken")?.let { tokens.add(it) }
                }
            }

            if (tokens.isEmpty()) return
            sendToTokens(tokens, title, body, type, roomId, isMention)

        } catch (e: Exception) {
            Log.e(TAG, "sendToUsers failed", e)
        }
    }

    // ── Отправка на один токен через Railway ─────────────────────────────────
    private suspend fun sendToToken(
        token: String,
        title: String,
        body: String,
        type: String,
        roomId: String,
        isMention: Boolean
    ) {
        val (railwayUrl, apiSecret) = getServerConfig() ?: return

        val payload = JSONObject().apply {
            put("token", token)
            put("title", title)
            put("body", body)
            put("data", JSONObject().apply {
                put("type", type)
                put("title", title)
                put("body", body)
                put("roomId", roomId)
                put("isMention", isMention.toString())
                put("navigate_to", if (type == "chat") "chat" else type)
            })
        }

        doPost("$railwayUrl/send-notification", apiSecret, payload.toString())
    }

    // ── Отправка на несколько токенов через Railway ──────────────────────────
    private suspend fun sendToTokens(
        tokens: List<String>,
        title: String,
        body: String,
        type: String,
        roomId: String,
        isMention: Boolean
    ) {
        val (railwayUrl, apiSecret) = getServerConfig() ?: return

        val payload = JSONObject().apply {
            put("tokens", JSONArray(tokens))
            put("title", title)
            put("body", body)
            put("data", JSONObject().apply {
                put("type", type)
                put("title", title)
                put("body", body)
                put("roomId", roomId)
                put("isMention", isMention.toString())
                put("navigate_to", if (type == "chat") "chat" else type)
            })
        }

        doPost("$railwayUrl/send-multicast", apiSecret, payload.toString())
    }

    // ── HTTP POST ────────────────────────────────────────────────────────────
    private fun doPost(url: String, apiSecret: String, jsonPayload: String) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-api-secret", apiSecret)
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 15_000
            }

            OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload) }

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Notification sent successfully via Railway")
            } else {
                Log.e(TAG, "Railway returned HTTP $code")
            }
            conn.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "HTTP POST failed", e)
        }
    }

    // ── Готовые методы для конкретных событий ────────────────────────────────

    suspend fun notifyShiftRequest(adminUserId: String, requesterName: String) {
        sendToUser(
            recipientUserId = adminUserId,
            title = "📋 Запрос смены",
            body = "$requesterName запрашивает начало смены",
            type = "shift_request"
        )
    }

    suspend fun notifyShiftResponse(userId: String, approved: Boolean, adminName: String) {
        sendToUser(
            recipientUserId = userId,
            title = if (approved) "✅ Смена одобрена" else "❌ Смена отклонена",
            body = if (approved) "$adminName одобрил вашу смену" else "$adminName отклонил запрос",
            type = "shift_response"
        )
    }

    suspend fun notifyTask(userId: String, taskTitle: String, assignerName: String) {
        sendToUser(
            recipientUserId = userId,
            title = "📌 Новая задача",
            body = "$assignerName назначил: $taskTitle",
            type = "task"
        )
    }
}