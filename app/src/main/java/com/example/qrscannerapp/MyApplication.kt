package com.example.qrscannerapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Инициализация NotificationHelper (если класс существует)
        try {
            NotificationHelper.init(this)
        } catch (e: Exception) {
            // NotificationHelper не найден или ошибка инициализации
            // Можно закомментировать или удалить эту строку если класс не нужен
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}