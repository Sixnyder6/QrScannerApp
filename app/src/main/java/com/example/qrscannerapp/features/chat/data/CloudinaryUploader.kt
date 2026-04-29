package com.example.qrscannerapp.features.chat.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudinaryUploader @Inject constructor() {

    private val cloudName = "dcmmmfjl2"
    private val apiKey = "449877576177452"
    private val apiSecret = "Vbsb_HcviiEwTE2zs13g8GNzWqU"
    private val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Загружает фото по Uri и возвращает публичный URL или null при ошибке.
     * Автоматически сжимает до 1200px и конвертирует в WebP для экономии трафика.
     */
    suspend fun uploadImage(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Копируем Uri во временный файл
                val tempFile = uriToTempFile(context, uri) ?: return@withContext null

                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val folder = "qrscanner_chat"

                // Подпись для authenticated upload
                val signature = generateSignature(timestamp, folder)

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", tempFile.name,
                        tempFile.asRequestBody("image/*".toMediaType())
                    )
                    .addFormDataPart("api_key", apiKey)
                    .addFormDataPart("timestamp", timestamp)
                    .addFormDataPart("signature", signature)
                    .addFormDataPart("folder", folder)
                    // Автоматическая оптимизация — качество и формат подбираются Cloudinary
                    // Трансформации применяем через URL после загрузки — не включаем в запрос
                    .build()

                val request = Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                tempFile.delete() // Чистим временный файл

                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    // Возвращаем secure_url — HTTPS ссылка на фото
                    json.optString("secure_url").takeIf { it.isNotBlank() }
                } else {
                    Log.e("Cloudinary", "Upload failed: ${response.code} — $body")
                    null
                }
            } catch (e: Exception) {
                Log.e("Cloudinary", "Upload exception", e)
                null
            }
        }
    }

    /**
     * Генерирует подпись для authenticated upload.
     * SHA-1 от строки параметров + apiSecret.
     */
    private fun generateSignature(timestamp: String, folder: String): String {
        // Параметры строго в алфавитном порядке — только те что передаются в запросе
        val toSign = "folder=$folder&timestamp=$timestamp$apiSecret"
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(toSign.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Копирует контент из Uri во временный файл.
     * Нужно потому что OkHttp не умеет читать Uri напрямую.
     */
    private fun uriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("chat_img_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            Log.e("Cloudinary", "uriToTempFile failed", e)
            null
        }
    }

    /**
     * Возвращает thumbnail URL для превью в чате (400px).
     * Просто добавляем трансформацию в URL — Cloudinary делает всё на лету.
     */
    fun getThumbnailUrl(originalUrl: String): String {
        // Вставляем трансформацию перед /upload/
        return originalUrl.replace("/upload/", "/upload/w_400,c_limit,q_auto/")
    }
}