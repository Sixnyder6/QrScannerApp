// File: MyApplication.kt

package com.example.qrscannerapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
// V-- НАЧАЛО ИЗМЕНЕНИЙ: Реализуем Configuration.Provider --V
class MyApplication : Application(), Configuration.Provider {

    // Hilt внедрит сюда фабрику для создания наших Worker'ов
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Создаем канал уведомлений при старте приложения
        NotificationHelper.createNotificationChannel(this)
    }

    // Этот метод будет вызван автоматически при первой инициализации WorkManager.
    // Мы предоставляем ему нашу кастомную конфигурацию с HiltWorkerFactory.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
// ^-- КОНЕЦ ИЗМЕНЕНИЙ --^