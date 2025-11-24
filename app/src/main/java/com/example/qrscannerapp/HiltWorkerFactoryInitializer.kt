// File: HiltWorkerFactoryInitializer.kt
package com.example.qrscannerapp

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// Эта точка входа позволяет нам безопасно получить HiltWorkerFactory
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltWorkerFactoryEntryPoint {
    fun hiltWorkerFactory(): HiltWorkerFactory
}

class HiltWorkerFactoryInitializer : Initializer<WorkManager> {

    override fun create(context: Context): WorkManager {
        // Получаем точку входа Hilt безопасным способом
        val workerFactory = EntryPointAccessors.fromApplication(
            context,
            HiltWorkerFactoryEntryPoint::class.java
        ).hiltWorkerFactory()

        // Создаем конфигурацию WorkManager с нашей фабрикой
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

        // Инициализируем WorkManager с этой конфигурацией
        WorkManager.initialize(context, config)
        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // Мы не зависим от других инициализаторов
        return emptyList()
    }
}