// File: NotificationHelper.kt
package com.example.qrscannerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {

    const val UPDATE_CHANNEL_ID = "update_channel"

    fun createNotificationChannel(context: Context) {
        // Каналы уведомлений нужны только для Android 8.0 (Oreo) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Канал обновлений"
            val descriptionText = "Уведомления о загрузке и установке обновлений приложения"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(UPDATE_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Регистрируем канал в системе
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}