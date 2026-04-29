package com.example.qrscannerapp.di

import com.example.qrscannerapp.AppDatabase
import com.example.qrscannerapp.data.local.dao.TelemetryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTelemetryDao(database: AppDatabase): TelemetryDao {
        return database.telemetryDao()
    }
}