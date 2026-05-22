package com.example.qrscannerapp.common.upload

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
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class UploadFolder(val path: String) {
    CHAT("qrscanner_chat"),
    AVATAR("qrscanner_avatars"),
    PARTS("qrscanner_parts"),
    DELIVERY("qrscanner_delivery"),
    VEHICLE("qrscanner_vehicles"),
    REPAIR("qrscanner_repair")
}

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

    suspend fun uploadImage(context: Context, uri: Uri, folder: UploadFolder = UploadFolder.CHAT): String? {
        return withContext(Dispatchers.IO) {
            try {
                val tempFile = uriToTempFile(context, uri) ?: return@withContext null

                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val signature = generateSignature(timestamp, folder.path)

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", tempFile.name,
                        tempFile.asRequestBody("image/*".toMediaType())
                    )
                    .addFormDataPart("api_key", apiKey)
                    .addFormDataPart("timestamp", timestamp)
                    .addFormDataPart("signature", signature)
                    .addFormDataPart("folder", folder.path)
                    .build()

                val request = Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                tempFile.delete()

                if (response.isSuccessful && body != null) {
                    val json = org.json.JSONObject(body)
                    json.optString("secure_url").takeIf { it.isNotBlank() }
                } else {
                    Log.e("CloudinaryUploader", "Upload failed: ${response.code} — $body")
                    null
                }
            } catch (e: Exception) {
                Log.e("CloudinaryUploader", "Upload exception", e)
                null
            }
        }
    }

    fun getThumbnailUrl(originalUrl: String): String {
        return originalUrl.replace("/upload/", "/upload/w_400,c_limit,q_auto/")
    }

    private fun generateSignature(timestamp: String, folder: String): String {
        val toSign = "folder=$folder&timestamp=$timestamp$apiSecret"
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(toSign.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun uriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "uriToTempFile failed", e)
            null
        }
    }
}