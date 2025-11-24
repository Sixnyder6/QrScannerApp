// File: UpdateWorker.kt
package com.example.qrscannerapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo // 1. ДОБАВЛЕН ИМПОРТ
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@HiltWorker
class UpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_URL = "apk_url"
        const val KEY_VERSION_NAME = "version_name"
        const val KEY_PROGRESS = "progress"
        const val NOTIFICATION_CHANNEL_ID = "update_channel"
        const val NOTIFICATION_ID = 1001
        private const val UPDATE_FILE_NAME = "QrScannerApp-update.apk"
    }

    override suspend fun doWork(): Result {
        val urlString = inputData.getString(KEY_URL) ?: return Result.failure()
        val versionName = inputData.getString(KEY_VERSION_NAME) ?: "new version"

        createNotificationChannel()
        val initialNotification = createNotification(versionName, "Начало загрузки...", 0, true)

        // 2. ДОБАВЛЕН ТРЕТИЙ ПАРАМЕТР, ЧТОБЫ УКАЗАТЬ ТИП СЕРВИСА
        val foregroundInfo = ForegroundInfo(
            NOTIFICATION_ID,
            initialNotification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        setForeground(foregroundInfo)

        return download(urlString, versionName)
    }

    private suspend fun download(urlString: String, versionName: String): Result {
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), UPDATE_FILE_NAME)
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode !in 200..299) {
                    throw IOException("Server returned HTTP ${connection.responseCode}")
                }

                val totalBytes = connection.contentLength
                FileOutputStream(destinationFile).use { output ->
                    connection.inputStream.use { input ->
                        val data = ByteArray(8 * 1024)
                        var downloadedBytes: Long = 0
                        var bytesRead: Int
                        var lastReportedProgress = -1

                        while (input.read(data).also { bytesRead = it } != -1 && !isStopped) {
                            output.write(data, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (totalBytes > 0) {
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                if (progress != lastReportedProgress) {
                                    setProgress(workDataOf(KEY_PROGRESS to progress))
                                    val notification = createNotification(versionName, "Загрузка...", progress)
                                    notificationManager.notify(NOTIFICATION_ID, notification)
                                    lastReportedProgress = progress
                                }
                            }
                        }
                    }
                }

                if(isStopped) {
                    destinationFile.delete()
                    return@withContext Result.failure()
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", destinationFile)
                showDownloadCompleteNotification(uri, versionName)
                Result.success()

            } catch (e: Exception) {
                Log.e("UpdateWorker", "Download failed", e)
                destinationFile.delete()
                showDownloadErrorNotification(versionName)
                Result.failure()
            }
        }
    }

    private fun createNotification(title: String, text: String, progress: Int = 0, indeterminate: Boolean = false) =
        NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ЗАМЕНИТЕ НА ВАШУ ИКОНКУ
            .setOngoing(true)
            .setProgress(100, progress, indeterminate)
            .build()

    private fun showDownloadCompleteNotification(uri: Uri, versionName: String) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Обновление $versionName загружено")
            .setContentText("Нажмите, чтобы установить")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ЗАМЕНИТЕ НА ВАШУ ИКОНКУ
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
        }
    }

    private fun showDownloadErrorNotification(versionName: String) {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Ошибка загрузки $versionName")
            .setContentText("Не удалось загрузить файл обновления.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ЗАМЕНИТЕ НА ВАШУ ИКОНКУ
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID + 2, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Канал обновлений"
            val descriptionText = "Уведомления о загрузке и установке обновлений приложения"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}